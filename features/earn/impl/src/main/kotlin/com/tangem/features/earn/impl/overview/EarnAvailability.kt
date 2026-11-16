package com.tangem.features.earn.impl.overview

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.features.earn.api.EarnFeatureToggles
import com.tangem.lib.crypto.BlockchainUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class EarnAvailability @Inject constructor(
    private val toggles: EarnFeatureToggles,
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    /**
     * Wallet type and lock state only. The Earn toggle is deliberately absent: a switched-off toggle must not empty
     * the overview together with the Staking and Yield Mode badges. Blocking, unlocking and removing a wallet switch
     * the value without a restart.
     */
    fun isWalletAvailable(userWalletId: UserWalletId): Flow<Boolean> = userWalletsListRepository.userWallets
        .map { wallets -> isWalletEligible(wallets, userWalletId) }
        // A rename or a reorder of wallets must not resubscribe the overview supplier
        .distinctUntilChanged()

    /** For entries into the Earn UI that bypass the overview supplier, such as a deeplink: toggle, currency, wallet. */
    fun isEarnAvailable(userWalletId: UserWalletId, currency: CryptoCurrency): Boolean = toggles.isEarnEnabled &&
        currency is CryptoCurrency.Coin &&
        BlockchainUtils.isEthereum(currency.network.rawId) &&
        // isEthereum accepts "ethereum/test" too, while Earn products exist only in mainnet
        !currency.network.isTestnet &&
        isWalletEligible(userWalletsListRepository.userWallets.value, userWalletId)

    private fun isWalletEligible(wallets: List<UserWallet>?, userWalletId: UserWalletId): Boolean {
        val wallet = wallets?.firstOrNull { it.walletId == userWalletId } ?: return false
        return wallet.isMultiCurrency && !wallet.isLocked
    }
}