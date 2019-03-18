package com.tangem.data.network.model;

import java.util.ArrayList;
import java.util.HashMap;

public class RippleBody {
    private String method;
    private ArrayList<HashMap<String,String>> params;

    public RippleBody() {
    }

    //for RIPPLE_FEE
    public RippleBody(String method) {
        this.method = method;
    }

    public RippleBody(String method, HashMap<String,String> paramsMap) {
        this.method = method;
        ArrayList<HashMap<String, String>> paramsList = new ArrayList<>();
        paramsList.add(paramsMap);
        this.params = paramsList;
    }
}
