package com.tangem.tap.features.tokens.impl.presentation.viewmodels

import arrow.core.Either
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.routing.AppRoute
import com.tangem.data.tokens.utils.CryptoCurrencyFactory
import com.tangem.domain.card.DerivePublicKeysUseCase
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.tokens.GetCryptoCurrenciesUseCase
import com.tangem.domain.tokens.TokenWithBlockchain
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import timber.log.Timber
import kotlin.properties.Delegates

/**
 * Class that divide a new and legacy logic when user uses tokens list screen
 *
 * @property getSelectedWalletSyncUseCase use case that returns selected wallet
 * @property getCurrenciesUseCase         use case that returns crypto currencies of a specified wallet
 */
internal class TokensListMigration(
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val getCurrenciesUseCase: GetCryptoCurrenciesUseCase,
    private val derivePublicKeysUseCase: DerivePublicKeysUseCase,
    private val addCryptoCurrenciesUseCase: AddCryptoCurrenciesUseCase,
) {

    private var currentNewCoins: List<CryptoCurrency.Coin> by Delegates.notNull()
    private var currentNewTokens: List<CryptoCurrency.Token> by Delegates.notNull()
    private var currentUserWallet: UserWallet by Delegates.notNull()

    private val cryptoCurrencyFactory by lazy { CryptoCurrencyFactory() }

    suspend fun getCurrentCryptoCurrencies(): TokensListCryptoCurrencies {
        return when (val selectedWalletEither = getSelectedWalletSyncUseCase()) {
            is Either.Left -> {
                Timber.e(selectedWalletEither.value.toString())
                TokensListCryptoCurrencies(coins = emptyList(), tokens = emptyList())
            }
            is Either.Right -> {
                currentUserWallet = selectedWalletEither.value

                when (
                    val currenciesEither = getCurrenciesUseCase.getSync(
                        userWalletId = selectedWalletEither.value.walletId,
                    )
                ) {
                    is Either.Left -> {
                        Timber.e(currenciesEither.value.toString())
                        TokensListCryptoCurrencies(coins = emptyList(), tokens = emptyList())
                    }
                    is Either.Right -> {
                        TokensListCryptoCurrencies(
                            coins = currenciesEither.value
                                .filterIsInstance<CryptoCurrency.Coin>()
                                .filterNot { it.isCustom }
                                .also { currentNewCoins = it }
                                .map { Blockchain.fromId(it.network.id.value) },
                            tokens = currenciesEither.value
                                .filterIsInstance<CryptoCurrency.Token>()
                                .filterNot(CryptoCurrency.Token::isCustom)
                                .also { currentNewTokens = it }
                                .map { token ->
                                    TokenWithBlockchain(
                                        token = Token(
                                            name = token.name,
                                            symbol = token.symbol,
                                            contractAddress = token.contractAddress,
                                            decimals = token.decimals,
                                            id = token.id.rawCurrencyId,
                                        ),
                                        blockchain = Blockchain.fromId(token.network.id.value),
                                    )
                                },
                        )
                    }
                }
            }
        }
    }

    suspend fun onSaveButtonClick(
        changedTokensList: MutableList<TokenWithBlockchain>,
        changedBlockchainList: List<Blockchain>,
    ) {
        val changedTokens = changedTokensList.mapNotNull {
            cryptoCurrencyFactory.createToken(
                sdkToken = it.token,
                blockchain = it.blockchain,
                extraDerivationPath = null,
                derivationStyleProvider = currentUserWallet.scanResponse.derivationStyleProvider,
            )
        }

        val changedCoins = changedBlockchainList.mapNotNull {
            cryptoCurrencyFactory.createCoin(
                blockchain = it,
                extraDerivationPath = null,
                derivationStyleProvider = currentUserWallet.scanResponse.derivationStyleProvider,
            )
        }

        val blockchainsToAdd = changedCoins.filterNot(currentNewCoins::contains)
        val blockchainsToRemove = currentNewCoins.filterNot(changedCoins::contains)

        val tokensToAdd = changedTokens.filterNot(currentNewTokens::contains)
        val tokensToRemove = currentNewTokens.filterNot { token -> changedTokens.any { it == token } }

        removeCurrenciesIfNeeded(
            userWalletId = currentUserWallet.walletId,
            currencies = blockchainsToRemove + tokensToRemove,
        )

        val isNothingToDoWithTokens = tokensToAdd.isEmpty() && tokensToRemove.isEmpty()
        val isNothingToDoWithBlockchain = blockchainsToAdd.isEmpty() && blockchainsToRemove.isEmpty()
        if (isNothingToDoWithTokens && isNothingToDoWithBlockchain) {
            store.dispatchDebugErrorNotification(message = "Nothing to save")
            navigateToWallet()
            return
        }

        val currencyList = blockchainsToAdd + tokensToAdd

        derivePublicKeysUseCase(userWalletId = currentUserWallet.walletId, currencies = currencyList)
            .onRight {
                addCryptoCurrenciesUseCase(userWalletId = currentUserWallet.walletId, currencies = currencyList)
                navigateToWallet()
            }
            .onLeft { Timber.e(it, "Failed to derive public keys") }
    }

    private fun navigateToWallet() {
        store.dispatchNavigationAction {
            val route = AppRoute.Wallet

            if (route in stack) {
                popTo(route)
            } else {
                replaceAll(route)
            }
        }
    }

    private suspend fun removeCurrenciesIfNeeded(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        if (currencies.isEmpty()) return
        val currenciesRepository = store.inject(DaggerGraphState::currenciesRepository)
        val walletManagersFacade = store.inject(DaggerGraphState::walletManagersFacade)

        currenciesRepository.removeCurrencies(userWalletId = userWalletId, currencies = currencies)

        walletManagersFacade.remove(
            userWalletId = userWalletId,
            networks = currencies
                .filterIsInstance<CryptoCurrency.Coin>()
                .mapTo(hashSetOf(), CryptoCurrency::network),
        )
        walletManagersFacade.removeTokens(
            userWalletId = userWalletId,
            tokens = currencies.filterIsInstance<CryptoCurrency.Token>().toSet(),
        )
    }
}