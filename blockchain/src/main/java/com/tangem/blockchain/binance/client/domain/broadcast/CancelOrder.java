package com.tangem.blockchain.binance.client.domain.broadcast;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tangem.blockchain.binance.client.BinanceDexConstants;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class CancelOrder {
    private String symbol;
    @JsonProperty("refid")
    private String refId;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, BinanceDexConstants.BINANCE_DEX_TO_STRING_STYLE)
                .append("symbol", symbol)
                .append("refId", refId)
                .toString();
    }
}
