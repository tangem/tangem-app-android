package com.tangem.tap.features.tokens.impl.presentation.viewmodels

import arrow.core.Either
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.data.tokens.utils.CryptoCurrencyFactory
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.tokens.GetCryptoCurrenciesUseCase
import com.tangem.domain.tokens.TokenWithBlockchain
import com.tangem.domain.tokens.TokensAction
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
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

                when (val currenciesEither = getCurrenciesUseCase(userWalletId = selectedWalletEither.value.walletId)) {
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

    fun onSaveButtonClick(
        changedTokensList: MutableList<TokenWithBlockchain>,
        changedBlockchainList: List<Blockchain>,
    ) {
        store.dispatch(
            action = TokensAction.SaveChanges(
                currentTokens = currentNewTokens,
                currentCoins = currentNewCoins,
                changedTokens = changedTokensList.mapNotNull {
                    cryptoCurrencyFactory.createToken(
                        sdkToken = it.token,
                        blockchain = it.blockchain,
                        extraDerivationPath = null,
                        derivationStyleProvider = currentUserWallet.scanResponse.derivationStyleProvider,
                    )
                },
                changedCoins = changedBlockchainList.mapNotNull {
                    cryptoCurrencyFactory.createCoin(
                        blockchain = it,
                        extraDerivationPath = null,
                        derivationStyleProvider = currentUserWallet.scanResponse.derivationStyleProvider,
                    )
                },
                userWallet = currentUserWallet,
            ),
        )
    }
}