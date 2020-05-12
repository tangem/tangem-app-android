package com.tangem.blockchain.blockchains.binance.client.domain;

public enum OrderStatus {
    Ack,
    PartialFill,
    IocNoFill,
    FullyFill,
    Canceled,
    Expired,
    Unknown
}
