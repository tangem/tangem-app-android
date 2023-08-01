package com.tangem.domain.walletmanager

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.WalletManager
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.datasource.config.ConfigManager
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.domain.common.util.hasDerivation
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import com.tangem.domain.walletmanager.utils.SdkTokenConverter
import com.tangem.domain.walletmanager.utils.UpdateWalletManagerResultFactory
import com.tangem.domain.walletmanager.utils.WalletManagerFactory
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import timber.log.Timber
import com.tangem.domain.tokens.model.Token as DomainToken

// TODO: Move to its own module
/**
 * A facade for managing wallets.
 */
class WalletManagersFacade(
    private val walletManagersStore: WalletManagersStore,
    private val userWalletsStore: UserWalletsStore,
    private val demoConfig: DemoConfig,
    configManager: ConfigManager,
) {

    private val resultFactory by lazy { UpdateWalletManagerResultFactory() }
    private val walletManagerFactory by lazy { WalletManagerFactory(configManager) }
    private val sdkTokenConverter by lazy { SdkTokenConverter() }

    /**
     * Updates the wallet manager associated with a user's wallet and network.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param networkId The network ID.
     * @param extraTokens Additional tokens.
     * @return The result of updating the wallet manager.
     * @throws IllegalArgumentException if the user's wallet is not found.
     */
    suspend fun update(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        extraTokens: Set<DomainToken>,
    ): UpdateWalletManagerResult {
        val userWallet = requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "Unable to find a user wallet with provided ID: $userWalletId"
        }
        val blockchain = Blockchain.fromId(networkId.value)

        return getAndUpdateWalletManager(userWallet, blockchain, extraTokens)
    }

    private suspend fun getAndUpdateWalletManager(
        userWallet: UserWallet,
        blockchain: Blockchain,
        extraTokens: Set<DomainToken>,
    ): UpdateWalletManagerResult {
        val scanResponse = userWallet.scanResponse
        val derivationPath = blockchain.derivationPath(scanResponse.card.derivationStyle)

        if (derivationPath != null && !scanResponse.hasDerivation(blockchain, derivationPath.rawPath)) {
            Timber.e("Derivation missed for: $blockchain")
            return UpdateWalletManagerResult.MissedDerivation
        }

        val walletManager = getWalletManager(userWallet, blockchain, derivationPath, extraTokens)
        if (walletManager == null || blockchain == Blockchain.Unknown) {
            Timber.e("Unable to get a wallet manager for blockchain: $blockchain")
            return UpdateWalletManagerResult.Unreachable
        }

        return try {
            if (demoConfig.isDemoCardId(userWallet.scanResponse.card.cardId)) {
                updateDemoWalletManager(walletManager, extraTokens)
            } else {
                updateWalletManager(walletManager)
            }
        } finally {
            walletManagersStore.store(userWallet.walletId, walletManager)
        }
    }

    private fun updateDemoWalletManager(
        walletManager: WalletManager,
        tokens: Set<DomainToken>,
    ): UpdateWalletManagerResult {
        val amount = demoConfig.getBalance(walletManager.wallet.blockchain)
        walletManager.wallet.setAmount(amount)

        return resultFactory.getDemoResult(amount, tokens)
    }

    private suspend fun updateWalletManager(walletManager: WalletManager): UpdateWalletManagerResult {
        return try {
            walletManager.update()

            resultFactory.getResult(walletManager)
        } catch (e: BlockchainSdkError.AccountNotFound) {
            resultFactory.getNoAccountResult(walletManager)
        } catch (e: Throwable) {
            Timber.e(e, "Unable to update a wallet manager for: ${walletManager.wallet.blockchain}")

            UpdateWalletManagerResult.Unreachable
        }
    }

    private suspend fun getWalletManager(
        userWallet: UserWallet,
        blockchain: Blockchain,
        derivationPath: DerivationPath?,
        tokens: Set<DomainToken> = emptySet(),
    ): WalletManager? {
        val userWalletId = userWallet.walletId

        var walletManager = walletManagersStore.getSyncOrNull(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = derivationPath?.rawPath,
        )

        if (walletManager == null) {
            walletManager = walletManagerFactory.createWalletManager(
                scanResponse = userWallet.scanResponse,
                blockchain = blockchain,
                derivationPath = derivationPath,
            ) ?: return null

            walletManagersStore.store(userWalletId, walletManager)
        }

        updateWalletManagerTokensIfNeeded(walletManager, tokens)

        return walletManager
    }

    private fun updateWalletManagerTokensIfNeeded(walletManager: WalletManager, tokens: Set<DomainToken>) {
        if (tokens.isEmpty()) return

        val tokensToAdd = sdkTokenConverter
            .convertList(tokens.toList())
            .filter { it !in walletManager.cardTokens }

        walletManager.addTokens(tokensToAdd)
    }
}
