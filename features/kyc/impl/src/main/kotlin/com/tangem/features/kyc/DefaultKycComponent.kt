package com.tangem.features.kyc

import com.sumsub.sns.core.SNSMobileSDK
import com.sumsub.sns.core.data.listener.SNSCompleteHandler
import com.sumsub.sns.core.data.listener.TokenExpirationHandler
import com.sumsub.sns.core.data.model.SNSCompletionResult
import com.sumsub.sns.core.data.model.SNSSDKState
import com.sumsub.sns.core.theme.SNSTheme
import com.sumsub.sns.core.theme.SNSThemeMetric
import com.sumsub.sns.core.theme.colors
import com.sumsub.sns.core.theme.metrics
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.domain.pay.repository.KycRepository
import com.tangem.domain.wallets.models.UserWalletId
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale

class DefaultKycComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    private val kycRepositoryFactory: KycRepository.Factory,
) : KycComponent, AppComponentContext by appComponentContext {

    private val kycRepository = kycRepositoryFactory.create(UserWalletId("0FFFFF"))

    override fun launch() {
        componentScope.launch {
            // val startInfo = kycRepository.getKycStartInfo().getOrNull() ?: return@launch

            val tokenExpirationHandler = object : TokenExpirationHandler {
                override fun onTokenExpired(): String? {
                    val newToken = runBlocking { kycRepository.getKycStartInfo().getOrNull()?.token }
                    return newToken
                }
            }

            val snsSdk = SNSMobileSDK.Builder(activity)
                .withAccessToken(
                    "TODO",
                    onTokenExpiration = tokenExpirationHandler,
                )
                .withTheme(
                    SNSTheme {
                        colors {
                        }
                        metrics {
                            this.screenHeaderAlignment = SNSThemeMetric.TextAlignment.CENTER
                        }
                    },
                )
                .withLocale(Locale("en"))
                .withCompleteHandler(
                    object : SNSCompleteHandler {
                        override fun onComplete(result: SNSCompletionResult, state: SNSSDKState) {
                        }
                    },
                )
                .build()

            snsSdk.launch()
        }
    }

    @AssistedFactory
    interface Factory : KycComponent.Factory {
        override fun create(appComponentContext: AppComponentContext): DefaultKycComponent
    }
}