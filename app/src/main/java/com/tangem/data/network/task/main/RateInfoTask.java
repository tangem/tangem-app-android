package com.tangem.data.network.task.main;

import com.tangem.data.network.request.ExchangeRequest;
import com.tangem.data.network.task.ExchangeTask;
import com.tangem.presentation.fragment.Main;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.List;

public class RateInfoTask extends ExchangeTask {
    private WeakReference<Main> reference;

    public RateInfoTask(Main context) {
        reference = new WeakReference<>(context);
    }

    protected void onPostExecute(List<ExchangeRequest> requests) {
        super.onPostExecute(requests);
        Main main = reference.get();

        for (ExchangeRequest request : requests) {
            if (request.error == null) {
                try {

                    JSONArray arr = request.getAnswerList();
                    for (int i = 0; i < arr.length(); ++i) {
                        JSONObject obj = arr.getJSONObject(i);
                        String currency = obj.getString("id");

                        boolean stop = false;
                        boolean stopAlter = false;
                        if (currency.equals(request.currency)) {
                            String usd = obj.getString("price_usd");

                            Float rate = Float.valueOf(usd);
                            main.mCardListAdapter.UpdateRate(request.WalletAddress, rate);
                            stop = true;
                        }

                        if (currency.equals(request.currencyAlter)) {
                            String usd = obj.getString("price_usd");

                            Float rate = Float.valueOf(usd);
                            main.mCardListAdapter.UpdateRateAlter(request.WalletAddress, rate);
                            stopAlter = true;
                        }

                        if (stop && stopAlter) {
                            break;
                        }
                    }

                    //mCardListAdapter.UpdateWalletBalance(mWalletAddress, balance, l.toString(10));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}