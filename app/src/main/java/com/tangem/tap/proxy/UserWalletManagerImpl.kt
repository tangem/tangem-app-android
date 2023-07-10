package com.tangem.tap.proxy

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.doOnFailure
import com.tangem.common.extensions.guard
import com.tangem.domain.common.BlockchainNetwork
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.common.extensions.toCoinId
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.lib.crypto.models.Currency
import com.tangem.lib.crypto.models.Currency.NativeToken
import com.tangem.lib.crypto.models.Currency.NonNativeToken
import com.tangem.lib.crypto.models.ProxyAmount
import com.tangem.lib.crypto.models.ProxyFiatCurrency
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.domain.model.builders.UserWalletIdBuilder
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.userWalletsListManager
import com.tangem.tap.walletCurrenciesManager
import com.tangem.tap.walletStoresManager
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import java.math.BigDecimal
import com.tangem.tap.features.wallet.models.Currency as WalletCurrency

class UserWalletManagerImpl(
    private val appStateHolder: AppStateHolder,
) : UserWalletManager {

    override suspend fun getUserTokens(
        networkId: String,
        derivationPath: String?,
        isExcludeCustom: Boolean,
    ): List<Currency> {
        val card = appStateHolder.getActualCard()
        val userTokensRepository =
            requireNotNull(appStateHolder.userTokensRepository) { "userTokensRepository is null" }
        return if (card != null) {
            userTokensRepository.getUserTokens(card)
                .filter {
                    val checkCustom = if (isExcludeCustom) {
                        !it.isCustomCurrency(null)
                    } else {
                        true
                    }
                    it.blockchain.toNetworkId() == networkId &&
                        checkCustom &&
                        it.derivationPath == derivationPath
                }
                .map {
                    if (it is com.tangem.tap.features.wallet.models.Currency.Token) {
                        NonNativeToken(
                            id = it.coinId ?: "",
                            name = it.currencyName,
                            symbol = it.currencySymbol,
                            networkId = it.blockchain.toNetworkId(),
                            contractAddress = it.token.contractAddress,
                            decimalCount = it.token.decimals,
                        )
                    } else {
                        NativeToken(
                            id = it.coinId ?: "",
                            name = it.currencyName,
                            symbol = it.currencySymbol,
                            networkId = it.blockchain.toNetworkId(),
                        )
                    }
                }
        } else {
            emptyList()
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
        return appStateHolder.getActualCard()?.let {
            UserWalletIdBuilder.card(it)
                .build()
                ?.stringValue
        }
            ?: ""
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

    override suspend fun addToken(currency: Currency, derivationPath: String?) {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(currency.networkId)) { "blockchain not found" }
        val blockchainNetwork = BlockchainNetwork(blockchain, derivationPath, emptyList())

        val selectedUserWallet = userWalletsListManager.selectedUserWalletSync.guard {
            Timber.e("Unable to add token, no user wallet selected")
            return
        }
        walletCurrenciesManager.addCurrencies(
            userWallet = selectedUserWallet,
            currenciesToAdd = listOf(currency.toWalletCurrency(blockchainNetwork)),
        )
    }

    override suspend fun hideAllTokens() {
        val userWallet = userWalletsListManager.selectedUserWalletSync.guard {
            Timber.e("No user wallets selected")
            return
        }

        val currencies = walletStoresManager.get(userWallet.walletId)
            .firstOrNull()
            ?.flatMap { walletStore ->
                walletStore.walletsData.map { it.currency }
            }
            .guard {
                Timber.d("No currencies found")
                return
            }

        walletCurrenciesManager.removeCurrencies(userWallet, currenciesToRemove = currencies)
            .doOnFailure { e ->
                Timber.e(e, "Unable to delete all currencies")
            }
    }

    override fun getWalletAddress(networkId: String, derivationPath: String?): String {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val walletManager = getActualWalletManager(blockchain, derivationPath)
        return walletManager.wallet.address
    }

    override fun getLastTransactionHash(networkId: String, derivationPath: String?): String? {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val walletManager = getActualWalletManager(blockchain, derivationPath)
        return walletManager.wallet.recentTransactions
            .lastOrNull { it.hash?.isNotEmpty() == true }
            ?.hash?.let { HEX_PREFIX + it }
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

    override fun getNativeTokenBalance(networkId: String, derivationPath: String?): ProxyAmount? {
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

    override fun getNetworkCurrency(networkId: String): String {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        return blockchain.currency
    }

    override fun getUserAppCurrency(): ProxyFiatCurrency {
        val appCurrency = appStateHolder.appFiatCurrency
        return ProxyFiatCurrency(
            code = appCurrency.code,
            name = appCurrency.name,
            symbol = appCurrency.symbol,
        )
    }

    override fun refreshWallet() {
        // workaround, should update wallet after transaction
        appStateHolder.mainStore?.dispatchOnMain(WalletAction.LoadData.Refresh)
    }

    @kotlin.jvm.Throws(IllegalArgumentException::class)
    private fun getActualWalletManager(blockchain: Blockchain, derivationPath: String?): WalletManager {
        val blockchainNetwork = BlockchainNetwork(blockchain, derivationPath, emptyList())
        return requireNotNull(appStateHolder.walletState?.getWalletManager(blockchainNetwork)) {
            "No wallet manager found"
        }
    }

    companion object {
        private const val HEX_PREFIX = "0x"
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

private fun Currency.toWalletCurrency(network: BlockchainNetwork): WalletCurrency {
    return when (this) {
        is NativeToken -> WalletCurrency.Blockchain(
            blockchain = network.blockchain,
            derivationPath = network.derivationPath,
        )
        is NonNativeToken -> WalletCurrency.Token(
            token = this.toSdkToken(),
            blockchain = network.blockchain,
            derivationPath = network.derivationPath,
        )
    }
}