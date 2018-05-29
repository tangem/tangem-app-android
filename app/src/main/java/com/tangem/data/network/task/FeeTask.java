package com.tangem.data.network.task;

import android.os.AsyncTask;

import com.tangem.data.network.request.FeeRequest;
import com.tangem.domain.wallet.SharedData;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ilia on 04.12.2017.
 */

public class FeeTask extends AsyncTask<FeeRequest, Void, List<FeeRequest>> {

    public SharedData sharedCounter = null;

    public FeeTask(SharedData sharedData)
    {
        sharedCounter = sharedData;
    }
    protected List<FeeRequest> doInBackground(FeeRequest... requests) {
        List<FeeRequest> result = new ArrayList<>();
        for (int i = 0; i < requests.length; i++) {
            result.add(requests[i]);
        }

        for (FeeRequest request: result)
        {
            HttpURLConnection httpcon = null;

            try {

                URL url = new URL("https://estimatefee.com/n/"+String.valueOf(request.getBlockCount()));
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