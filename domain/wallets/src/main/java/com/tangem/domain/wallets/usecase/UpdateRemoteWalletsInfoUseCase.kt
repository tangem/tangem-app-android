package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.notifications.models.ApplicationId
import com.tangem.domain.wallets.delegate.UserWalletsSyncDelegate
import com.tangem.domain.wallets.models.UpdateWalletError
import com.tangem.domain.wallets.repository.WalletsRepository

class UpdateRemoteWalletsInfoUseCase(
    private val walletsRepository: WalletsRepository,
    private val userWalletsSyncDelegate: UserWalletsSyncDelegate,
) {

    suspend operator fun invoke(applicationId: ApplicationId): Either<UpdateWalletError, Unit> = either {
        val walletsInfo = walletsRepository.getWalletsInfo(applicationId.value)
        userWalletsSyncDelegate.syncWallets(walletsInfo).bind()
    }
}