package com.tangem.data.network.request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dvol on 16.07.2017.
 */

public class ElectrumRequest {
    public static final String METHOD_GetBalance = "blockchain.address.get_balance";
    public static final String METHOD_ListUnspent = "blockchain.address.listunspent";
    public static final String METHOD_GetHistory = "blockchain.address.get_history";
    public static final String METHOD_GetTransaction = "blockchain.transaction.get";
    public static final String METHOD_GetHeader = "blockchain.block.get_header";
    public static final String METHOD_SendTransaction = "blockchain.transaction.broadcast";
    public static final String METHOD_GetFee = "blockchain.estimatefee";

    public JSONObject jsRequestData;
    public String answerData;
    public String error;
    public String WalletAddress;
    public String TxHash;
    public String Host;
    public int Port;

    private ElectrumRequest() {
    }

    public ElectrumRequest(JSONObject jsRequest) {
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
            jsRequestData.put("id", String.format("%d", value));
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

    public static ElectrumRequest CheckBalance(String wallet) {
        ElectrumRequest request = new ElectrumRequest();
        try {
            request.WalletAddress=wallet;
            request.jsRequestData = new JSONObject("{ \"method\":\"" + METHOD_GetBalance + "\", \"params\":[\"" + wallet + "\"] }");
        } catch (JSONException e) {
            e.printStackTrace();
            request.error = e.toString();
        }
        return request;
    }


    public static ElectrumRequest GetFee(String wallet) {
        ElectrumRequest request = new ElectrumRequest();
        try{
            request.WalletAddress = wallet; //METHOD_GetFee
            request.jsRequestData = new JSONObject("{ \"method\":\"" + METHOD_GetFee + "\", \"params\":[\"" + 6 + "\"] }");
        }
        catch(JSONException e)
        {
            e.printStackTrace();
            request.error = e.toString();
        }
        return request;
    }

    public static ElectrumRequest GetHeader(String wallet, String height) {
        ElectrumRequest request = new ElectrumRequest();
        try {
            request.WalletAddress=wallet;
            request.jsRequestData = new JSONObject("{ \"method\":\"" + METHOD_GetHeader + "\", \"params\":[\"" + height + "\"] }");
        } catch (JSONException e) {
            e.printStackTrace();
            request.error = e.toString();
        }
        return request;
    }

    public static ElectrumRequest ListUnspent(String wallet) {
        ElectrumRequest request = new ElectrumRequest();
        try {
            request.WalletAddress=wallet;
            request.jsRequestData = new JSONObject("{ \"method\":\"" + METHOD_ListUnspent + "\", \"params\":[\"" + wallet + "\"] }");
        } catch (JSONException e) {
            e.printStackTrace();
            request.error = e.toString();
        }
        return request;
    }

    public static ElectrumRequest Broadcast(String wallet, String tx) {
        ElectrumRequest request = new ElectrumRequest();
        try {
            request.WalletAddress=wallet;
            request.jsRequestData = new JSONObject("{ \"method\":\"" + METHOD_SendTransaction + "\", \"params\":[\"" + tx + "\"] }");
        } catch (JSONException e) {
            e.printStackTrace();
            request.error = e.toString();
        }
        return request;
    }

    public static ElectrumRequest ListHistory(String wallet) {
        ElectrumRequest request = new ElectrumRequest();
        try {
            request.WalletAddress=wallet;
            request.jsRequestData = new JSONObject("{ \"method\":\"" + METHOD_GetHistory + "\", \"params\":[\"" + wallet + "\"] }");
        } catch (JSONException e) {
            e.printStackTrace();
            request.error = e.toString();
        }
        return request;
    }

    public static ElectrumRequest GetTransaction(String wallet, String tx_hash) {
        ElectrumRequest request = new ElectrumRequest();
        try {
            request.WalletAddress=wallet;
            request.TxHash = tx_hash;
            request.jsRequestData = new JSONObject("{ \"method\":\"" + METHOD_GetTransaction + "\", \"params\":[\"" + tx_hash + "\"] }");
        } catch (JSONException e) {
            e.printStackTrace();
            request.error = e.toString();
        }
        return request;
    }

    public boolean isMethod(String methodName) throws JSONException {
        return jsRequestData.getString("method").equals(methodName);
    }

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
