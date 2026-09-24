package com.tangem.domain.pay.model

data class TangemPayCardProvisioningData(
    val processorCardId: String,
    val cardSecret: String,
) {
    override fun toString(): String = "TangemPayCardProvisioningData(processorCardId=$processorCardId, cardSecret=***)"
}