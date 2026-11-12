package com.tangem.domain.tangempay.model

data class TangemPayCardProvisioningData(
    val processorCardId: String,
    val cardSecret: String,
) {
    override fun toString(): String = "TangemPayCardProvisioningData(processorCardId=$processorCardId, cardSecret=***)"
}