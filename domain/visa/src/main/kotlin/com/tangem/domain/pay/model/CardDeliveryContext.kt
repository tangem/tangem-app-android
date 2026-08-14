package com.tangem.domain.pay.model

enum class CardDeliveryContext(val queryValue: String) {
    ISSUE("ISSUE"),
    REISSUE("REISSUE"),
}