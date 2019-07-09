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
            static final String PRICE_CONVERSION = URL_COINMARKET + "v1/tools/price-conversion";
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


    public static class ApiSoChain {
        public static final String URL = ServerURL.API_SOCHAIN_V2;

        public static class Method {
            public static final String ADDRESS_BALANCE = "api/v2/get_address_balance/{network}/{address}";
            public static final String UNSPENT_TX = "api/v2/get_tx_unspent/{network}/{address}";
            public static final String GET_TX = "api/v2/get_tx/{network}/{txid}";
            public static final String SEND_TRANSACTION = "api/v2/send_tx/{network}";
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
     * https://testnet2.matic.network
     */
    public static class ApiMaticTesnet {
        public static final String URL_MATIC_TESTNET = ServerURL.API_MATIC_TESTNET ;

        public static class Method {
            static final String MAIN = URL_MATIC_TESTNET;
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

    /**
     * https://dex.binance.org/
     */

    public static class ApiBinance {
        public static final String URL_BINANCE = ServerURL.API_BINANCE;

        public static class Method {
            public static final String API_V1 = URL_BINANCE + "api/v1/";
        }
    }

    /**
     * https://testnet-dex.binance.org/
     */

    public static class ApiBinanceTestnet {
        public static final String URL_BINANCE_TESTNET = ServerURL.API_BINANCE_TESTNET;

        public static class Method {
            public static final String API_V1 = URL_BINANCE_TESTNET + "api/v1/";
        }
    }

    /**
     * https://api.blockcypher.com/
     */

    public static class ApiBlockcypher {
        public static final String URL_BLOCKCYPHER = ServerURL.API_BLOCKCYPHER;
        static final String V1_MAIN = "v1/{blockchain}/main";

        public static class Method {
            static final String MAIN = URL_BLOCKCYPHER + V1_MAIN;
            static final String ADDRESS = MAIN + "/addrs/{address}?unspentOnly=true&includeScript=true";
            static final String PUSH = MAIN + "/txs/push";
        }
    }
}