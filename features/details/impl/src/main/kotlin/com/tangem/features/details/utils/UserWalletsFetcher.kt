package com.tangem.features.details.utils

import arrow.core.Either
import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.userwallet.converter.UserWalletItemUMConverter
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.decompose.di.ModelScoped
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
import com.tangem.features.details.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ModelScoped
internal class UserWalletsFetcher @Inject constructor(
    getWalletsUseCase: GetWalletsUseCase,
    private val getWalletTotalBalanceUseCase: GetWalletTotalBalanceUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val router: Router,
    private val messageSender: UiMessageSender,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val userWallets: Flow<ImmutableList<UserWalletItemUM>> = getWalletsUseCase().transformLatest { wallets ->
        val uiModels = UserWalletItemUMConverter(onClick = ::navigateToWalletSettings).convertList(wallets)
            .toImmutableList()

        emit(uiModels)

        combine(
            flow = getSelectedAppCurrencyUseCase().distinctUntilChanged(),
            flow2 = getBalanceHidingSettingsUseCase().distinctUntilChanged(),
            flow3 = getWalletTotalBalanceUseCase(wallets.map(UserWallet::walletId)).distinctUntilChanged(),
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
    ): Lce<Error, ImmutableList<UserWalletItemUM>> = lce {
        val balances = withError(
            transform = { Error.UnableToGetBalances },
            block = {
                maybeBalances.bindOrNull().orEmpty()
                    .filterKeys { userWalletId -> wallets.any { it.walletId == userWalletId } }
                    .mapKeys { entry -> wallets.first { it.walletId == entry.key } }
            },
        )

        val appCurrency = withError(
            transform = { Error.UnableToGetAppCurrency },
            block = { maybeAppCurrency.toLce().bind() },
        )

        balances
            .map { (userWallet, balance) ->
                UserWalletItemUMConverter(
                    onClick = ::navigateToWalletSettings,
                    appCurrency = appCurrency,
                    balance = balance,
                    isBalanceHidden = balanceHidingSettings.isBalanceHidden,
                )
                    .convert(userWallet)
            }
            .toImmutableList()
    }

    private fun navigateToWalletSettings(userWalletId: UserWalletId) {
        router.push(AppRoute.WalletSettings(userWalletId))
    }

    sealed class Error {

        data object UnableToGetAppCurrency : Error()

        data object UnableToGetBalances : Error()
    }
}