package com.tangem.domain.wallets.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository
import kotlinx.coroutines.flow.Flow

class GetWalletNotificationsEnabledUseCase(
    private val walletsRepository: WalletsRepository,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<Boolean> = walletsRepository
        .notificationsEnabledStatus(userWalletId)
}