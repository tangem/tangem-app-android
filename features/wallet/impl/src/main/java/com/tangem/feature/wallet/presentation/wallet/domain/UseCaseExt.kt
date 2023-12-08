package com.tangem.feature.wallet.presentation.wallet.domain

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.GetPrimaryCurrencyStatusUpdatesUseCase
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import kotlinx.coroutines.flow.*
import timber.log.Timber

internal fun GetSelectedWalletSyncUseCase.unwrap(): UserWallet? {
    return this().fold(
        ifLeft = {
            Timber.e("Impossible to get selected wallet $it")
            null
        },
        ifRight = { it },
    )
}

internal suspend fun GetPrimaryCurrencyStatusUpdatesUseCase.unwrap(userWalletId: UserWalletId): CryptoCurrencyStatus? {
    return this(userWalletId)
        .conflate()
        .distinctUntilChanged()
        .filter(Either<CurrencyStatusError, CryptoCurrencyStatus>::isRight)
        .firstOrNull()
        ?.fold(
            ifLeft = {
                Timber.e("Impossible to get primary currency status $it")
                null
            },
            ifRight = { it },
        )
}

internal suspend fun GetSelectedAppCurrencyUseCase.unwrap(): AppCurrency {
    return this()
        .map { maybeAppCurrency ->
            maybeAppCurrency.getOrElse { AppCurrency.Default }
        }
        .firstOrNull()
        ?: AppCurrency.Default
}

internal suspend fun GetPrimaryCurrencyStatusUpdatesUseCase.collectLatest(
    userWalletId: UserWalletId,
    onRight: suspend (CryptoCurrencyStatus) -> Unit,
) {
    this(userWalletId = userWalletId)
        .conflate()
        .distinctUntilChanged()
        .collectLatest { maybeStatus ->
            maybeStatus.onRight { onRight(it) }
        }
}