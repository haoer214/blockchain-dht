package org.app.chaincode.hash;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.app.client.CAClient;
import org.app.client.ChannelClient;
import org.app.client.FabricClient;
import org.app.config.Config;
import org.app.user.UserContext;
import org.app.util.Util;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 该类负责从区块链读取映射数据的hash，由dht控制组件调用
 */
public class QueryHash {
    private static FabricClient fabClient_query;
    private static ChannelClient channelClient_query;

    // 初始化配置信息
    public QueryHash(JSONObject configJson){
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

            fabClient_query = new FabricClient(adminUserContext);

            channelClient_query = fabClient_query.createChannelClient(Config.CHANNEL_NAME);
            Channel channel = channelClient_query.getChannel();
            Peer peer = fabClient_query.getInstance().newPeer(configJson.getString("Eroll_Name"), configJson.getString("Eroll_Address"));
            Orderer orderer = fabClient_query.getInstance().newOrderer(configJson.getString("Orderer_Name"), configJson.getString("Orderer_Address"));
            channel.addPeer(peer);
            channel.addOrderer(orderer);
            channel.initialize();
            Logger.getLogger(QueryHash.class.getName()).log(Level.INFO, "准备读取映射数据hash...");

        } catch (Exception e) {
            System.out.println("配置信息初始化失败！");
            e.printStackTrace();
        }
    }

    // 读取映射数据hash
    public String query(String identifier){
        String stringResponse = null;
        try {
            Logger.getLogger(QueryHash.class.getName()).log(Level.INFO, "正在读取标识 " + identifier + " 的映射数据hash...");

            Collection<ProposalResponse> responses1Query = channelClient_query.queryByChainCode(Config.CHAINCODE_2_NAME, "queryHashByIdentifier", new String[]{identifier});
            for (ProposalResponse pres : responses1Query) {
                stringResponse = new String(pres.getChaincodeActionResponsePayload());
                Logger.getLogger(QueryHash.class.getName()).log(Level.INFO, stringResponse);
            }
        } catch (Exception e) {
            System.out.println("读取数据失败！");
            e.printStackTrace();
        }
        return stringResponse;
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

        QueryHash queryAuthority = new QueryHash(configJson);
        System.out.println(queryAuthority.query("bupt/123"));
        System.out.println(queryAuthority.query("bupt.fnl/987"));
        System.out.println(queryAuthority.query("beishi/000"));
    }
}
