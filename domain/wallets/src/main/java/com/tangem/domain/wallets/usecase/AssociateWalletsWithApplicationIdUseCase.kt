package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.notifications.models.ApplicationId
import com.tangem.domain.wallets.repository.WalletsRepository

class AssociateWalletsWithApplicationIdUseCase(
    private val walletsRepository: WalletsRepository,
) {

    suspend operator fun invoke(applicationId: ApplicationId, wallets: List<UserWallet>): Either<Throwable, Unit> =
        Either.catch {
            walletsRepository.associateWallets(applicationId.value, wallets)
        }
}