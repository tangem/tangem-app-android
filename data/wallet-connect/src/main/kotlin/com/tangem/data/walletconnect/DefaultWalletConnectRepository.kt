package com.tangem.data.walletconnect

import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.walletconnect.repository.WalletConnectRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultWalletConnectRepository(
    private val userWalletsStore: UserWalletsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : WalletConnectRepository {

    override suspend fun checkIsAvailable(userWalletId: UserWalletId): Boolean = withContext(dispatchers.io) {
        val userWallet = requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "User wallet with id $userWalletId not found"
        }

        userWallet.isMultiCurrency
    }
}