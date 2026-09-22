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
            deepLinkValue = "tpay_for_oxford",
            expected = AppsFlyerDeeplinkTarget.TangemPayCampaign(key = "tpay_for_oxford"),
        ),
        FromModel(
            deepLinkValue = "tpay_for_a",
            expected = AppsFlyerDeeplinkTarget.TangemPayCampaign(key = "tpay_for_a"),
        ),
        FromModel(
            deepLinkValue = "tpay_for_OXFORD-2026_summer",
            expected = AppsFlyerDeeplinkTarget.TangemPayCampaign(key = "tpay_for_OXFORD-2026_summer"),
        ),
        FromModel(deepLinkValue = "tpay_for_", expected = null),
        FromModel(deepLinkValue = "tpay_for_   ", expected = null),
        FromModel(deepLinkValue = "TPAY_FOR_oxford", expected = null),
        FromModel(deepLinkValue = "tpay_foroxford", expected = null),
        FromModel(deepLinkValue = " tpay_for_oxford", expected = null),
        FromModel(deepLinkValue = "some_other_deep_link_value", expected = null),
        FromModel(deepLinkValue = "", expected = null),
        FromModel(deepLinkValue = null, expected = null),
    )

    internal data class FromModel(val deepLinkValue: String?, val expected: AppsFlyerDeeplinkTarget?)
}