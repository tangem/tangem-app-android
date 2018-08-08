package com.tangem.data.network;

import java.util.HashMap;
import java.util.Map;

public class Server {

    public static Map<String, String> getHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }

    public static class API {
        private static final String URL_TANGEM = ServerURL.API_TANGEM;

        public static class Method {
            /**
             * https://tangem-webapp.appspot.com/
             */
            public static final String INFO_VERIFY = URL_TANGEM + "info/version";

            public static final String CARD_VERIFY = URL_TANGEM + "card/verify";
            public static final String VERIFY = URL_TANGEM + "verify";
            public static final String CARD_VALIDATE = URL_TANGEM + "card/validate";
            public static final String CARD_ACTIVATE = URL_TANGEM + "card/activate";
        }
    }

}