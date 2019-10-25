package main

import (
	"bytes"
	"encoding/json"
	"fmt"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)

// IdentityData example simple Chaincode implementation
type IdentityData struct {
}

type identity struct {
	ObjectType string `json:"docType"`
	Identifier string `json:"identifier"`
	MappingData_hash string `json:"mappingData_hash"`

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
	if function == "invokeMappingDataHash" { //create identity
		return t.invokeMappingDataHash(stub, args)
	} else if function == "queryHashByIdentifier" {
		return t.queryHashByIdentifier(stub, args)
	} 

	fmt.Println("invoke did not find func: " + function) //error
	return shim.Error("Received unknown function invocation")
}

// ============================================================
// invokeMappingDataHash - create a new Identity, store into chaincode state
// ============================================================
func (t *IdentityData) invokeMappingDataHash(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	var err error

	if len(args) != 2 {
		return shim.Error("Incorrect number of arguments. Expecting 2")
	}

	// ==== Input sanitation ====
	fmt.Println("- start invoke Hash")
	if len(args[0]) <= 0 {
		return shim.Error("1st argument must be a non-empty string")
	}
	if len(args[1]) <= 0 {
		return shim.Error("2nd argument must be a non-empty string")
	}
	
	identifier := args[0]
	mappingData_hash := args[1]
	//invoke_time := time.Now().Unix()

	// ==== Create identity object and marshal to JSON ====
	objectType := "identity"
	identity := &identity{objectType, identifier, mappingData_hash}
	identityJSONasBytes, err := json.Marshal(identity)
	if err != nil {
		return shim.Error(err.Error())
	}

	// === Save identity to state ===
	err = stub.PutState(identifier, identityJSONasBytes)
	if err != nil {
		return shim.Error(err.Error())
	}

	// === Index the identity to enable type-based range queries, e.g. return all handle ===
	// indexName := "type~identifier"
	// typeNameIndexKey, err := stub.CreateCompositeKey(indexName, []string{identity.MappingData_hash, identity.Identifier})
	// if err != nil {
	// 	return shim.Error(err.Error())
	// }
	//  Save index entry to state. Only the key name is needed, no need to store a duplicate copy of the identity.
	//  Note - passing a 'nil' value will effectively delete the key from state, therefore we pass null character as value
	// value := []byte{0x00}
	// stub.PutState(typeNameIndexKey, value)

	// ==== Identity saved and indexed. Return success ====
	fmt.Println("- end invoke Hash")
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


func (t *IdentityData) queryHashByIdentifier(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) < 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1")
	}

	identifier := args[0]

	queryString := fmt.Sprintf("{\"selector\":{\"docType\":\"identity\",\"identifier\":\"%s\"}}", identifier)

	queryResults, err := getQueryResultForQueryString(stub, queryString)
	if err != nil {
		return shim.Error(err.Error())
	}
	return shim.Success(queryResults)
}
