package org.app.chaincode.authority;

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
 * 该类负责向区块链写入企业公钥、标识前缀以及对应的操作权限，由标志分配机构调用
 */
public class InvokeAuthority {
    private static final byte[] EXPECTED_EVENT_DATA = "!".getBytes(UTF_8);
    private static final String EXPECTED_EVENT_NAME = "event";

    private static FabricClient fabClient_invoke;
    private static ChannelClient channelClient_invoke;

    // 初始化配置信息
    public InvokeAuthority(JSONObject configJson){
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
            Logger.getLogger(InvokeAuthority.class.getName()).log(Level.INFO, "准备写入企业信息...");

        } catch (Exception e) {
            System.out.println("配置信息初始化失败！");
            e.printStackTrace();
        }
    }

    // 写入企业信息
    public void invoke(JSONObject dataJson){
        try {
            TransactionProposalRequest request = fabClient_invoke.getInstance().newTransactionProposalRequest();
            ChaincodeID ccid = ChaincodeID.newBuilder().setName(Config.CHAINCODE_1_NAME).build();
            request.setChaincodeID(ccid);
            request.setFcn("initOrg");
            String[] arguments = new String[5];
            arguments[0] = dataJson.getString("item_num");
            arguments[1] = dataJson.getString("org_name");
            arguments[2] = dataJson.getString("identity_prefix");
            arguments[3] = dataJson.getString("public_key");
            arguments[4] = dataJson.getString("authority");
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
                Logger.getLogger(InvokeAuthority.class.getName()).log(Level.INFO, "【写入企业 " + arguments[1] + " 】 - " + status);
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
        dataJson0.put("item_num", "0");
        dataJson0.put("org_name", "bupt");
        dataJson0.put("identity_prefix", "bupt");
        dataJson0.put("public_key", "0");
        dataJson0.put("authority", "1001");

        JSONObject dataJson1 = new JSONObject();
        dataJson1.put("item_num", "1");
        dataJson1.put("org_name", "bupt");
        dataJson1.put("identity_prefix", "bupt.fnl");
        dataJson1.put("public_key", "0");
        dataJson1.put("authority", "1001");

        JSONObject dataJson2 = new JSONObject();
        dataJson2.put("item_num", "2");
        dataJson2.put("org_name", "bupt");
        dataJson2.put("identity_prefix", "beishi");
        dataJson2.put("public_key", "0");
        dataJson2.put("authority", "0001");

        JSONObject dataJson3 = new JSONObject();
        dataJson3.put("item_num", "3");
        dataJson3.put("org_name", "beishi");
        dataJson3.put("identity_prefix", "beishi");
        dataJson3.put("public_key", "0");
        dataJson3.put("authority", "1001");

        JSONObject dataJson4 = new JSONObject();
        dataJson4.put("item_num", "4");
        dataJson4.put("org_name", "beishi");
        dataJson4.put("identity_prefix", "bupt");
        dataJson4.put("public_key", "0");
        dataJson4.put("authority", "0001");

        InvokeAuthority invokeAuthority = new InvokeAuthority(configJson);
        invokeAuthority.invoke(dataJson0);
        invokeAuthority.invoke(dataJson1);
        invokeAuthority.invoke(dataJson2);
        invokeAuthority.invoke(dataJson3);
        invokeAuthority.invoke(dataJson4);
    }
}
