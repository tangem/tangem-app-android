package com.tangem.data.managetokens.utils

import com.tangem.blockchain.blockchains.cardano.CardanoTokenAddressConverter
import com.tangem.blockchain.blockchains.hedera.HederaTokenAddressConverter
import com.tangem.blockchain.blockchains.sui.SuiTokenAddressConverter
import com.tangem.blockchain.common.Blockchain
import com.tangem.data.common.currency.getBlockchain
import com.tangem.domain.tokens.model.Network

internal class TokenAddressesConverter {
    private val hederaTokenAddressConverter = HederaTokenAddressConverter()
    private val cardanoTokenAddressConverter = CardanoTokenAddressConverter()
    private val suiTokenAddressConverter = SuiTokenAddressConverter()

    fun convertTokenAddress(networkId: Network.ID, contractAddress: String, symbol: String?): String {
        val convertedAddress = when (getBlockchain(networkId)) {
            Blockchain.Hedera,
            Blockchain.HederaTestnet,
            -> hederaTokenAddressConverter.convertToTokenId(contractAddress)
            Blockchain.Sui,
            Blockchain.SuiTestnet,
            -> suiTokenAddressConverter.normalizeAddress(contractAddress)
            Blockchain.Cardano -> {
                // TODO: [REDACTED_JIRA]
                cardanoTokenAddressConverter.convertToFingerprint(contractAddress, symbol)
            }
            else -> contractAddress
        }

        return requireNotNull(convertedAddress) {
            "Token contract address is invalid"
        }
    }
}