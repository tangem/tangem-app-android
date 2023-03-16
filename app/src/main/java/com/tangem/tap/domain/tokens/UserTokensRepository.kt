package com.tangem.tap.domain.tokens

import android.content.Context
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.common.core.TangemSdkError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.TangemTechService
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.connection.NetworkConnectionManager
import com.tangem.domain.common.CardDTO
import com.tangem.tap.common.AndroidFileReader
import com.tangem.tap.domain.model.builders.UserWalletIdBuilder
import com.tangem.tap.domain.tokens.converters.CurrencyConverter
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.toBlockchainNetworks
import com.tangem.tap.features.wallet.models.toCurrencies
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber

class UserTokensRepository(
    private val storageService: UserTokensStorageService,
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val networkConnectionManager: NetworkConnectionManager,
) {

    suspend fun getUserTokens(card: CardDTO): List<Currency> = withContext(dispatchers.io) {
        val userId = getUserWalletId(card) ?: return@withContext emptyList()

        if (DemoHelper.isDemoCardId(card.cardId)) {
            return@withContext loadTokensOffline(userId).ifEmpty(::loadDemoCurrencies)
        }

        if (!networkConnectionManager.isOnline) return@withContext loadTokensOffline(userId)

        return@withContext remoteGetUserTokens(userId)
    }

    suspend fun saveUserTokens(card: CardDTO, tokens: List<Currency>) = withContext(dispatchers.io) {
        val userId = getUserWalletId(card) ?: return@withContext
        val userTokens = tokens.toUserTokensResponse()
        remoteSaveUserTokens(userId = userId, userTokens = userTokens)
        storageService.saveUserTokens(userId = userId, tokens = userTokens)
    }

    suspend fun loadBlockchainsToDerive(card: CardDTO): List<BlockchainNetwork> = withContext(dispatchers.io) {
        val userId = getUserWalletId(card) ?: return@withContext emptyList()
        val blockchainNetworks = loadTokensOffline(userId = userId).toBlockchainNetworks()

        if (DemoHelper.isDemoCardId(card.cardId)) {
            return@withContext blockchainNetworks.ifEmpty(loadDemoCurrencies()::toBlockchainNetworks)
        }

        return@withContext blockchainNetworks
    }

    private fun loadTokensOffline(userId: String): List<Currency> = storageService.getUserTokens(userId) ?: emptyList()

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

    private fun List<Currency>.toUserTokensResponse(): UserTokensResponse {
        return UserTokensResponse(
            tokens = CurrencyConverter.convertList(input = this),
            group = GROUP_DEFAULT_VALUE,
            sort = SORT_DEFAULT_VALUE,
        )
    }

    private suspend fun handleGetUserTokensFailure(userId: String, error: Throwable): List<Currency> {
        return when {
            error is TangemSdkError.NetworkError && error.customMessage.contains(NOT_FOUND_HTTP_CODE) -> {
                storageService
                    .getUserTokens(userId)
                    ?.also { remoteSaveUserTokens(userId = userId, userTokens = it.toUserTokensResponse()) }
                    ?: emptyList()
            }
            else -> {
                storageService.getUserTokens(userId)?.distinct() ?: emptyList()
            }
        }
    }

    private suspend fun remoteGetUserTokens(userId: String): List<Currency> {
        return runCatching { tangemTechApi.getUserTokens(userId) }
            .fold(
                onSuccess = { response ->
                    response.tokens
                        .mapNotNull(Currency.Companion::fromTokenResponse)
                        .also { storageService.saveUserTokens(userId, it.toUserTokensResponse()) }
                        .distinct()
                },
                onFailure = { handleGetUserTokensFailure(userId = userId, error = it) },
            )
    }

    private suspend fun remoteSaveUserTokens(userId: String, userTokens: UserTokensResponse) {
        // it can throw okhttp3.internal.http2.StreamResetException: stream was reset: INTERNAL_ERROR
        // if the /user-tokens endpoint disabled
        runCatching { tangemTechApi.saveUserTokens(userId, userTokens) }
            .onFailure { Timber.e(it) }
    }

    private fun getUserWalletId(card: CardDTO): String? = UserWalletIdBuilder.card(card).build()?.stringValue

    companion object {
        private const val GROUP_DEFAULT_VALUE = "none"
        private const val SORT_DEFAULT_VALUE = "manual"
        private const val NOT_FOUND_HTTP_CODE = "404"

        // TODO("After adding DI") get dependencies by DI
        fun init(
            context: Context,
            tangemTechService: TangemTechService,
            networkConnectionManager: NetworkConnectionManager,
        ): UserTokensRepository {
            return UserTokensRepository(
                storageService = UserTokensStorageService(fileReader = AndroidFileReader(context)),
                tangemTechApi = tangemTechService.api,
                dispatchers = AppCoroutineDispatcherProvider(),
                networkConnectionManager = networkConnectionManager,
            )
        }
    }
}
