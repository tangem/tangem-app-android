package com.tangem.tap.domain.tokens

import android.content.Context
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.common.services.Result
import com.tangem.datasource.api.tangemTech.TangemTechService
import com.tangem.datasource.api.tangemTech.UserTokensResponse
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.util.userWalletId
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
    suspend fun getUserTokens(card: CardDTO): List<Currency> {
        val userId = card.userWalletId.stringValue
        if (DemoHelper.isDemoCardId(card.cardId)) {
            return loadTokensOffline(card, userId).ifEmpty { loadDemoCurrencies() }
        }

        if (!NetworkConnectivity.getInstance().isOnlineOrConnecting()) {
            return loadTokensOffline(card, userId)
        }

        return when (val networkResult = networkService.getUserTokens(userId)) {
            is Result.Success -> {
                val tokens = networkResult.data.tokens.mapNotNull { Currency.fromTokenResponse(it) }
                storageService.saveUserTokens(userId, tokens.toUserTokensResponse())
                tokens.distinct()
            }

            is Result.Failure -> {
                handleGetUserTokensFailure(card = card, userId = userId, error = networkResult.error)
            }
        }
    }

    suspend fun saveUserTokens(card: CardDTO, tokens: List<Currency>) {
        val userId = card.userWalletId.stringValue
        val userTokens = tokens.toUserTokensResponse()
        networkService.saveUserTokens(userId, userTokens)
        storageService.saveUserTokens(userId, userTokens)
    }

    suspend fun removeUserTokens(card: CardDTO) {
        val userId = card.userWalletId.stringValue
        val userTokens = emptyList<Currency>().toUserTokensResponse()
        networkService.saveUserTokens(userId, userTokens)
        storageService.saveUserTokens(userId, userTokens)
    }

    private fun List<Currency>.toUserTokensResponse(): UserTokensResponse {
        val tokensResponse = this.map { it.toTokenResponse() }
        return UserTokensResponse(
            tokens = tokensResponse,
            group = GROUP_DEFAULT_VALUE,
            sort = SORT_DEFAULT_VALUE,
        )
    }

    suspend fun loadBlockchainsToDerive(card: CardDTO): List<BlockchainNetwork> {
        val userId = card.userWalletId.stringValue
        val blockchainNetworks = loadTokensOffline(card, userId).toBlockchainNetworks()

        if (DemoHelper.isDemoCardId(card.cardId)) {
            return blockchainNetworks
                .ifEmpty { loadDemoCurrencies().toBlockchainNetworks() }
        }

        return blockchainNetworks
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
        card: CardDTO,
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
                tokens.distinct()
            }
        }
    }

    private suspend fun loadTokensOffline(card: CardDTO, userId: String): List<Currency> {
        return storageService.getUserTokens(userId) ?: storageService.getUserTokens(card)
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
