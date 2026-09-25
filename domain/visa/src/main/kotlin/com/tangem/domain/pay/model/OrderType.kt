package com.tangem.domain.pay.model

enum class OrderType(val wireValue: String) {
    CARD_ISSUE_ADDITIONAL("CARD_ISSUE_ADDITIONAL"),
    CARD_ISSUE_VIRTUAL_RAIN("CARD_ISSUE_VIRTUAL_RAIN"),
    CARD_ISSUE_VIRTUAL_RAIN_KYC("CARD_ISSUE_VIRTUAL_RAIN_KYC"),
    CARD_ISSUE_VIRTUAL_RAIN_KYC_V2("CARD_ISSUE_VIRTUAL_RAIN_KYC_V2"),
    CARD_ISSUE_PLASTIC_RAIN("CARD_ISSUE_PLASTIC_RAIN"),
    CARD_ACTIVATION_PLASTIC_RAIN("CARD_ACTIVATION_PLASTIC_RAIN"),
    CARD_REISSUE("CARD_REISSUE"),
    CARD_REISSUE_PLASTIC_RAIN("CARD_REISSUE_PLASTIC_RAIN"),
    CARD_FREEZE("CARD_FREEZE"),
    CARD_UNFREEZE("CARD_UNFREEZE"),
    WITHDRAW("WITHDRAW"),
    BANK_OFFRAMP_RAIN("BANK_OFFRAMP_RAIN"),
    TARIFF_PLAN_TRANSITION("TARIFF_PLAN_TRANSITION"),
    SMART_CONTRACT_ISSUE_RAIN("SMART_CONTRACT_ISSUE_RAIN"),
    ACCOUNT_ISSUE_VIRTUAL_RAIN("ACCOUNT_ISSUE_VIRTUAL_RAIN"),
    ACCOUNT_ISSUE_BANKING_RAIN("ACCOUNT_ISSUE_BANKING_RAIN"),
    UNKNOWN(""),
    ;

    val isIssuing: Boolean get() = issueCardTypes.contains(this)

    companion object {

        val virtualIssueCardTypes = setOf(
            CARD_ISSUE_VIRTUAL_RAIN,
            CARD_ISSUE_VIRTUAL_RAIN_KYC,
            CARD_ISSUE_VIRTUAL_RAIN_KYC_V2,
        )

        val issueCardTypes = virtualIssueCardTypes + CARD_ISSUE_PLASTIC_RAIN

        fun fromString(value: String?): OrderType {
            if (value.isNullOrBlank()) return UNKNOWN
            return entries.firstOrNull { it.wireValue == value || it.name == value } ?: UNKNOWN
        }
    }
}