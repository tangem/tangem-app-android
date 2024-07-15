package com.tangem.features.details.utils

import arrow.core.Either
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.error.SelectedAppCurrencyError
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ComponentScoped
internal class UserWalletsFetcher @Inject constructor(
    getWalletsUseCase: GetWalletsUseCase,
    private val getWalletTotalBalanceUseCase: GetWalletTotalBalanceUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val router: Router,
    private val messageSender: UiMessageSender,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val userWallets: Flow<ImmutableList<UserWalletUM>> = getWalletsUseCase().transformLatest { wallets ->
        emit(wallets.toUiModels(onClick = ::navigateToWalletSettings))

        combine(
            getSelectedAppCurrencyUseCase().distinctUntilChanged(),
            getBalanceHidingSettingsUseCase().distinctUntilChanged(),
            getWalletTotalBalanceUseCase(wallets.map(UserWallet::walletId)).distinctUntilChanged(),
        ) { maybeAppCurrency, balanceHidingSettings, maybeBalances ->
            val models = createUiModels(
                wallets = wallets,
                maybeAppCurrency = maybeAppCurrency,
                maybeBalances = maybeBalances,
                balanceHidingSettings = balanceHidingSettings,
            ).getOrElse(
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
        balanceHidingSettings: BalanceHidingSettings,
    ): Lce<Error, ImmutableList<UserWalletUM>> = lce {
        val balances = withError(
            transform = { Error.UnableToGetBalances },
            block = { maybeBalances.bindOrNull().orEmpty() },
        )
        val appCurrency = withError(
            transform = { Error.UnableToGetAppCurrency },
            block = { maybeAppCurrency.toLce().bind() },
        )

        wallets.toUiModels(
            appCurrency = appCurrency,
            balances = balances,
            onClick = ::navigateToWalletSettings,
            isBalancesHidden = balanceHidingSettings.isBalanceHidden,
            isLoading = maybeBalances.isLoading(),
        )
    }

    private fun navigateToWalletSettings(userWalletId: UserWalletId) {
        router.push(AppRoute.WalletSettings(userWalletId))
    }

    sealed class Error {

        data object UnableToGetAppCurrency : Error()

        data object UnableToGetBalances : Error()
    }
}