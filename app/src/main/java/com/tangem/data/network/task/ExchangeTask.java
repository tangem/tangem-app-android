package com.tangem.data.network.task;

/**
 * Created by Ilia on 16.01.2018.
 */

import android.os.AsyncTask;

import com.tangem.data.network.request.ExchangeRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ilia on 04.12.2017.
 */

public class ExchangeTask extends AsyncTask<ExchangeRequest, Void, List<ExchangeRequest>> {
    public ExchangeTask()
    {

    }
    protected List<ExchangeRequest> doInBackground(ExchangeRequest... requests) {
        List<ExchangeRequest> result = new ArrayList<>();
        for (int i = 0; i < requests.length; i++) {
            result.add(requests[i]);
        }

        for (ExchangeRequest request: result)
        {
            HttpURLConnection httpcon = null;

            try {

                URL url = new URL("https://api.coinmarketcap.com/v1/ticker/?convert=USD&lmit=10");
                httpcon = (HttpURLConnection) url.openConnection();
                httpcon.setRequestMethod("GET");

                httpcon.connect();

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(httpcon.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                request.answerData = response.toString();

            } catch (Exception e) {
                request.error = e.getMessage();
            } finally {
                httpcon.disconnect();
            }
        }

        return result;
    }
}
