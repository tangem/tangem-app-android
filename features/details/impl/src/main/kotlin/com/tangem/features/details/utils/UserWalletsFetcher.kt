package com.tangem.features.details.utils

import arrow.core.Either
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.error.SelectedAppCurrencyError
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.lce.lce
import com.tangem.domain.core.utils.getOrElse
import com.tangem.domain.core.utils.toLce
import com.tangem.domain.tokens.GetWalletTotalBalanceUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.TotalFiatBalance
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.details.entity.UserWalletListUM.UserWalletUM
import com.tangem.features.details.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ComponentScoped
internal class UserWalletsFetcher @Inject constructor(
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getWalletTotalBalanceUseCase: GetWalletTotalBalanceUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val messageSender: UiMessageSender,
) {

    val userWallets: Flow<ImmutableList<UserWalletUM>> = getWalletsUseCase()
        .distinctUntilChanged()
        .transform { wallets ->
            if (wallets.isEmpty()) {
                error("Wallets must not be empty")
            } else {
                emit(wallets.toUiModels(onClick = ::navigateToWalletSettings))
            }

            combine(
                getSelectedAppCurrencyUseCase(),
                getWalletTotalBalanceUseCase(wallets.map(UserWallet::walletId)),
            ) { maybeAppCurrency, maybeBalances ->
                val models = createUiModels(wallets, maybeAppCurrency, maybeBalances).getOrElse(
                    ifLoading = { return@combine },
                    ifError = {
                        val message = resourceReference(R.string.common_unknown_error)
                        messageSender.send(SnackbarMessage(message))

                        return@combine
                    },
                )

                emit(models)
            }.collect()
        }

    private fun createUiModels(
        wallets: List<UserWallet>,
        maybeAppCurrency: Either<SelectedAppCurrencyError, AppCurrency>,
        maybeBalances: Lce<TokenListError, Map<UserWalletId, TotalFiatBalance>>,
    ): Lce<Error, ImmutableList<UserWalletUM>> = lce {
        val balances = withError(
            transform = { Error.UnableToGetBalances },
            block = { maybeBalances.bind() },
        )
        val appCurrency = withError(
            transform = { Error.UnableToGetAppCurrency },
            block = { maybeAppCurrency.toLce().bind() },
        )

        wallets.toUiModels(
            appCurrency = appCurrency,
            balances = balances,
            onClick = ::navigateToWalletSettings,
        )
    }

    private fun navigateToWalletSettings(userWalletId: UserWalletId) {
        val message = stringReference("Wallet settings have not yet been implemented: $userWalletId")
        messageSender.send(SnackbarMessage(message))
    }

    sealed class Error {

        data object UnableToGetAppCurrency : Error()

        data object UnableToGetBalances : Error()
    }
}