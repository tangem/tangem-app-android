package com.tangem.data.network.request;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class VerificationServerProtocol {

    public static class Request {
        public Request(CustomCommand command, Class answerClass) {
            this.command = command;
            this.answerClass = answerClass;
        }

        public String error;
        public CustomCommand command;
        public CustomAnswer answer;
        public Type answerClass;

        public void doPost(String hostURL) throws IOException {
            URL url = new URL(hostURL + "/" + command.getURL());
            Log.i("isOnlineVerified", hostURL + "/" + command.getURL());
            URLConnection con = url.openConnection();
            HttpURLConnection http = (HttpURLConnection) con;
            try {
                http.setConnectTimeout(30000);
                http.setReadTimeout(30000);
                http.setRequestMethod("POST"); // PUT is another valid option
                http.setDoOutput(true);

                String sRequestBody = getGson().toJson(this.command);

                byte[] out = sRequestBody.getBytes(StandardCharsets.UTF_8);
                int length = out.length;

                http.setFixedLengthStreamingMode(length);
                http.setRequestProperty("Content-Type", "application/json");
                http.connect();
                try (OutputStream os = http.getOutputStream()) {
                    os.write(out);
                }
                try (InputStream is = http.getInputStream()) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        answer = getGson().fromJson(br, answerClass);
                    }
                }
            } finally {
                http.disconnect();
            }
        }
    }

    static abstract class CustomCommand {
        public abstract String getURL();

        public byte[] toBytes() {
            return getGson().toJson(this).getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String toString() {
            return getGson().toJson(this);
        }

    }

    static class CustomAnswer {
        public String error;

        @Override
        public String toString() {
            return getGson().toJson(this);
        }
    }

    public static class Verify {

        static class RequestItem {
            public String CID;
            public String publicKey;
        }

        static class Command extends CustomCommand {
            public RequestItem[] requests;

            @Override
            public String getURL() {
                return "verify";
            }
        }

        public static class ResultItem {
            public ResultItem(RequestItem request) {
                CID = request.CID;
            }

            public String error;
            public String CID;
            public Boolean passed;
        }

        public static class Answer extends CustomAnswer {
            public ResultItem[] results;
        }

        public static Request prepare(TangemCard card) {
            Command c = new Command();
            c.requests = new RequestItem[1];
            c.requests[0] = new RequestItem();
            c.requests[0].CID = Util.bytesToHex(card.getCID());
            c.requests[0].publicKey = Util.bytesToHex(card.getCardPublicKey());

            return new Request(c, Answer.class);
        }
    }

    public static class Validate {

        static class RequestItem {
            public String CID;
            public int counter;
            public String signature;
        }

        static class Command extends CustomCommand {
            public RequestItem[] requests;

            @Override
            public String getURL() {
                return "validate";
            }
        }

        static class ResultItem {
            public ResultItem(RequestItem request) {
                CID = request.CID;
            }

            public String error;
            public String CID;
            public Integer previousCounter;
            public Boolean passed;
        }

        static class Answer extends CustomAnswer {
            public ResultItem[] results;
        }

        public Request prepare(TangemCard card, int ValidationCounter, byte[] ValidationSignature) {
            Command c = new Command();
            c.requests = new RequestItem[1];
            c.requests[0].CID = Util.bytesToHex(card.getCID());
            c.requests[0].counter = ValidationCounter;
            c.requests[0].signature = Util.bytesToHex(ValidationSignature);
            return new Request(new Command(), Answer.class);
        }
    }

    public static Date strToDate(String date) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return new Date(formatter.parse(date).getTime());
    }

    public static String dateToStr(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return formatter.format(date);
    }

    public static Gson getGson() {
        return new GsonBuilder().create();
    }

}