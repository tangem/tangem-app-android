package com.tangem.blockchain.binance.client.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tangem.blockchain.binance.client.BinanceDexConstants;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TradePage {
    private Long total;
    private List<Trade> trade;

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<Trade> getTrade() {
        return trade;
    }

    public void setTrade(List<Trade> trade) {
        this.trade = trade;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, BinanceDexConstants.BINANCE_DEX_TO_STRING_STYLE)
                .append("total", total)
                .append("trade", trade)
                .toString();
    }
}
