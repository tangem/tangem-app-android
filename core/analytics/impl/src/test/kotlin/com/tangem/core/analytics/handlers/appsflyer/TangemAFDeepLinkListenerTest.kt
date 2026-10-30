package com.tangem.core.analytics.handlers.appsflyer

import com.appsflyer.deeplink.DeepLink
import com.appsflyer.deeplink.DeepLinkResult
import com.tangem.core.analytics.api.AppsFlyerDeepLink
import com.tangem.core.analytics.api.AppsFlyerDeepLinkHandler
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemAFDeepLinkListenerTest {

    private val deepLinkHandler: AppsFlyerDeepLinkHandler = mockk(relaxUnitFun = true)

    private val listener = TangemAFDeepLinkListener(deepLinkHandler = deepLinkHandler)

    @BeforeEach
    fun resetMocks() {
        clearMocks(deepLinkHandler)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN deep link result WHEN onDeepLinking THEN only a found link is handled`(model: OnDeepLinkingModel) {
        // Act
        listener.onDeepLinking(p0 = model.deepLinkResult)

        // Assert
        if (model.expected != null) {
            verify(exactly = 1) { deepLinkHandler.handle(deepLink = model.expected) }
        } else {
            verify(inverse = true) { deepLinkHandler.handle(deepLink = any()) }
        }
    }

    private fun provideTestModels() = listOf(
        OnDeepLinkingModel(
            deepLinkResult = DeepLinkResult(null, null),
            expected = null,
        ),
        OnDeepLinkingModel(
            deepLinkResult = DeepLinkResult(null, DeepLinkResult.Error.NETWORK),
            expected = null,
        ),
        OnDeepLinkingModel(
            deepLinkResult = createFoundResult(
                deepLinkValue = "deep_link_value",
                afDp = "tangem://onboard-visa",
                refcode = "refcode",
                campaign = "campaign",
                isDeferred = true,
            ),
            expected = AppsFlyerDeepLink(
                deepLinkValue = "deep_link_value",
                afDp = "tangem://onboard-visa",
                deepLinkSub1 = "refcode",
                deepLinkSub2 = "campaign",
                isDeferred = true,
            ),
        ),
        OnDeepLinkingModel(
            deepLinkResult = createFoundResult(
                deepLinkValue = null,
                afDp = null,
                refcode = null,
                campaign = null,
                isDeferred = null,
            ),
            expected = AppsFlyerDeepLink(
                deepLinkValue = null,
                afDp = null,
                deepLinkSub1 = null,
                deepLinkSub2 = null,
                isDeferred = null,
            ),
        ),
    )

    data class OnDeepLinkingModel(
        val deepLinkResult: DeepLinkResult,
        val expected: AppsFlyerDeepLink?,
    )

    private fun createFoundResult(
        deepLinkValue: String?,
        afDp: String?,
        refcode: String?,
        campaign: String?,
        isDeferred: Boolean?,
    ): DeepLinkResult {
        val deepLink = mockk<DeepLink> {
            every { this@mockk.deepLinkValue } returns deepLinkValue
            every { getStringValue("af_dp") } returns afDp
            every { getStringValue("deep_link_sub1") } returns refcode
            every { getStringValue("deep_link_sub2") } returns campaign
            every { this@mockk.isDeferred } returns isDeferred
            every { clickEvent } returns null
        }

        return mockk {
            every { status } returns DeepLinkResult.Status.FOUND
            every { this@mockk.deepLink } returns deepLink
        }
    }
}