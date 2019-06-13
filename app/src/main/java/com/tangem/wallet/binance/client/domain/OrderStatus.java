package com.tangem.wallet.binance.client.domain;

public enum OrderStatus {
    Ack,
    PartialFill,
    IocNoFill,
    FullyFill,
    Canceled,
    Expired,
    Unknown
}
