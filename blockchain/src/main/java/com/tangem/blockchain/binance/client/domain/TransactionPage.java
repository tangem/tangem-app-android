package com.tangem.blockchain.binance.client.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tangem.blockchain.binance.client.BinanceDexConstants;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionPage {
    private Long total;
    private List<Transaction> tx;

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<Transaction> getTx() {
        return tx;
    }

    public void setTx(List<Transaction> tx) {
        this.tx = tx;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, BinanceDexConstants.BINANCE_DEX_TO_STRING_STYLE)
                .append("total", total)
                .append("tx", tx)
                .toString();
    }
}
