package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.notifications.models.ApplicationId
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.repository.WalletsRepository

class AssociateWalletsWithApplicationIdUseCase(
    private val userWalletsListManager: UserWalletsListManager,
    private val walletsRepository: WalletsRepository,
) {

    suspend operator fun invoke(applicationId: ApplicationId): Either<Throwable, Unit> = Either.catch {
        val wallets = userWalletsListManager.userWalletsSync
        walletsRepository.associateWallets(applicationId.value, wallets)
    }
}