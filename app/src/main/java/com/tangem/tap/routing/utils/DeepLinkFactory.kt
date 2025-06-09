package com.tangem.tap.routing.utils

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.DeepLinkRoute
import com.tangem.common.routing.DeepLinkScheme
import com.tangem.feature.referral.api.deeplink.ReferralDeepLinkHandler
import com.tangem.features.markets.deeplink.MarketsDeepLinkHandler
import com.tangem.features.markets.deeplink.MarketsTokenDetailDeepLinkHandler
import com.tangem.features.onramp.deeplink.BuyDeepLinkHandler
import com.tangem.features.onramp.deeplink.OnrampDeepLinkHandler
import com.tangem.features.send.v2.api.deeplink.SellDeepLinkHandler
import com.tangem.features.staking.api.deeplink.StakingDeepLinkHandler
import com.tangem.features.tokendetails.deeplink.TokenDetailsDeepLinkHandler
import com.tangem.features.wallet.deeplink.WalletDeepLinkHandler
import com.tangem.features.walletconnect.components.deeplink.WalletConnectDeepLinkHandler
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.extensions.uriValidate
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.transformLatest
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@ActivityScoped
internal class DeepLinkFactory @Inject constructor(
    private val onrampDeepLink: OnrampDeepLinkHandler.Factory,
    private val sellDeepLink: SellDeepLinkHandler.Factory,
    private val buyDeepLink: BuyDeepLinkHandler.Factory,
    private val referralDeepLink: ReferralDeepLinkHandler.Factory,
    private val walletConnectDeepLink: WalletConnectDeepLinkHandler.Factory,
    private val walletDeepLink: WalletDeepLinkHandler.Factory,
    private val tokenDetailsDeepLink: TokenDetailsDeepLinkHandler.Factory,
    private val stakingDeepLink: StakingDeepLinkHandler.Factory,
    private val marketsDeepLink: MarketsDeepLinkHandler.Factory,
    private val marketsTokenDetailDeepLink: MarketsTokenDetailDeepLinkHandler.Factory,
) {
    private val permittedAppRoute = MutableStateFlow(false)

    private var lastDeepLink: Uri? = null
    private val deepLinkHandlerJobHolder = JobHolder()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun handleDeeplink(deeplinkUri: Uri, coroutineScope: CoroutineScope, isFromOnNewIntent: Boolean) {
        lastDeepLink = deeplinkUri

        Timber.i(
            """
                Received deep link intent
                |- Received URI: $deeplinkUri
            """.trimIndent(),
        )
        permittedAppRoute
            .transformLatest<Boolean, Unit> { isPermitted ->
                if (isPermitted) {
                    lastDeepLink?.let {
                        launchDeepLink(it, coroutineScope, isFromOnNewIntent)
                    }
                    lastDeepLink = null
                }
            }
            .launchIn(coroutineScope)
            .saveIn(deepLinkHandlerJobHolder)
    }

    /**
     * Check if app is ready to handle deeplink
     */
    fun checkRoutingReadiness(appRoute: AppRoute) {
        permittedAppRoute.value = when (appRoute) {
            AppRoute.Initial,
            AppRoute.Home,
            is AppRoute.Welcome,
            is AppRoute.Disclaimer,
            is AppRoute.Stories,
            is AppRoute.Onboarding,
            -> false
            else -> true
        }
    }

    private fun launchDeepLink(deeplinkUri: Uri, coroutineScope: CoroutineScope, isFromOnNewIntent: Boolean) {
        when (deeplinkUri.scheme) {
            DeepLinkScheme.Tangem.scheme -> handleTangemDeepLinks(deeplinkUri, coroutineScope, isFromOnNewIntent)
            DeepLinkScheme.WalletConnect.scheme -> walletConnectDeepLink.create(deeplinkUri)
            else -> {
                Timber.i(
                    """
                        No match found for deep link
                        |- Received URI: $deeplinkUri
                    """.trimIndent(),
                )
            }
        }
    }

    private fun handleTangemDeepLinks(deeplinkUri: Uri, coroutineScope: CoroutineScope, isFromOnNewIntent: Boolean) {
        val queryParams = getQueryParams(deeplinkUri)
        when (deeplinkUri.host) {
            DeepLinkRoute.Onramp.host -> onrampDeepLink.create(coroutineScope, queryParams)
            DeepLinkRoute.Sell.host -> sellDeepLink.create(coroutineScope, queryParams)
            DeepLinkRoute.Buy.host -> buyDeepLink.create(coroutineScope)
            DeepLinkRoute.Referral.host -> referralDeepLink.create()
            DeepLinkRoute.Wallet.host -> walletDeepLink.create()
            DeepLinkRoute.TokenDetails.host -> tokenDetailsDeepLink.create(
                coroutineScope = coroutineScope,
                queryParams = queryParams,
                isFromOnNewIntent = isFromOnNewIntent,
            )
            DeepLinkRoute.Staking.host -> stakingDeepLink.create(coroutineScope, queryParams)
            DeepLinkRoute.Markets.host -> marketsDeepLink.create()
            DeepLinkRoute.MarketTokenDetail.host -> marketsTokenDetailDeepLink.create(coroutineScope, queryParams)
            else -> {
                Timber.i(
                    """
                        No match found for deep link
                        |- Received URI: $deeplinkUri
                        |- With params: $queryParams
                    """.trimIndent(),
                )
            }
        }
    }

    private fun getQueryParams(uri: Uri): Map<String, String> {
        val params = mutableMapOf<String, String>()

        uri.queryParameterNames.forEach { paramName ->
            val paramValue = uri.getQueryParameter(paramName)

            if (paramName.uriValidate() && paramValue?.uriValidate() == true) {
                params[paramName] = paramValue
            }
        }

        return params
    }
}