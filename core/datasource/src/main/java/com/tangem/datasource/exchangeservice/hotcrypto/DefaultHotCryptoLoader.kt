package com.tangem.datasource.exchangeservice.hotcrypto

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.datasource.api.tangemTech.models.HotCryptoResponse
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.features.onramp.OnrampFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import timber.log.Timber
import javax.inject.Inject

/**
 * Default implementation of [HotCryptoLoader]
 *
 * @property tangemTechApi          tangem tech api
 * @property hotCryptoResponseStore store of [HotCryptoResponse]
 * @property appPreferencesStore    app preferences store
 * @property dispatchers            dispatchers
 * @property onrampFeatureToggles   onramp feature toggles
 *
[REDACTED_AUTHOR]
 */
internal class DefaultHotCryptoLoader @Inject constructor(
    private val tangemTechApi: TangemTechApi,
    private val hotCryptoResponseStore: HotCryptoResponseStore,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
    private val onrampFeatureToggles: OnrampFeatureToggles,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : HotCryptoLoader {

    override suspend fun fetch(tokens: List<UserTokensResponse.Token>) {
        updateInternal { hotToken ->
            tokens.none {
                it.id == hotToken.id && it.contractAddress == hotToken.contractAddress &&
                    it.networkId == hotToken.networkId
            }
        }
    }

    override suspend fun update(currencies: List<CryptoCurrency>) {
        updateInternal { hotToken ->
            currencies.none {
                it.id.rawCurrencyId?.value == hotToken.id &&
                    (it as? CryptoCurrency.Token)?.contractAddress == hotToken.contractAddress &&
                    it.network.id.value == hotToken.networkId
            }
        }
    }

    private suspend fun updateInternal(filterPredicate: (HotCryptoResponse.Token) -> Boolean) {
        if (!onrampFeatureToggles.isHotTokensEnabled) return

        runCatching(dispatchers.io) {
            tangemTechApi.getHotCrypto(currencyId = getAppCurrencyId()).getOrThrow()
        }
            .onSuccess {
                Timber.d("HotCrypto is successfully updated")

                hotCryptoResponseStore.store(
                    value = it.copy(tokens = it.tokens.filter(filterPredicate)),
                )
            }
            .onFailure {
                Timber.e(it, "Unable to fetch hot crypto")

                analyticsEventHandler.send(
                    event = MainScreenAnalyticsEvent.HotTokenError(
                        errorCode = (it as? ApiResponseError.HttpException)?.code?.code?.toString().orEmpty(),
                    ),
                )
            }
    }

    private suspend fun getAppCurrencyId(): String {
        return appPreferencesStore.getObjectSyncOrNull<CurrenciesResponse.Currency>(
            key = PreferencesKeys.SELECTED_APP_CURRENCY_KEY,
        )
            ?.id
            ?: "usd"
    }
}