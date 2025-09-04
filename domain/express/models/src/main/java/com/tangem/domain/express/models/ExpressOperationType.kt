package com.tangem.domain.express.models

enum class ExpressOperationType(val value: String) {
    SWAP("swap"),
    SEND_WITH_SWAP("swap-and-send"),
}