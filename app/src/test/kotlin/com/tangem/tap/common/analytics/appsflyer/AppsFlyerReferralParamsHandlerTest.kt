package com.tangem.tap.common.analytics.appsflyer

import com.appsflyer.deeplink.DeepLink
import com.tangem.datasource.local.appsflyer.AppsFlyerConversionStore
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppsFlyerReferralParamsHandlerTest {

    private val appsFlyerConversionStore: AppsFlyerConversionStore = mockk(relaxUnitFun = true)
    private val handler = AppsFlyerReferralParamsHandler(
        appsFlyerConversionStore = appsFlyerConversionStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @AfterEach
    fun tearDown() {
        clearMocks(appsFlyerConversionStore)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class HandleDeepLink {

        @ParameterizedTest
        @ProvideTestModels
        fun handle(model: HandleDeepLinkModel) = runTest {
            handler.handle(deepLink = model.deepLink)

            if (model.shouldStore) {
                val value = AppsFlyerConversionData(refcode = SUCCESS_REFCODE, campaign = SUCCESS_CAMPAIGN)
                coVerify { appsFlyerConversionStore.storeIfAbsent(value = value) }
            } else {
                coVerify(inverse = true) { appsFlyerConversionStore.storeIfAbsent(value = any()) }
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

        private fun createDeepLink(
            deepLinkValue: String,
            refcode: String? = null,
            campaign: String? = null,
        ): DeepLink {
            return mockk<DeepLink> {
                every { this@mockk.deepLinkValue } returns deepLinkValue
                every { this@mockk.getStringValue("deep_link_sub1") } returns refcode
                every { this@mockk.getStringValue("deep_link_sub2") } returns campaign
            }
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
                coVerify { appsFlyerConversionStore.storeIfAbsent(value = value) }
            } else {
                coVerify(inverse = true) { appsFlyerConversionStore.storeIfAbsent(value = any()) }
            }
        }

        private fun provideTestModels(): List<HandleParamsModel> {
            return listOf(
                HandleParamsModel(
                    params = mapOf(
                        "deep_link_value" to "referral",
                        "deep_link_sub1" to SUCCESS_REFCODE,
                        "deep_link_sub2" to SUCCESS_CAMPAIGN,
                    ),
                    shouldStore = true,
                ),
                HandleParamsModel(
                    params = mapOf("deep_link_value" to "some_other_deep_link_value"),
                    shouldStore = false,
                ),
                HandleParamsModel(
                    params = mapOf("deep_link_value" to ""),
                    shouldStore = false,
                ),
                HandleParamsModel(
                    params = mapOf(
                        "deep_link_value" to "referral",
                        "deep_link_sub1" to "",
                        "deep_link_sub2" to SUCCESS_CAMPAIGN,
                    ),
                    shouldStore = false,
                ),
                HandleParamsModel(
                    params = mapOf(
                        "deep_link_value" to "referral",
                        "deep_link_sub2" to SUCCESS_CAMPAIGN,
                    ),
                    shouldStore = false,
                ),
            )
        }
    }

    data class HandleParamsModel(val params: Map<String?, Any?>, val shouldStore: Boolean)

    private companion object Companion {
        const val SUCCESS_REFCODE = "valid_refcode"
        const val SUCCESS_CAMPAIGN = "valid_campaign"
    }
}