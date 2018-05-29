package com.tangem.data.network.task;

/**
 * Created by Ilia on 19.12.2017.
 */

import android.os.AsyncTask;

import com.tangem.wallet.Blockchain;
import com.tangem.wallet.Infura_Request;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Ilia on 04.12.2017.
 */

public class Infura_Task extends AsyncTask<Infura_Request, Void, List<Infura_Request>> {
    private Exception exception;
    private Blockchain blockchain;
    public Infura_Task(Blockchain blockchainNet)
    {
        blockchain = blockchainNet;
    }
    boolean useOurNode = false;
    protected List<Infura_Request> doInBackground(Infura_Request... requests) {
        List<Infura_Request> result = new ArrayList<>();
        for (int i = 0; i < requests.length; i++) {
            result.add(requests[i]);
        }

        for (Infura_Request request: result)
        {
            HttpURLConnection httpcon = null;

            try {
                URL url = new URL("https://rinkeby.infura.io/AfWg0tmYEX5Kukn2UkKV");

                if(blockchain == Blockchain.Ethereum || blockchain == Blockchain.Token){
                    if(useOurNode) {
                        URL tmp = new URL("http://52.230.23.88");
                        url = new URL(tmp.getProtocol(), tmp.getHost(), 27172, tmp.getFile());
                    }else
                        url = new URL("https://mainnet.infura.io/AfWg0tmYEX5Kukn2UkKV");

                }

                if(useOurNode)
                {
                    httpcon = (HttpURLConnection)url.openConnection();
                }
                else
                {
                    httpcon = (HttpsURLConnection)url.openConnection();
                }

                if(httpcon == null)
                {
                    request.error = String.format("Cann't connect to %s", url.getHost());
                    return result;
                }

                httpcon.setRequestMethod("POST");
                httpcon.setRequestProperty("Content-Type", "application/json");
                String params = request.getAsString();

                OutputStream os = httpcon.getOutputStream();
                if(os == null)
                {
                    request.error = String.format("Cann't recieve data from %s", url.getHost());
                    return result;
                }
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                if(writer == null)
                {
                    request.error = String.format("Cann't send data to %s", url.getHost());

                }
                writer.write(params);
                writer.flush();
                writer.close();
                os.close();


                httpcon.connect();
                request.getParams();
                System.out.println("code:"+httpcon.getResponseCode());
                int code = httpcon.getResponseCode();

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
                this.exception = e;
                request.error = e.getMessage();
            } finally {
                httpcon.disconnect();
            }
        }

        return result;
    }

    public String getValidationNodeDescription() {
        if(blockchain == Blockchain.Ethereum || blockchain == Blockchain.Token)
        {
            if(useOurNode)
                return "52.230.23.88:27172";
            else
                return "Infura, infura.io";
        }


        return "Infura, rinkeby.infura.io";
    }

}
