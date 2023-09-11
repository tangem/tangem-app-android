package com.tangem.domain.tokens

import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.tokens.repository.MarketCryptoCurrencyRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Use case to determine which TokenActions are available for a [CryptoCurrency]
 *
 * @property rampManager Ramp manager to check ramp availability
 */
class GetCryptoCurrencyActionsUseCase(
    private val rampManager: RampStateManager,
    private val marketCryptoCurrencyRepository: MarketCryptoCurrencyRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    operator fun invoke(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Flow<TokenActionsState> {
        return flow {
            val actionStates = createTokenActionsState(userWalletId, cryptoCurrency)
            emit(actionStates)
        }.flowOn(dispatchers.io)
    }

    private suspend fun createTokenActionsState(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): TokenActionsState {
        return TokenActionsState(
            walletId = userWalletId,
            cryptoCurrencyId = cryptoCurrency.id,
            states = createListOfActions(userWalletId, cryptoCurrency),
        )
    }

    /**
     * Creates list of action for expected order
     * Actions priority: [Buy Send Receive Sell Swap]
     */
    private suspend fun createListOfActions(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): List<TokenActionsState.ActionState> {
        return buildList {
            // todo add check available in swap 1inch etc if backend doen't handle it
            if (marketCryptoCurrencyRepository.isExchangeable(userWalletId, cryptoCurrency.id) &&
                !isCustomToken(cryptoCurrency)
            ) {
                addFirst(TokenActionsState.ActionState.Swap(true))
            } else {
                add(TokenActionsState.ActionState.Swap(false))
            }

            if (rampManager.availableForSell(cryptoCurrency)) {
                addFirst(TokenActionsState.ActionState.Sell(true))
            } else {
                add(TokenActionsState.ActionState.Sell(false))
            }

            addFirst(TokenActionsState.ActionState.Receive(true))
            addFirst(TokenActionsState.ActionState.Send(true))

            if (rampManager.availableForBuy(cryptoCurrency)) {
                addFirst(TokenActionsState.ActionState.Buy(true))
            } else {
                add(TokenActionsState.ActionState.Buy(false))
            }
        }
    }

    private fun isCustomToken(currency: CryptoCurrency): Boolean {
        return currency is CryptoCurrency.Token && currency.isCustom
    }

    private fun <T> MutableList<T>.addFirst(item: T) {
        this.add(0, item)
    }
}