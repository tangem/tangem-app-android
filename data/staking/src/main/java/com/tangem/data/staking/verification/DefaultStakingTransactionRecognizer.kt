package com.tangem.data.staking.verification

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.transaction.staking.StakingTransactionRecognizer as SdkStakingTransactionRecognizer
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.staking.verification.StakingTransactionRecognizer

/**
 * Adapts the app's StakeKit [NetworkType] to the blockchain SDK's per-network staking recognition.
 * The actual transaction parsing lives in the SDK ([SdkStakingTransactionRecognizer]); here we only
 * route a supported [NetworkType] to its [Blockchain]. Unsupported networks are not recognized.
 */
internal class DefaultStakingTransactionRecognizer : StakingTransactionRecognizer {

    override fun isRecognizedStakingTransaction(network: NetworkType, unsignedTransaction: String): Boolean {
        val blockchain = network.toBlockchainOrNull() ?: return false
        return SdkStakingTransactionRecognizer.isRecognizedStakingTransaction(blockchain, unsignedTransaction)
    }

    private fun NetworkType.toBlockchainOrNull(): Blockchain? = when (this) {
        NetworkType.TRON -> Blockchain.Tron
        NetworkType.COSMOS -> Blockchain.Cosmos
        NetworkType.CARDANO -> Blockchain.Cardano
        NetworkType.SOLANA -> Blockchain.Solana
        NetworkType.POLYGON -> Blockchain.Polygon
        NetworkType.BINANCE -> Blockchain.BSC
        else -> null
    }
}