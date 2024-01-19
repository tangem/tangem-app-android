package com.tangem.tap.domain.configurable.warningMessage

import com.tangem.blockchain.common.Blockchain
import java.util.concurrent.CopyOnWriteArrayList

/**
[REDACTED_AUTHOR]
 */
// TODO: Delete with SendFeatureToggles
@Deprecated(message = "Used only in old send screen")
class WarningMessagesManager {

    private val warningsList = CopyOnWriteArrayList<WarningMessage>()

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

    private fun findWarning(warning: WarningMessage): WarningMessage? {
        return warningsList.firstOrNull { it == warning }
    }
}