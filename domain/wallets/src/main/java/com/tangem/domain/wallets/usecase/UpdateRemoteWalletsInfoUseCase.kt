package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.notifications.models.ApplicationId
import com.tangem.domain.wallets.delegate.UserWalletsSyncDelegate
import com.tangem.domain.wallets.repository.WalletsRepository

class UpdateRemoteWalletsInfoUseCase(
    private val walletsRepository: WalletsRepository,
    private val userWalletsSyncDelegate: UserWalletsSyncDelegate,
) {

    suspend operator fun invoke(applicationId: ApplicationId): Either<Throwable, Unit> = Either.catch {
        val walletsInfo = walletsRepository.getWalletsInfo(applicationId.value)
        userWalletsSyncDelegate.syncWallets(walletsInfo)
    }
}