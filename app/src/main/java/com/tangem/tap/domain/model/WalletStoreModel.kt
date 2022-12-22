package com.tangem.tap.domain.model

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.model.WalletStoreModel.WalletRent
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import java.math.BigDecimal

/**
 * Contains info about the blockchain and its currencies
 * @param userWalletId ID of the associated [UserWallet]
 * @param blockchain [Blockchain] of this WalletStore
 * @param derivationPath [DerivationPath] of this store, null if the card does not support the
 * [HD Wallet](https://coinsutra.com/hd-wallets-deterministic-wallet/)
 * @param walletsData List of [WalletDataModel] which represents store's blockchain currency and tokens currencies
 * @param walletRent [WalletRent], null if store has no rent or currency balance is greater then
 * [WalletRent.exemptionAmount]
 * @param blockchainNetwork [BlockchainNetwork].
 * TODO: Remove after WalletMiddleware refactoring
 * @param walletManager [WalletManager], may be null if it fails to create this manager.
 * TODO: Remove after WalletMiddleware refactoring
 * */
data class WalletStoreModel(
    val userWalletId: UserWalletId,
    val blockchain: Blockchain,
    val derivationPath: DerivationPath?,
    val walletsData: List<WalletDataModel>,
    val walletRent: WalletRent?,
    @Deprecated("Don't use it, will be removed")
    val blockchainNetwork: BlockchainNetwork,
    @Deprecated("Don't use it, will be removed")
    val walletManager: WalletManager?,
) {

    /**
     * Represents wallet blockchain rent
     * @param rent Amount that will be charged in overtime if the blockchain does not have an amount greater than
     * the [WalletRent.exemptionAmount]
     * @param exemptionAmount Amount that should be on the blockchain balance not to pay rent
     * */
    data class WalletRent(
        val rent: BigDecimal,
        val exemptionAmount: BigDecimal,
    )

    // TODO: Remove the generated methods after blockchainNetwork and walletManager are removed from the model
    // region Generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WalletStoreModel) return false

        if (userWalletId != other.userWalletId) return false
        if (blockchain != other.blockchain) return false
        if (derivationPath != other.derivationPath) return false
        if (walletsData != other.walletsData) return false
        if (walletRent != other.walletRent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userWalletId.hashCode()
        result = 31 * result + blockchain.hashCode()
        result = 31 * result + (derivationPath?.hashCode() ?: 0)
        result = 31 * result + walletsData.hashCode()
        result = 31 * result + (walletRent?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "WalletStoreModel(userWalletId=$userWalletId, blockchain=$blockchain, derivationPath=$derivationPath, " +
            "walletsData=$walletsData, walletRent=$walletRent)"
    }
    // endregion Generated
}
