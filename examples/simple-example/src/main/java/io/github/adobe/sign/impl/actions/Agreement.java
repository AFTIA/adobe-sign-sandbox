package io.github.adobe.sign.impl.actions;

import java.net.SocketPermission;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import io.github.adobe.sign.core.actions.SignAction;
import io.github.adobe.sign.core.actions.SignActionException;
import io.github.adobe.sign.core.auth.Authenticated;
import io.github.adobe.sign.core.auth.CredentialLoader;
import io.github.adobe.sign.core.auth.SignAuth;
import io.github.adobe.sign.core.logger.SignLogger;
import io.github.adobe.sign.core.metadata.SignMetadata;

public class Agreement implements SignAction{

    private CredentialLoader credentialLoader;
    private String AGREEMENTS = "https://api.na1.echosign.com/api/rest/v6/agreements";

    public Agreement(CredentialLoader credentialLoader) {
        this.credentialLoader = credentialLoader;
    }

    @Override
    public SignMetadata beforeAction(SignAuth signAuth, SignMetadata metadata, SignLogger logger) throws SignActionException {
        List<Header> headers = null;
        
        //authenticate 
        try {
            Authenticated authenticated = signAuth.refresh(this.credentialLoader, metadata);
            Header bearerToken = new BasicHeader("Authorization",
                String.format("Bearer %s", authenticated.getAccessToken()));
            Header apiUser = new BasicHeader("x-api-user", "email:arran.mccullough@aftia.com");
            headers = List.of(bearerToken, apiUser);
        } catch (Exception e) {
            throw new SignActionException(e.getMessage(), e);
        }
        
        try (CloseableHttpClient client = HttpClients.custom().setDefaultHeaders(headers).build()) {

            HttpPost httpPost = new HttpPost(AGREEMENTS);

            String transId = (String)metadata.getValue("TRANSIENT_ID");
            String agreementInfo = createAgreementJson(transId);
            System.out.println(agreementInfo);

            StringEntity requestEntity = new StringEntity(agreementInfo, ContentType.APPLICATION_JSON);
            httpPost.setEntity(requestEntity);

            // Create a custom response handler
            ResponseHandler<String> responseHandler = response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity responseEntity = response.getEntity();
                    if (responseEntity != null) {
                        JsonObject payload = JsonParser.parseString(EntityUtils.toString(responseEntity)).getAsJsonObject();
                        return payload.get("id").getAsString();
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            };

            String agreementId = client.execute(httpPost, responseHandler);
            metadata.put("AGREEMENT_ID", agreementId);
        } catch (Exception e) {
            throw new SignActionException(e.getMessage(), e);
        }

        return metadata;
    }

    @Override
    public SignMetadata doAction(SignAuth signAuth, SignMetadata metadata, SignLogger logger)
            throws SignActionException {
        return null;
    }

    @Override
    public SignMetadata postAction(SignAuth signAuth, SignMetadata metadata, SignLogger logger)
            throws SignActionException {
        return metadata;
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    private String createAgreementJson(String transientId){
        JsonObject root = new JsonObject();
        JsonObject transientDocId = new JsonObject();
        transientDocId.addProperty("transientDocumentId", transientId);
        JsonArray fileInfos = new JsonArray();
        fileInfos.add(transientDocId);
        root.add("fileInfos", fileInfos);
        root.addProperty("name", "Test Agreement");
        JsonArray participantSets = new JsonArray();
        JsonArray memberInfoArray = new JsonArray();
        JsonObject memberInfos = new JsonObject();
        JsonObject email = new JsonObject();
        email.addProperty("email", "arran.mccullough+test1@aftia.com");
        memberInfoArray.add(email);
        memberInfos.add("memberInfos", memberInfoArray);
        memberInfos.addProperty("order", 1);
        memberInfos.addProperty("role", "SIGNER");
        participantSets.add(memberInfos);
        root.add("participantSetsInfo", participantSets);
        root.addProperty("signatureType", "ESIGN");
        root.addProperty("state", "IN_PROCESS");
        
        return root.toString();
    }
}
