package com.tangem.tap.proxy

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.lib.crypto.models.Currency
import com.tangem.lib.crypto.models.Currency.NativeToken
import com.tangem.lib.crypto.models.Currency.NonNativeToken
import com.tangem.lib.crypto.models.ProxyAmount
import com.tangem.tap.userWalletsListManager
import timber.log.Timber
import java.math.BigDecimal

class UserWalletManagerImpl(
    private val walletManagersFacade: WalletManagersFacade,
    private val currenciesRepository: CurrenciesRepository,
    private val userWalletsStore: UserWalletsStore,
) : UserWalletManager {

    override suspend fun getUserTokens(
        networkId: String,
        derivationPath: String?,
        isExcludeCustom: Boolean,
    ): List<Currency> {
// [REDACTED_TODO_COMMENT]
        val userWallet = requireNotNull(userWalletsStore.selectedUserWalletOrNull) {
            "No user wallet selected"
        }
        return currenciesRepository.getMultiCurrencyWalletCurrenciesSync(userWallet.walletId)
            .filter {
                val checkCustom = if (isExcludeCustom) {
                    !it.isCustom
                } else {
                    true
                }
                val blockchain = Blockchain.fromId(it.network.id.value)

                blockchain.toNetworkId() == networkId &&
                    checkCustom &&
                    it.network.derivationPath.value == derivationPath
            }
            .map {
                val blockchain = Blockchain.fromId(it.network.id.value)

                if (it is CryptoCurrency.Token) {
                    NonNativeToken(
                        id = it.id.rawCurrencyId ?: "",
                        name = it.name,
                        symbol = it.symbol,
                        networkId = blockchain.toNetworkId(),
                        contractAddress = it.contractAddress,
                        decimalCount = it.decimals,
                    )
                } else {
                    NativeToken(
                        id = it.id.rawCurrencyId ?: "",
                        name = it.name,
                        symbol = it.symbol,
                        networkId = blockchain.toNetworkId(),
                    )
                }
            }
    }

    override fun getNativeTokenForNetwork(networkId: String): Currency {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        return NativeToken(
            id = blockchain.toCoinId(),
            name = blockchain.fullName,
            symbol = blockchain.currency,
            networkId = networkId,
        )
    }

    override fun getWalletId(): String {
        val selectedUserWallet = requireNotNull(
            userWalletsListManager.selectedUserWalletSync,
        ) { "selectedUserWallet shouldn't be null" }
        return selectedUserWallet.walletId.stringValue
    }

    override suspend fun isTokenAdded(currency: Currency, derivationPath: String?): Boolean {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(currency.networkId)) { "blockchain not found" }
        return try {
            val walletManager = getActualWalletManager(blockchain, derivationPath)
            walletManager.cardTokens.any {
                it.id == currency.id
            }
        } catch (e: IllegalArgumentException) {
            false
        }
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

    override suspend fun getCurrentWalletTokensBalance(
        networkId: String,
        extraTokens: List<Currency>,
        derivationPath: String?,
    ): Map<String, ProxyAmount> {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val walletManager = getActualWalletManager(blockchain, derivationPath)

        // workaround for get balance for tokens that doesn't exist in wallet
        val extraTokensToLoadBalance = extraTokens
            .filterIsInstance<NonNativeToken>()
            .map {
                it.toSdkToken()
            }
            .filter {
                !walletManager.cardTokens.contains(it)
            }
        walletManager.addTokens(extraTokensToLoadBalance)
        walletManager.update()
        val balances = walletManager.wallet.amounts.map { entry ->
            val amount = entry.value
            amount.currencySymbol to ProxyAmount(
                amount.currencySymbol,
                amount.value ?: BigDecimal.ZERO,
                amount.decimals,
            )
        }.toMap()
        extraTokensToLoadBalance.forEach { walletManager.removeToken(it) }
        return balances
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

private fun NonNativeToken.toSdkToken(): Token {
    return Token(
        id = this.id,
        name = this.name,
        symbol = this.symbol,
        contractAddress = this.contractAddress,
        decimals = this.decimalCount,
    )
}
