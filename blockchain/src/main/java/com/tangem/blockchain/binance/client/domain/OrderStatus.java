package com.tangem.blockchain.binance.client.domain;

public enum OrderStatus {
    Ack,
    PartialFill,
    IocNoFill,
    FullyFill,
    Canceled,
    Expired,
    Unknown
}
