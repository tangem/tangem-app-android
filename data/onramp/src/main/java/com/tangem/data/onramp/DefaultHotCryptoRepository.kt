package com.tangem.data.onramp

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.NEW_POLYGON_NAME
import com.tangem.blockchainsdk.utils.OLD_POLYGON_NAME
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.onramp.converters.HotCryptoCurrencyConverter
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.HotCryptoResponse
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.toUserTokensResponse
import com.tangem.datasource.appcurrency.AppCurrencyResponseStore
import com.tangem.datasource.exchangeservice.hotcrypto.HotCryptoResponseStore
import com.tangem.domain.card.common.extensions.canHandleBlockchain
import com.tangem.domain.card.common.extensions.canHandleToken
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.common.wallets.loadAndGet
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.HotCryptoCurrency
import com.tangem.domain.onramp.repositories.HotCryptoRepository
import com.tangem.utils.coroutines.*
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.plus

/**
 * Default implementation of [HotCryptoRepository]
 *
 * @property excludedBlockchains      excluded blockchains
 * @property hotCryptoResponseStore   store of `HotCryptoResponse`
 * @property tangemTechApi            tangem tech api
 * @property appCurrencyResponseStore store of current app currency
 * @property dispatchers              dispatchers
 * @property analyticsEventHandler    analytics event handler
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultHotCryptoRepository(
    private val excludedBlockchains: ExcludedBlockchains,
    private val hotCryptoResponseStore: HotCryptoResponseStore,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val tangemTechApi: TangemTechApi,
    private val appCurrencyResponseStore: AppCurrencyResponseStore,
    private val walletAccountsFetcher: WalletAccountsFetcher,
    private val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    appScope: AppCoroutineScope,
) : HotCryptoRepository {

    private val coroutineScope = appScope + dispatchers.main

    private val hotCryptoJobHolder = JobHolder()

    override fun fetchHotCrypto() {
        combine(
            flow = getWalletsWithTokensFlow(),
            flow2 = getHotCryptoFlow(),
        ) { walletsWithTokens, hotCrypto ->
            hotCrypto ?: return@combine emptyMap()

            walletsWithTokens
                .mapValues { hotCrypto.filterTokens(it) }
                .mapKeys { it.key.walletId }
        }
            .onEach(hotCryptoResponseStore::store)
            .launchIn(scope = coroutineScope)
            .saveIn(jobHolder = hotCryptoJobHolder)
    }

    override fun getCurrencies(userWalletId: UserWalletId): Flow<List<HotCryptoCurrency>> {
        return hotCryptoResponseStore.get()
            .map { it[userWalletId] }
            .filterNotNull()
            .map { response ->
                val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)

                HotCryptoCurrencyConverter(
                    userWallet = userWallet,
                    imageHost = response.imageHost,
                    excludedBlockchains = excludedBlockchains,
                )
                    .convertList(input = response.tokens)
                    .filterNotNull()
            }
    }

    private fun getWalletsWithTokensFlow(): Flow<Map<UserWallet, List<UserTokensResponse.Token>>> {
        return userWalletsListRepository.loadAndGet().flatMapLatest { userWallets ->
            val flows = userWallets.map { userWallet ->
                walletAccountsFetcher.get(userWalletId = userWallet.walletId).map { it.toUserTokensResponse() }
                    .map { userWallet to it.tokens }
            }

            combine(flows) { it.toMap() }
        }
    }

    private fun getHotCryptoFlow(): Flow<HotCryptoResponse?> {
        return appCurrencyResponseStore
            .get()
            .map { it?.id ?: "usd" }
            .distinctUntilChanged()
            .map { getHotCrypto(appCurrencyId = it).getOrNull() }
            .distinctUntilChanged()
    }

    private suspend fun getHotCrypto(appCurrencyId: String): Result<HotCryptoResponse> {
        return runCatching(dispatchers.io) {
            tangemTechApi.getHotCrypto(currencyId = appCurrencyId).getOrThrow()
        }
            .onSuccess { TangemLogger.d("HotCrypto is successfully updated") }
            .onFailure { throwable ->
                TangemLogger.e("Unable to fetch hot crypto", throwable)

                val httpException = throwable as? ApiResponseError.HttpException
                analyticsEventHandler.send(
                    event = MainScreenAnalyticsEvent.HotTokenError(
                        errorCode = httpException?.code?.numericCode?.toString().orEmpty(),
                    ),
                )
            }
    }

    private fun HotCryptoResponse.filterTokens(
        walletWithTokens: Map.Entry<UserWallet, List<UserTokensResponse.Token>>,
    ): HotCryptoResponse {
        val (userWallet, tokens) = walletWithTokens

        return copy(
            tokens = this.tokens
                .applyTokensIdMigrations()
                .filter { hotToken ->
                    !tokens.hasAlreadyAdded(hotToken) && userWallet.canHandleHotCrypto(hotToken)
                },
        )
    }

    private fun List<UserTokensResponse.Token>.hasAlreadyAdded(hotToken: HotCryptoResponse.Token): Boolean {
        return any { token ->
            token.id == hotToken.id && token.contractAddress == hotToken.contractAddress &&
                token.networkId == hotToken.networkId
        }
    }

    private fun List<HotCryptoResponse.Token>.applyTokensIdMigrations(): List<HotCryptoResponse.Token> {
        return this.map { token ->
            if (token.id == OLD_POLYGON_NAME) {
                token.copy(id = NEW_POLYGON_NAME)
            } else {
                token
            }
        }
    }

    // TODO: [REDACTED_JIRA]
    private fun UserWallet.canHandleHotCrypto(hotToken: HotCryptoResponse.Token): Boolean {
        val isToken = hotToken.contractAddress != null && hotToken.decimalCount != null
        val blockchain = hotToken.networkId?.let { Blockchain.fromNetworkId(it) } ?: return false

        return if (isToken) {
            canHandleToken(
                blockchain = blockchain,
                excludedBlockchains = excludedBlockchains,
            )
        } else {
            canHandleBlockchain(
                blockchain = blockchain,
                excludedBlockchains = excludedBlockchains,
            )
        }
    }
}