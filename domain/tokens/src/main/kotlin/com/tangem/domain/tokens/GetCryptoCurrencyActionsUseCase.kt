package com.tangem.domain.tokens

import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.MarketCryptoCurrencyRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.isNullOrZero
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/**
 * Use case to determine which TokenActions are available for a [CryptoCurrency]
 *
 * @property rampManager Ramp manager to check ramp availability
 */
class GetCryptoCurrencyActionsUseCase(
    private val rampManager: RampStateManager,
    private val marketCryptoCurrencyRepository: MarketCryptoCurrencyRepository,
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(userWallet: UserWallet, cryptoCurrencyStatus: CryptoCurrencyStatus): Flow<TokenActionsState> {
        val operations = CurrenciesStatusesOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            userWalletId = userWallet.walletId,
        )
        val networkId = cryptoCurrencyStatus.currency.network.id

        return flow {
            val networkFlow = if (userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()) {
                operations.getNetworkCoinForSingleWalletWithTokenFlow(networkId)
            } else if (!userWallet.isMultiCurrency) {
                operations.getPrimaryCurrencyStatusFlow()
            } else {
                operations.getNetworkCoinFlow(networkId, cryptoCurrencyStatus.currency.network.derivationPath)
            }

            val flow = networkFlow.mapLatest { maybeCoinStatus ->
                createTokenActionsState(
                    userWallet = userWallet,
                    coinStatus = maybeCoinStatus.getOrNull(),
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                )
            }

            emitAll(flow)
        }.flowOn(dispatchers.io)
    }

    private suspend fun createTokenActionsState(
        userWallet: UserWallet,
        coinStatus: CryptoCurrencyStatus?,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): TokenActionsState {
        return TokenActionsState(
            walletId = userWallet.walletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            states = createListOfActions(
                userWallet,
                coinStatus,
                cryptoCurrencyStatus,
            ),
        )
    }

    /**
     * Creates list of action for expected order
     * Actions priority: [Buy Send Receive Sell Swap]
     */
    private suspend fun createListOfActions(
        userWallet: UserWallet,
        coinStatus: CryptoCurrencyStatus?,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): List<TokenActionsState.ActionState> {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.MissedDerivation) {
            return listOf(TokenActionsState.ActionState.HideToken(true))
        }
        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.Unreachable) {
            return getActionsForUnreachableCurrency(cryptoCurrency)
        }

        val activeList = mutableListOf<TokenActionsState.ActionState>()
        val disabledList = mutableListOf<TokenActionsState.ActionState>()

        // copy address
        activeList.add(TokenActionsState.ActionState.CopyAddress(true))

        // receive
        activeList.add(TokenActionsState.ActionState.Receive(true))

        // send
        if (isSendDisabled(cryptoCurrencyStatus = cryptoCurrencyStatus, coinStatus = coinStatus)) {
            disabledList.add(TokenActionsState.ActionState.Send(false))
        } else {
            activeList.add(TokenActionsState.ActionState.Send(true))
        }

        // swap
        if (userWallet.isMultiCurrency) {
            if (marketCryptoCurrencyRepository.isExchangeable(userWallet.walletId, cryptoCurrency)) {
                activeList.add(TokenActionsState.ActionState.Swap(true))
            } else {
                disabledList.add(TokenActionsState.ActionState.Swap(false))
            }
        }

        // buy
        if (rampManager.availableForBuy(cryptoCurrency)) {
            activeList.add(TokenActionsState.ActionState.Buy(true))
        } else {
            disabledList.add(TokenActionsState.ActionState.Buy(false))
        }

        // sell
        if (rampManager.availableForSell(cryptoCurrency)) {
            activeList.add(TokenActionsState.ActionState.Sell(true))
        } else {
            disabledList.add(TokenActionsState.ActionState.Sell(false))
        }

        // hide
        activeList.add(TokenActionsState.ActionState.HideToken(true))

        return activeList + disabledList
    }

    private fun getActionsForUnreachableCurrency(cryptoCurrency: CryptoCurrency): List<TokenActionsState.ActionState> {
        val activeList = mutableListOf<TokenActionsState.ActionState>()
        val disabledList = mutableListOf<TokenActionsState.ActionState>()

        activeList.add(TokenActionsState.ActionState.CopyAddress(true))
        if (rampManager.availableForBuy(cryptoCurrency)) {
            activeList.add(TokenActionsState.ActionState.Buy(true))
        } else {
            disabledList.add(TokenActionsState.ActionState.Buy(false))
        }
        disabledList.add(TokenActionsState.ActionState.Send(false))
        disabledList.add(TokenActionsState.ActionState.Swap(false))
        disabledList.add(TokenActionsState.ActionState.Sell(false))
        activeList.add(TokenActionsState.ActionState.Receive(true))
        activeList.add(TokenActionsState.ActionState.HideToken(true))
        return activeList + disabledList
    }

    private fun isSendDisabled(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        coinStatus: CryptoCurrencyStatus?,
    ): Boolean = cryptoCurrencyStatus.value.amount.isNullOrZero() ||
        coinStatus?.value?.amount.isNullOrZero() ||
        currenciesRepository.hasPendingTransactions(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            coinStatus = coinStatus,
        )
}