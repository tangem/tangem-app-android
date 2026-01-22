package com.tangem.tap.common.analytics.appsflyer

import com.appsflyer.deeplink.DeepLink
import com.appsflyer.deeplink.DeepLinkResult
import com.tangem.test.core.ProvideTestModels
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppsFlyerDeepLinkListenerTest {

    private val referralParamsHandler: AppsFlyerReferralParamsHandler = mockk()
    private val listener = AppsFlyerDeepLinkListener(
        referralParamsHandler = referralParamsHandler,
    )

    @AfterEach
    fun tearDown() {
        clearMocks(referralParamsHandler)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun onDeepLinking(model: OnDeepLinkingModel) = runTest {
        if (model.shouldHandle) {
            every { referralParamsHandler.handle(deepLink = model.deepLinkResult.deepLink) } just Runs
        }

        listener.onDeepLinking(p0 = model.deepLinkResult)

        if (model.shouldHandle) {
            coVerify { referralParamsHandler.handle(deepLink = model.deepLinkResult.deepLink) }
        } else {
            coVerify(inverse = true) { referralParamsHandler.handle(deepLink = any()) }
        }
    }

    private fun provideTestModels(): List<OnDeepLinkingModel> {
        return listOf(
            // DeepLinkResult.Status.NOT_FOUND
            OnDeepLinkingModel(
                deepLinkResult = DeepLinkResult(null, null),
                shouldHandle = false,
            ),
            // DeepLinkResult.Status.ERROR
            OnDeepLinkingModel(
                deepLinkResult = DeepLinkResult(null, DeepLinkResult.Error.NETWORK),
                shouldHandle = false,
            ),
            // DeepLinkResult.Status.FOUND
            OnDeepLinkingModel(
                deepLinkResult = createFoundDeepLink(
                    deepLinkValue = "deep_link_value",
                    refcode = "refcode",
                    campaign = "campaign",
                ),
                shouldHandle = true,
            ),
        )
    }

    data class OnDeepLinkingModel(
        val deepLinkResult: DeepLinkResult,
        val shouldHandle: Boolean,
    )

    private fun createFoundDeepLink(
        deepLinkValue: String,
        refcode: String? = null,
        campaign: String? = null,
    ): DeepLinkResult {
        val deeplink = mockk<DeepLink> {
            every { this@mockk.deepLinkValue } returns deepLinkValue
            every { this@mockk.getStringValue("deep_link_sub1") } returns refcode
            every { this@mockk.getStringValue("deep_link_sub2") } returns campaign
        }

        return mockk<DeepLinkResult> {
            every { this@mockk.status } returns DeepLinkResult.Status.FOUND
            every { this@mockk.deepLink } returns deeplink
        }
    }
}