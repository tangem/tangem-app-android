package com.tangem.features.markets.portfolio.impl.loader

import arrow.core.getOrElse
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.tokens.GetAllWalletsCryptoCurrencyStatusesUseCase
import com.tangem.domain.tokens.GetWalletTotalBalanceUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TotalFiatBalance
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

/**
 * Loader of portfolio data
 *
 * @property getAllWalletsCryptoCurrencyStatusesUseCase use case for getting all wallets crypto currency statuses
 * @property getSelectedAppCurrencyUseCase              use case for getting selected app currency
 * @property getBalanceHidingSettingsUseCase            use case for getting balance hiding settings
 * @property getWalletTotalBalanceUseCase               use case for getting wallet total balance
 *
[REDACTED_AUTHOR]
 */
internal class PortfolioDataLoader @Inject constructor(
    private val getAllWalletsCryptoCurrencyStatusesUseCase: GetAllWalletsCryptoCurrencyStatusesUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getWalletTotalBalanceUseCase: GetWalletTotalBalanceUseCase,
) {

    /** Load data by [currencyRawId] */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun load(currencyRawId: String): Flow<PortfolioData> {
        return combine(
            flow = getAllWalletsCryptoCurrencyStatusesFlow(currencyRawId = currencyRawId),
            flow2 = getSelectedAppCurrencyFlow(),
            flow3 = getBalanceHidingSettingsFlow(),
        ) { walletsWithCurrencyStatuses, appCurrency, isBalanceHidden ->
            PortfolioData(
                walletsWithCurrencyStatuses = walletsWithCurrencyStatuses,
                appCurrency = appCurrency,
                isBalanceHidden = isBalanceHidden,
                walletsWithBalance = emptyMap(),
            )
        }
            // setup balances for wallets from walletsWithCurrencyStatuses
            .flatMapLatest { portfolioData ->
                getWalletsWithTotalBalanceFlow(
                    ids = portfolioData.walletsWithCurrencyStatuses.keys.map(UserWallet::walletId),
                )
                    .map { portfolioData.copy(walletsWithBalance = it) }
            }
    }

    private fun getAllWalletsCryptoCurrencyStatusesFlow(
        currencyRawId: String,
    ): Flow<Map<UserWallet, List<CryptoCurrencyStatus>>> {
        return getAllWalletsCryptoCurrencyStatusesUseCase(currencyRawId)
            .distinctUntilChanged()
            .map { walletsWithMaybeStatuses ->
                walletsWithMaybeStatuses.mapValues { entry ->
                    entry.value.mapNotNull { it.getOrNull() }
                }
            }
    }

    private fun getSelectedAppCurrencyFlow(): Flow<AppCurrency> {
        return getSelectedAppCurrencyUseCase()
            .map {
                it.getOrElse { e ->
                    Timber.e("Failed to load app currency: $e")
                    AppCurrency.Default
                }
            }
            .distinctUntilChanged()
    }

    private fun getBalanceHidingSettingsFlow(): Flow<Boolean> {
        return getBalanceHidingSettingsUseCase()
            .map { it.isBalanceHidden }
            .distinctUntilChanged()
    }

    private fun getWalletsWithTotalBalanceFlow(
        ids: List<UserWalletId>,
    ): Flow<Map<UserWalletId, Lce<TokenListError, TotalFiatBalance>>> {
        return combine(
            flows = ids
                .map { userWalletId ->
                    getWalletTotalBalanceUseCase(userWalletId)
                        .map { userWalletId to it }
                        .distinctUntilChanged()
                },
            transform = { it.toMap() },
        )
            .distinctUntilChanged()
            .onEmpty { ids.associateWith { Lce.Loading<TotalFiatBalance>(partialContent = null) } }
    }
}