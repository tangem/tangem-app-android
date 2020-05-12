package com.tangem.blockchain.blockchains.binance.client.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tangem.blockchain.blockchains.binance.client.BinanceDexConstants;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderList {
    private List<Order> order;
    private Long total;

    public List<Order> getOrder() {
        return order;
    }

    public void setOrder(List<Order> order) {
        this.order = order;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, BinanceDexConstants.BINANCE_DEX_TO_STRING_STYLE)
                .append("order", order)
                .append("total", total)
                .toString();
    }
}
