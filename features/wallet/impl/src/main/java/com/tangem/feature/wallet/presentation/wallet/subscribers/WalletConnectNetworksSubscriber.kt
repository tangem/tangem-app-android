package com.tangem.feature.wallet.presentation.wallet.subscribers

import arrow.core.Either
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkGroup
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.walletconnect.WalletConnectActions
import com.tangem.domain.wallets.models.UserWallet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * WalletConnect networks subscriber. Update WalletConnect networks for a specified [userWallet].
 *
 * @property userWallet          user wallet
 * @property getTokenListUseCase use case for subscribing on token list changes
 * @property reduxStateHolder    redux state holder
 *
[REDACTED_AUTHOR]
 */
internal class WalletConnectNetworksSubscriber(
    private val userWallet: UserWallet,
    private val getTokenListUseCase: GetTokenListUseCase,
    private val reduxStateHolder: ReduxStateHolder,
) : WalletSubscriber() {

    private val mutex = Mutex()

    override fun create(coroutineScope: CoroutineScope): Flow<Either<TokenListError, TokenList>> {
        return getTokenListUseCase(userWalletId = userWallet.walletId)
            .conflate()
            .distinctUntilCurrenciesChanged()
            .filterLoadedTokens()
            .onEach {
                mutex.withLock {
                    Timber.d("WalletConnect: ${userWallet.walletId} networks is updated")

                    reduxStateHolder.dispatch(
                        action = WalletConnectActions.New.SetupUserChains(userWallet = userWallet),
                    )
                }
            }
    }

    private fun MaybeTokenListFlow.distinctUntilCurrenciesChanged(): MaybeTokenListFlow {
        return distinctUntilChanged { old, new ->
            val oldCurrencies = old.fold(ifLeft = { null }, ifRight = { it.getCryptoCurrencies() })
            val newCurrencies = new.fold(ifLeft = { null }, ifRight = { it.getCryptoCurrencies() })

            oldCurrencies == newCurrencies
        }
    }

    private fun MaybeTokenListFlow.filterLoadedTokens(): MaybeTokenListFlow {
        return filter { either ->
            either.fold(
                ifLeft = { false },
                ifRight = { it.getCryptoCurrencies().isAllCurrenciesLoaded() },
            )
        }
    }

    private fun TokenList.getCryptoCurrencies(): List<CryptoCurrencyStatus> {
        return when (this) {
            is TokenList.Ungrouped -> currencies
            is TokenList.GroupedByNetwork -> groups.flatMap(NetworkGroup::currencies)
            else -> emptyList()
        }
    }

    private fun List<CryptoCurrencyStatus>.isAllCurrenciesLoaded(): Boolean {
        return none { it.value is CryptoCurrencyStatus.Loading }
    }
}