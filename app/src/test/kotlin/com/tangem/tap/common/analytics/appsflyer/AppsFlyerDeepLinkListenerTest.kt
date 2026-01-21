package com.tangem.tap.common.analytics.appsflyer

import com.appsflyer.deeplink.DeepLink
import com.appsflyer.deeplink.DeepLinkResult
import com.tangem.datasource.local.appsflyer.AppsFlyerConversionStore
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.tap.common.analytics.appsflyer.AppsFlyerDeepLinkListener.Companion.CAMPAIGN_PARAM_KEY
import com.tangem.tap.common.analytics.appsflyer.AppsFlyerDeepLinkListener.Companion.REFCODE_PARAM_KEY
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppsFlyerDeepLinkListenerTest {

    private val appsFlyerConversionStore: AppsFlyerConversionStore = mockk(relaxUnitFun = true)
    private val listener = AppsFlyerDeepLinkListener(
        appsFlyerConversionStore = appsFlyerConversionStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @AfterEach
    fun tearDown() {
        clearMocks(appsFlyerConversionStore)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun onDeepLinking(model: OnDeepLinkingModel) = runTest {
        listener.onDeepLinking(p0 = model.deepLinkResult)

        if (model.shouldStore) {
            val value = AppsFlyerConversionData(refcode = SUCCESS_REFCODE, campaign = SUCCESS_CAMPAIGN)
            coVerify { appsFlyerConversionStore.storeIfAbsent(value = value) }
        } else {
            coVerify(inverse = true) { appsFlyerConversionStore.storeIfAbsent(value = any()) }
        }
    }

    private fun provideTestModels(): List<OnDeepLinkingModel> {
        return listOf(
            // DeepLinkResult.Status.NOT_FOUND
            OnDeepLinkingModel(
                deepLinkResult = DeepLinkResult(null, null),
                shouldStore = false,
            ),
            // DeepLinkResult.Status.ERROR
            OnDeepLinkingModel(
                deepLinkResult = DeepLinkResult(null, DeepLinkResult.Error.NETWORK),
                shouldStore = false,
            ),
            // DeepLinkResult.Status.FOUND
            OnDeepLinkingModel(
                deepLinkResult = createFoundDeepLink(
                    deepLinkValue = AppsFlyerDeepLinkListener.REFERRAL_DEEP_LINK_VALUE,
                    refcode = SUCCESS_REFCODE,
                    campaign = SUCCESS_CAMPAIGN,
                ),
                shouldStore = true,
            ),
            OnDeepLinkingModel(
                deepLinkResult = createFoundDeepLink(
                    deepLinkValue = AppsFlyerDeepLinkListener.REFERRAL_DEEP_LINK_VALUE,
                    refcode = "",
                    campaign = SUCCESS_CAMPAIGN,
                ),
                shouldStore = false,
            ),
            OnDeepLinkingModel(
                deepLinkResult = createFoundDeepLink(
                    deepLinkValue = AppsFlyerDeepLinkListener.REFERRAL_DEEP_LINK_VALUE,
                    refcode = null,
                    campaign = SUCCESS_CAMPAIGN,
                ),
                shouldStore = false,
            ),
            OnDeepLinkingModel(
                deepLinkResult = createFoundDeepLink(deepLinkValue = "some_other_deep_link_value"),
                shouldStore = false,
            ),
        )
    }

    data class OnDeepLinkingModel(
        val deepLinkResult: DeepLinkResult,
        val shouldStore: Boolean,
    )

    private fun createFoundDeepLink(
        deepLinkValue: String,
        refcode: String? = null,
        campaign: String? = null,
    ): DeepLinkResult {
        val deeplink = mockk<DeepLink> {
            every { this@mockk.deepLinkValue } returns deepLinkValue
            every { this@mockk.getStringValue(REFCODE_PARAM_KEY) } returns refcode
            every { this@mockk.getStringValue(CAMPAIGN_PARAM_KEY) } returns campaign
        }

        return mockk<DeepLinkResult> {
            every { this@mockk.status } returns DeepLinkResult.Status.FOUND
            every { this@mockk.deepLink } returns deeplink
        }
    }

    private companion object {
        const val SUCCESS_REFCODE = "valid_refcode"
        const val SUCCESS_CAMPAIGN = "valid_campaign"
    }
}