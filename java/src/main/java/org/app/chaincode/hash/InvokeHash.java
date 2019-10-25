package org.app.chaincode.hash;

import org.app.client.CAClient;
import org.app.client.ChannelClient;
import org.app.client.FabricClient;
import org.app.config.Config;
import org.app.user.UserContext;
import org.app.util.Util;
import org.hyperledger.fabric.sdk.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 该类负责向区块链写入映射数据的hash，由dht控制组件调用
 */
public class InvokeHash {
    private static final byte[] EXPECTED_EVENT_DATA = "!".getBytes(UTF_8);
    private static final String EXPECTED_EVENT_NAME = "event";

    private static FabricClient fabClient_invoke;
    private static ChannelClient channelClient_invoke;

    // 初始化配置信息
    public InvokeHash(JSONObject configJson){
        try {
            Util.cleanUp();
            String caUrl = configJson.getString("caUrl");
            CAClient caClient = new CAClient(caUrl, null);
            // Enroll Admin to Org1MSP
            UserContext adminUserContext = new UserContext();
            adminUserContext.setName(configJson.getString("Admin"));
            adminUserContext.setAffiliation(Config.ORG1);
            adminUserContext.setMspId(Config.ORG1_MSP);
            caClient.setAdminUserContext(adminUserContext);
            adminUserContext = caClient.enrollAdminUser(configJson.getString("Admin"), configJson.getString("Adminpw"));

            fabClient_invoke = new FabricClient(adminUserContext);

            channelClient_invoke = fabClient_invoke.createChannelClient(Config.CHANNEL_NAME);
            Channel channel = channelClient_invoke.getChannel();
            Peer peer = fabClient_invoke.getInstance().newPeer(configJson.getString("Eroll_Name"), configJson.getString("Eroll_Address"));
            Orderer orderer = fabClient_invoke.getInstance().newOrderer(configJson.getString("Orderer_Name"), configJson.getString("Orderer_Address"));
            channel.addPeer(peer);
            channel.addOrderer(orderer);
            channel.initialize();
            Logger.getLogger(InvokeHash.class.getName()).log(Level.INFO, "准备写入映射数据hash...");

        } catch (Exception e) {
            System.out.println("配置信息初始化失败！");
            e.printStackTrace();
        }
    }

    // 写入映射数据hash
    public void invoke(JSONObject dataJson){
        try {
            TransactionProposalRequest request = fabClient_invoke.getInstance().newTransactionProposalRequest();
            ChaincodeID ccid = ChaincodeID.newBuilder().setName(Config.CHAINCODE_2_NAME).build();
            request.setChaincodeID(ccid);
            request.setFcn("invokeMappingDataHash");
            String[] arguments = new String[2];
            arguments[0] = dataJson.getString("identifier");
            arguments[1] = dataJson.getString("mappingData_hash");
            request.setArgs(arguments);
            request.setProposalWaitTime(1000);

            Map<String, byte[]> tm2 = new HashMap<>();
            tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
            tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
            tm2.put("result", ":)".getBytes(UTF_8));
            tm2.put(EXPECTED_EVENT_NAME, EXPECTED_EVENT_DATA);
            request.setTransientMap(tm2);
            Collection<ProposalResponse> responses = channelClient_invoke.sendTransactionProposal(request);
            for (ProposalResponse res: responses) {
                ChaincodeResponse.Status status = res.getStatus();
                Logger.getLogger(InvokeHash.class.getName()).log(Level.INFO, "【写入标识 " + arguments[0] + " 的映射数据hash】 - " + status);
            }
        } catch (Exception e) {
            System.out.println("写入数据失败！");
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws JSONException {

        JSONObject configJson = new JSONObject();
        configJson.put("caUrl","http://localhost:7054");
        configJson.put("Admin","admin");
        configJson.put("Adminpw","adminpw");
        configJson.put("Eroll_Name","peer0.org1.example.com");
        configJson.put("Eroll_Address","grpc://localhost:7051");
        configJson.put("Orderer_Name","orderer.example.com");
        configJson.put("Orderer_Address","grpc://localhost:7050");

        JSONObject dataJson0 = new JSONObject();
        dataJson0.put("identifier", "bupt/123");
        dataJson0.put("mappingData_hash", "s7ehdnj3");

        JSONObject dataJson1 = new JSONObject();
        dataJson1.put("identifier", "bupt.fnl/987");
        dataJson1.put("mappingData_hash", "d92jh4nd");

        InvokeHash invokeHash = new InvokeHash(configJson);
        invokeHash.invoke(dataJson0);
        invokeHash.invoke(dataJson1);
    }
}
