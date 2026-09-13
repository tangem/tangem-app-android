package com.tangem.domain.notifications.models

enum class NotificationType(val type: String) {
    Promo("promo"),
    IncomeTransactions("income_transaction"),
    SwapStatus("swap_status_update"),
    OnrampStatus("onramp_status_update"),
    JointMembers("joint_members"),
    JointOverview("joint_overview"),
    JointTxSent("joint_tx_sent"),
    Unknown("unknown"),
    ;

    companion object {
        fun getType(type: String?): NotificationType {
            return entries.firstOrNull { it.type == type } ?: Unknown
        }
    }
}