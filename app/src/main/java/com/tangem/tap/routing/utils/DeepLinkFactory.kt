package com.tangem.tap.routing.utils

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.DeepLinkRoute
import com.tangem.common.routing.DeepLinkScheme
import com.tangem.feature.referral.api.deeplink.ReferralDeepLinkHandler
import com.tangem.features.onramp.deeplink.BuyDeepLinkHandler
import com.tangem.features.onramp.deeplink.OnrampDeepLinkHandler
import com.tangem.features.send.v2.api.deeplink.SellDeepLinkHandler
import com.tangem.features.tokendetails.deeplink.TokenDetailsDeepLinkHandler
import com.tangem.features.wallet.deeplink.WalletDeepLinkHandler
import com.tangem.features.walletconnect.components.deeplink.WalletConnectDeepLinkHandler
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
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
) {
    private val permittedAppRoute = MutableStateFlow(false)

    private var lastDeepLink: Uri? = null
    private val deepLinkHandlerJobHolder = JobHolder()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun handleDeeplink(deeplinkUri: Uri, coroutineScope: CoroutineScope) {
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
                        launchDeepLink(it, coroutineScope)
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

    private fun launchDeepLink(deeplinkUri: Uri, coroutineScope: CoroutineScope) {
        when (deeplinkUri.scheme) {
            DeepLinkScheme.Tangem.scheme -> handleTangemDeepLinks(deeplinkUri, coroutineScope)
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

    private fun handleTangemDeepLinks(deeplinkUri: Uri, coroutineScope: CoroutineScope) {
        val params = getParams(deeplinkUri)
        when (deeplinkUri.host) {
            DeepLinkRoute.Onramp.host -> onrampDeepLink.create(coroutineScope, params)
            DeepLinkRoute.Sell.host -> sellDeepLink.create(coroutineScope, params)
            DeepLinkRoute.Buy.host -> buyDeepLink.create(coroutineScope)
            DeepLinkRoute.Referral.host -> referralDeepLink.create()
            DeepLinkRoute.Wallet.host -> walletDeepLink.create()
            DeepLinkRoute.TokenDetails.host -> tokenDetailsDeepLink.create(coroutineScope, params)
            else -> {
                Timber.i(
                    """
                        No match found for deep link
                        |- Received URI: $deeplinkUri
                        |- With params: $params
                    """.trimIndent(),
                )
            }
        }
    }

    private fun getParams(uri: Uri): Map<String, String> {
        val params = mutableMapOf<String, String>()

        uri.queryParameterNames.forEach { paramName ->
            val paramValue = uri.getQueryParameter(paramName)

            if (paramName.validate() && paramValue?.validate() == true) {
                params[paramName] = paramValue
            }
        }

        return params
    }

    /**
     * Check for malicious symbol in uri part
     */
    private fun String.validate(): Boolean {
        val regex = DEEPLINK_VALIDATION_REGEX.toRegex()

        return !regex.containsMatchIn(this)
    }

    private companion object {
        const val DEEPLINK_VALIDATION_REGEX = "['\";<>()+\\\\]"
    }
}