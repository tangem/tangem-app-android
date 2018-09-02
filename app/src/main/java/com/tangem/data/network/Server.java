package com.tangem.data.network;

public class Server {

    /**
     * https://tangem-webapp.appspot.com/
     * https://tangem-services.appspot.com/
     */
    public static class ApiTangem {
        public static final String URL_TANGEM = ServerURL.API_TANGEM;

        public static class Method {
            public static final String VERIFY = URL_TANGEM + "verify";
        }
    }

    /**
     * https://coinmarketcap.com/api/
     */
    public static class ApiCoinmarket {
        public static final String URL_COINMARKET = ServerURL.API_COINMARKET;

        public static class Method {
            public static final String V1_TICKER_CONVERT = URL_COINMARKET + "v1/ticker/?convert=USD&lmit=10";
        }
    }

}