package com.tangem.data.tokens

import arrow.core.Either
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.data.tokens.utils.CustomTokensMerger
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.express.models.TangemExpressValues.EMPTY_CONTRACT_ADDRESS_VALUE
import com.tangem.datasource.api.express.models.request.LeastTokenInfo
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.exchangeservice.swap.ExpressServiceLoader
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.models.wallet.requireColdWallet
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesFetcher
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesFetcher.Params
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Default implementation of [MultiWalletCryptoCurrenciesFetcher]
 *
 * @property userWalletsStore          [UserWallet]'s store
 * @property tangemTechApi             Tangem Tech API
 * @property userTokensResponseStore   store of [UserTokensResponse]
 * @property userTokensSaver           user tokens saver
 * @property cardCryptoCurrencyFactory factory for creating crypto currencies for specified card
 * @property expressServiceLoader      express service loader
 * @property dispatchers               dispatchers
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
internal class DefaultMultiWalletCryptoCurrenciesFetcher(
    private val demoConfig: DemoConfig,
    private val userWalletsStore: UserWalletsStore,
    private val tangemTechApi: TangemTechApi,
    private val customTokensMerger: CustomTokensMerger,
    private val userTokensResponseStore: UserTokensResponseStore,
    private val userTokensSaver: UserTokensSaver,
    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
    private val expressServiceLoader: ExpressServiceLoader,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiWalletCryptoCurrenciesFetcher {

    private val userTokensResponseFactory = UserTokensResponseFactory()

    override suspend fun invoke(params: Params) = Either.catchOn(dispatchers.default) {
        val userWallet = userWalletsStore.getSyncStrict(key = params.userWalletId)

        if (!userWallet.isMultiCurrency) error("${this::class.simpleName} supports only multi-currency wallet")

        val response = if (userWallet is UserWallet.Cold && userWallet.isDemoWalletWithoutSavedTokens()) {
            createDefaultUserTokensResponse(userWallet = userWallet)
        } else {
            safeApiCall(
                call = {
                    withContext(dispatchers.io) {
                        tangemTechApi.getUserTokens(userId = userWallet.walletId.stringValue).bind()
                    }
                },
                onError = {
                    handleFetchTokensError(error = it, userWallet = userWallet.requireColdWallet()) // TODO 11142
                },
            )
        }

        val compatibleUserTokensResponse = response
            .let { it.copy(tokens = it.tokens.distinct()) }
            .let { customTokensMerger.mergeIfPresented(userWalletId = userWallet.walletId, response = it) }

        userTokensSaver.store(userWalletId = userWallet.walletId, response = compatibleUserTokensResponse)

        fetchExpressAssetsByNetworkIds(userWallet = userWallet, userTokens = compatibleUserTokensResponse)
    }

    private suspend fun UserWallet.Cold.isDemoWalletWithoutSavedTokens(): Boolean {
        val isDemoCard = demoConfig.isDemoCardId(cardId = cardId)

        return if (isDemoCard) {
            val response = userTokensResponseStore.getSyncOrNull(userWalletId = walletId)

            response == null
        } else {
            false
        }
    }

    private suspend fun handleFetchTokensError(error: ApiResponseError, userWallet: UserWallet): UserTokensResponse {
        val userWalletId = userWallet.walletId

        val response = userTokensResponseStore.getSyncOrNull(userWalletId = userWalletId)
            ?: createDefaultUserTokensResponse(userWallet = userWallet)

        if (error is ApiResponseError.HttpException && error.code == ApiResponseError.HttpException.Code.NOT_FOUND) {
            Timber.w(error, "Requested currencies could not be found in the remote store for: $userWalletId")

            userTokensSaver.push(userWalletId, response)
        }

        return response
    }

    private suspend fun fetchExpressAssetsByNetworkIds(userWallet: UserWallet, userTokens: UserTokensResponse) {
        val tokens = userTokens.tokens.map { token ->
            LeastTokenInfo(
                contractAddress = token.contractAddress ?: EMPTY_CONTRACT_ADDRESS_VALUE,
                network = token.networkId,
            )
        }

        expressServiceLoader.update(userWallet = userWallet, userTokens = tokens)
    }

    private fun createDefaultUserTokensResponse(userWallet: UserWallet): UserTokensResponse {
        return userTokensResponseFactory.createUserTokensResponse(
            currencies = cardCryptoCurrencyFactory.createDefaultCoinsForMultiCurrencyWallet(userWallet),
            isGroupedByNetwork = false,
            isSortedByBalance = false,
        )
    }
}