package com.tangem.tap.domain.tokens

import android.content.Context
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.common.core.TangemSdkError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.TangemTechService
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.common.CardDTO
import com.tangem.tap.common.AndroidFileReader
import com.tangem.tap.domain.model.builders.UserWalletIdBuilder
import com.tangem.tap.domain.tokens.converters.CurrencyConverter
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.toBlockchainNetworks
import com.tangem.tap.features.wallet.models.toCurrencies
import com.tangem.tap.network.NetworkConnectivity
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

class UserTokensRepository(
    private val storageService: UserTokensStorageService,
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    // TODO("After adding DI") replace with CoroutineDispatcherProvider
    suspend fun getUserTokens(card: CardDTO): List<Currency> = withContext(dispatchers.io) {
        val userId = getUserWalletId(card) ?: return@withContext emptyList()
        if (DemoHelper.isDemoCardId(card.cardId)) {
            return@withContext loadTokensOffline(card, userId).ifEmpty(::loadDemoCurrencies)
        }

        if (!NetworkConnectivity.getInstance().isOnlineOrConnecting()) {
            return@withContext loadTokensOffline(card, userId)
        }

        runCatching { tangemTechApi.getUserTokens(userId) }
            .onSuccess { response ->
                return@withContext response.tokens
                    .mapNotNull(Currency.Companion::fromTokenResponse).also {
                        storageService.saveUserTokens(userId, it.toUserTokensResponse())
                    }
                    .distinct()
            }
            .onFailure {
                return@withContext handleGetUserTokensFailure(card = card, userId = userId, error = it)
            }

        error("Unreachable code because runCatching must return result")
    }

    // TODO("After adding DI") replace with CoroutineDispatcherProvider
    suspend fun saveUserTokens(card: CardDTO, tokens: List<Currency>) = withContext(dispatchers.io) {
        val userId = getUserWalletId(card) ?: return@withContext
        val userTokens = tokens.toUserTokensResponse()
        tangemTechApi.saveUserTokens(userId, userTokens)
        storageService.saveUserTokens(userId, userTokens)
    }

    suspend fun loadBlockchainsToDerive(card: CardDTO): List<BlockchainNetwork> = withContext(dispatchers.io) {
        val userId = getUserWalletId(card) ?: return@withContext emptyList()
        val blockchainNetworks = loadTokensOffline(card = card, userId = userId).toBlockchainNetworks()

        if (DemoHelper.isDemoCardId(card.cardId)) {
            return@withContext blockchainNetworks.ifEmpty(loadDemoCurrencies()::toBlockchainNetworks)
        }

        return@withContext blockchainNetworks
    }

    private suspend fun loadTokensOffline(card: CardDTO, userId: String): List<Currency> {
        return storageService.getUserTokens(userId) ?: storageService.getUserTokens(card)
    }

    private fun loadDemoCurrencies(): List<Currency> {
        return DemoHelper.config.demoBlockchains
            .map { blockchain ->
                BlockchainNetwork(
                    blockchain = blockchain,
                    derivationPath = blockchain.derivationPath(DerivationStyle.LEGACY)?.rawPath,
                    tokens = emptyList(),
                )
            }
            .flatMap(BlockchainNetwork::toCurrencies)
    }

    private fun List<Currency>.toUserTokensResponse() = UserTokensResponse(
        tokens = CurrencyConverter.convertList(this),
        group = GROUP_DEFAULT_VALUE,
        sort = SORT_DEFAULT_VALUE,
    )

    private suspend fun handleGetUserTokensFailure(card: CardDTO, userId: String, error: Throwable): List<Currency> {
        return when {
            error is TangemSdkError.NetworkError && error.customMessage.contains(NOT_FOUND_HTTP_CODE) ->
                storageService.getUserTokens(card).also {
                    tangemTechApi.saveUserTokens(userId = userId, userTokens = it.toUserTokensResponse())
                }
            else -> {
                val tokens = storageService.getUserTokens(userId) ?: storageService.getUserTokens(card)
                tokens.distinct()
            }
        }
    }

    private fun getUserWalletId(card: CardDTO): String? {
        return UserWalletIdBuilder.card(card).build()
            ?.stringValue
    }

    companion object {
        private const val GROUP_DEFAULT_VALUE = "none"
        private const val SORT_DEFAULT_VALUE = "manual"
        private const val NOT_FOUND_HTTP_CODE = "404"

        // TODO("After adding DI") get dependencies by DI
        fun init(context: Context, tangemTechService: TangemTechService): UserTokensRepository {
            val fileReader = AndroidFileReader(context)
            val dispatchers = AppCoroutineDispatcherProvider()

            val oldUserTokensRepository = OldUserTokensRepository(
                fileReader = fileReader,
                tangemTechApi = tangemTechService.api,
                dispatchers = dispatchers,
            )
            val storageService = UserTokensStorageService(
                oldUserTokensRepository = oldUserTokensRepository,
                fileReader = fileReader,
            )

            return UserTokensRepository(
                storageService = storageService,
                tangemTechApi = tangemTechService.api,
                dispatchers = dispatchers,
            )
        }
    }
}
