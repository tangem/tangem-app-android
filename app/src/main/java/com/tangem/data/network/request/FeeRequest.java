package com.tangem.data.network.request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dvol on 16.07.2017.
 */

public class FeeRequest {
    public JSONObject jsRequestData;
    public String answerData;
    public String error;
    public String WalletAddress;
    public long txSize = 0;

    private FeeRequest() {
    }

    public FeeRequest(JSONObject jsRequest) {
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
        return answerData;
    }

    public static final int PRIORITY = 2;
    public static final int NORMAL = 3;
    public static final int MINIMAL = 6;
    private int blockCount = NORMAL;
    public void setBlockCount(int count
    )
    {
        blockCount = count;
    }

    public int getBlockCount()
    {
        return blockCount;
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

    public static FeeRequest GetFee(String wallet, long txSize, int blockCount) {
        FeeRequest request = new FeeRequest();
        request.WalletAddress=wallet;
        request.txSize = txSize;
        request.setBlockCount(blockCount);
        return request;
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
