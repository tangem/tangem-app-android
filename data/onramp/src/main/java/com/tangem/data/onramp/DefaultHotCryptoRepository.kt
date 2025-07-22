package com.tangem.data.onramp

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.NEW_POLYGON_NAME
import com.tangem.blockchainsdk.utils.OLD_POLYGON_NAME
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent
import com.tangem.data.onramp.converters.HotCryptoCurrencyConverter
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.datasource.api.tangemTech.models.HotCryptoResponse
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.exchangeservice.hotcrypto.HotCryptoResponseStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObject
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.extensions.canHandleBlockchain
import com.tangem.domain.common.extensions.canHandleToken
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.HotCryptoCurrency
import com.tangem.domain.onramp.repositories.HotCryptoRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.runCatching
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import timber.log.Timber

/**
 * Default implementation of [HotCryptoRepository]
 *
 * @property excludedBlockchains    excluded blockchains
 * @property hotCryptoResponseStore store of `HotCryptoResponse`
 * @property userWalletsStore       store of `UserWallet`
 * @property tangemTechApi          tangem tech api
 * @property appPreferencesStore    app preferences store
 * @property dispatchers            dispatchers
 * @property analyticsEventHandler  analytics event handler
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultHotCryptoRepository(
    private val excludedBlockchains: ExcludedBlockchains,
    private val hotCryptoResponseStore: HotCryptoResponseStore,
    private val userWalletsStore: UserWalletsStore,
    private val tangemTechApi: TangemTechApi,
    private val appPreferencesStore: AppPreferencesStore,
    private val userTokensResponseStore: UserTokensResponseStore,
    private val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : HotCryptoRepository {

    private val coroutineScope = CoroutineScope(dispatchers.main + SupervisorJob())

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
            .map {
                val userWallet = userWalletsStore.getSyncOrNull(userWalletId)
                    ?: error("UserWalletId [$userWalletId] not found")

                HotCryptoCurrencyConverter(
                    userWallet = userWallet,
                    imageHost = it.imageHost,
                    excludedBlockchains = excludedBlockchains,
                )
                    .convertList(input = it.tokens)
                    .filterNotNull()
            }
    }

    private fun getWalletsWithTokensFlow(): Flow<Map<UserWallet, List<UserTokensResponse.Token>>> {
        return userWalletsStore.userWallets.flatMapLatest { userWallets ->
            val flows = userWallets.map { userWallet ->
                userTokensResponseStore.get(userWalletId = userWallet.walletId)
                    .map { userWallet to it?.tokens.orEmpty() }
            }

            combine(flows) { it.toMap() }
        }
    }

    private fun getHotCryptoFlow(): Flow<HotCryptoResponse?> {
        return appPreferencesStore
            .getObject<CurrenciesResponse.Currency>(key = PreferencesKeys.SELECTED_APP_CURRENCY_KEY)
            .map { it?.id ?: "usd" }
            .distinctUntilChanged()
            .map { getHotCrypto(appCurrencyId = it).getOrNull() }
            .distinctUntilChanged()
    }

    private suspend fun getHotCrypto(appCurrencyId: String): Result<HotCryptoResponse> {
        return runCatching(dispatchers.io) {
            tangemTechApi.getHotCrypto(currencyId = appCurrencyId).getOrThrow()
        }
            .onSuccess { Timber.d("HotCrypto is successfully updated") }
            .onFailure {
                Timber.e(it, "Unable to fetch hot crypto")

                analyticsEventHandler.send(
                    event = MainScreenAnalyticsEvent.HotTokenError(
                        errorCode = (it as? ApiResponseError.HttpException)?.code?.numericCode?.toString().orEmpty(),
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
        return this.map {
            if (it.id == OLD_POLYGON_NAME) {
                it.copy(id = NEW_POLYGON_NAME)
            } else {
                it
            }
        }
    }

    // TODO: [REDACTED_JIRA]
    private fun UserWallet.canHandleHotCrypto(hotToken: HotCryptoResponse.Token): Boolean {
        if (this !is UserWallet.Cold) {
            return true // TODO [REDACTED_TASK_KEY]
        }

        val isToken = hotToken.contractAddress != null && hotToken.decimalCount != null
        val blockchain = hotToken.networkId?.let { Blockchain.fromNetworkId(it) } ?: return false

        return if (isToken) {
            scanResponse.card.canHandleToken(
                blockchain = blockchain,
                cardTypesResolver = cardTypesResolver,
                excludedBlockchains = excludedBlockchains,
            )
        } else {
            scanResponse.card.canHandleBlockchain(
                blockchain = blockchain,
                cardTypesResolver = cardTypesResolver,
                excludedBlockchains = excludedBlockchains,
            )
        }
    }
}