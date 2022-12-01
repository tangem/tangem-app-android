package com.tangem.tap.domain.model

import com.tangem.blockchain.common.WalletManager
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.model.WalletStoreModel.WalletRent
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import java.math.BigDecimal

/**
 * Contains info about the blockchain and its currencies
 * @param userWalletId ID of the [UserWallet] which uses that store
 * @param blockchainNetwork Store's [BlockchainNetwork]
 * @param walletManager Store's [WalletManager], may be null if it fails to create this manager. TODO: Remove after
 * WalletMiddleware refactoring
 * @param walletsData List of [WalletDataModel] which represents store's blockchain currency and tokens currencies
 * @param walletRent Store's [WalletRent], null if store has no rent or currency balance is greater then
 * [WalletRent.exemptionAmount]
 * */
data class WalletStoreModel(
    val userWalletId: UserWalletId,
    val blockchainNetwork: BlockchainNetwork,
    @Deprecated("Don't use it, will be removed")
    val walletManager: WalletManager?,
    val walletsData: List<WalletDataModel>,
    val walletRent: WalletRent?,
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
}
