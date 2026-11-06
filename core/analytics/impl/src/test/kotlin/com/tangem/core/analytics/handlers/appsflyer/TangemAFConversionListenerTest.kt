package com.tangem.core.analytics.handlers.appsflyer

import com.tangem.core.analytics.api.AppsFlyerConversionDataHandler
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TangemAFConversionListenerTest {

    private val firstHandler: AppsFlyerConversionDataHandler = mockk(relaxUnitFun = true)
    private val secondHandler: AppsFlyerConversionDataHandler = mockk(relaxUnitFun = true)
    private val googleAdsConversionDataReporter: GoogleAdsConversionDataReporter = mockk(relaxUnitFun = true)
    private val listener = TangemAFConversionListener(
        googleAdsConversionDataReporter = googleAdsConversionDataReporter,
        conversionDataHandlers = setOf(firstHandler, secondHandler),
    )

    @AfterEach
    fun tearDown() {
        clearMocks(googleAdsConversionDataReporter, firstHandler, secondHandler)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun onConversionDataSuccess(model: OnConversionDataSuccessModel) {
        listener.onConversionDataSuccess(p0 = model.params)

        if (model.shouldHandle) {
            verify { googleAdsConversionDataReporter.handle(conversionData = model.params!!) }
            verify { firstHandler.handle(conversionData = model.params!!) }
            verify { secondHandler.handle(conversionData = model.params!!) }
        } else {
            verify(inverse = true) { googleAdsConversionDataReporter.handle(conversionData = any()) }
            verify(inverse = true) { firstHandler.handle(conversionData = any()) }
            verify(inverse = true) { secondHandler.handle(conversionData = any()) }
        }
    }

    private fun provideTestModels(): List<OnConversionDataSuccessModel> {
        return listOf(
            OnConversionDataSuccessModel(
                params = null,
                shouldHandle = false,
            ),
            OnConversionDataSuccessModel(
                params = emptyMap(),
                shouldHandle = true,
            ),
            OnConversionDataSuccessModel(
                params = mapOf(
                    "deep_link_value" to "referral",
                ),
                shouldHandle = true,
            ),
        )
    }

    data class OnConversionDataSuccessModel(
        val params: Map<String?, Any?>?,
        val shouldHandle: Boolean,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TangemPayGoogleAdsCampaign {

        @ParameterizedTest
        @ProvideTestModels
        fun onConversionDataSuccess(model: TangemPayCampaignModel) {
            // Arrange
            val expectedHandledData = if (model.isTangemPayCampaign) {
                model.params + ("deep_link_value" to "tpay_mobileonboard")
            } else {
                model.params
            }

            // Act
            listener.onConversionDataSuccess(p0 = model.params)

            // Assert
            verify(exactly = 1) { googleAdsConversionDataReporter.handle(conversionData = model.params) }
            verify(exactly = 1) { firstHandler.handle(conversionData = expectedHandledData) }
            verify(exactly = 1) { secondHandler.handle(conversionData = expectedHandledData) }
        }

        private fun provideTestModels(): List<TangemPayCampaignModel> = listOf(
            TangemPayCampaignModel(params = googleAdsConversionData(), isTangemPayCampaign = true),
            TangemPayCampaignModel(
                params = googleAdsConversionData(campaign = "US – Tangem Pay [MO] – KYC Started"),
                isTangemPayCampaign = true,
            ),
            TangemPayCampaignModel(
                params = googleAdsConversionData(campaign = "AR – tangem pay [mo] – Freelance"),
                isTangemPayCampaign = true,
            ),
            TangemPayCampaignModel(
                params = googleAdsConversionData(campaign = "US – Tangem Pay – Installs"),
                isTangemPayCampaign = false,
            ),
            TangemPayCampaignModel(
                params = googleAdsConversionData(campaign = "US – Tangem Pay MO – Installs"),
                isTangemPayCampaign = false,
            ),
            TangemPayCampaignModel(params = googleAdsConversionData(campaign = null), isTangemPayCampaign = false),
            TangemPayCampaignModel(
                params = googleAdsConversionData(mediaSource = "Facebook Ads"),
                isTangemPayCampaign = false,
            ),
            TangemPayCampaignModel(params = googleAdsConversionData(mediaSource = null), isTangemPayCampaign = false),
            TangemPayCampaignModel(
                params = googleAdsConversionData(afDp = "tangem://onboard-visa"),
                isTangemPayCampaign = false,
            ),
            TangemPayCampaignModel(
                params = googleAdsConversionData(deepLinkValue = "referral"),
                isTangemPayCampaign = false,
            ),
            TangemPayCampaignModel(params = googleAdsConversionData(deepLinkValue = ""), isTangemPayCampaign = true),
            TangemPayCampaignModel(params = googleAdsConversionData(afDp = "null"), isTangemPayCampaign = true),
        )
    }

    data class TangemPayCampaignModel(
        val params: Map<String?, Any?>,
        val isTangemPayCampaign: Boolean,
    )

    private fun googleAdsConversionData(
        mediaSource: String? = "googleadwords_int",
        campaign: String? = "US – Tangem Pay [MO] – Installs",
        deepLinkValue: String? = null,
        afDp: String? = null,
    ): Map<String?, Any?> = buildMap {
        put("is_first_launch", true)
        mediaSource?.let { put("media_source", it) }
        campaign?.let { put("campaign", it) }
        deepLinkValue?.let { put("deep_link_value", it) }
        afDp?.let { put("af_dp", it) }
    }
}