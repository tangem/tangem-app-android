package com.tangem.tap.domain.configurable.warningMessage

import com.tangem.blockchain.common.Blockchain
import com.tangem.tangem_sdk_new.ui.animation.VoidCallback
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
class WarningMessagesManager(
        private val warningLoader: RemoteWarningLoader,
) {

    private var blockchain: Blockchain? = null
    private val warningsList: MutableList<WarningMessage> = mutableListOf()

    fun load(onComplete: VoidCallback? = null) {
        warningLoader.load { remoteList ->
            warningsList.clear()
            warningsList.addAll(remoteList)
            sortByPriority()
            onComplete?.invoke()
        }
    }

    fun setBlockchain(blockchain: Blockchain?) {
        this.blockchain = blockchain
    }

    fun addWarning(warning: WarningMessage) {
        if (findWarning(warning) == null) {
            warningsList.add(warning)
            sortByPriority()
        }
    }

    fun getWarnings(location: WarningMessage.Location): List<WarningMessage> {
        return warningsList
                .filter { !it.isHidden && it.location.contains(location) }
                .filter {
                    val blockchainList = it.blockchainList
                    when {
                        blockchainList == null -> true
                        blockchainList.contains(blockchain) -> true
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
        warningsList.removeIf { it.origin == origin }
        sortByPriority()
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
                R.string.alert_title,
                R.string.alert_developer_card,
                WarningMessage.Origin.Local
        )

        fun alreadySignedHashesWarning(): WarningMessage = WarningMessage(
                "",
                "",
                type = WarningMessage.Type.Temporary,
                priority = WarningMessage.Priority.Info,
                listOf(WarningMessage.Location.MainScreen),
                null,
                R.string.alert_title,
                R.string.alert_card_signed_transactions,
                WarningMessage.Origin.Local
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
                WarningMessage.Origin.Local
        )

        fun isAlreadySignedHashesWarning(warning: WarningMessage): Boolean {
            return warning.messageResId == R.string.alert_card_signed_transactions
        }
    }
}