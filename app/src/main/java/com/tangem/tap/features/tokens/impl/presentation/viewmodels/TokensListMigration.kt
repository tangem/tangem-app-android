package com.tangem.tap.features.tokens.impl.presentation.viewmodels

import arrow.core.Either
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.data.tokens.utils.CryptoCurrencyFactory
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.tokens.GetCryptoCurrenciesUseCase
import com.tangem.domain.tokens.TokenWithBlockchain
import com.tangem.domain.tokens.TokensAction
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.features.wallet.featuretoggles.WalletFeatureToggles
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.store
import timber.log.Timber
import kotlin.properties.Delegates

/**
 * Class that divide a new and legacy logic when user uses tokens list screen
 *
 * @property walletFeatureToggles     wallet feature toggles
 * @property getSelectedWalletUseCase use case that returns selected wallet
 * @property getCurrenciesUseCase     use case that returns crypto currencies of a specified wallet
 */
internal class TokensListMigration(
    private val walletFeatureToggles: WalletFeatureToggles,
    private val getSelectedWalletUseCase: GetSelectedWalletUseCase,
    private val getCurrenciesUseCase: GetCryptoCurrenciesUseCase,
) {

    private var currentNewCoins: List<CryptoCurrency.Coin> by Delegates.notNull()
    private var currentNewTokens: List<CryptoCurrency.Token> by Delegates.notNull()
    private var currentUserWallet: UserWallet by Delegates.notNull()

    private val cryptoCurrencyFactory by lazy { CryptoCurrencyFactory() }

    suspend fun getCurrentCryptoCurrencies(): TokensListCryptoCurrencies {
        return if (walletFeatureToggles.isRedesignedScreenEnabled) {
            getNewCryptoCurrencies()
        } else {
            getLegacyCryptoCurrencies()
        }
    }

    private suspend fun getNewCryptoCurrencies(): TokensListCryptoCurrencies {
        return when (val selectedWalletEither = getSelectedWalletUseCase()) {
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

    private fun getLegacyCryptoCurrencies(): TokensListCryptoCurrencies {
        val wallets = store.state.walletState.walletsDataFromStores
        val derivationStyle = store.state.globalState.scanResponse?.derivationStyleProvider?.getDerivationStyle()

        return TokensListCryptoCurrencies(
            coins = wallets.toNonCustomBlockchains(derivationStyle),
            tokens = wallets.toNonCustomTokensWithBlockchains(derivationStyle),
        )
    }

    private fun List<WalletDataModel>.toNonCustomBlockchains(derivationStyle: DerivationStyle?): List<Blockchain> {
        return this
            .mapNotNull { walletDataModel ->
                if (walletDataModel.currency.isCustomCurrency(derivationStyle)) {
                    null
                } else {
                    (walletDataModel.currency as? Currency.Blockchain)?.blockchain
                }
            }
            .distinct()
    }

    private fun List<WalletDataModel>.toNonCustomTokensWithBlockchains(
        derivationStyle: DerivationStyle?,
    ): List<TokenWithBlockchain> {
        return this
            .mapNotNull { walletDataModel ->
                if (walletDataModel.currency !is Currency.Token) return@mapNotNull null
                if (walletDataModel.currency.isCustomCurrency(derivationStyle)) return@mapNotNull null

                TokenWithBlockchain(walletDataModel.currency.token, walletDataModel.currency.blockchain)
            }
            .distinct()
    }

    fun onSaveButtonClick(
        currentTokensList: List<TokenWithBlockchain>,
        currentBlockchainList: List<Blockchain>,
        changedTokensList: MutableList<TokenWithBlockchain>,
        changedBlockchainList: List<Blockchain>,
    ) {
        if (walletFeatureToggles.isRedesignedScreenEnabled) {
            saveByNewWay(changedTokensList = changedTokensList, changedBlockchainList = changedBlockchainList)
        } else {
            saveByOldWay(currentTokensList, currentBlockchainList, changedTokensList, changedBlockchainList)
        }
    }

    private fun saveByNewWay(
        changedTokensList: MutableList<TokenWithBlockchain>,
        changedBlockchainList: List<Blockchain>,
    ) {
        store.dispatch(
            action = TokensAction.NewSaveChanges(
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

    private fun saveByOldWay(
        currentTokensList: List<TokenWithBlockchain>,
        currentBlockchainList: List<Blockchain>,
        changedTokensList: MutableList<TokenWithBlockchain>,
        changedBlockchainList: List<Blockchain>,
    ) {
        val scanResponse = store.state.globalState.scanResponse ?: return

        store.dispatch(
            action = TokensAction.LegacySaveChanges(
                currentTokens = currentTokensList,
                currentBlockchains = currentBlockchainList,
                changedTokens = changedTokensList,
                changedBlockchains = changedBlockchainList,
                scanResponse = scanResponse,
            ),
        )
    }
}