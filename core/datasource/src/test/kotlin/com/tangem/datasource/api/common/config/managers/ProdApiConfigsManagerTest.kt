package com.tangem.datasource.api.common.config.managers

import android.os.Build
import com.google.common.truth.Truth
import com.tangem.datasource.BuildConfig
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.api.common.config.*
import com.tangem.datasource.utils.AuthenticationHeader
import com.tangem.datasource.utils.TangemApiKeyHeader
import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiConfigs
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.ApiEnvironmentConfig
import com.tangem.core.remote.config.ApiConfig.Companion.DEBUG_BUILD_TYPE
import com.tangem.core.remote.config.ApiConfig.Companion.EXTERNAL_BUILD_TYPE
import com.tangem.core.remote.config.ApiConfig.Companion.INTERNAL_BUILD_TYPE
import com.tangem.core.remote.config.ApiConfig.Companion.MOCKED_BUILD_TYPE
import com.tangem.core.remote.config.ApiConfig.Companion.RELEASE_BUILD_TYPE
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.info.AppInfoProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.util.Locale
import java.util.TimeZone

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ProdApiConfigsManagerTest {

    private val appAuthProvider = mockk<AuthProvider>()
    private val appInfoProvider = mockk<AppInfoProvider>()
    private val tangemApiKeyProvider = mockk<ProviderSuspend<String>>()

    private lateinit var manager: ProdApiConfigsManager

    @BeforeEach
    fun setup() {
        clearMocks(
            appAuthProvider,
            appInfoProvider,
        )

        every { appInfoProvider.appVersion } returns VERSION_NAME
        every { appAuthProvider.getApiKey(any()) } returns tangemApiKeyProvider
        coEvery { tangemApiKeyProvider.invoke() } returns TANGEM_API_KEY
        coEvery { appAuthProvider.getCardId() } returns APP_CARD_ID
        coEvery { appAuthProvider.getCardPublicKey() } returns APP_CARD_PUBLIC_KEY

        every { appInfoProvider.osVersion } returns "Android 16"
        every { appInfoProvider.language } returns Locale.getDefault().toLanguageTag()
        every { appInfoProvider.device } returns "${Build.MANUFACTURER} ${Build.MODEL}"
        every { appInfoProvider.deviceScale } returns DEVICE_SCALE

        manager = ProdApiConfigsManager(apiConfigs = createApiConfigs())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun getEnvironmentConfig(model: TestModel) {
        val actual = manager.getEnvironmentConfig(id = model.id)

        Truth.assertThat(actual.environment).isEqualTo(model.expected.environment)
        Truth.assertThat(actual.baseUrl).isEqualTo(model.expected.baseUrl)

        Truth.assertThat(actual.headers.mapValues { runBlocking { it.value() } })
            .isEqualTo(model.expected.headers.mapValues { runBlocking { it.value() } })
    }

    private fun createApiConfigs(): ApiConfigs {
        val configs = listOf(
            TangemTech(
                apiKeyHeader = { environment -> TangemApiKeyHeader(appAuthProvider, environment) },
                cardAuthHeader = { AuthenticationHeader(appAuthProvider) },
                appInfoProvider = appInfoProvider,
            ),
            PolymarketWeb(),
            PolymarketRelayer(),
            PolymarketClob(),
        )

        return configs.associateBy { it.id.name }
            .also { check(it.size == configs.size) { "Duplicate ApiConfig id in test setup" } }
    }

    private fun provideTestModels() = listOf(
        createTangemTechModel(),
        createPolymarketWebModel(),
        createPolymarketRelayerModel(),
        createPolymarketClobModel(),
    )

    private fun createPolymarketWebModel(): TestModel {
        return TestModel(
            id = PolymarketWeb.ID,
            expected = ApiEnvironmentConfig(
                environment = ApiEnvironment.PROD,
                baseUrl = "https://polymarket.com/",
            ),
        )
    }

    private fun createPolymarketRelayerModel(): TestModel {
        return TestModel(
            id = PolymarketRelayer.ID,
            expected = ApiEnvironmentConfig(
                environment = ApiEnvironment.PROD,
                baseUrl = "https://relayer-v2.polymarket.com/",
            ),
        )
    }

    private fun createPolymarketClobModel(): TestModel {
        return TestModel(
            id = PolymarketClob.ID,
            expected = ApiEnvironmentConfig(
                environment = ApiEnvironment.PROD,
                baseUrl = "https://clob.polymarket.com/",
            ),
        )
    }

    private fun createTangemTechModel(): TestModel {
        return TestModel(
            id = TangemTech.ID,
            expected = ApiEnvironmentConfig(
                environment = ApiEnvironment.PROD,
                baseUrl = "https://api.tangem.org/",
                headers = mapOf(
                    "api-key" to ProviderSuspend { TANGEM_API_KEY },
                    "card_id" to ProviderSuspend { APP_CARD_ID },
                    "card_public_key" to ProviderSuspend { APP_CARD_PUBLIC_KEY },
                    "version" to ProviderSuspend { VERSION_NAME },
                    "platform" to ProviderSuspend { "android" },
                    "system_version" to ProviderSuspend { "Android 16" },
                    "language" to ProviderSuspend { Locale.getDefault().toLanguageTag().checkHeaderValueOrEmpty() },
                    "timezone" to ProviderSuspend {
                        TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT).checkHeaderValueOrEmpty()
                    },
                    "device" to ProviderSuspend { "${Build.MANUFACTURER} ${Build.MODEL}".checkHeaderValueOrEmpty() },
                ),
            ),
        )
    }

    private fun String.checkHeaderValueOrEmpty(): String {
        for (i in this.indices) {
            val c = this[i]
            val charCondition = c == '\t' || c in '\u0020'..'\u007e'
            if (!charCondition) {
                return ""
            }
        }
        return this
    }

    data class TestModel(val id: ApiConfig.ID, val expected: ApiEnvironmentConfig)

    private companion object {

        const val VERSION_NAME = "debug"
        const val DEVICE_SCALE = 3f
        const val APP_CARD_ID = "app_card_id"
        const val APP_CARD_PUBLIC_KEY = "Bearer app_public_key"
        const val TANGEM_API_KEY = "tangem_api_key"
    }
}