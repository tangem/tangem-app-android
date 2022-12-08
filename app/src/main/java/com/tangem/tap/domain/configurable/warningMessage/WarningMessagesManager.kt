package com.tangem.tap.domain.configurable.warningMessage

import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.common.extensions.containsAny
import com.tangem.tap.common.extensions.removeBy
import com.tangem.wallet.R

/**
 * Created by Anton Zhilenkov on 16/02/2021.
 */
class WarningMessagesManager {

    private val warningsList: MutableList<WarningMessage> = mutableListOf()

    fun addWarning(warning: WarningMessage) {
        if (findWarning(warning) == null) {
            warningsList.add(warning)
            sortByPriority()
        }
    }

    fun getWarnings(
        location: WarningMessage.Location,
        forBlockchains: List<Blockchain> = emptyList(),
    ): List<WarningMessage> {
        return warningsList
            .filter { !it.isHidden && it.location.contains(location) }
            .filter {
                val list = it.blockchainList
                when {
                    list == null -> true
                    list.containsAny(forBlockchains) -> true
                    else -> false
                }
            }
    }

    fun hideWarning(warning: WarningMessage): Boolean {
        val foundWarning = findWarning(warning)
        return when {
            foundWarning == null -> false
            foundWarning.type == WarningMessage.Type.Temporary
                || foundWarning.type == WarningMessage.Type.AppRating -> {
                if (foundWarning.isHidden) {
                    false
                } else {
                    foundWarning.isHidden = true
                    true
                }
            }
            else -> false
        }
    }

    fun removeWarnings(origin: WarningMessage.Origin) {
        warningsList.removeBy { it.origin == origin }
        sortByPriority()
    }

    fun removeWarnings(messageRes: Int) {
        warningsList.removeBy { it.messageResId == messageRes }
    }

    private fun sortByPriority() {
        warningsList.sortBy { it.priority.ordinal }
    }

    private fun findWarning(warning: WarningMessage): WarningMessage? {
        return warningsList.firstOrNull { it == warning }
    }

    companion object {
        fun devCardWarning(): WarningMessage = WarningMessage(
            "",
            "",
            type = WarningMessage.Type.Permanent,
            priority = WarningMessage.Priority.Critical,
            listOf(WarningMessage.Location.MainScreen),
            null,
            R.string.common_warning,
            R.string.alert_developer_card,
            WarningMessage.Origin.Local,
        )

        fun alreadySignedHashesWarning(): WarningMessage = WarningMessage(
            "",
            "",
            type = WarningMessage.Type.Temporary,
            priority = WarningMessage.Priority.Info,
            listOf(WarningMessage.Location.MainScreen),
            null,
            R.string.common_warning,
            R.string.alert_card_signed_transactions,
            WarningMessage.Origin.Local,
        )

        fun signedHashesMultiWalletWarning(): WarningMessage = WarningMessage(
            title = "",
            message = "",
            type = WarningMessage.Type.Temporary,
            priority = WarningMessage.Priority.Info,
            location = listOf(WarningMessage.Location.MainScreen),
            blockchains = null,
            titleResId = R.string.warning_important_security_info,
            messageResId = R.string.warning_signed_tx_previously,
            origin = WarningMessage.Origin.Local,
            buttonTextId = R.string.warning_button_learn_more,
        )

        fun appRatingWarning(): WarningMessage = WarningMessage(
            "",
            "",
            WarningMessage.Type.AppRating,
            WarningMessage.Priority.Info,
            listOf(WarningMessage.Location.MainScreen),
            null,
            R.string.warning_rate_app_title,
            R.string.warning_rate_app_message,
            WarningMessage.Origin.Local,
        )

        fun isAlreadySignedHashesWarning(warning: WarningMessage): Boolean {
            return warning.messageResId == R.string.alert_card_signed_transactions
        }

        fun onlineVerificationFailed(): WarningMessage = WarningMessage(
            "",
            "",
            type = WarningMessage.Type.Permanent,
            priority = WarningMessage.Priority.Critical,
            listOf(WarningMessage.Location.MainScreen),
            null,
            R.string.warning_failed_to_verify_card_title,
            R.string.warning_failed_to_verify_card_message,
            WarningMessage.Origin.Local,
        )

        fun remainingSignaturesNotEnough(remainingSignatures: Int): WarningMessage = WarningMessage(
            title = "",
            message = "",
            type = WarningMessage.Type.Permanent,
            priority = WarningMessage.Priority.Critical,
            listOf(WarningMessage.Location.MainScreen),
            blockchains = null,
            titleResId = R.string.common_warning,
            messageResId = R.string.warning_low_signatures_format,
            origin = WarningMessage.Origin.Local,
            messageFormatArg = remainingSignatures.toString(),
        )

        fun testCardWarning(): WarningMessage = WarningMessage(
            "",
            "",
            type = WarningMessage.Type.TestCard,
            priority = WarningMessage.Priority.Critical,
            listOf(WarningMessage.Location.MainScreen, WarningMessage.Location.SendScreen),
            null,
            R.string.common_warning,
            R.string.warning_testnet_card_message,
            WarningMessage.Origin.Local,
        )

        fun demoCardWarning(): WarningMessage = WarningMessage(
            "",
            "",
            type = WarningMessage.Type.Permanent,
            priority = WarningMessage.Priority.Critical,
            listOf(WarningMessage.Location.MainScreen),
            null,
            R.string.common_warning,
            R.string.alert_demo_message,
            WarningMessage.Origin.Local,
        )

        const val REMAINING_SIGNATURES_WARNING = 10
    }
}
