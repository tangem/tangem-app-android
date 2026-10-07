package com.tangem.tap.common.analytics.appsflyer

import android.content.Context
import com.appsflyer.AppsFlyerLib
import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GoogleAdsConversionDataReporterTest {

    private val context: Context = mockk()
    private val appsFlyerLib: AppsFlyerLib = mockk(relaxUnitFun = true)

    private val reporter = GoogleAdsConversionDataReporter(context = context)

    @BeforeAll
    fun mockAppsFlyer() {
        mockkStatic(AppsFlyerLib::class)
        every { AppsFlyerLib.getInstance() } returns appsFlyerLib
    }

    @AfterAll
    fun unmockAppsFlyer() {
        unmockkStatic(AppsFlyerLib::class)
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(appsFlyerLib)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Report {

        @Test
        fun `GIVEN first launch of a google ads install WHEN report THEN payload sent as AppsFlyer event`() {
            // Act
            reporter.report(conversionData = GOOGLE_ADS_PAYLOAD)

            // Assert
            verify(exactly = 1) { appsFlyerLib.logEvent(context, REPORT_EVENT_NAME, REPORT_VALUES) }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN not a google ads first launch WHEN report THEN nothing sent`(model: NotSentModel) {
            // Act
            reporter.report(conversionData = model.conversionData)

            // Assert
            verify(exactly = 0) { appsFlyerLib.logEvent(any(), any(), any()) }
        }

        private fun provideTestModels(): List<NotSentModel> = listOf(
            NotSentModel(conversionData = GOOGLE_ADS_PAYLOAD + ("is_first_launch" to false)),
            NotSentModel(conversionData = GOOGLE_ADS_PAYLOAD - "is_first_launch"),
            NotSentModel(conversionData = GOOGLE_ADS_PAYLOAD + ("media_source" to "Facebook Ads")),
            NotSentModel(conversionData = GOOGLE_ADS_PAYLOAD - "media_source"),
        )
    }

    data class NotSentModel(val conversionData: Map<String?, Any?>)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToReportValues {

        @Test
        fun `GIVEN conversion data WHEN toReportValues THEN key fields verbatim and non-null other keys listed`() {
            // Act
            val actual = (GOOGLE_ADS_PAYLOAD + (null to "dropped")).toReportValues()

            // Assert
            assertThat(actual).isEqualTo(REPORT_VALUES)
        }
    }

    private companion object {
        const val REPORT_EVENT_NAME = "debug_google_ads_conversion_data"

        val GOOGLE_ADS_PAYLOAD: Map<String?, Any?> = mapOf(
            "media_source" to "googleadwords_int",
            "af_status" to "Non-organic",
            "is_first_launch" to true,
            "af_dp" to "tangem://onboard-visa?entry=ff1&id=VRTACC01",
            "campaign" to null,
            "network" to "Search",
            "iscache" to false,
            "af_sub1" to null,
        )

        val REPORT_VALUES = mapOf(
            "af_dp" to "tangem://onboard-visa?entry=ff1&id=VRTACC01",
            "media_source" to "googleadwords_int",
            "af_status" to "Non-organic",
            "is_first_launch" to "true",
            "campaign" to "null",
            "tangem_other_keys" to "iscache,network",
        )
    }
}