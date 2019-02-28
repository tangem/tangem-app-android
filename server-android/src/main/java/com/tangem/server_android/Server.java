package com.tangem.server_android;

public class Server {
    /**
     * https://tangem-webapp.appspot.com/
     * https://tangem-services.appspot.com/
     */
    public static class ApiTangem {
        public static final String URL_TANGEM = ServerURL.API_TANGEM;

        public static class Method {
            static final String VERIFY = URL_TANGEM + "verify";
            static final String VERIFY_AND_GET_INFO = URL_TANGEM + "card/verify-and-get-info";
            static final String ARTWORK = URL_TANGEM + "card/artwork";
        }
    }
}