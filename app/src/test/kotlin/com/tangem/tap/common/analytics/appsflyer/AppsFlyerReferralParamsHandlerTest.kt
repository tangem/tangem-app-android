package com.tangem.tap.common.analytics.appsflyer

import com.appsflyer.deeplink.DeepLink
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppsFlyerReferralParamsHandlerTest {

    private val appsFlyerRepository: AppsFlyerRepository = mockk(relaxUnitFun = true)
    private val tangemPayFeatureToggles: TangemPayFeatureToggles = mockk()

    private lateinit var handler: AppsFlyerReferralParamsHandler

    @BeforeEach
    fun setUp() {
        clearMocks(appsFlyerRepository)
        every { tangemPayFeatureToggles.isCustomCardsCobrandEnabled } returns true
        handler = AppsFlyerReferralParamsHandler(
            appsFlyerRepository = appsFlyerRepository,
            coroutineScope = TestAppCoroutineScope(),
            tangemPayFeatureToggles = tangemPayFeatureToggles,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class HandleDeepLink {

        @ParameterizedTest
        @ProvideTestModels
        fun handle(model: HandleDeepLinkModel) = runTest {
            handler.handleDeeplink(deepLink = model.deepLink)

            if (model.shouldStore) {
                val value = AppsFlyerConversionData(refcode = SUCCESS_REFCODE, campaign = SUCCESS_CAMPAIGN)

                coVerify { appsFlyerRepository.storeIfAbsent(value = value) }
            } else {
                coVerify(inverse = true) { appsFlyerRepository.storeIfAbsent(value = any()) }
            }
        }

        private fun provideTestModels(): List<HandleDeepLinkModel> {
            return listOf(
                HandleDeepLinkModel(
                    deepLink = createDeepLink(
                        deepLinkValue = "referral",
                        refcode = SUCCESS_REFCODE,
                        campaign = SUCCESS_CAMPAIGN,
                    ),
                    shouldStore = true,
                ),
                HandleDeepLinkModel(
                    deepLink = createDeepLink(
                        deepLinkValue = "tpay_mobileonboard",
                        refcode = SUCCESS_REFCODE,
                        campaign = SUCCESS_CAMPAIGN,
                    ),
                    shouldStore = true,
                ),
                HandleDeepLinkModel(
                    deepLink = createDeepLink(
                        deepLinkValue = "some_other_deep_link_value",
                        refcode = SUCCESS_REFCODE,
                        campaign = SUCCESS_CAMPAIGN,
                    ),
                    shouldStore = true,
                ),
                HandleDeepLinkModel(
                    deepLink = createDeepLink(
                        deepLinkValue = null,
                        refcode = SUCCESS_REFCODE,
                        campaign = SUCCESS_CAMPAIGN,
                    ),
                    shouldStore = true,
                ),
                HandleDeepLinkModel(
                    deepLink = createDeepLink(
                        deepLinkValue = "",
                        refcode = SUCCESS_REFCODE,
                        campaign = SUCCESS_CAMPAIGN,
                    ),
                    shouldStore = true,
                ),
                HandleDeepLinkModel(
                    deepLink = createDeepLink(
                        deepLinkValue = "some_other_deep_link_value",
                        refcode = "null",
                        campaign = SUCCESS_CAMPAIGN,
                    ),
                    shouldStore = false,
                ),
                HandleDeepLinkModel(
                    deepLink = createDeepLink(deepLinkValue = "some_other_deep_link_value"),
                    shouldStore = false,
                ),
                HandleDeepLinkModel(
                    deepLink = createDeepLink(deepLinkValue = ""),
                    shouldStore = false,
                ),
                HandleDeepLinkModel(
                    deepLink = createDeepLink(
                        deepLinkValue = "referral",
                        refcode = "",
                        campaign = SUCCESS_CAMPAIGN,
                    ),
                    shouldStore = false,
                ),
                HandleDeepLinkModel(
                    deepLink = createDeepLink(
                        deepLinkValue = "referral",
                        refcode = null,
                        campaign = SUCCESS_CAMPAIGN,
                    ),
                    shouldStore = false,
                ),
            )
        }
    }

    data class HandleDeepLinkModel(val deepLink: DeepLink, val shouldStore: Boolean)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class HandleParams {

        @ParameterizedTest
        @ProvideTestModels
        fun handle(model: HandleParamsModel) = runTest {
            handler.handle(params = model.params)

            if (model.shouldStore) {
                val value = AppsFlyerConversionData(refcode = SUCCESS_REFCODE, campaign = SUCCESS_CAMPAIGN)
                coVerify { appsFlyerRepository.storeIfAbsent(value = value) }
            } else {
                coVerify(inverse = true) { appsFlyerRepository.storeIfAbsent(value = any()) }
            }
        }

        private fun provideTestModels(): List<HandleParamsModel> {
            return listOf(
                HandleParamsModel(
                    params = conversionData(
                        deepLinkValue = "referral",
                        refcode = SUCCESS_REFCODE,
                        campaign = SUCCESS_CAMPAIGN,
                    ),
                    shouldStore = true,
                ),
                HandleParamsModel(
                    params = conversionData(
                        deepLinkValue = "tpay_mobileonboard",
                        refcode = SUCCESS_REFCODE,
                        campaign = SUCCESS_CAMPAIGN,
                    ),
                    shouldStore = true,
                ),
                HandleParamsModel(
                    params = conversionData(
                        deepLinkValue = "some_other_deep_link_value",
                        refcode = SUCCESS_REFCODE,
                        campaign = SUCCESS_CAMPAIGN,
                    ),
                    shouldStore = true,
                ),
                HandleParamsModel(
                    params = conversionData(refcode = SUCCESS_REFCODE, campaign = SUCCESS_CAMPAIGN),
                    shouldStore = true,
                ),
                HandleParamsModel(
                    params = conversionData(
                        deepLinkValue = "",
                        refcode = SUCCESS_REFCODE,
                        campaign = SUCCESS_CAMPAIGN,
                    ),
                    shouldStore = true,
                ),
                HandleParamsModel(
                    params = conversionData(
                        deepLinkValue = "some_other_deep_link_value",
                        refcode = "null",
                        campaign = SUCCESS_CAMPAIGN,
                    ),
                    shouldStore = false,
                ),
                HandleParamsModel(
                    params = conversionData(deepLinkValue = "some_other_deep_link_value"),
                    shouldStore = false,
                ),
                HandleParamsModel(
                    params = conversionData(deepLinkValue = ""),
                    shouldStore = false,
                ),
                HandleParamsModel(
                    params = conversionData(deepLinkValue = "referral", refcode = "", campaign = SUCCESS_CAMPAIGN),
                    shouldStore = false,
                ),
                HandleParamsModel(
                    params = conversionData(deepLinkValue = "referral", campaign = SUCCESS_CAMPAIGN),
                    shouldStore = false,
                ),
            )
        }
    }

    data class HandleParamsModel(val params: Map<String?, Any?>, val shouldStore: Boolean)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CampaignValidation {

        @ParameterizedTest
        @ProvideTestModels
        fun handle(model: CampaignModel) = runTest {
            val params = conversionData(
                deepLinkValue = "referral",
                refcode = SUCCESS_REFCODE,
                campaign = model.rawCampaign,
            )

            handler.handle(params = params)

            val value = AppsFlyerConversionData(refcode = SUCCESS_REFCODE, campaign = model.expectedCampaign)
            coVerify { appsFlyerRepository.storeIfAbsent(value = value) }
        }

        private fun provideTestModels(): List<CampaignModel> {
            return listOf(
                CampaignModel(rawCampaign = SUCCESS_CAMPAIGN, expectedCampaign = SUCCESS_CAMPAIGN),
                CampaignModel(rawCampaign = null, expectedCampaign = null),
                CampaignModel(rawCampaign = "", expectedCampaign = null),
                CampaignModel(rawCampaign = " ", expectedCampaign = null),
                CampaignModel(rawCampaign = "null", expectedCampaign = null),
            )
        }
    }

    data class CampaignModel(val rawCampaign: String?, val expectedCampaign: String?)

    @Nested
    inner class NavigationDeeplink {

        @Test
        fun `GIVEN tpay_mobileonboard params WHEN handle THEN navigation deeplink stored`() = runTest {
            handler.handle(params = conversionData(deepLinkValue = "tpay_mobileonboard"))

            coVerify { appsFlyerRepository.storeNavigationDeeplink("tpay_mobileonboard") }
        }

        @Test
        fun `GIVEN tpay_mobileonboard deeplink WHEN handleDeeplink THEN navigation deeplink stored`() = runTest {
            handler.handleDeeplink(createDeepLink(deepLinkValue = "tpay_mobileonboard"))

            coVerify { appsFlyerRepository.storeNavigationDeeplink("tpay_mobileonboard") }
        }

        @Test
        fun `GIVEN referral params WHEN handle THEN navigation deeplink stored`() = runTest {
            handler.handle(params = conversionData(deepLinkValue = "referral"))

            coVerify { appsFlyerRepository.storeNavigationDeeplink("referral") }
        }

        @Test
        fun `GIVEN referral deeplink WHEN handleDeeplink THEN navigation deeplink stored`() = runTest {
            handler.handleDeeplink(createDeepLink(deepLinkValue = "referral"))

            coVerify { appsFlyerRepository.storeNavigationDeeplink("referral") }
        }

        @Test
        fun `GIVEN referral with valid refcode WHEN handle THEN navigation and conversion stored`() = runTest {
            handler.handle(
                params = conversionData(
                    deepLinkValue = "referral",
                    refcode = SUCCESS_REFCODE,
                    campaign = SUCCESS_CAMPAIGN,
                ),
            )

            coVerify { appsFlyerRepository.storeNavigationDeeplink("referral") }
            coVerify {
                appsFlyerRepository.storeIfAbsent(
                    value = AppsFlyerConversionData(refcode = SUCCESS_REFCODE, campaign = SUCCESS_CAMPAIGN),
                )
            }
        }

        @Test
        fun `GIVEN empty value with valid refcode WHEN handle THEN navigation deeplink not stored`() = runTest {
            handler.handle(params = conversionData(deepLinkValue = "", refcode = SUCCESS_REFCODE))

            coVerify(inverse = true) { appsFlyerRepository.storeNavigationDeeplink(any()) }
        }

        @Test
        fun `GIVEN tpay_mobileonboard with valid refcode WHEN handle THEN navigation deeplink stored`() = runTest {
            handler.handle(
                params = conversionData(deepLinkValue = "tpay_mobileonboard", refcode = SUCCESS_REFCODE),
            )

            coVerify { appsFlyerRepository.storeNavigationDeeplink("tpay_mobileonboard") }
        }

        @Test
        fun `GIVEN unknown value with valid refcode WHEN handle THEN navigation deeplink not stored`() = runTest {
            handler.handle(
                params = conversionData(deepLinkValue = "some_other_deep_link_value", refcode = SUCCESS_REFCODE),
            )

            coVerify(inverse = true) { appsFlyerRepository.storeNavigationDeeplink(any()) }
        }
    }

    @Nested
    inner class CampaignDeeplink {

        @Test
        fun `GIVEN campaign value AND toggle enabled WHEN handle THEN value stored`() = runTest {
            // Arrange
            every { tangemPayFeatureToggles.isCustomCardsCobrandEnabled } returns true

            // Act
            handler.handle(params = conversionData(deepLinkValue = CAMPAIGN_VALUE))

            // Assert
            coVerify { appsFlyerRepository.storeNavigationDeeplink(CAMPAIGN_VALUE) }
        }

        @Test
        fun `GIVEN campaign deeplink AND toggle enabled WHEN handleDeeplink THEN value stored`() = runTest {
            // Arrange
            every { tangemPayFeatureToggles.isCustomCardsCobrandEnabled } returns true

            // Act
            handler.handleDeeplink(createDeepLink(deepLinkValue = CAMPAIGN_VALUE))

            // Assert
            coVerify { appsFlyerRepository.storeNavigationDeeplink(CAMPAIGN_VALUE) }
        }

        @Test
        fun `GIVEN campaign value AND toggle disabled WHEN handle THEN nothing stored`() = runTest {
            // Arrange
            every { tangemPayFeatureToggles.isCustomCardsCobrandEnabled } returns false

            // Act
            handler.handle(params = conversionData(deepLinkValue = CAMPAIGN_VALUE))

            // Assert
            coVerify(inverse = true) { appsFlyerRepository.storeNavigationDeeplink(any()) }
        }

        @Test
        fun `GIVEN campaign prefix without key WHEN handle THEN nothing stored`() = runTest {
            // Arrange
            every { tangemPayFeatureToggles.isCustomCardsCobrandEnabled } returns true

            // Act
            handler.handle(params = conversionData(deepLinkValue = "tpay_for_"))

            // Assert
            coVerify(inverse = true) { appsFlyerRepository.storeNavigationDeeplink(any()) }
        }

        @Test
        fun `GIVEN referral AND toggle disabled WHEN handle THEN navigation deeplink still stored`() = runTest {
            // Arrange
            every { tangemPayFeatureToggles.isCustomCardsCobrandEnabled } returns false

            // Act
            handler.handle(params = conversionData(deepLinkValue = "referral"))

            // Assert
            coVerify { appsFlyerRepository.storeNavigationDeeplink("referral") }
        }
    }

    @Nested
    inner class ConversionDataReplay {

        @Test
        fun `GIVEN not a first launch WHEN handle THEN nothing stored`() = runTest {
            // Act
            handler.handle(params = conversionData(deepLinkValue = CAMPAIGN_VALUE, isFirstLaunch = false))

            // Assert
            coVerify(inverse = true) { appsFlyerRepository.storeNavigationDeeplink(any()) }
            coVerify(inverse = true) { appsFlyerRepository.storeIfAbsent(value = any()) }
        }

        @Test
        fun `GIVEN no first launch flag WHEN handle THEN nothing stored`() = runTest {
            // Act
            handler.handle(params = mapOf("deep_link_value" to CAMPAIGN_VALUE))

            // Assert
            coVerify(inverse = true) { appsFlyerRepository.storeNavigationDeeplink(any()) }
        }

        @Test
        fun `GIVEN first launch flag as string WHEN handle THEN navigation deeplink stored`() = runTest {
            // Act
            handler.handle(params = conversionData(deepLinkValue = CAMPAIGN_VALUE, isFirstLaunch = "true"))

            // Assert
            coVerify(exactly = 1) { appsFlyerRepository.storeNavigationDeeplink(CAMPAIGN_VALUE) }
        }

        @Test
        fun `GIVEN deferred deeplink handled by UDL WHEN handle THEN navigation deeplink stored once`() = runTest {
            // Arrange
            handler.handleDeeplink(createDeepLink(deepLinkValue = CAMPAIGN_VALUE, isDeferred = true))

            // Act
            handler.handle(params = conversionData(deepLinkValue = CAMPAIGN_VALUE))

            // Assert
            coVerify(exactly = 1) { appsFlyerRepository.storeNavigationDeeplink(CAMPAIGN_VALUE) }
        }

        @Test
        fun `GIVEN direct deeplink handled by UDL WHEN handle THEN first launch data still handled`() = runTest {
            // Arrange
            handler.handleDeeplink(createDeepLink(deepLinkValue = CAMPAIGN_VALUE, isDeferred = false))

            // Act
            handler.handle(params = conversionData(deepLinkValue = "referral", refcode = SUCCESS_REFCODE))

            // Assert
            coVerify(exactly = 1) { appsFlyerRepository.storeNavigationDeeplink("referral") }
            coVerify(exactly = 1) {
                appsFlyerRepository.storeIfAbsent(AppsFlyerConversionData(refcode = SUCCESS_REFCODE, campaign = null))
            }
        }

        @Test
        fun `GIVEN deeplink without deferred flag handled by UDL WHEN handle THEN treated as direct`() = runTest {
            // Arrange
            handler.handleDeeplink(createDeepLink(deepLinkValue = CAMPAIGN_VALUE, isDeferred = null))

            // Act
            handler.handle(params = conversionData(deepLinkValue = CAMPAIGN_VALUE))

            // Assert
            coVerify(exactly = 2) { appsFlyerRepository.storeNavigationDeeplink(CAMPAIGN_VALUE) }
        }
    }

    private fun createDeepLink(
        deepLinkValue: String?,
        refcode: String? = null,
        campaign: String? = null,
        isDeferred: Boolean? = false,
    ): DeepLink {
        return mockk<DeepLink> {
            every { this@mockk.deepLinkValue } returns deepLinkValue
            every { this@mockk.getStringValue("deep_link_sub1") } returns refcode
            every { this@mockk.getStringValue("deep_link_sub2") } returns campaign
            every { this@mockk.isDeferred } returns isDeferred
        }
    }

    private fun conversionData(
        deepLinkValue: String? = null,
        refcode: String? = null,
        campaign: String? = null,
        isFirstLaunch: Any = true,
    ): Map<String?, Any?> = buildMap {
        put("is_first_launch", isFirstLaunch)
        deepLinkValue?.let { put("deep_link_value", it) }
        refcode?.let { put("deep_link_sub1", it) }
        campaign?.let { put("deep_link_sub2", it) }
    }

    private companion object Companion {
        const val SUCCESS_REFCODE = "valid_refcode"
        const val SUCCESS_CAMPAIGN = "valid_campaign"
        const val CAMPAIGN_VALUE = "tpay_for_oxford"
    }
}