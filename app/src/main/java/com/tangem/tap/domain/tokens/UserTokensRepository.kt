package com.tangem.tap.domain.tokens

import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.common.core.TangemSdkError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.TangemTechService
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.connection.NetworkConnectionManager
import com.tangem.domain.common.BlockchainNetwork
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.userwallets.UserWalletIdBuilder
import com.tangem.tap.domain.tokens.converters.CurrencyConverter
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

    suspend fun getUserTokens(card: CardDTO, derivationStyle: DerivationStyle?): List<Currency> =
        withContext(dispatchers.io) {
            val userWalletId = getUserWalletId(card) ?: return@withContext emptyList()

            if (DemoHelper.isDemoCardId(card.cardId)) {
                return@withContext loadTokensOffline(userWalletId = userWalletId).ifEmpty {
                    loadDemoCurrencies(
                        derivationStyle,
                    )
                }
            }

            if (!networkConnectionManager.isOnline) return@withContext loadTokensOffline(userWalletId = userWalletId)

            return@withContext remoteGetUserTokens(userWalletId = userWalletId)
        }

    suspend fun saveUserTokens(card: CardDTO, tokens: List<Currency>) = withContext(dispatchers.io) {
        val userWalletId = getUserWalletId(card) ?: return@withContext
        val userTokens = tokens.toUserTokensResponse()
        remoteSaveUserTokens(userWalletId = userWalletId, userTokens = userTokens)
        storageService.saveUserTokens(userWalletId = userWalletId, tokens = userTokens)
    }

    // FIXME: Move to data layer
    suspend fun loadBlockchainsToDerive(card: CardDTO, derivationStyle: DerivationStyle?): List<BlockchainNetwork> =
        withContext(dispatchers.io) {
            val userWalletId = getUserWalletId(card) ?: return@withContext emptyList()
            val blockchainNetworks = loadTokensOffline(userWalletId = userWalletId).toBlockchainNetworks()

            if (DemoHelper.isDemoCardId(card.cardId)) {
                return@withContext blockchainNetworks.ifEmpty(loadDemoCurrencies(derivationStyle)::toBlockchainNetworks)
            }

            return@withContext blockchainNetworks
        }

    private fun loadTokensOffline(userWalletId: String): List<Currency> {
        return storageService.getUserTokens(userWalletId = userWalletId) ?: emptyList()
    }

    // FIXME: Move to user wallet config
    private fun loadDemoCurrencies(derivationStyle: DerivationStyle?): List<Currency> {
        return DemoHelper.config.demoBlockchains
            .map { blockchain ->
                BlockchainNetwork(
                    blockchain = blockchain,
                    derivationPath = blockchain.derivationPath(derivationStyle)?.rawPath,
                    tokens = emptyList(),
                )
            }
            .flatMap(BlockchainNetwork::toCurrencies)
    }

    private fun List<Currency>.toUserTokensResponse(): UserTokensResponse {
        return UserTokensResponse(
            tokens = CurrencyConverter.convertList(input = this),
            group = UserTokensResponse.GroupType.NONE,
            sort = UserTokensResponse.SortType.MANUAL,
        )
    }

    private suspend fun handleGetUserTokensFailure(userWalletId: String, error: Throwable): List<Currency> {
        return when {
            error is TangemSdkError.NetworkError && error.customMessage.contains(NOT_FOUND_HTTP_CODE) -> {
                storageService
                    .getUserTokens(userWalletId)
                    ?.also { remoteSaveUserTokens(userWalletId = userWalletId, userTokens = it.toUserTokensResponse()) }
                    ?: emptyList()
            }
            else -> {
                storageService.getUserTokens(userWalletId)?.distinct() ?: emptyList()
            }
        }
    }

    private suspend fun remoteGetUserTokens(userWalletId: String): List<Currency> {
        return runCatching { tangemTechApi.getUserTokens(userWalletId) }
            .fold(
                onSuccess = { response ->
                    response.getOrThrow()
                        .also { storageService.saveUserTokens(userWalletId, it) }
                        .tokens
                        .mapNotNull(Currency.Companion::fromTokenResponse)
                        .distinct()
                },
                onFailure = { handleGetUserTokensFailure(userWalletId = userWalletId, error = it) },
            )
    }

    private suspend fun remoteSaveUserTokens(userWalletId: String, userTokens: UserTokensResponse) {
        // it can throw okhttp3.internal.http2.StreamResetException: stream was reset: INTERNAL_ERROR
        // if the /user-tokens endpoint disabled
        runCatching { tangemTechApi.saveUserTokens(userWalletId, userTokens) }
            .onFailure { Timber.e(it) }
    }

    private fun getUserWalletId(card: CardDTO): String? = UserWalletIdBuilder.card(card).build()?.stringValue

    companion object {
        private const val NOT_FOUND_HTTP_CODE = "404"

        // TODO("After adding DI") get dependencies by DI
        fun init(
            tangemTechService: TangemTechService,
            networkConnectionManager: NetworkConnectionManager,
            storageService: UserTokensStorageService,
        ): UserTokensRepository {
            return UserTokensRepository(
                storageService = storageService,
                tangemTechApi = tangemTechService.api,
                dispatchers = AppCoroutineDispatcherProvider(),
                networkConnectionManager = networkConnectionManager,
            )
        }
    }
}
