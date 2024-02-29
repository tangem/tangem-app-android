package com.tangem.tap.common.feedback

import com.tangem.tap.common.extensions.breakLine

class FeedbackDataBuilder(
    private val infoHolder: AdditionalFeedbackInfo,
) {
    val builder = StringBuilder()

    fun appendDelimiter(): FeedbackDataBuilder {
        builder.appendDelimiter()
        return this
    }

    fun breakLine(count: Int = 1): FeedbackDataBuilder {
        builder.breakLine(count)
        return this
    }

    fun appendCardInfo(): FeedbackDataBuilder {
        builder.appendKeyValue("Card ID", infoHolder.cardId)
        builder.appendKeyValue("Firmware version", infoHolder.cardFirmwareVersion)
        builder.appendKeyValue("Card Blockchain", infoHolder.cardBlockchain)
        builder.appendKeyValue("", infoHolder.signedHashesCount)
        builder.appendKeyValue("User Wallet ID", infoHolder.userWalletId)
        return this
    }

    fun appendWalletsInfo(): FeedbackDataBuilder {
        infoHolder.walletsInfo.forEach { walletInfo ->
            builder.appendDelimiter()
            builder.appendKeyValue("Blockchain", walletInfo.blockchain.fullName)
            builder.appendKeyValue("Derivation path", walletInfo.derivationPath)

            // enable later
            // if (walletInfo.blockchain == Blockchain.Bitcoin) {
            //     builder.appendKeyValue("XPUB", infoHolder.extendedPublicKey)
            // }

            builder.appendKeyValue("Outputs count", walletInfo.outputsCount)

            if (walletInfo.tokens.isNotEmpty()) {
                builder.append("Tokens:")
                breakLine()
                walletInfo.tokens.forEach { token ->
                    builder.appendKeyValue("ID", token.id ?: "[custom token]")
                    builder.appendKeyValue("Name", token.name)
                    builder.appendKeyValue("Contract address", token.contractAddress)
                }
            }

            builder.appendKeyValue("Host", walletInfo.host)
            builder.appendKeyValue("Wallet address", walletInfo.addresses)
            builder.appendKeyValue("Explorer link", walletInfo.explorerLink)
        }

        return this
    }

    fun appendTxFailedBlockchainInfo(error: String): FeedbackDataBuilder {
        val walletInfo = infoHolder.onSendErrorWalletInfo ?: AdditionalFeedbackInfo.EmailWalletInfo()
        builder.appendKeyValue("Blockchain", walletInfo.blockchain.fullName)
        builder.appendKeyValue("Derivation path", walletInfo.derivationPath)
        builder.appendKeyValue("Host", walletInfo.host)
        builder.appendKeyValue("Token", infoHolder.token)
        builder.appendKeyValue("Error", error)
        builder.appendDelimiter()
        builder.appendKeyValue("Source address", walletInfo.addresses)
        builder.appendKeyValue("Destination address", infoHolder.destinationAddress)
        builder.appendKeyValue("Amount", infoHolder.amount)
        builder.appendKeyValue("Fee", infoHolder.fee)
        return this
    }

    fun appendPhoneInfo(): FeedbackDataBuilder {
        builder.appendKeyValue("Phone model", infoHolder.phoneModel)
        builder.appendKeyValue("OS version", infoHolder.osVersion)
        builder.appendKeyValue("App version", infoHolder.appVersion)
        return this
    }

    fun build(): String = builder.toString()
}

private fun StringBuilder.appendKeyValue(key: String, value: String?): StringBuilder = when {
    value.isNullOrBlank() -> this
    key.isBlank() -> this.append("$value\n")
    else -> this.append("$key: $value\n")
}

private fun StringBuilder.appendDelimiter(): StringBuilder = append("----------\n")
