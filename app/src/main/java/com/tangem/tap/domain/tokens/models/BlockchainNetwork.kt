package com.tangem.tap.domain.tokens.models

import com.squareup.moshi.JsonClass
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.extensions.calculateHashCode
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.TapWorkarounds.derivationStyle

@JsonClass(generateAdapter = true)
data class BlockchainNetwork(
    val blockchain: Blockchain,
    val derivationPath: String?,
    val tokens: List<Token>
) {

    constructor(
        blockchain: Blockchain,
        card: CardDTO,
    ) : this(
        blockchain = blockchain,
        derivationPath = if (card.settings.isHDWalletAllowed) blockchain.derivationPath(card.derivationStyle)?.rawPath else null,
        tokens = emptyList(),
    )

    fun updateTokens(tokens: List<Token>): BlockchainNetwork {
        return copy(
            tokens = (this.tokens + tokens).distinct(),
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BlockchainNetwork

        if (blockchain != other.blockchain) return false
        if (derivationPath != other.derivationPath) return false

        return true
    }

    override fun hashCode(): Int = calculateHashCode(
        blockchain.hashCode(), derivationPath?.hashCode() ?: 0
    )

    companion object {
        fun fromWalletManager(walletManager: WalletManager): BlockchainNetwork {
            return BlockchainNetwork(
                walletManager.wallet.blockchain,
                walletManager.wallet.publicKey.derivationPath?.rawPath,
                walletManager.cardTokens.toList()
            )
        }
    }
}
