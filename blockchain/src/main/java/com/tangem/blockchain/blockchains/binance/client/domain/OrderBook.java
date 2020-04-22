package com.tangem.blockchain.blockchains.binance.client.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tangem.blockchain.blockchains.binance.client.BinanceDexConstants;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderBook {
    private List<OrderBookEntry> asks;
    private List<OrderBookEntry> bids;
    private long height;

    public List<OrderBookEntry> getAsks() {
        return asks;
    }

    public void setAsks(List<OrderBookEntry> asks) {
        this.asks = asks;
    }

    public List<OrderBookEntry> getBids() {
        return bids;
    }

    public void setBids(List<OrderBookEntry> bids) {
        this.bids = bids;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, BinanceDexConstants.BINANCE_DEX_TO_STRING_STYLE)
                .append("asks", asks)
                .append("bids", bids)
                .append("height", height)
                .toString();
    }
}
