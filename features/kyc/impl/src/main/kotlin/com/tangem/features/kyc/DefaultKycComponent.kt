package com.tangem.features.kyc

import com.sumsub.sns.core.SNSMobileSDK
import com.sumsub.sns.core.data.listener.TokenExpirationHandler
import com.tangem.core.decompose.context.AppComponentContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.util.Locale

class DefaultKycComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
) : KycComponent, AppComponentContext by appComponentContext {

    override fun launch() {
        val accessToken = "123456"

        val tokenExpirationHandler = object : TokenExpirationHandler {
            override fun onTokenExpired(): String? {
                // Access token expired
                // get a new one and pass it to the callback to re-initiate the SDK
                val newToken = "..." // get a new token from your backend
                return newToken
            }
        }

        val snsSdk = SNSMobileSDK.Builder(activity)
            .withAccessToken(accessToken, onTokenExpiration = tokenExpirationHandler)
            .withLocale(Locale("en"))
            .build()

        snsSdk.launch()
    }

    @AssistedFactory
    interface Factory : KycComponent.Factory {
        override fun create(appComponentContext: AppComponentContext): DefaultKycComponent
    }
}