package com.tangem.domain.feedback

import com.tangem.domain.feedback.models.*
import com.tangem.domain.feedback.utils.breakLine
import com.tangem.domain.feedback.models.BlockchainInfo.Addresses as BlockchainAddresses

internal class FeedbackDataBuilder {

    private val builder = StringBuilder()

    fun addUserWalletsInfo(userWalletsInfo: UserWalletsInfo) {
        builder.appendKeyValue("User Wallet ID", userWalletsInfo.selectedUserWalletId)
        builder.appendKeyValue("Total saved wallets", userWalletsInfo.totalUserWallets.toString())
    }

    fun addCardInfo(cardInfo: CardInfo) {
        builder.appendKeyValue("Card ID", cardInfo.cardId)
        builder.appendKeyValue("Firmware version", cardInfo.firmwareVersion)
        builder.appendKeyValue("Linked cards count", cardInfo.cardsCount)
        builder.appendKeyValue("Has seed phrase", cardInfo.isImported.toString())
        builder.appendKeyValue("Card Blockchain", cardInfo.cardBlockchain)
        builder.appendSignedHashes(cardInfo.signedHashesList)
    }

    fun addBlockchainInfoList(blockchainInfoList: List<BlockchainInfo>) {
        blockchainInfoList.forEachBlockchain { isLastIndex ->
            builder.appendKeyValue("Blockchain", blockchain)
            builder.appendKeyValue("Derivation path", derivationPath)

            // enable later
            // if (walletInfo.blockchain == Blockchain.Bitcoin) {
            //     builder.appendKeyValue("XPUB", infoHolder.extendedPublicKey)
            // }

            builder.appendKeyValue("Outputs count", outputsCount)

            if (tokens.isNotEmpty()) {
                builder.breakLine()
                tokens.forEach { token ->
                    builder.appendKeyValue("Token ID", token.id ?: "[custom token]")
                    builder.appendKeyValue("Name", token.name)
                    builder.appendKeyValue("Contract address", token.contractAddress)
                    builder.appendKeyValue("Decimals", token.decimals)

                    builder.breakLine()
                }
            }

            builder.appendKeyValue("Host", host)

            builder.appendAddresses(
                key = "Wallet address${addresses.isMultiple(suffix = "es")}",
                addresses = addresses,
            )

            builder.appendAddresses(
                key = "Explorer link${explorerLinks.isMultiple(suffix = "s")}",
                addresses = explorerLinks,
            )

            if (!isLastIndex) builder.appendDelimiter()
        }
    }

    fun addPhoneInfo(phoneInfo: PhoneInfo) {
        builder.appendKeyValue("Phone model", phoneInfo.phoneModel)
        builder.appendKeyValue("OS version", phoneInfo.osVersion)
        builder.appendKeyValue("App version", phoneInfo.appVersion)
    }

    fun addBlockchainError(info: BlockchainInfo, error: BlockchainErrorInfo) {
        builder.appendKeyValue("Blockchain", info.blockchain)
        builder.appendKeyValue("Derivation path", info.derivationPath)
        builder.appendKeyValue("Host", info.host)
        builder.appendKeyValue("Token", error.tokenSymbol)
        builder.appendKeyValue("Error", error.errorMessage)

        builder.appendDelimiter()

        builder.appendAddresses(
            key = "Source address${info.addresses.isMultiple(suffix = "es")}",
            addresses = info.addresses,
        )
        builder.appendKeyValue("Destination address", error.destinationAddress)
        builder.appendKeyValue("Amount", error.amount)
        builder.appendKeyValue("Fee", error.fee ?: "Unable to receive")
    }

    fun addDelimiter(): StringBuilder = builder.appendDelimiter()

    fun build(): String = builder.trimEnd().toString()

    private fun StringBuilder.appendKeyValue(key: String, value: String?) {
        if (value.isNullOrBlank()) return

        val keyValuePrefix = if (key.isBlank()) "" else "$key: "
        append("$keyValuePrefix$value\n")
    }

    private fun StringBuilder.appendSignedHashes(signedHashesList: List<CardInfo.SignedHashes>) {
        signedHashesList.forEach {
            appendKeyValue("Signed hashes [${it.curve}]", it.total)
        }
    }

    private fun StringBuilder.appendAddresses(key: String, addresses: BlockchainAddresses) {
        appendKeyValue(
            key = key,
            value = when (addresses) {
                is BlockchainInfo.Addresses.Multiple -> {
                    addresses.values.joinToString(separator = "\n", prefix = "\n") {
                        "${it.type} — ${it.value}"
                    }
                }
                is BlockchainInfo.Addresses.Single -> addresses.value
            },
        )
    }

    private fun BlockchainAddresses.isMultiple(suffix: String): String {
        return if (this is BlockchainAddresses.Multiple) suffix else ""
    }

    private fun StringBuilder.appendDelimiter(): StringBuilder = append("----------\n")

    private inline fun List<BlockchainInfo>.forEachBlockchain(action: BlockchainInfo.(Boolean) -> Unit) {
        return forEachIndexed { index, info -> info.action(index == lastIndex) }
    }
}
