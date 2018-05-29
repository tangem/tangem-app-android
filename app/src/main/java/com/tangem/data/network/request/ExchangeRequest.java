package com.tangem.data.network.request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dvol on 16.07.2017.
 */

public class ExchangeRequest {
    public JSONObject jsRequestData;
    public String answerData;
    public String error;
    public String WalletAddress;
    public String currency;
    public String currencyAlter;

    private ExchangeRequest() {
    }

    public ExchangeRequest(JSONObject jsRequest) {
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

    public JSONArray getAnswerList() throws  JSONException {
            return new JSONArray(answerData);
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

    public static ExchangeRequest GetRate(String wallet, String currency, String alterCurrency) {
        ExchangeRequest request = new ExchangeRequest();
        request.WalletAddress=wallet;
        request.currency = currency;
        request.currencyAlter = alterCurrency;
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
