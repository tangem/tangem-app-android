package com.tangem.data.network.model;

import java.util.ArrayList;
import java.util.List;

public class BitcoreSendBody {
    private List<String> rawTx;

    public BitcoreSendBody(String tx) {
        List<String> txList = new ArrayList<>();
        txList.add(tx);
        rawTx = txList;
    }
}
