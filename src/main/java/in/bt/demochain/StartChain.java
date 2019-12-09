package in.bt.demochain;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Blockchain Trainer (www.blockchaintrainer.in)
 * 
 * This is start blockchain spring boot class it adds four blocks after setting
 * the mining difficulty level
 */

@SpringBootApplication
public class StartChain implements CommandLineRunner {

	public static void main(String[] args) {
		new SpringApplicationBuilder(StartChain.class).headless(false).run(args);
	}

	final HFClient client = HFClient.createNewInstance();
	Channel channel;
	QueryByChaincodeRequest qpr;

	void setupCryptoMaterialsForClient() throws CryptoException, InvalidArgumentException, IllegalAccessException,
			InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
		// Set default crypto suite for HF client

		client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

		client.setUserContext(new User() {

			public String getName() {
				return "PeerAdmin";
			}

			public Set<String> getRoles() {
				return null;
			}

			public String getAccount() {
				return null;
			}

			public String getAffiliation() {
				return null;
			}

			public Enrollment getEnrollment() {
				return new Enrollment() {
					public PrivateKey getKey() {
						PrivateKey privateKey = null;
						try {
							File privateKeyFile = new File(
									"/media/viraj/DATA1/multi-channel-network/3org2ch_143/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/keystore/9bb95163b9254a5bf2a862201b9969ec6d1417660abb31f4961e0b20e114e78b_sk");
							privateKey = getPrivateKeyFromBytes(
									IOUtils.toByteArray(new FileInputStream(privateKeyFile)));
						} catch (InvalidKeySpecException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						} catch (NoSuchProviderException e) {
							e.printStackTrace();
						} catch (NoSuchAlgorithmException e) {
							e.printStackTrace();
						}
						return privateKey;
					}

					public String getCert() {

						String certificate = null;
						try {
							File certificateFile = new File(
									"/media/viraj/DATA1/multi-channel-network/3org2ch_143/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/signcerts/Admin@org1.example.com-cert.pem");
							certificate = new String(IOUtils.toByteArray(new FileInputStream(certificateFile)),
									"UTF-8");
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
						return certificate;
					}
				};
			}

			public String getMspId() {
				return "Org1MSP";
			}
		});
	}

	

	static PrivateKey getPrivateKeyFromBytes(byte[] data)
			throws IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
		final Reader pemReader = new StringReader(new String(data));

		final PrivateKeyInfo pemPair;
		try (PEMParser pemParser = new PEMParser(pemReader)) {
			pemPair = (PrivateKeyInfo) pemParser.readObject();
		}

		PrivateKey privateKey = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
				.getPrivateKey(pemPair);

		return privateKey;
	}

	void createChannel() throws InvalidArgumentException, TransactionException, ProposalException {
		channel = client.newChannel("channelall");
		Properties ordererProperties = new Properties();
		ordererProperties.setProperty("pemFile",
				"/media/viraj/DATA1/multi-channel-network/3org2ch_143/crypto-config/ordererOrganizations/example.com/orderers/orderer.example.com/tls/server.crt");
		ordererProperties.setProperty("trustServerCertificate", "true"); // testing
																			// environment
																			// only
																			// NOT
																			// FOR
																			// PRODUCTION!
		ordererProperties.setProperty("hostnameOverride", "orderer.example.com");
		ordererProperties.setProperty("sslProvider", "openSSL");
		ordererProperties.setProperty("negotiationType", "TLS");
		ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] { 5L, TimeUnit.MINUTES });
		ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] { 8L, TimeUnit.SECONDS });
		channel.addOrderer(client.newOrderer("orderer.example.com", "grpcs://localhost:7050", ordererProperties)); // use
																													// the
																													// network
																													// orderer
																													// container
																													// URL

		Properties peerProperties = new Properties();
		peerProperties.setProperty("pemFile",
				"/media/viraj/DATA1/multi-channel-network/3org2ch_143/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/server.crt");
		peerProperties.setProperty("trustServerCertificate", "true"); // testing // // PRODUCTION!
		peerProperties.setProperty("hostnameOverride", "peer0.org1.example.com");
		peerProperties.setProperty("sslProvider", "openSSL");
		peerProperties.setProperty("negotiationType", "TLS");
		peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);

		channel.addPeer(client.newPeer("peer0.org1.example.com", "grpcs://localhost:7051", peerProperties)); // use the
																												// network
																												// peer
																												// container
																												// URL

		channel.initialize();
	}

	void queryChain()
			throws InvalidArgumentException, ProposalException, ChaincodeEndorsementPolicyParseException, IOException {

		// get channel instance from client

		Channel channel2 = client.getChannel("channelall");

		int blocksize = (int) channel2.queryBlockchainInfo().getHeight();
		System.out.println("NO of Blocks: " + blocksize);

		// create chaincode request
		qpr = client.newQueryProposalRequest();
		// build cc id providing the chaincode name. Version is omitted here.
		ChaincodeID fabcarCCId = ChaincodeID.newBuilder().setName("balance").build();
		qpr.setChaincodeID(fabcarCCId);
		// CC function to be called.
		qpr.setFcn("query");
		qpr.setArgs(new String[] { "a" });

		java.util.Collection<org.hyperledger.fabric.sdk.ProposalResponse> res = channel2.queryByChaincode(qpr, channel2.getPeers());

		// display response
		for (org.hyperledger.fabric.sdk.ProposalResponse pres : res) {

			String stringResponse = new String(pres.getChaincodeActionResponsePayload());
			System.out.println("Query Response from Peer " + pres.getPeer().getName() + ":" + stringResponse);

		}
	}

	void invokeChain()
			throws InvalidArgumentException, ProposalException, ChaincodeEndorsementPolicyParseException, IOException {

		Channel channel = client.getChannel("channelall");
		TransactionProposalRequest req = client.newTransactionProposalRequest();
		ChaincodeID cid = ChaincodeID.newBuilder().setName("balance").build();
		req.setChaincodeID(cid);
		req.setFcn("move");
		req.setArgs(new String[] { "a","b","20"});
		java.util.Collection<org.hyperledger.fabric.sdk.ProposalResponse> resps = channel.sendTransactionProposal(req);

		channel.sendTransaction(resps);
		System.out.println(resps.iterator().next().getMessage());

	}

	@Override
	public void run(String... args) throws CryptoException, InvalidArgumentException, IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, TransactionException, ProposalException, ChaincodeEndorsementPolicyParseException, IOException {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		StartChain t = new StartChain();
	        t.setupCryptoMaterialsForClient();
	        t.createChannel();
	      //  t.invokeChain();
	        t.queryChain(); //For querying

	}
}
