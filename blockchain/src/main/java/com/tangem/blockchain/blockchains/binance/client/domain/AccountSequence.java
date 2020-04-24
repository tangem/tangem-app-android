package com.tangem.blockchain.blockchains.binance.client.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tangem.blockchain.blockchains.binance.client.BinanceDexConstants;

import org.apache.commons.lang3.builder.ToStringBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountSequence {
    private Long sequence;

    public Long getSequence() {
        return sequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, BinanceDexConstants.BINANCE_DEX_TO_STRING_STYLE)
                .append("sequence", sequence)
                .toString();
    }
}
