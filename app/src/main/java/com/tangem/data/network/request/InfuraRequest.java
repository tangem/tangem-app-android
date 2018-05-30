package com.tangem.data.network.request;

/**
 * Created by Ilia on 19.12.2017.
 */

import com.tangem.domain.wallet.Blockchain;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class InfuraRequest {
    public static final String METHOD_ETH_GetBalance = "eth_getBalance";
    public static final String METHOD_ETH_GetOutTransactionCount = "eth_getTransactionCount";
    public static final String METHOD_ETH_GetGasPrice = "eth_gasPrice";
    public static final String METHOD_ETH_SendRawTransaction =  "eth_sendRawTransaction";
    public static final String METHOD_ETH_Call =  "eth_call";

    public JSONObject jsRequestData;
    public String answerData;
    public String error;
    public String WalletAddress;
    public int Dec;
    public String amount;
    public Blockchain blockchain;

    public void setBlockchain(Blockchain value) {
        blockchain = value;
    }

    public Blockchain getBlockchain()
    {
        return blockchain;
    }

    private InfuraRequest() {
    }

    public InfuraRequest(JSONObject jsRequest) {
        try {
            jsRequestData = new JSONObject(jsRequest.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject getAnswer() {
        try {
            return new JSONObject(answerData);
        } catch (Exception e) {
            try {
                return new JSONObject(String.format("[\"Error\":\"%s\"]", e.getMessage()));
            } catch (JSONException e1) {
                e1.printStackTrace();
                return null;
            }
        }
    }

    public String getAsString() {
        return jsRequestData.toString();
    }

    public void setID(int value) {
        try {
            jsRequestData.put("id", value/*String.format("%d", value)*/);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public int getID() {
        try {
            return jsRequestData.getInt("id");
        } catch (JSONException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public boolean isMethod(String methodName) throws JSONException {
        return jsRequestData.getString("method").equals(methodName);
    }

    public static InfuraRequest GetBalance(String wallet) {
        InfuraRequest request = new InfuraRequest();
        try {
            request.WalletAddress=wallet;
            request.jsRequestData = new JSONObject("{ \"method\":\"" + METHOD_ETH_GetBalance + "\", \"params\":[\"" + wallet + "\", \"latest\"] }");
        } catch (JSONException e) {
            e.printStackTrace();
            request.error = e.toString();
        }
        return request;
    }

    public static InfuraRequest GetTokenBalance(String wallet, String contract, int dec)
    {
        InfuraRequest request = new InfuraRequest();
        try {
            request.WalletAddress=wallet;
            request.Dec = dec;
            String address = wallet.substring(2);
            String dataValue = String.format("{\"data\": \"0x70a08231000000000000000000000000%s\", \"to\": \"%s\"}", address, contract);
            request.jsRequestData = new JSONObject("{ \"method\":\"" + METHOD_ETH_Call + "\", \"params\":[" +dataValue+", \"latest\"] }");
        } catch (JSONException e) {
            e.printStackTrace();
            request.error = e.toString();
        }
        return request;
    }

    public static InfuraRequest SendTransaction(String wallet, String tx) {
        InfuraRequest request = new InfuraRequest();
        try {
            request.WalletAddress=wallet;
            request.jsRequestData = new JSONObject("{ \"method\":\"" + METHOD_ETH_SendRawTransaction + "\", \"params\":[\"" + tx + "\"] }");
        } catch (JSONException e) {
            e.printStackTrace();
            request.error = e.toString();
        }
        return request;
    }



    public static InfuraRequest GetOutTransactionCount(String wallet) {
        InfuraRequest request = new InfuraRequest();
        try {
            request.WalletAddress=wallet;
            request.jsRequestData = new JSONObject("{ \"method\":\"" + METHOD_ETH_GetOutTransactionCount + "\", \"params\":[\"" + wallet + "\", \"latest\"] }");
        } catch (JSONException e) {
            e.printStackTrace();
            request.error = e.toString();
        }
        return request;
    }

    public static InfuraRequest GetPendingTransactionCount(String wallet) {
        InfuraRequest request = new InfuraRequest();
        try {
            request.WalletAddress=wallet;
            request.jsRequestData = new JSONObject("{ \"method\":\"" + METHOD_ETH_GetOutTransactionCount + "\", \"params\":[\"" + wallet + "\", \"pending\"] }");
        } catch (JSONException e) {
            e.printStackTrace();
            request.error = e.toString();
        }
        return request;
    }

    public static InfuraRequest GetGasPrise(String wallet) {
        InfuraRequest request = new InfuraRequest();
        try {
            request.WalletAddress=wallet;
            request.jsRequestData = new JSONObject("{ \"method\":\"" + METHOD_ETH_GetGasPrice + "\", \"params\":[] }");
        } catch (JSONException e) {
            e.printStackTrace();
            request.error = e.toString();
        }
        return request;
    }


    public static InfuraRequest SendTransactionCount(String wallet, String TX) {
        InfuraRequest request = new InfuraRequest();
        try {
            request.WalletAddress=wallet;
            request.jsRequestData = new JSONObject("{ \"method\":\"" + METHOD_ETH_SendRawTransaction + "\", \"params\":[\"" + TX + "\"] }");
        } catch (JSONException e) {
            e.printStackTrace();
            request.error = e.toString();
        }
        return request;
    }



    //METHOD_ETH_GetOutTransactionCount


    public JSONArray getParams() throws JSONException {
        return jsRequestData.getJSONArray("params");
    }

    public JSONObject getResult() throws JSONException {
        return getAnswer().getJSONObject("result");
    }

    public String getResultString() throws JSONException {
        return getAnswer().getString("result");
    }

    public JSONArray getResultArray() throws JSONException {
        return getAnswer().getJSONArray("result");
    }
}

