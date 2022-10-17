package com.tangem.tap.domain.tokens

import android.content.Context
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.common.card.Card
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toHexString
import com.tangem.common.services.Result
import com.tangem.domain.common.extensions.calculateHmacSha256
import com.tangem.network.api.tangemTech.TangemTechService
import com.tangem.network.api.tangemTech.UserTokensResponse
import com.tangem.tap.common.AndroidFileReader
import com.tangem.tap.domain.NoDataError
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.toBlockchainNetworks
import com.tangem.tap.features.wallet.models.toCurrencies
import com.tangem.tap.network.NetworkConnectivity
import com.tangem.tap.store
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class UserTokensRepository(
    private val storageService: UserTokensStorageService,
    private val networkService: UserTokensNetworkService,
) {
    suspend fun getUserTokens(card: Card): List<Currency> {
        if (DemoHelper.isDemoCardId(card.cardId)) {
            return loadDemoCurrencies()
        }
        val userId = card.getUserId()
        if (!NetworkConnectivity.getInstance().isOnlineOrConnecting()) {
            return loadTokensOffline(card, userId)
        }

        return when (val networkResult = networkService.getUserTokens(userId)) {
            is Result.Success -> {
                val tokens = networkResult.data.tokens.map { Currency.fromTokenResponse(it) }
                storageService.saveUserTokens(card.getUserId(), tokens.toUserTokensResponse())
                tokens
            }
            is Result.Failure -> {
                handleGetUserTokensFailure(card = card, userId = userId, error = networkResult.error)
            }
        }
    }

    suspend fun saveUserTokens(card: Card, tokens: List<Currency>) {
        val userTokens = tokens.toUserTokensResponse()
        networkService.saveUserTokens(card.getUserId(), userTokens)
        storageService.saveUserTokens(card.getUserId(), userTokens)
    }

    suspend fun removeUserTokens(card: Card) {
        val userTokens = emptyList<Currency>().toUserTokensResponse()
        networkService.saveUserTokens(card.getUserId(), userTokens)
        storageService.saveUserTokens(card.getUserId(), userTokens)
    }

    private fun List<Currency>.toUserTokensResponse(): UserTokensResponse {
        val tokensResponse = this.map { it.toTokenResponse() }
        return UserTokensResponse(
            tokens = tokensResponse,
            group = GROUP_DEFAULT_VALUE,
            sort = SORT_DEFAULT_VALUE,
        )
    }

    fun loadBlockchainsToDerive(card: Card): List<BlockchainNetwork> {
        return storageService.getUserTokens(card.getUserId())?.toBlockchainNetworks() ?: emptyList()
    }

    private fun loadDemoCurrencies(): List<Currency> {
        return DemoHelper.config.demoBlockchains.map {
            BlockchainNetwork(
                blockchain = it,
                derivationPath = it.derivationPath(DerivationStyle.LEGACY)?.rawPath,
                tokens = emptyList(),
            )
        }.flatMap { it.toCurrencies() }
    }

    private suspend fun handleGetUserTokensFailure(
        card: Card,
        userId: String,
        error: Throwable,
    ): List<Currency> {
        return when (error) {
            is NoDataError -> {
                val tokens = storageService.getUserTokens(card)
                val userTokens = tokens.toUserTokensResponse()
                coroutineScope { launch { networkService.saveUserTokens(userId = userId, tokens = userTokens) } }
                tokens
            }
            else -> {
                val tokens = storageService.getUserTokens(userId) ?: storageService.getUserTokens(card)
                tokens
            }
        }
    }

    private suspend fun loadTokensOffline(card: Card, userId: String): List<Currency> {
        return storageService.getUserTokens(userId) ?: storageService.getUserTokens(card)
    }

    private fun Card.getUserId(): String {
        val walletPublicKey = this.wallets.firstOrNull()?.publicKey ?: return ""
        return UserWalletId(walletPublicKey).stringValue
    }

    companion object {
        const val SORT_DEFAULT_VALUE = "manual"
        const val GROUP_DEFAULT_VALUE = "none"
        fun init(context: Context, tangemTechService: TangemTechService): UserTokensRepository {
            val fileReader = AndroidFileReader(context)
            val oldUserTokensRepository = OldUserTokensRepository(
                fileReader, store.state.domainNetworks.tangemTechService,
            )
            val storageService = UserTokensStorageService(oldUserTokensRepository, fileReader)
            val networkService = UserTokensNetworkService(tangemTechService)
            return UserTokensRepository(storageService, networkService)
        }
    }
}

data class UserWalletId(
    val walletPublicKey: ByteArray,
) {
    val stringValue: String = calculateUserId(walletPublicKey)

    private fun calculateUserId(walletPublicKey: ByteArray): String {
        val message = "UserWalletID".toByteArray()
        val keyHash = walletPublicKey.calculateSha256()
        return message.calculateHmacSha256(keyHash).toHexString()
    }
}