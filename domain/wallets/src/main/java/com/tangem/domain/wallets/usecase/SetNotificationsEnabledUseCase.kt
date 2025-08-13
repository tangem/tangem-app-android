package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository

class SetNotificationsEnabledUseCase(
    private val walletsRepository: WalletsRepository,
    private val currenciesRepository: CurrenciesRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, isEnabled: Boolean): Either<Throwable, Unit> =
        Either.catch {
            walletsRepository.setNotificationsEnabled(
                userWalletId = userWalletId,
                isEnabled = isEnabled,
            )
            currenciesRepository.syncTokens(userWalletId)
        }.onLeft {
            walletsRepository.setNotificationsEnabled(
                userWalletId = userWalletId,
                isEnabled = !isEnabled,
            )
        }
}