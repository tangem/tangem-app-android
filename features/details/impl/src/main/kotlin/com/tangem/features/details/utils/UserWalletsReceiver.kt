package com.tangem.features.details.utils

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.error.SelectedAppCurrencyError
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.utils.getOrElse
import com.tangem.domain.tokens.GetWalletTotalBalanceUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.TotalFiatBalance
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.details.entity.UserWalletListUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transform
import timber.log.Timber
import javax.inject.Inject

@ComponentScoped
internal class UserWalletsReceiver @Inject constructor(
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getWalletTotalBalanceUseCase: GetWalletTotalBalanceUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val messageSender: UiMessageSender,
) {

    val userWallets = getWalletsUseCase()
        .distinctUntilChanged()
        .transform { wallets ->
            if (wallets.isEmpty()) {
                error("User wallets must not be empty")
            } else {
                emit(wallets.toUiModels(onClick = ::navigateToWalletSettings))
            }

            combine(
                getSelectedAppCurrencyUseCase(),
                getWalletTotalBalanceUseCase(wallets.map(UserWallet::walletId)),
            ) { maybeAppCurrency, maybeBalances ->
                val models = createUiModels(wallets, maybeAppCurrency, maybeBalances)

                if (models != null) {
                    emit(models)
                }
            }.collect()
        }

    private fun createUiModels(
        wallets: List<UserWallet>,
        maybeAppCurrency: Either<SelectedAppCurrencyError, AppCurrency>,
        maybeBalances: Lce<TokenListError, Map<UserWalletId, TotalFiatBalance>>,
    ): ImmutableList<UserWalletListUM.UserWalletUM>? {
        val balances = maybeBalances.getOrElse(
            ifLoading = { return null },
            ifError = {
                Timber.w("Failed to get user wallets balances: $it")
                return null
            },
        )
        val appCurrency = maybeAppCurrency.getOrElse {
            Timber.w("Failed to get selected app currency: $it")
            return null
        }

        return wallets.toUiModels(
            appCurrency = appCurrency,
            balances = balances,
            onClick = ::navigateToWalletSettings,
        )
    }

    private fun navigateToWalletSettings(userWalletId: UserWalletId) {
        val message = stringReference("Wallet settings have not yet been implemented: $userWalletId")
        messageSender.send(SnackbarMessage(message))
    }
}
