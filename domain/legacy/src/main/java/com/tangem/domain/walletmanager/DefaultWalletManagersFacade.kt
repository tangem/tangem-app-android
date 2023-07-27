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
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import com.tangem.domain.walletmanager.utils.SdkTokenConverter
import com.tangem.domain.walletmanager.utils.UpdateWalletManagerResultFactory
import com.tangem.domain.walletmanager.utils.WalletManagerFactory
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import timber.log.Timber
// [REDACTED_TODO_COMMENT]
@Deprecated("Inject the WalletManagerFacade interface using DI instead")
class DefaultWalletManagersFacade(
    private val walletManagersStore: WalletManagersStore,
    private val userWalletsStore: UserWalletsStore,
    configManager: ConfigManager,
) : WalletManagersFacade {

    private val demoConfig by lazy { DemoConfig() }
    private val resultFactory by lazy { UpdateWalletManagerResultFactory() }
    private val walletManagerFactory by lazy { WalletManagerFactory(configManager) }
    private val sdkTokenConverter by lazy { SdkTokenConverter() }

    override suspend fun update(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        extraTokens: Set<CryptoCurrency.Token>,
    ): UpdateWalletManagerResult {
        val userWallet = requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "Unable to find a user wallet with provided ID: $userWalletId"
        }
        val blockchain = Blockchain.fromId(networkId.value)

        return getAndUpdateWalletManager(userWallet, blockchain, extraTokens)
    }

    override suspend fun getExploreUrl(userWalletId: UserWalletId, networkId: Network.ID): String {
        val userWallet = requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "Unable to find a user wallet with provided ID: $userWalletId"
        }

        val blockchain = Blockchain.fromId(networkId.value)

        return getOrCreateWalletManager(
            userWallet = userWallet,
            blockchain = blockchain,
            derivationPath = blockchain.derivationPath(userWallet.scanResponse.card.derivationStyle),
        )
            ?.wallet
            ?.getExploreUrl()
            .orEmpty()
    }

    private suspend fun getAndUpdateWalletManager(
        userWallet: UserWallet,
        blockchain: Blockchain,
        extraTokens: Set<CryptoCurrency.Token>,
    ): UpdateWalletManagerResult {
        val scanResponse = userWallet.scanResponse
        val derivationPath = blockchain.derivationPath(scanResponse.card.derivationStyle)

        if (derivationPath != null && !scanResponse.hasDerivation(blockchain, derivationPath.rawPath)) {
            Timber.e("Derivation missed for: $blockchain")
            return UpdateWalletManagerResult.MissedDerivation
        }

        val walletManager = getOrCreateWalletManager(userWallet, blockchain, derivationPath)
        if (walletManager == null || blockchain == Blockchain.Unknown) {
            Timber.e("Unable to get a wallet manager for blockchain: $blockchain")
            return UpdateWalletManagerResult.Unreachable
        }

        updateWalletManagerTokensIfNeeded(walletManager, extraTokens)

        return try {
            if (demoConfig.isDemoCardId(userWallet.scanResponse.card.cardId)) {
                updateDemoWalletManager(walletManager)
            } else {
                updateWalletManager(walletManager)
            }
        } finally {
            walletManagersStore.store(userWallet.walletId, walletManager)
        }
    }

    private fun updateDemoWalletManager(walletManager: WalletManager): UpdateWalletManagerResult {
        val amount = demoConfig.getBalance(walletManager.wallet.blockchain)
        walletManager.wallet.setAmount(amount)

        return resultFactory.getDemoResult(walletManager, amount)
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

    private suspend fun getOrCreateWalletManager(
        userWallet: UserWallet,
        blockchain: Blockchain,
        derivationPath: DerivationPath?,
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

        return walletManager
    }

    private fun updateWalletManagerTokensIfNeeded(walletManager: WalletManager, tokens: Set<CryptoCurrency.Token>) {
        if (tokens.isEmpty()) return

        val tokensToAdd = sdkTokenConverter
            .convertList(tokens)
            .filter { it !in walletManager.cardTokens }

        walletManager.addTokens(tokensToAdd)
    }
}
