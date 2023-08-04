package com.tangem.tap.domain.walletStores.repository.implementation

import com.tangem.blockchain.common.*
import com.tangem.common.CompletionResult
import com.tangem.common.catching
import com.tangem.common.doOnSuccess
import com.tangem.common.mapFailure
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.common.BlockchainNetwork
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.common.TapWorkarounds.useOldStyleDerivation
import com.tangem.domain.common.extensions.makeWalletManagerForApp
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.legacy.WalletManagersRepository
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.domain.walletStores.WalletStoresError
import com.tangem.tap.domain.walletStores.storage.WalletManagerStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class DefaultWalletManagersRepository(
    private val walletManagerFactory: WalletManagerFactory,
) : WalletManagersRepository {
    private val walletManagersStorage = WalletManagerStorage

    override suspend fun findOrMakeMultiCurrencyWalletManager(
        userWallet: UserWallet,
        blockchainNetwork: BlockchainNetwork,
    ): CompletionResult<WalletManager> {
        return findOrMakeInternal(userWallet, blockchainNetwork)
    }

    override suspend fun findOrMakeSingleCurrencyWalletManager(
        userWallet: UserWallet,
    ): CompletionResult<WalletManager> {
        return findOrMakeInternal(userWallet, blockchainNetwork = null)
    }

    private suspend fun findOrMakeInternal(
        userWallet: UserWallet,
        blockchainNetwork: BlockchainNetwork?,
    ): CompletionResult<WalletManager> = withContext(Dispatchers.Default) {
        val foundWalletManager = findWalletManager(
            userWalletId = userWallet.walletId,
            blockchain = blockchainNetwork?.blockchain,
            derivationPath = blockchainNetwork?.derivationPath,
        )

        foundWalletManager?.updateTokens(
            scanResponse = userWallet.scanResponse,
            blockchainNetwork = blockchainNetwork,
        )
            ?: makeAndStore(userWallet, blockchainNetwork)
    }

    private suspend fun makeAndStore(
        userWallet: UserWallet,
        blockchainNetwork: BlockchainNetwork?,
    ): CompletionResult<WalletManager> {
        val scanResponse = userWallet.scanResponse
        val blockchain = blockchainNetwork?.blockchain
            ?: scanResponse.cardTypesResolver.getBlockchain().let { blockchain ->
                if (scanResponse.card.isTestCard) blockchain.getTestnetVersion() else blockchain
            }
        val derivationParams = getDerivationParams(
            derivationPath = blockchainNetwork?.derivationPath,
            card = scanResponse.card,
        )

        val walletManager = blockchain?.let {
            walletManagerFactory.makeWalletManagerForApp(
                scanResponse = userWallet.scanResponse,
                blockchain = blockchain,
                derivationParams = derivationParams,
            )
        }

        return when {
            blockchain == Blockchain.Unknown || blockchain == null -> {
                val error = WalletStoresError.UnknownBlockchain()
                Timber.e(
                    error,
                    """
                        Unknown blockchain while creating wallet manager
                        |- User wallet ID: ${userWallet.walletId}
                    """.trimIndent(),
                )
                CompletionResult.Failure(error)
            }
            walletManager != null -> {
                walletManager.updateTokens(
                    scanResponse = scanResponse,
                    blockchainNetwork = blockchainNetwork,
                )
                    .doOnSuccess { store(userWallet.walletId, it) }
            }
            else -> {
                val error = WalletStoresError.WalletManagerNotCreated(blockchain)
                Timber.e(
                    error,
                    """
                        Unable to create wallet manager
                        |- User wallet ID: ${userWallet.walletId}
                        |- Blockchain: $blockchain
                        |- Derivation path: ${blockchainNetwork?.derivationPath}
                    """.trimIndent(),
                )
                CompletionResult.Failure(error)
            }
        }
    }

    override suspend fun delete(userWalletIds: List<UserWalletId>): CompletionResult<Unit> = catching {
        walletManagersStorage.update { prevManagers ->
            prevManagers.filterKeys { it !in userWalletIds } as HashMap<UserWalletId, List<WalletManager>>
        }
    }

    override suspend fun delete(userWalletId: UserWalletId, blockchain: Blockchain): CompletionResult<Unit> = catching {
        deleteInternal(userWalletId, blockchain)
    }

    private suspend fun store(userWalletId: UserWalletId, walletManager: WalletManager) {
        walletManagersStorage.update { prevManagers ->
            prevManagers.apply {
                set(
                    key = userWalletId,
                    value = this[userWalletId].orEmpty() + walletManager,
                )
            }
        }
    }

    private suspend fun deleteInternal(userWalletId: UserWalletId, blockchain: Blockchain?) {
        walletManagersStorage.update { prevManagers ->
            prevManagers.apply {
                if (blockchain == null) {
                    set(
                        key = userWalletId,
                        value = emptyList(),
                    )
                } else {
                    set(
                        key = userWalletId,
                        value = this[userWalletId]
                            ?.filter { it.wallet.blockchain == blockchain }
                            .orEmpty(),
                    )
                }
            }
        }
    }

    private fun WalletManager.updateTokens(
        scanResponse: ScanResponse,
        blockchainNetwork: BlockchainNetwork?,
    ): CompletionResult<WalletManager> {
        val walletManager = this
        return catching {
            val tokens = blockchainNetwork?.tokens ?: listOfNotNull(scanResponse.cardTypesResolver.getPrimaryToken())

            if (tokens != walletManager.cardTokens) {
                // TODO: remove ability to manipulate with walletManager.cardTokens
                walletManager.cardTokens.clear()
                walletManager.wallet.removeAllTokens()
                if (tokens.isNotEmpty()) {
                    walletManager.cardTokens.addAll(tokens)
                    // add empty amounts to prepare templates of tokens WalletDataModel
                    // see: WalletMangerWalletStoreBuilderImpl.build()
                    tokens.forEach { walletManager.wallet.setAmount(Amount(it)) }
                }
            }

            walletManager
        }
            .mapFailure {
                val error = WalletStoresError.UpdateWalletManagerTokensError(
                    blockchain = walletManager.wallet.blockchain,
                    cause = it,
                )
                Timber.e(error)
                error
            }
    }

    private suspend fun findWalletManager(
        userWalletId: UserWalletId,
        blockchain: Blockchain?,
        derivationPath: String?,
    ): WalletManager? {
        return walletManagersStorage.getAll()
            .firstOrNull()
            ?.get(userWalletId)
            ?.let { userWalletManagers ->
                if (blockchain == null) {
                    userWalletManagers.firstOrNull()
                } else {
                    userWalletManagers.firstOrNull {
                        it.wallet.blockchain == blockchain &&
                            it.wallet.publicKey.derivationPath?.rawPath == derivationPath
                    }
                }
            }
    }

    private fun getDerivationParams(derivationPath: String?, card: CardDTO): DerivationParams? {
        return derivationPath?.let {
            DerivationParams.Custom(
                path = DerivationPath(it),
            )
        } ?: if (!card.settings.isHDWalletAllowed) {
            null
        } else if (card.useOldStyleDerivation) {
            DerivationParams.Default(DerivationStyle.LEGACY)
        } else {
            DerivationParams.Default(DerivationStyle.NEW)
        }
    }
}