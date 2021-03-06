package org.app.chaincode.authority;

import org.app.client.CAClient;
import org.app.client.ChannelClient;
import org.app.client.FabricClient;
import org.app.config.Config;
import org.app.user.UserContext;
import org.app.util.Util;
import org.hyperledger.fabric.sdk.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONException;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 该类负责向区块链读取企业公钥、标识前缀以及对应的操作权限，由dht控制组件调用
 */
public class QueryAuthority {

    private static ChannelClient channelClient_query;

    // 初始化配置信息
    static {
        try {
            Util.cleanUp();
            String caUrl = Config.CA_ORG1_URL;
            CAClient caClient = new CAClient(caUrl, null);
            // Enroll Admin to Org1MSP
            UserContext adminUserContext = new UserContext();
            adminUserContext.setName(Config.ADMIN);
            adminUserContext.setAffiliation(Config.ORG1);
            adminUserContext.setMspId(Config.ORG1_MSP);
            caClient.setAdminUserContext(adminUserContext);
            adminUserContext = caClient.enrollAdminUser(Config.ADMIN, Config.ADMIN_PASSWORD);

            FabricClient fabClient_query = new FabricClient(adminUserContext);

            channelClient_query = fabClient_query.createChannelClient(Config.CHANNEL_NAME);
            Channel channel = channelClient_query.getChannel();

            Orderer orderer = fabClient_query.getInstance().newOrderer(Config.ORDERER_NAME, Config.ORDERER_URL);
            Peer peer = fabClient_query.getInstance().newPeer("peer0.org1.example.com", "grpc://localhost:7051");
            channel.addOrderer(orderer);
            channel.addPeer(peer);
            channel.initialize();
            Logger.getLogger(QueryAuthority.class.getName()).log(Level.INFO, " 准备进行权限验证...");

        } catch (Exception e) {
            System.out.println("配置信息初始化失败！");
            e.printStackTrace();
        }
    }

    // 读取企业信息
    public static JSONArray query(String org_name){
        JSONArray jsonArrayResponse = null;
        try {
            Collection<ProposalResponse>  responses1Query = channelClient_query.queryByChainCode(Config.CHAINCODE_1_NAME, "queryInfoByOrg", new String[]{org_name});
            for (ProposalResponse pres : responses1Query) {
                String stringResponse = new String(pres.getChaincodeActionResponsePayload());
                jsonArrayResponse = JSONArray.fromObject(stringResponse);
//                Logger.getLogger(QueryAuthority.class.getName()).log(Level.INFO, stringResponse);
            }
        } catch (Exception e) {
            System.out.println("读取数据失败！");
            e.printStackTrace();
        }
        return jsonArrayResponse;
    }
//    public static void main(String[] args) throws JSONException {

//        JSONObject configJson = new JSONObject();
//        configJson.put("caUrl","http://localhost:7054");
//        configJson.put("Admin","admin");
//        configJson.put("Adminpw","adminpw");
//        configJson.put("Eroll_Name","peer0.org1.example.com");
//        configJson.put("Eroll_Address","grpc://localhost:7051");
//        configJson.put("Orderer_Name","orderer.example.com");
//        configJson.put("Orderer_Address","grpc://localhost:7050");

//        query("bupt");
//        query("bnu");
//        query("other");
//    }
}
