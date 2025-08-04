package com.tangem.features.markets.portfolio.impl.loader

import arrow.core.getOrElse
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.GetAllWalletsCryptoCurrencyStatusesUseCase
import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.domain.tokens.GetWalletTotalBalanceUseCase
import com.tangem.domain.tokens.error.TokenListError
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
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase,
) {

    /** Load data by [currencyRawId] */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun load(currencyRawId: CryptoCurrency.RawID): Flow<PortfolioData> {
        return combine(
            flow = getAllWalletsCryptoCurrenciesData(currencyRawId = currencyRawId),
            flow2 = getSelectedAppCurrencyFlow(),
            flow3 = getBalanceHidingSettingsFlow(),
        ) { walletsWithCurrencies, appCurrency, isBalanceHidden ->
            PortfolioData(
                walletsWithCurrencies = walletsWithCurrencies,
                appCurrency = appCurrency,
                isBalanceHidden = isBalanceHidden,
                walletsWithBalance = emptyMap(),
            )
        }
            // setup balances for wallets from walletsWithCurrencyStatuses
            .flatMapLatest { portfolioData ->
                getWalletsWithTotalBalanceFlow(
                    ids = portfolioData.walletsWithCurrencies.keys.map(UserWallet::walletId),
                )
                    .map { portfolioData.copy(walletsWithBalance = it) }
                    .onEmpty { emit(portfolioData) }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getAllWalletsCryptoCurrenciesData(
        currencyRawId: CryptoCurrency.RawID,
    ): Flow<Map<UserWallet, List<PortfolioData.CryptoCurrencyData>>> {
        return getAllWalletsCryptoCurrencyStatusesUseCase(currencyRawId)
            .distinctUntilChanged()
            .map { walletsWithMaybeStatuses ->
                walletsWithMaybeStatuses.mapValues { entry ->
                    entry.value.mapNotNull { it.getOrNull() }
                }
            }
            .flatMapLatest { walletsWithStatuses ->
                val actionsFlows = walletsWithStatuses.flatMap { (wallet, statuses) ->
                    statuses.map { status ->
                        getCryptoCurrencyActionsUseCase(wallet, status)
                            .map {
                                PortfolioData.CryptoCurrencyData(
                                    userWallet = wallet,
                                    status = status,
                                    actions = it.states,
                                )
                            }
                    }
                }

                combine(actionsFlows) { actions ->
                    walletsWithStatuses.mapValues { entry ->
                        entry.value.mapNotNull { status ->
                            actions.firstOrNull {
                                it.userWallet == entry.key && it.status == status
                            }
                        }
                    }
                }.onEmpty {
                    emit(
                        walletsWithStatuses.mapValues { (wallet, statuses) ->
                            statuses.map {
                                PortfolioData.CryptoCurrencyData(
                                    userWallet = wallet,
                                    status = it,
                                    actions = emptyList(),
                                )
                            }
                        },
                    )
                }
            }.onEmpty {
                emit(emptyMap())
            }
            .distinctUntilChanged()
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