package org.app.config;

import java.io.File;

public class Config {
	
	public static final String ORG1_MSP = "Org1MSP";

	public static final String ORG1 = "org1";

	public static final String ADMIN = "admin";

	public static final String ADMIN_PASSWORD = "adminpw";
	
	public static final String CHANNEL_CONFIG_PATH = "config/channel.tx";
	
	public static final String ORG1_USR_BASE_PATH = "crypto-config" + File.separator + "peerOrganizations" + File.separator
			+ "org1.example.com" + File.separator + "users" + File.separator + "Admin@org1.example.com"
			+ File.separator + "msp";
	
	public static final String ORG1_USR_ADMIN_PK = ORG1_USR_BASE_PATH + File.separator + "keystore";

	public static final String ORG1_USR_ADMIN_CERT = ORG1_USR_BASE_PATH + File.separator + "admincerts";
	
	public static final String CA_ORG1_URL = "http://localhost:7054";

	public static final String ORDERER_URL = "grpc://localhost:7050";
	
	public static final String ORDERER_NAME = "orderer.example.com";

	public static final String CHANNEL_NAME = "mychannel";

	public static final String CHAINCODE_ROOT_DIR = "chaincode";
	
	public static final String CHAINCODE_1_NAME = "cc_authority";
	
	public static final String CHAINCODE_1_PATH = "github.com/cc_authority";
	
	public static final String CHAINCODE_1_VERSION = "1";

	public static final String CHAINCODE_2_NAME = "cc_hash";

	public static final String CHAINCODE_2_PATH = "github.com/cc_hash";

	public static final String CHAINCODE_2_VERSION = "1";

	public static final String BUPT_PUB_KRY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDjL/WBdYV/PgXsdPwkf8Ch818H8bO7+01/O+bWT2SszP32RVaJ59is/dfANcsUg/RpN3LpuJtQPXGDJA0fVKgLGnsSNFeexiIxaZH4KHjqp5/YS2pkp7WI4QBaYGuGQn0rtLvWuiRbN7DRKNOGYt7ITY7pcLopurXtmAzNKMApmwIDAQAB";

	public static final String BNU_PUB_KRY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCk92g4MsLxVN9+i2cHH7VRymMrafjhJ5QZhjtE3lm+TMVm7rRye/QjEBhh6eGKLCA60itucJ836t8WcxZ+MR/sXZDzCCRO7ShJpJOR/ZHXSeeNvOH6cAeSYbNyQ1FN+5sChUqMwbOVdme2Afv//Bj2kvTwNqDCG029MQf7RwbYhQIDAQAB";

}
