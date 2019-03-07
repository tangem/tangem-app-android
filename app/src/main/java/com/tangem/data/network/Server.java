package com.tangem.data.network;

public class Server {

    public static class ApiUpdateVersion {
        public static final String URL_UPDATE_VERSION = ServerURL.API_UPDATE_VERSION;

        public static class Method {
            static final String LAST_VERSION = URL_UPDATE_VERSION + "TangemCash/tangem-binaries/master/apk-version.txt";
        }
    }

    /**
     * https://coinmarketcap.com/api/
     */
    public static class ApiCoinmarket {
        public static final String URL_COINMARKET = ServerURL.API_COINMARKETCAP;

        public static class Method {
            static final String V1_TICKER_CONVERT = URL_COINMARKET + "v1/ticker/?convert=USD&lmit=10";
        }
    }

    /**
     * https://infura.io/
     */
    public static class ApiInfura {
        public static final String URL_INFURA = ServerURL.API_INFURA;

        public static class Method {
            static final String MAIN = URL_INFURA + "v3/613a0b14833145968b1f656240c7d245";
        }
    }

    /**
     * https://public-node.rsk.co/
     */
    public static class ApiRootstock {
        public static final String URL_ROOTSTOCK = ServerURL.API_ROOTSTOCK;

        public static class Method {
            static final String MAIN = URL_ROOTSTOCK;
        }
    }

    /**
     * https://estimatefee.com/
     */
    public static class ApiEstimatefee {
        public static final String URL_ESTIMATEFEE = ServerURL.API_ESTIMATEFEE;

        public static class Method {
            static final String N_2 = URL_ESTIMATEFEE + "n/2";
            static final String N_3 = URL_ESTIMATEFEE + "n/3";
            static final String N_6 = URL_ESTIMATEFEE + "n/6";
        }
    }
}