package com.tangem.datasource.api.stakekit.models.request

enum class RevenueOption(val value: String) {
    SUPPORTS_FEE("supportsFee"),
    SUPPORTS_REV_SHARE("supportsRevShare"),
    ;

    override fun toString(): String {
        return value
    }
}