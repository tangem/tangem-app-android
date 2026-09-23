package com.tangem.domain.appsflyer

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AppsFlyerDeeplinkTargetTest {

    @ParameterizedTest
    @ProvideTestModels
    fun from(model: FromModel) {
        // Act
        val actual = AppsFlyerDeeplinkTarget.from(model.deepLinkValue)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels(): List<FromModel> = listOf(
        FromModel(
            deepLinkValue = "referral",
            expected = AppsFlyerDeeplinkTarget.Known(AppsFlyerDeeplink.Referral),
        ),
        FromModel(
            deepLinkValue = "tpay_mobileonboard",
            expected = AppsFlyerDeeplinkTarget.Known(AppsFlyerDeeplink.TangemPayMobileOnboarding),
        ),
        FromModel(
            deepLinkValue = "tangem://main",
            expected = AppsFlyerDeeplinkTarget.Direct(uri = "tangem://main"),
        ),
        FromModel(
            deepLinkValue = "tangem://onboard-visa",
            expected = AppsFlyerDeeplinkTarget.Direct(uri = "tangem://onboard-visa"),
        ),
        FromModel(
            deepLinkValue = "https://tangem.com/pay-app",
            expected = AppsFlyerDeeplinkTarget.Direct(uri = "https://tangem.com/pay-app"),
        ),
        FromModel(deepLinkValue = "://main", expected = null),
        FromModel(deepLinkValue = "1tangem://main", expected = null),
        FromModel(deepLinkValue = "tangem://", expected = null),
        FromModel(deepLinkValue = "tangem://main with space", expected = null),
        FromModel(deepLinkValue = " tangem://main", expected = null),
        FromModel(deepLinkValue = "TPAY_MOBILEONBOARD", expected = null),
        FromModel(deepLinkValue = "some_other_deep_link_value", expected = null),
        FromModel(deepLinkValue = "", expected = null),
        FromModel(deepLinkValue = null, expected = null),
    )

    internal data class FromModel(val deepLinkValue: String?, val expected: AppsFlyerDeeplinkTarget?)
}