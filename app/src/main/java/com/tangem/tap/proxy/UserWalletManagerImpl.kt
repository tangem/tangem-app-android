package com.tangem.tap.proxy

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.lib.crypto.models.ProxyAmount
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.math.BigDecimal

class UserWalletManagerImpl(
    private val walletManagersFacade: WalletManagersFacade,
    private val getSelectedWalletUseCase: GetSelectedWalletUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
) : UserWalletManager {

    override fun getWalletId(): String {
        val selectedUserWallet = requireNotNull(
            getSelectedWalletUseCase.sync().getOrNull(),
        ) { "selectedUserWallet shouldn't be null" }
        return selectedUserWallet.walletId.stringValue
    }

    override suspend fun hideAllTokens() {
        // FIXME: Used only in Tester Actions
        Timber.w("Not implemented")
    }

    override suspend fun getWalletAddress(networkId: String, derivationPath: String?): String {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val walletManager = getActualWalletManager(blockchain, derivationPath)
        return walletManager.wallet.address
    }

    override suspend fun getLastTransactionHash(networkId: String, derivationPath: String?): String? {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val walletManager = getActualWalletManager(blockchain, derivationPath)
        return walletManager.wallet.recentTransactions
            .lastOrNull { it.hash?.isNotEmpty() == true }
            ?.hash
    }

    override suspend fun getNativeTokenBalance(networkId: String, derivationPath: String?): ProxyAmount? {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val walletManager = getActualWalletManager(blockchain, derivationPath)
        return walletManager.wallet.amounts.firstNotNullOfOrNull {
            it.takeIf { it.key is AmountType.Coin }
        }?.value?.let {
            ProxyAmount(
                it.currencySymbol,
                it.value ?: BigDecimal.ZERO,
                it.decimals,
            )
        }
    }

    @Throws(IllegalArgumentException::class)
    private suspend fun getActualWalletManager(blockchain: Blockchain, derivationPath: String?): WalletManager {
        val selectedUserWallet = requireNotNull(
            getSelectedWalletUseCase.sync().getOrNull(),
        ) { "userWallet or userWalletsListManager is null" }
        val walletManager = withContext(dispatchers.io) {
            walletManagersFacade.getOrCreateWalletManager(
                selectedUserWallet.walletId,
                blockchain,
                derivationPath,
            )
        }

        return requireNotNull(walletManager) {
            "No wallet manager found"
        }
    }
}