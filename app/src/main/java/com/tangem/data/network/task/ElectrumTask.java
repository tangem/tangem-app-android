package com.tangem.data.network.task;

import android.os.AsyncTask;
import android.util.Log;

import com.tangem.data.network.request.ElectrumRequest;
import com.tangem.domain.wallet.SharedData;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dvol on 16.07.2017.
 */

public class ElectrumTask extends AsyncTask<ElectrumRequest, Integer, List<ElectrumRequest>> {
    public static final String logTag = "Electrum";
    //public static final String Host = /*"hsmiths.changeip.net";*/ "testnetnode.arihanc.com";
    //public static final int Port = /*8080*/51001;
    private int reqID = 1;
    private String Host = "";
    private int Port = 0;
    OutputStreamWriter out;
    BufferedReader in;

    public SharedData sharedCounter = null;


    public ElectrumTask(String host, int port) {
        super();
        Host = host;
        Port = port;
    }

    public ElectrumTask(String host, int port, SharedData sharedCounter) {
        super();
        Host = host;
        Port = port;
        this.sharedCounter = sharedCounter;
    }

    @Override
    protected List<ElectrumRequest> doInBackground(ElectrumRequest... requests) {
        List<ElectrumRequest> result = new ArrayList<>();
        for (int i = 0; i < requests.length; i++) {
            result.add(requests[i]);
        }
        try {

            InetAddress serverAddress = InetAddress.getByName(Host);
            Log.v(logTag, "Connecting..."+Host);
            Socket socket = new Socket(serverAddress, Port);
            socket.setSoTimeout(5000);
            try {
                OutputStream os = socket.getOutputStream();
                out = new OutputStreamWriter(os, "UTF-8");
                Log.v(logTag, "Connected");
                InputStream is = socket.getInputStream();
                in = new BufferedReader(new InputStreamReader(is));

                publishProgress(5);

                for (int i = 0; i < requests.length; i++) {
                    requests[i].setID(reqID++);
                    doRequest(requests[i]);
                    publishProgress(5 + 90 * (i + 1) / requests.length);
                }

                publishProgress(100);

            } catch (Exception e) {
                Log.e(logTag, "Error: ", e);
            } finally {
                socket.close();
            }
        } catch (Exception e) {
            Log.e(logTag, "Error: ", e);
            for (int i = 0; i < requests.length; i++) {
                result.get(i).error = e.toString();
            }
        }
        return result;
    }

    private void doRequest(ElectrumRequest request) {
        try {

            Log.v(logTag, "<< " + request.getAsString());

            out.write(request.getAsString() + "\n");
            out.flush();

            request.answerData = in.readLine();
            request.Host=Host;
            request.Port=Port;
            if (request.answerData != null) {
                Log.v(logTag, ">> " + request.answerData);
            } else {
                request.error = "No answer from server";
                Log.v(logTag, ">> <NULL>");
            }
        } catch (Exception e) {
            request.error = e.toString();
        }
    }

    public String getValidationNodeDescription() {
        return "Electrum, "+Host+":"+String.valueOf(Port);
    }


}
