package com.tangem.domain.feedback

import com.tangem.domain.feedback.models.*
import com.tangem.domain.feedback.utils.breakLine
import com.tangem.domain.visa.model.VisaTxDetails
import com.tangem.domain.feedback.models.BlockchainInfo.Addresses as BlockchainAddresses

internal class FeedbackDataBuilder {

    private val builder = StringBuilder()

    fun addVisaTxInfo(txDetails: VisaTxDetails) {
        builder.appendKeyValue("Type", txDetails.type)
        builder.appendKeyValue("Status", txDetails.status)
        builder.appendKeyValue("Blockchain amount", txDetails.blockchainAmount.toString())
        builder.appendKeyValue("Transaction amount", txDetails.transactionAmount.toString())
        builder.appendKeyValue("Currency code", txDetails.transactionCurrencyCode.toString())
        builder.appendKeyValue("Merchant name", txDetails.merchantName)
        builder.appendKeyValue("Merchant city", txDetails.merchantCity)
        builder.appendKeyValue("Merchant country code", txDetails.merchantCountryCode)
        builder.appendKeyValue("Merchant category code", txDetails.merchantCategoryCode)

        builder.appendDelimiter()
        builder.breakLine()
        builder.append("Requests:")

        txDetails.requests.forEach { request ->
            builder.appendKeyValue("Type", request.requestType)
            builder.appendKeyValue("Status", request.requestStatus)
            builder.appendKeyValue("Blockchain amount", request.blockchainAmount.toString())
            builder.appendKeyValue("Transaction amount", request.transactionAmount.toString())
            builder.appendKeyValue("Currency code", request.txCurrencyCode.toString())
            builder.appendKeyValue("Error code", request.errorCode.toString())
            builder.appendKeyValue("Date", request.requestDate.toString())
            builder.appendKeyValue("Transaction hash", request.txHash)
            builder.appendKeyValue("Transaction status", request.txStatus)
            builder.appendDelimiter()
            builder.breakLine()
        }
    }

    fun addUserWalletsInfo(userWalletsInfo: UserWalletsInfo) {
        builder.appendKeyValue("User Wallet ID", userWalletsInfo.selectedUserWalletId)
        builder.appendKeyValue("Total saved wallets", userWalletsInfo.totalUserWallets.toString())
    }

    fun addUserWalletMetaInfo(walletMetaInfo: WalletMetaInfo) {
        builder.appendKeyValue("Mobile Wallet is backed up", walletMetaInfo.hotWalletIsBackedUp?.toString())
        builder.appendKeyValue("Card ID", walletMetaInfo.cardId)
        builder.appendKeyValue("Firmware version", walletMetaInfo.firmwareVersion)
        builder.appendKeyValue("Linked cards count", walletMetaInfo.cardsCount)
        builder.appendKeyValue("Has seed phrase", walletMetaInfo.isImported?.toString())
        builder.appendKeyValue("Card Blockchain", walletMetaInfo.cardBlockchain)
        walletMetaInfo.signedHashesList?.let { builder.appendSignedHashes(it) }
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
                    addTokenShortInfo(id = token.id, name = token.name)
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

    fun addStakingInfo(validatorName: String?, transactionTypes: List<String>, unsignedTransactions: List<String?>) {
        builder.appendKeyValue("Validator", validatorName ?: "unknown")
        builder.appendKeyValue("Action", transactionTypes.joinToString(separator = "\n").ifEmpty { "unknown" })
        builder.appendKeyValue(
            key = "Unsigned transaction",
            value = unsignedTransactions.joinToString(separator = "\n") { it ?: "unknown" }.ifEmpty { "unknown" },
        )
    }

    fun addSwapInfo(providerName: String, txId: String) {
        builder.appendKeyValue("Provider", providerName)
        builder.appendKeyValue("Transaction ID", txId)
    }

    fun addTokenShortInfo(id: String?, name: String) {
        builder.appendKeyValue("Token ID", id ?: "[custom token]")
        builder.appendKeyValue("Name", name)
    }

    fun addDelimiter(): StringBuilder = builder.appendDelimiter()

    fun build(): String = builder.trimEnd().toString()

    private fun StringBuilder.appendKeyValue(key: String, value: String?) {
        if (value.isNullOrBlank()) return

        val keyValuePrefix = if (key.isBlank()) "" else "$key: "
        append("$keyValuePrefix$value\n")
    }

    private fun StringBuilder.appendSignedHashes(signedHashesList: List<WalletMetaInfo.SignedHashes>) {
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