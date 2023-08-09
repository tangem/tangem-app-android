package com.tangem.tap.domain.configurable.warningMessage

import com.tangem.blockchain.common.Blockchain
import com.tangem.utils.extensions.removeBy
import com.tangem.wallet.R
import java.util.concurrent.CopyOnWriteArrayList

/**
[REDACTED_AUTHOR]
 */
class WarningMessagesManager {

    private val warningsList = CopyOnWriteArrayList<WarningMessage>()

    fun addWarning(warning: WarningMessage) {
        if (findWarning(warning) == null) {
            warningsList.add(warning)
            sortByPriority()
        }
    }

    fun getWarnings(location: WarningMessage.Location, blockchains: List<Blockchain>): List<WarningMessage> {
        return warningsList.filter { message ->
            val messageBlockchains = message.blockchainList
            val isCorrespondingMessageBlockchains = messageBlockchains == null ||
                messageBlockchains.any(blockchains::contains)
            val isCorrespondingMessageLocation = message.location.contains(location)

            !message.isHidden && isCorrespondingMessageLocation && isCorrespondingMessageBlockchains
        }
    }

    fun hideWarning(warning: WarningMessage): Boolean {
        val foundWarning = findWarning(warning) ?: return false
        val isCorrectType = foundWarning.type == WarningMessage.Type.Temporary ||
            foundWarning.type == WarningMessage.Type.AppRating

        return if (!foundWarning.isHidden && isCorrectType) {
            foundWarning.isHidden = true
            true
        } else {
            false
        }
    }

    fun removeWarnings(origin: WarningMessage.Origin) {
        warningsList.removeBy { it.origin == origin }
        sortByPriority()
    }

    fun removeWarnings(messageRes: Int) {
        warningsList.removeBy { it.messageResId == messageRes }
    }

    fun containsWarning(warning: WarningMessage) = warning in warningsList

    private fun sortByPriority() {
        warningsList.sortBy { it.priority.ordinal }
    }

    private fun findWarning(warning: WarningMessage): WarningMessage? {
        return warningsList.firstOrNull { it == warning }
    }

    companion object {
        const val REMAINING_SIGNATURES_WARNING = 10

        val devCardWarning = WarningMessage(
            title = "",
            message = "",
            type = WarningMessage.Type.Permanent,
            priority = WarningMessage.Priority.Critical,
            location = listOf(WarningMessage.Location.MainScreen),
            blockchains = null,
            titleResId = R.string.common_warning,
            messageResId = R.string.alert_developer_card,
            origin = WarningMessage.Origin.Local,
        )

        val alreadySignedHashesWarning = WarningMessage(
            title = "",
            message = "",
            type = WarningMessage.Type.Temporary,
            priority = WarningMessage.Priority.Info,
            location = listOf(WarningMessage.Location.MainScreen),
            blockchains = null,
            titleResId = R.string.common_warning,
            messageResId = R.string.alert_card_signed_transactions,
            origin = WarningMessage.Origin.Local,
        )

        val signedHashesMultiWalletWarning = WarningMessage(
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
            titleFormatArg = "\u26A0",
        )

        val appRatingWarning = WarningMessage(
            title = "",
            message = "",
            type = WarningMessage.Type.AppRating,
            priority = WarningMessage.Priority.Info,
            location = listOf(WarningMessage.Location.MainScreen),
            blockchains = null,
            titleResId = R.string.warning_rate_app_title,
            messageResId = R.string.warning_rate_app_message,
            origin = WarningMessage.Origin.Local,
        )

        val onlineVerificationFailed = WarningMessage(
            title = "",
            message = "",
            type = WarningMessage.Type.Permanent,
            priority = WarningMessage.Priority.Critical,
            location = listOf(WarningMessage.Location.MainScreen),
            blockchains = null,
            titleResId = R.string.warning_failed_to_verify_card_title,
            messageResId = R.string.warning_failed_to_verify_card_message,
            origin = WarningMessage.Origin.Local,
        )

        val testCardWarning = WarningMessage(
            title = "",
            message = "",
            type = WarningMessage.Type.TestCard,
            priority = WarningMessage.Priority.Critical,
            location = listOf(WarningMessage.Location.MainScreen, WarningMessage.Location.SendScreen),
            blockchains = null,
            titleResId = R.string.common_warning,
            messageResId = R.string.warning_testnet_card_message,
            origin = WarningMessage.Origin.Local,
        )

        val demoCardWarning = WarningMessage(
            title = "",
            message = "",
            type = WarningMessage.Type.Permanent,
            priority = WarningMessage.Priority.Critical,
            location = listOf(WarningMessage.Location.MainScreen),
            blockchains = null,
            titleResId = R.string.common_warning,
            messageResId = R.string.alert_demo_message,
            origin = WarningMessage.Origin.Local,
        )

        fun remainingSignaturesNotEnough(remainingSignatures: Int): WarningMessage {
            return WarningMessage(
                title = "",
                message = "",
                type = WarningMessage.Type.Permanent,
                priority = WarningMessage.Priority.Critical,
                location = listOf(WarningMessage.Location.MainScreen),
                blockchains = null,
                titleResId = R.string.common_warning,
                messageResId = R.string.warning_low_signatures_format,
                origin = WarningMessage.Origin.Local,
                messageFormatArg = remainingSignatures.toString(),
            )
        }

        fun isAlreadySignedHashesWarning(warning: WarningMessage): Boolean {
            return warning.messageResId == R.string.alert_card_signed_transactions
        }
    }
}