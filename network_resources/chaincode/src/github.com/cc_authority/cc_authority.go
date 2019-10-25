package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"strings"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)

// IdentityData example simple Chaincode implementation
type IdentityData struct {
}

type org struct {
	ObjectType string `json:"docType"`
	Item_num string `json:"item_num"`
	Org_name string `json:"org_name"`
	Identity_prefix string `json:"identity_prefix"`
	Public_key string `json:"public_key"`
	Authority string `json:"authority"`
}

// ===================================================================================
// Main
// ===================================================================================
func main() {
	err := shim.Start(new(IdentityData))
	if err != nil {
		fmt.Printf("Error starting Simple chaincode: %s", err)
	}
}

// Init initializes chaincode
// ===========================
func (t *IdentityData) Init(stub shim.ChaincodeStubInterface) pb.Response {
	return shim.Success(nil)
}

// Invoke - Our entry point for Invocations
// ========================================
func (t *IdentityData) Invoke(stub shim.ChaincodeStubInterface) pb.Response {
	function, args := stub.GetFunctionAndParameters()
	fmt.Println("invoke is running " + function)

	// Handle different functions 
	if function == "initOrg" { //create identity
		return t.initOrg(stub, args)
	} else if function == "queryInfoByOrg" {
		return t.queryInfoByOrg(stub, args)
	} 

	fmt.Println("invoke did not find func: " + function) //error
	return shim.Error("Received unknown function invocation")
}

// ============================================================
// initIdentity - create a new Identity, store into chaincode state
// ============================================================
func (t *IdentityData) initOrg(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	var err error

	if len(args) != 5 {
		return shim.Error("Incorrect number of arguments. Expecting 5")
	}

	// ==== Input sanitation ====
	fmt.Println("- start init Org")
	if len(args[0]) <= 0 {
		return shim.Error("1st argument must be a non-empty string")
	}
	if len(args[1]) <= 0 {
		return shim.Error("2nd argument must be a non-empty string")
	}
	if len(args[2]) <= 0 {
		return shim.Error("3rd argument must be a non-empty string")
	}
	if len(args[3]) <= 0 {
		return shim.Error("4th argument must be a non-empty string")
	}
	if len(args[4]) <= 0 {
		return shim.Error("5th argument must be a non-empty string")
	}
	item_num := args[0]
	org_name := strings.ToLower(args[1])
	identity_prefix := args[2]
	public_key:= args[3]
	authority := args[4]
	//invoke_time := time.Now().Unix()

	// ==== Check if identity already exists ====
	// ==== if annotation, mains can be modified ====

	// identityAsBytes, err := stub.GetState(identityName)
	// if err != nil {
	// 	return shim.Error("Failed to get identity: " + err.Error())
	// } else if identityAsBytes != nil {
	// 	fmt.Println("This identity already exists: " + identityName)
	// 	return shim.Error("This identity already exists: " + identityName)
	// }

	// ==== Create identity object and marshal to JSON ====
	objectType := "org"
	org := &org{objectType, item_num, org_name, identity_prefix, public_key, authority}
	orgJSONasBytes, err := json.Marshal(org)
	if err != nil {
		return shim.Error(err.Error())
	}

	// === Save identity to state ===
	err = stub.PutState(item_num, orgJSONasBytes)
	if err != nil {
		return shim.Error(err.Error())
	}

	// === Index the identity to enable type-based range queries, e.g. return all handle ===
	
	// indexName := "type~org_name"
	// typeNameIndexKey, err := stub.CreateCompositeKey(indexName, []string{org.Identity_prefix, org.Org_name})
	// if err != nil {
	// 	return shim.Error(err.Error())
	// }
	
	//  Save index entry to state. Only the key name is needed, no need to store a duplicate copy of the identity.
	//  Note - passing a 'nil' value will effectively delete the key from state, therefore we pass null character as value
	
	// value := []byte{0x00}
	// stub.PutState(typeNameIndexKey, value)

	// ==== Identity saved and indexed. Return success ====
	fmt.Println("- end init Org")
	return shim.Success(nil)
}



// ===========================================================================================
// constructQueryResponseFromIterator constructs a JSON array containing query results from
// a given result iterator
// ===========================================================================================
func constructQueryResponseFromIterator(resultsIterator shim.StateQueryIteratorInterface) (*bytes.Buffer, error) {
	// buffer is a JSON array containing QueryResults
	var buffer bytes.Buffer
	buffer.WriteString("[")

	bArrayMemberAlreadyWritten := false
	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()
		if err != nil {
			return nil, err
		}
		// Add a comma before array members, suppress it for the first array member
		if bArrayMemberAlreadyWritten == true {
			buffer.WriteString(",")
		}
		buffer.WriteString("{\"Key\":")
		buffer.WriteString("\"")
		buffer.WriteString(queryResponse.Key)
		buffer.WriteString("\"")

		buffer.WriteString(", \"Record\":")
		// Record is a JSON object, so we write as-is
		buffer.WriteString(string(queryResponse.Value))
		buffer.WriteString("}")
		bArrayMemberAlreadyWritten = true
	}
	buffer.WriteString("]")

	return &buffer, nil
}

func getQueryResultForQueryString(stub shim.ChaincodeStubInterface, queryString string) ([]byte, error) {

	fmt.Printf("- getQueryResultForQueryString queryString:\n%s\n", queryString)

	resultsIterator, err := stub.GetQueryResult(queryString)
	if err != nil {
		return nil, err
	}
	defer resultsIterator.Close()

	buffer, err := constructQueryResponseFromIterator(resultsIterator)
	if err != nil {
		return nil, err
	}

	fmt.Printf("- getQueryResultForQueryString queryResult:\n%s\n", buffer.String())

	return buffer.Bytes(), nil
}

// =======Rich queries =========================================================================
// Two examples of rich queries are provided below (parameterized query and ad hoc query).
// Rich queries pass a query string to the state database.
// Rich queries are only supported by state database implementations
//  that support rich query (e.g. CouchDB).
// The query string is in the syntax of the underlying state database.
// With rich queries there is no guarantee that the result set hasn't changed between
//  endorsement time and commit time, aka 'phantom reads'.
// Therefore, rich queries should not be used in update transactions, unless the
// application handles the possibility of result set changes between endorsement and commit time.
// Rich queries can be used for point-in-time queries against a peer.
// ============================================================================================

// ===== Example: Parameterized rich query =================================================
// queryIdentityByType queries for identities based on a passed in type.
// This is an example of a parameterized query where the query logic is baked into the chaincode,
// and accepting a single query parameter (type).
// Only available on state databases that support rich query (e.g. CouchDB)
// =========================================================================================
func (t *IdentityData) queryInfoByOrg(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	//   0
	// "handle"
	if len(args) < 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1")
	}

	org_name := strings.ToLower(args[0])

	queryString := fmt.Sprintf("{\"selector\":{\"docType\":\"org\",\"org_name\":\"%s\"}}", org_name)

	queryResults, err := getQueryResultForQueryString(stub, queryString)
	if err != nil {
		return shim.Error(err.Error())
	}
	return shim.Success(queryResults)
}
