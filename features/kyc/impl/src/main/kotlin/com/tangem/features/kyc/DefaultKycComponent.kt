package com.tangem.features.kyc

import com.sumsub.sns.core.SNSMobileSDK
import com.sumsub.sns.core.data.listener.TokenExpirationHandler
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.kyc.theme.TangemSNSIconHandler
import com.tangem.features.kyc.theme.TangemSNSTheme
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import java.util.Locale

class DefaultKycComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
) : KycComponent, AppComponentContext by appComponentContext {

    private val model: DefaultKycModel = getOrCreateModel()

    override fun launch() {
        componentScope.launch {
            model.uiState.collect {
                it?.let { startInfo ->
                    val tokenExpirationHandler = object : TokenExpirationHandler {
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
            }
        }
        model.getKycToken()
    }

    @AssistedFactory
    interface Factory : KycComponent.Factory {
        override fun create(appComponentContext: AppComponentContext): DefaultKycComponent
    }
}