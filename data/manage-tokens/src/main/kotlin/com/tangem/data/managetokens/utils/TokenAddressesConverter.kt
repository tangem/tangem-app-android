package com.tangem.data.managetokens.utils

import com.tangem.blockchain.blockchains.cardano.CardanoTokenAddressConverter
import com.tangem.blockchain.blockchains.stellar.StellarTokenAddressConverter
import com.tangem.blockchain.blockchains.sui.SuiTokenAddressConverter
import com.tangem.blockchain.blockchains.xrp.XrpTokenAddressConverter
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.network.Network

internal class TokenAddressesConverter(
    private val hederaTokenAddressResolver: HederaTokenAddressResolver,
) {
    private val cardanoTokenAddressConverter = CardanoTokenAddressConverter()
    private val xrpTokenAddressConverter = XrpTokenAddressConverter()
    private val stellarTokenAddressConverter = StellarTokenAddressConverter()
    private val suiTokenAddressConverter = SuiTokenAddressConverter()

    suspend fun convertTokenAddress(networkId: Network.ID, contractAddress: String, symbol: String?): String {
        val convertedAddress = when (val blockchain = networkId.toBlockchain()) {
            Blockchain.Hedera,
            Blockchain.HederaTestnet,
            -> hederaTokenAddressResolver.resolveAddress(blockchain, contractAddress)
            Blockchain.Sui,
            Blockchain.SuiTestnet,
            -> suiTokenAddressConverter.normalizeAddress(contractAddress)
            Blockchain.Stellar,
            Blockchain.StellarTestnet,
            -> stellarTokenAddressConverter.normalizeAddress(contractAddress)
            Blockchain.XRP,
            -> xrpTokenAddressConverter.normalizeAddress(contractAddress)
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