package com.tangem.domain.wallets.usecase

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger

/**
 * A save can fail after the wallet got listed (e.g. setting its lock), so a listed wallet keeps its keys.
 *
 * @return true when the wallet is listed and its entry was kept
 */
class DiscardUnsavedHotWalletUseCase(
    private val tangemHotSdk: TangemHotSdk,
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    suspend operator fun invoke(hotWalletId: HotWalletId): Boolean {
        val isListed = userWalletsListRepository.userWallets.value.orEmpty()
            .any { it is UserWallet.Hot && it.hotWalletId == hotWalletId }
        if (isListed) return true

        runSuspendCatching { tangemHotSdk.delete(hotWalletId) }
            .onFailure { TangemLogger.e("Failed to delete an unsaved wallet from the Hot SDK", it) }
        return false
    }
}