package com.tangem.data.network.task.loaded_wallet;

import com.tangem.data.network.request.ExchangeRequest;
import com.tangem.data.network.task.ExchangeTask;
import com.tangem.presentation.fragment.LoadedWallet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.List;

public class RateInfoTask extends ExchangeTask {
    private WeakReference<LoadedWallet> reference;

    public RateInfoTask(LoadedWallet context) {
        reference = new WeakReference<>(context);
    }

    protected void onPostExecute(List<ExchangeRequest> requests) {
        super.onPostExecute(requests);
        LoadedWallet loadedWallet = reference.get();

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
                            loadedWallet.mCard.setRate(rate);
                            loadedWallet.updateViews();
                            stop = true;
                        }

                        if (currency.equals(request.currencyAlter)) {
                            String usd = obj.getString("price_usd");

                            Float rate = Float.valueOf(usd);
                            loadedWallet.mCard.setRateAlter(rate);
                            loadedWallet.updateViews();
                            stopAlter = true;
                        }

                        if (stop && stopAlter) {
                            break;
                        }

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}