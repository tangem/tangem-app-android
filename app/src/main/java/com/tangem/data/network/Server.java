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
            public static final String VERIFY_AND_GET_ARTWORK = URL_TANGEM + "card/verify-and-get-artwork";
            public static final String ARTWORK = URL_TANGEM + "card/artwork";
        }
    }

    public static class ApiUpdateVersion {
        public static final String URL_UPDATE_VERSION = ServerURL.API_UPDATE_VERSION;

        public static class Method {
            public static final String LAST_VERSION = URL_UPDATE_VERSION +"TangemCash/tangem-binaries/master/apk-version.txt";
        }
    }

    /**
     * https://coinmarketcap.com/api/
     */
    public static class ApiCoinmarket {
        public static final String URL_COINMARKET = ServerURL.API_COINMARKETCAP;

        public static class Method {
            public static final String V1_TICKER_CONVERT = URL_COINMARKET + "v1/ticker/?convert=USD&lmit=10";
        }
    }

    /**
     * https://infura.io/
     */
    public static class ApiInfura {
        public static final String URL_INFURA = ServerURL.API_INFURA;

        public static class Method {
            public static final String MAIN = URL_INFURA + "AfWg0tmYEX5Kukn2UkKV";
        }
    }

    /**
     * https://estimatefee.com/
     */
    public static class ApiEstimatefee {
        public static final String URL_ESTIMATEFEE = ServerURL.API_ESTIMATEFEE;

        public static class Method {
            public static final String N_2 = URL_ESTIMATEFEE + "n/2";
            public static final String N_3 = URL_ESTIMATEFEE + "n/3";
            public static final String N_6 = URL_ESTIMATEFEE + "n/6";
        }
    }

}