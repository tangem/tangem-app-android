package com.tangem.data.pay.util

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import javax.inject.Inject

// TODO remove after implement wallet selector in pay
class TangemPayWalletsManager @Inject constructor(
    private val manager: UserWalletsListManager,
    private val repository: UserWalletsListRepository,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
) {

    @Deprecated("Don't use and put userWallet in features that need it")
    suspend fun getDefaultWalletForTangemPay(): UserWallet.Cold {
        val userWalletsFlow = if (useNewRepository()) repository.userWallets else manager.userWallets
        val userWallets = userWalletsFlow.filter { !it.isNullOrEmpty() }.first()
        return findColdWallet(userWallets)
    }

    @Deprecated("Don't use and put userWallet in features that need it")
    fun getDefaultWalletForTangemPayBlocking(): UserWallet.Cold {
        val userWallets = if (useNewRepository()) repository.userWallets.value else manager.userWalletsSync
        return findColdWallet(userWallets)
    }

    private fun useNewRepository(): Boolean = hotWalletFeatureToggles.isHotWalletEnabled

    private fun findColdWallet(userWallets: List<UserWallet>?): UserWallet.Cold {
        return userWallets?.find {
            it is UserWallet.Cold && it.isMultiCurrency
        } as? UserWallet.Cold
            ?: error("Cannot find cold user wallet")
    }
}