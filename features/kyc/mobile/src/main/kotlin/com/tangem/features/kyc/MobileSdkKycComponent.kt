package com.tangem.features.kyc

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sumsub.sns.core.SNSMobileSDK
import com.sumsub.sns.core.data.listener.TokenExpirationHandler
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.domain.pay.KycStartInfo
import com.tangem.features.kyc.theme.TangemSNSIconHandler
import com.tangem.features.kyc.theme.TangemSNSTheme
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.Locale

class MobileSdkKycComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: KycComponent.Params,
) : KycComponent, AppComponentContext by appComponentContext {

    private val model: MobileSdkKycModel = getOrCreateModel(params)

    init {
        componentScope.launch {
            model.uiState.drop(1).collectLatest { startInfo ->
                startInfo?.let { launchSdk(startInfo) }
                router.pop()
            }
        }
    }

    private fun launchSdk(startInfo: KycStartInfo) {
        val tokenExpirationHandler = object : TokenExpirationHandler {
            /**
             * We don't refresh this token for f&f release. Assume it lives long enough to finish KYC
             * [REDACTED_TODO_COMMENT]
             */
            override fun onTokenExpired() = ""
        }
        val snsSdk = SNSMobileSDK.Builder(activity)
            .withAccessToken(accessToken = startInfo.token, onTokenExpiration = tokenExpirationHandler)
            .withTheme(TangemSNSTheme.theme(activity))
            .withIconHandler(TangemSNSIconHandler())
            .withLocale(Locale(startInfo.locale))
            .build()
        snsSdk.launch()
    }

    @Composable
    override fun Content(modifier: Modifier) {
        KycLoadingScreen(router::pop, modifier)
    }

    @AssistedFactory
    interface Factory : KycComponent.Factory {
        override fun create(context: AppComponentContext, params: KycComponent.Params): MobileSdkKycComponent
    }
}