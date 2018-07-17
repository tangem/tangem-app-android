package com.tangem.data.network.task;

import android.os.AsyncTask;

import com.tangem.data.network.request.VerificationServerProtocol;

import java.util.ArrayList;
import java.util.List;

public class VerificationServerTask extends AsyncTask<VerificationServerProtocol.Request, Void, List<VerificationServerProtocol.Request>> {
        public static final String hostURL ="https://tangem-webapp.appspot.com";

        public VerificationServerTask() {

        }

        protected List<VerificationServerProtocol.Request> doInBackground(VerificationServerProtocol.Request... requests) {
            List<VerificationServerProtocol.Request> result = new ArrayList<>();
            for (int i = 0; i < requests.length; i++) {
                result.add(requests[i]);
            }

            for (VerificationServerProtocol.Request request : result) {
                try {
                    request.doPost(hostURL);
                } catch (Exception e) {
                    request.error = e.getMessage();
                    if( request.error==null || request.error.isEmpty() )
                    {
                        request.error = e.getClass().getName();
                    }
                }
            }

            return result;
        }

        public String getValidationNodeDescription() {
            return hostURL;
        }

    }
