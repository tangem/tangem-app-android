package com.tangem.tap.domain.walletCurrencies.implementation

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.common.CompletionResult
import com.tangem.common.flatMap
import com.tangem.common.fold
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
import com.tangem.tap.domain.walletStores.repository.WalletAmountsRepository
import com.tangem.tap.domain.walletStores.repository.WalletManagersRepository
import com.tangem.tap.domain.walletStores.repository.WalletStoresRepository
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.toBlockchainNetworks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
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
                it.blockchain == currency.blockchain &&
                    it.derivationPath?.rawPath == currency.derivationPath
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
        val newCurrencies = (getSavedCurrencies(userWallet.walletId) + currenciesToAdd)
            .addMissingBlockchains(card)

        updateWalletStores(userWallet, newCurrencies.toBlockchainNetworks())
            .map {
                saveUserCurrencies(card, newCurrencies)
            }
            .flatMap {
                updateWalletStoresAmounts(
                    userWallet = userWallet,
                    updatedBlockchains = currenciesToAdd.map { it.blockchain }.distinct(),
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
                .flatMap { walletStore ->
                    walletStore.walletsData.map { it.currency }
                }
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
        if (this.isEmpty()) return this
        val currencies = this.asSequence()

        return currencies
            .groupBy { currency ->
                findBlockchainCurrency(currency, currencies, card.derivationStyle)
            }
            .mapValues { (blockchainCurrency, blockchainCurrencies) ->
                findBlockchainTokens(blockchainCurrency, blockchainCurrencies)
            }
            .flatMap { (blockchainCurrency, blockchainTokens) ->
                arrayListOf(blockchainCurrency) + blockchainTokens
            }
    }

    private fun findBlockchainCurrency(
        currency: Currency,
        currencies: Sequence<Currency>,
        cardDerivationStyle: DerivationStyle?,
    ): Currency.Blockchain {
        return currencies
            .filterIsInstance<Currency.Blockchain>()
            .firstOrNull {
                it.blockchain == currency.blockchain &&
                    it.derivationPath == currency.derivationPath
            }
            ?: Currency.Blockchain(
                blockchain = currency.blockchain,
                derivationPath = currency.derivationPath
                    ?: currency.blockchain.derivationPath(cardDerivationStyle)?.rawPath,
            )
    }

    private fun findBlockchainTokens(
        blockchainCurrency: Currency.Blockchain,
        blockchainCurrencies: List<Currency>,
    ): List<Currency.Token> {
        return blockchainCurrencies
            .filterIsInstance<Currency.Token>()
            .map { token ->
                token.copy(
                    derivationPath = token.derivationPath ?: blockchainCurrency.derivationPath,
                )
            }
    }

    private suspend fun updateWalletStores(
        userWallet: UserWallet,
        blockchainNetworks: List<BlockchainNetwork>,
    ): CompletionResult<Unit> {
        val userWalletId = userWallet.walletId
        return blockchainNetworks
            .map { blockchainNetwork ->
                walletManagersRepository.findOrMakeMultiCurrencyWalletManager(
                    userWallet = userWallet,
                    blockchainNetwork = blockchainNetwork,
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

    private suspend fun updateWalletStoresAmounts(
        userWallet: UserWallet,
        updatedBlockchains: List<Blockchain>,
    ): CompletionResult<Unit> {
        val updatedWalletStores = walletStoresRepository.get(userWallet.walletId)
            .firstOrNull()
            ?.filter { it.blockchain in updatedBlockchains }
            ?: return CompletionResult.Success(Unit)

        return walletAmountsRepository.updateAmountsForWalletStores(
            walletStores = updatedWalletStores,
            userWallet = userWallet,
            fiatCurrency = appCurrencyProvider(),
        )
    }
}
