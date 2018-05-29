package com.tangem.data.network.task;

import android.os.AsyncTask;

import com.tangem.wallet.Fee_Request;
import com.tangem.wallet.SharedData;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ilia on 04.12.2017.
 */

public class Fee_Task extends AsyncTask<Fee_Request, Void, List<Fee_Request>> {

    public SharedData sharedCounter = null;

    public Fee_Task(SharedData sharedData)
    {
        sharedCounter = sharedData;
    }
    protected List<Fee_Request> doInBackground(Fee_Request... requests) {
        List<Fee_Request> result = new ArrayList<>();
        for (int i = 0; i < requests.length; i++) {
            result.add(requests[i]);
        }

        for (Fee_Request request: result)
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