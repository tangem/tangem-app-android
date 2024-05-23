package com.tangem.tap.proxy

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.lib.crypto.models.ProxyAmount
import timber.log.Timber
import java.math.BigDecimal

class UserWalletManagerImpl(
    private val walletManagersFacade: WalletManagersFacade,
    private val userWalletsListManager: UserWalletsListManager,
) : UserWalletManager {

    override fun getWalletId(): String {
        val selectedUserWallet = requireNotNull(
            userWalletsListManager.selectedUserWalletSync,
        ) { "selectedUserWallet shouldn't be null" }
        return selectedUserWallet.walletId.stringValue
    }

    override suspend fun hideAllTokens() {
// [REDACTED_TODO_COMMENT]
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
            userWalletsListManager.selectedUserWalletSync,
        ) { "userWallet or userWalletsListManager is null" }
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            selectedUserWallet.walletId,
            blockchain,
            derivationPath,
        )

        return requireNotNull(walletManager) {
            "No wallet manager found"
        }
    }
}
