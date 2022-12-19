package com.tangem.tap.domain.walletCurrencies.implementation

import com.tangem.blockchain.common.DerivationStyle
import com.tangem.common.CompletionResult
import com.tangem.common.flatMap
import com.tangem.common.map
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.model.builders.WalletStoreBuilder
import com.tangem.tap.domain.tokens.UserTokensRepository
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.domain.walletCurrencies.WalletCurrenciesManager
import com.tangem.tap.domain.walletStores.implementation.utils.fold
import com.tangem.tap.domain.walletStores.repository.WalletAmountsRepository
import com.tangem.tap.domain.walletStores.repository.WalletManagersRepository
import com.tangem.tap.domain.walletStores.repository.WalletStoresRepository
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.toBlockchainNetworks
import com.tangem.tap.features.wallet.models.toCurrencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class DefaultWalletCurrenciesManager(
    private val userTokensRepository: UserTokensRepository,
    private val walletStoresRepository: WalletStoresRepository,
    private val walletAmountsRepository: WalletAmountsRepository,
    private val walletManagersRepository: WalletManagersRepository,
    private val appCurrencyProvider: () -> FiatCurrency,
) : WalletCurrenciesManager {
    override suspend fun update(
        userWallet: UserWallet,
        currency: Currency,
    ): CompletionResult<Unit> = withContext(Dispatchers.Default) {
        val walletStore = walletStoresRepository.getSync(userWallet.walletId)
            .find {
                it.blockchainNetwork.blockchain == currency.blockchain
                    && it.blockchainNetwork.derivationPath == currency.derivationPath
            }

        if (walletStore != null) {
            walletAmountsRepository.updateAmountsForWalletStore(
                walletStore = walletStore,
                userWallet = userWallet,
                fiatCurrency = appCurrencyProvider(),
            )
        } else CompletionResult.Success(Unit)
    }

    override suspend fun addCurrencies(
        userWallet: UserWallet,
        currenciesToAdd: List<Currency>,
    ): CompletionResult<Unit> = withContext(Dispatchers.Default) {
        val card = userWallet.scanResponse.card
        val updatedBlockchainNetworks = currenciesToAdd
            .addMissingBlockchains(card)
            .toBlockchainNetworks()
        val newCurrencies = (getSavedCurrencies(userWallet.walletId) + currenciesToAdd)
            .addMissingBlockchains(card)

        updateWalletStores(userWallet, updatedBlockchainNetworks)
            .map {
                saveUserCurrencies(card, newCurrencies)
            }
            .flatMap {
                walletAmountsRepository.updateAmountsForUserWallet(
                    userWallet = userWallet,
                    fiatCurrency = appCurrencyProvider(),
                )
            }
    }

    override suspend fun removeCurrencies(
        userWallet: UserWallet,
        currenciesToRemove: List<Currency>,
    ): CompletionResult<Unit> = withContext(Dispatchers.Default) {
        val card = userWallet.scanResponse.card
        val remainingCurrencies = getSavedCurrencies(userWallet.walletId)
            .filter { it !in currenciesToRemove }
        val remainingBlockchains = remainingCurrencies
            .filterIsInstance<Currency.Blockchain>()
            .map { it.blockchain }

        walletStoresRepository.deleteDifference(userWallet.walletId, remainingBlockchains)
            .flatMap {
                updateWalletStores(userWallet, remainingCurrencies.toBlockchainNetworks())
            }
            .map {
                saveUserCurrencies(card, remainingCurrencies)
            }
    }

    override suspend fun removeCurrency(
        userWallet: UserWallet,
        currencyToRemove: Currency,
    ): CompletionResult<Unit> {
        return removeCurrencies(userWallet, listOf(currencyToRemove))
    }

    private suspend fun getSavedCurrencies(userWalletId: UserWalletId): List<Currency> {
        return withContext(Dispatchers.Default) {
            walletStoresRepository.getSync(userWalletId)
                .flatMap { it.blockchainNetwork.toCurrencies() }
        }
    }

    private suspend fun saveUserCurrencies(card: CardDTO, currencies: List<Currency>) {
        withContext(Dispatchers.IO) {
            userTokensRepository.saveUserTokens(
                card = card,
                tokens = currencies,
            )
        }
    }

    private fun List<Currency>.addMissingBlockchains(card: CardDTO): List<Currency> {
        val newCurrencies = arrayListOf<Currency>()

        for (currency in this.sortedByDescending { it is Currency.Blockchain }) {
            when (currency) {
                is Currency.Blockchain -> {
                    newCurrencies.add(currency.updateDerivationPath(card.derivationStyle))
                }

                is Currency.Token -> {
                    val containsTokenBlockchain = newCurrencies.any {
                        it.isBlockchain() && it.blockchain == currency.blockchain
                    }

                    if (containsTokenBlockchain) {
                        newCurrencies.add(currency.updateDerivationPath(card.derivationStyle))
                    } else {
                        val derivationPath = findDerivationPath(currency, card.derivationStyle)
                        newCurrencies.add(
                            Currency.Blockchain(
                                blockchain = currency.blockchain,
                                derivationPath = derivationPath,
                            ),
                        )
                        newCurrencies.add(
                            currency.copy(derivationPath = derivationPath),
                        )
                    }
                }
            }
        }

        return newCurrencies
    }

    private suspend fun updateWalletStores(
        userWallet: UserWallet,
        blockchainNetworks: List<BlockchainNetwork>,
    ): CompletionResult<Unit> {
        val userWalletId = userWallet.walletId
        return blockchainNetworks
            .map { blockchainNetwork ->
                walletManagersRepository.findOrMake(
                    userWallet = userWallet,
                    blockchainNetwork = blockchainNetwork,
                    refresh = true,
                )
                    .flatMap { walletManager ->
                        walletStoresRepository.storeOrUpdate(
                            userWalletId = userWalletId,
                            walletStore = WalletStoreBuilder(userWalletId, blockchainNetwork)
                                .walletManager(walletManager)
                                .build(),
                        )
                    }
            }
            .fold()
    }

    private fun Currency.updateDerivationPath(cardDerivationStyle: DerivationStyle?): Currency {
        val findDerivationPath: () -> String? = {
            findDerivationPath(this, cardDerivationStyle)
        }

        return when (this) {
            is Currency.Blockchain -> this.copy(
                derivationPath = findDerivationPath(),
            )

            is Currency.Token -> this.copy(
                derivationPath = findDerivationPath(),
            )
        }
    }

    private fun findDerivationPath(currency: Currency, cardDerivationStyle: DerivationStyle?): String? {
        return currency.derivationPath ?: currency.blockchain.derivationPath(cardDerivationStyle)?.rawPath
    }
}
