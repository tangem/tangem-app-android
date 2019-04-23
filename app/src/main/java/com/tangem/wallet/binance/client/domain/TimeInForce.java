package com.tangem.wallet.binance.client.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TimeInForce {
    GTE(1L), IOC(3L);

    private long value;

    TimeInForce(long value) {
        this.value = value;
    }

    @JsonCreator
    public static TimeInForce fromValue(long value) {
        for (TimeInForce tif : TimeInForce.values()) {
            if (tif.value == value) {
                return tif;
            }
        }
        return null;
    }

    @JsonValue
    public long toValue() {
        return this.value;
    }
}
