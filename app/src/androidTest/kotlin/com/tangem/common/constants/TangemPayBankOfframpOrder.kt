package com.tangem.common.constants

data class TangemPayBankOfframpOrder(
    val amountInCents: Long,
    val chainId: Int,
    val rail: String? = null,
    val bankAccountType: String? = null,
    val beneficiaryType: String? = null,
    val isThirdParty: Boolean? = null,
    val bankAccountId: String? = null,
    val accountNumber: String? = null,
    val bankName: String? = null,
)