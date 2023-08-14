package com.tangem.tap.domain.walletCurrencies.implementation

import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.common.*
import com.tangem.domain.common.BlockchainNetwork
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.common.extensions.derivationPath
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.wallets.legacy.WalletManagersRepository
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.domain.model.builders.WalletStoreBuilder
import com.tangem.tap.domain.tokens.UserTokensRepository
import com.tangem.tap.domain.walletCurrencies.WalletCurrenciesManager
import com.tangem.tap.domain.walletStores.repository.WalletAmountsRepository
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

    private val listeners = mutableListOf<WalletCurrenciesManager.Listener>()

    override suspend fun update(userWallet: UserWallet, currency: Currency): CompletionResult<Unit> =
        withContext(Dispatchers.Default) {
            listeners.forEach { it.willUpdate(userWallet, currency) }
            val walletStore = walletStoresRepository.getSync(userWallet.walletId)
                .find {
                    it.blockchain == currency.blockchain &&
                        it.derivationPath?.rawPath == currency.derivationPath
                }

            val updateResult = if (walletStore == null) {
                CompletionResult.Success(Unit)
            } else {
                walletAmountsRepository.updateAmountsForWalletStore(
                    walletStore = walletStore,
                    userWallet = userWallet,
                    fiatCurrency = appCurrencyProvider(),
                )
            }
            listeners.forEach { it.didUpdate(userWallet, currency) }
            updateResult
        }

    override suspend fun addCurrencies(
        userWallet: UserWallet,
        currenciesToAdd: List<Currency>,
    ): CompletionResult<Unit> = withContext(Dispatchers.Default) {
        if (currenciesToAdd.isEmpty()) {
            return@withContext CompletionResult.Success(Unit)
        }

        val card = userWallet.scanResponse.card
        val currenciesToAddWithMissingBlockchains = currenciesToAdd.addMissingBlockchainsIfNeeded(
            userWallet.scanResponse.derivationStyleProvider,
        )
        listeners.forEach { it.willCurrenciesAdd(userWallet, currenciesToAddWithMissingBlockchains) }

        updateWalletStores(
            userWallet = userWallet,
            blockchainNetworks = currenciesToAddWithMissingBlockchains
                .toBlockchainNetworks()
                .addSameBlockchainTokens(userWallet.walletId),
        )
            .map {
                saveUserCurrencies(card, getSavedCurrencies(userWallet.walletId))
            }
            .flatMap {
                updateWalletStoresAmounts(
                    userWallet = userWallet,
                    updatedCurrencies = currenciesToAddWithMissingBlockchains,
                )
            }
    }

    override suspend fun removeCurrencies(
        userWallet: UserWallet,
        currenciesToRemove: List<Currency>,
    ): CompletionResult<Unit> = withContext(Dispatchers.Default) {
        if (currenciesToRemove.isEmpty()) {
            return@withContext CompletionResult.Success(Unit)
        }

        listeners.forEach { it.willCurrenciesRemove(userWallet, currenciesToRemove) }
        val card = userWallet.scanResponse.card
        val remainingCurrencies = getSavedCurrencies(userWallet.walletId)
            .filter { it !in currenciesToRemove }
        val remainingBlockchains = remainingCurrencies
            .filterIsInstance<Currency.Blockchain>()

        walletStoresRepository.deleteDifference(userWallet.walletId, remainingBlockchains)
            .flatMap {
                updateWalletStores(userWallet, remainingCurrencies.toBlockchainNetworks())
            }
            .doOnResult {
                saveUserCurrencies(card, remainingCurrencies)
            }
    }

    override suspend fun removeCurrency(userWallet: UserWallet, currencyToRemove: Currency): CompletionResult<Unit> {
        listeners.forEach { it.willCurrencyRemove(userWallet, currencyToRemove) }
        return removeCurrencies(userWallet, listOf(currencyToRemove))
    }

    override fun addListener(listener: WalletCurrenciesManager.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: WalletCurrenciesManager.Listener) {
        listeners.remove(listener)
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
// [REDACTED_TODO_COMMENT]
    private suspend fun List<BlockchainNetwork>.addSameBlockchainTokens(
        userWalletId: UserWalletId,
    ): List<BlockchainNetwork> {
        val networks = arrayListOf<BlockchainNetwork>()
        val savedWalletStores = withContext(Dispatchers.Default) {
            walletStoresRepository.getSync(userWalletId)
        }

        this.forEach { network ->
            val walletStore = savedWalletStores.firstOrNull {
                it.blockchain == network.blockchain && it.derivationPath?.rawPath == network.derivationPath
            }

            if (walletStore != null) {
                val tokens = walletStore.walletsData
                    .asSequence()
                    .map { it.currency }
                    .filterIsInstance<Currency.Token>()
                    .map { it.token }
                    .toList()

                networks.add(network.copy(tokens = tokens + network.tokens.toSet()))
            } else {
                networks.add(network)
            }
        }

        return networks
    }

    private fun List<Currency>.addMissingBlockchainsIfNeeded(
        derivationStyleProvider: DerivationStyleProvider,
    ): List<Currency> {
        if (this.isEmpty()) return this
        val currencies = this.asSequence()

        return currencies
            .groupBy { currency ->
                findBlockchainCurrency(currency, currencies, derivationStyleProvider.getDerivationStyle())
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
        return blockchainNetworks
            .map { blockchainNetwork ->
                walletManagersRepository.findOrMakeMultiCurrencyWalletManager(
                    userWallet = userWallet,
                    blockchainNetwork = blockchainNetwork,
                )
                    .flatMap { walletManager ->
                        walletStoresRepository.storeOrUpdate(
                            userWalletId = userWallet.walletId,
                            walletStore = WalletStoreBuilder(userWallet, blockchainNetwork)
                                .walletManager(walletManager)
                                .build(),
                        )
                    }
            }
            .fold()
    }

    private suspend fun updateWalletStoresAmounts(
        userWallet: UserWallet,
        updatedCurrencies: List<Currency>,
    ): CompletionResult<Unit> {
        val updatedBlockchains = updatedCurrencies
            .filterIsInstance<Currency.Blockchain>()
        val updatedWalletStores = walletStoresRepository.get(userWallet.walletId)
            .firstOrNull()
            ?.filter { it.blockchainWalletData.currency in updatedBlockchains }
            ?: return CompletionResult.Success(Unit)

        return walletAmountsRepository.updateAmountsForWalletStores(
            walletStores = updatedWalletStores,
            userWallet = userWallet,
            fiatCurrency = appCurrencyProvider(),
        )
    }
}
