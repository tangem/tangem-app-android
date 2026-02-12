package com.tangem.data.walletconnect

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.walletconnect.repository.WalletConnectRepository

internal class DefaultWalletConnectRepository(
    private val userWalletsListRepository: UserWalletsListRepository,
) : WalletConnectRepository {

    override suspend fun checkIsAvailable(userWalletId: UserWalletId): Boolean {
        return userWalletsListRepository.getSyncStrict(userWalletId).isMultiCurrency
    }
}