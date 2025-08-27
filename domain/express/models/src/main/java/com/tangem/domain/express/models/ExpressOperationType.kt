package com.tangem.domain.express.models

enum class ExpressOperationType(val value: String) {
    SEND("send"),
    SEND_WITH_SWAP("swap-and-send"),
}