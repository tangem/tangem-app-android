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

    public static class ApiBinance {
        public static final String URL_BINANCE = ServerURL.API_BINANCE;
        static final String API_V1 = "api/v1/";

        public static class Method {
            static final String ACCOUNT = URL_BINANCE + API_V1 + "account";
            static final String FEES = URL_BINANCE + API_V1 + "fees";
            static final String BROADCAST = URL_BINANCE + API_V1 + "broadcast";
        }
    }

    public static class ApiBlockcypher {
        public static final String URL_BLOCKCYPHER = ServerURL.API_BLOCKCYPHER;
        static final String V1_BTC_MAIN = "v1/btc/main";

        public static class Method {
            static final String MAIN = URL_BLOCKCYPHER + V1_BTC_MAIN;
            static final String ADDRESS = MAIN + "/addrs/{address}?unspentOnly=true&includeScript=true";
            static final String PUSH = MAIN + "/txs/push";
        }
    }
}