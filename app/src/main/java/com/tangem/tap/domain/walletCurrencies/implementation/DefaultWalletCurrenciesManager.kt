package com.tangem.tap.domain.walletCurrencies.implementation

import com.tangem.common.CompletionResult
import com.tangem.common.catching
import com.tangem.common.flatMap
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.TapWorkarounds.derivationStyle
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
import com.tangem.tap.features.wallet.models.getTokens
import com.tangem.tap.features.wallet.models.toCurrencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
        blockchainNetwork: BlockchainNetwork,
    ): CompletionResult<Unit> {
        val walletStore = walletStoresRepository.get(userWallet.walletId).first()
            .find {
                it.blockchainNetwork.blockchain == blockchainNetwork.blockchain
                    && it.blockchainNetwork.derivationPath == blockchainNetwork.derivationPath
            }

        return if (walletStore != null) {
            walletAmountsRepository.update(
                userWallet = userWallet,
                walletStore = walletStore,
                fiatCurrency = appCurrencyProvider(),
            )
        } else CompletionResult.Success(Unit)
    }

    override suspend fun addCurrencies(
        userWallet: UserWallet,
        currenciesToAdd: List<Currency>,
    ): CompletionResult<Unit> = withContext(Dispatchers.Default) {
        var newBlockchainNetworks = listOf<BlockchainNetwork>()
        catching {
            val card = userWallet.scanResponse.card
            val savedCurrencies = withContext(Dispatchers.IO) {
                userTokensRepository.getUserTokens(card)
            }
            newBlockchainNetworks = (savedCurrencies + currenciesToAdd)
                .toBlockchainNetworks(userWallet.scanResponse.card)
            val newCurrencies = newBlockchainNetworks.toCurrencies()

            withContext(Dispatchers.IO) {
                userTokensRepository.saveUserTokens(
                    card = card,
                    tokens = newCurrencies,
                )
            }
        }
            .flatMap {
                newBlockchainNetworks.updateWalletStores(userWallet)
            }
    }

    override suspend fun removeCurrency(
        userWallet: UserWallet,
        currencyToRemove: Currency,
    ): CompletionResult<Unit> {
        return removeCurrencies(userWallet, listOf(currencyToRemove))
    }

    override suspend fun removeCurrencies(
        userWallet: UserWallet,
        currenciesToRemove: List<Currency>,
    ): CompletionResult<Unit> = withContext(Dispatchers.Default) {
        var remainingBlockchainsNetworks = emptyList<BlockchainNetwork>()
        catching {
            val card = userWallet.scanResponse.card
            val savedCurrencies = withContext(Dispatchers.IO) {
                userTokensRepository.getUserTokens(card)
            }

            val remainingCurrencies = arrayListOf<Currency>()
            savedCurrencies.forEach { savedCurrency ->
                if (savedCurrency !in currenciesToRemove) {
                    remainingCurrencies.add(savedCurrency)
                }
            }

            remainingBlockchainsNetworks = remainingCurrencies.toBlockchainNetworks(userWallet.scanResponse.card)

            withContext(Dispatchers.IO) {
                userTokensRepository.saveUserTokens(
                    card = card,
                    tokens = remainingCurrencies,
                )
            }
        }
            .flatMap {
                remainingBlockchainsNetworks.updateWalletStores(userWallet)
            }
    }

    private fun List<Currency>.toBlockchainNetworks(card: CardDTO): List<BlockchainNetwork> {
        val blockchainNetworks = arrayListOf<BlockchainNetwork>()
        val findDerivationPath: (currency: Currency) -> String? = { currency ->
            currency.derivationPath
                ?: currency.blockchain.derivationPath(card.derivationStyle)
                    ?.rawPath
        }

        for (currency in this.sortedByDescending { it.isBlockchain() }) {
            when (currency) {
                is Currency.Blockchain -> {
                    val blockchainNetwork = BlockchainNetwork(
                        blockchain = currency.blockchain,
                        derivationPath = findDerivationPath(currency),
                        tokens = getTokens(currency),
                    )

                    blockchainNetworks.add(blockchainNetwork)
                }
                is Currency.Token -> {
                    val tokenBlockchainNetworkIndex = blockchainNetworks
                        .indexOfFirst {
                            it.blockchain == currency.blockchain &&
                                it.derivationPath == currency.derivationPath
                        }

                    if (tokenBlockchainNetworkIndex == -1) {
                        blockchainNetworks.add(
                            BlockchainNetwork(
                                blockchain = currency.blockchain,
                                derivationPath = findDerivationPath(currency),
                                tokens = listOf(currency.token),
                            ),
                        )
                    } else {
                        val tokenBlockchainNetwork = blockchainNetworks[tokenBlockchainNetworkIndex]
                        if (currency.token in tokenBlockchainNetwork.tokens) {
                            continue
                        } else {
                            blockchainNetworks.add(
                                tokenBlockchainNetworkIndex,
                                tokenBlockchainNetwork.copy(
                                    tokens = tokenBlockchainNetwork.tokens + currency.token,
                                ),
                            )
                        }
                    }
                }
            }
        }

        return blockchainNetworks
    }

    private suspend fun List<BlockchainNetwork>.updateWalletStores(
        userWallet: UserWallet,
    ): CompletionResult<Unit> {
        val userWalletId = userWallet.walletId
        return this
            .also { blockchainNetworks ->
                walletStoresRepository.deleteDifference(
                    userWalletId = userWalletId,
                    currentBlockchains = blockchainNetworks.map { it.blockchain },
                )
            }
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
            .flatMap {
                walletAmountsRepository.update(
                    userWallet = userWallet,
                    fiatCurrency = appCurrencyProvider(),
                )
            }
    }
}
