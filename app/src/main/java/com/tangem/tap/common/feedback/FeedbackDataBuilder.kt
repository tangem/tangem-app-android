package com.tangem.tap.common.feedback

import com.tangem.tap.common.extensions.breakLine

class FeedbackDataBuilder(
    private val infoHolder: AdditionalFeedbackInfo
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
        builder.appendKeyValue("Signed hashes", infoHolder.signedHashesCount)
        return this
    }

    fun appendWalletsInfo(): FeedbackDataBuilder {
        infoHolder.walletsInfo.forEach {
            builder.appendDelimiter()
            builder.appendKeyValue("Blockchain", it.blockchain.fullName)
            builder.appendKeyValue("Host", it.host)
            builder.appendKeyValue("Wallet address", it.address)
            builder.appendKeyValue("Derivation path", it.derivationPath)
            builder.appendKeyValue("Explorer link", it.explorerLink)

            infoHolder.tokens[it.blockchain]?.let { tokens ->
                builder.append("Tokens:")
                breakLine()
                tokens.forEach { token ->
                    builder.appendKeyValue("Name", token.name)
                    builder.appendKeyValue("ID", token.id ?: "[custom token]")
                    builder.appendKeyValue("Contract address", token.contractAddress)
                }
            }
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
        builder.appendKeyValue("Source address", walletInfo.address)
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

private fun StringBuilder.appendKeyValue(key: String, value: String): StringBuilder {
    return if (value.isNotBlank()) this.append("$key: $value\n") else this
}

private fun StringBuilder.appendDelimiter(): StringBuilder = append("----------\n")