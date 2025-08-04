package com.tangem.datasource.api.common.config.managers

import android.os.Build
import com.google.common.truth.Truth
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.datasource.BuildConfig
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.api.common.config.*
import com.tangem.datasource.api.common.config.ApiConfig.Companion.DEBUG_BUILD_TYPE
import com.tangem.datasource.api.common.config.ApiConfig.Companion.EXTERNAL_BUILD_TYPE
import com.tangem.datasource.api.common.config.ApiConfig.Companion.INTERNAL_BUILD_TYPE
import com.tangem.datasource.api.common.config.ApiConfig.Companion.MOCKED_BUILD_TYPE
import com.tangem.datasource.api.common.config.ApiConfig.Companion.RELEASE_BUILD_TYPE
import com.tangem.datasource.api.common.config.managers.MockEnvironmentConfigStorage.Companion.BLOCK_AID_API_KEY
import com.tangem.lib.auth.ExpressAuthProvider
import com.tangem.lib.auth.StakeKitAuthProvider
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.info.AppInfoProvider
import com.tangem.utils.version.AppVersionProvider
import io.mockk.clearMocks
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

    private val environmentConfigStorage = MockEnvironmentConfigStorage()
    private val appVersionProvider = mockk<AppVersionProvider>()
    private val expressAuthProvider = mockk<ExpressAuthProvider>()
    private val stakeKitAuthProvider = mockk<StakeKitAuthProvider>()
    private val appAuthProvider = mockk<AuthProvider>()
    private val appInfoProvider = mockk<AppInfoProvider>()

    private val manager = ProdApiConfigsManager(apiConfigs = createApiConfigs())

    @BeforeEach
    fun setup() {
        clearMocks(
            appVersionProvider,
            expressAuthProvider,
            stakeKitAuthProvider,
            appAuthProvider,
            appInfoProvider,
        )

        every { appVersionProvider.versionName } returns VERSION_NAME
        every { expressAuthProvider.getSessionId() } returns EXPRESS_SESSION_ID
        every { stakeKitAuthProvider.getApiKey() } returns STAKE_KIT_API_KEY
        every { appAuthProvider.getCardId() } returns APP_CARD_ID
        every { appAuthProvider.getCardPublicKey() } returns APP_CARD_PUBLIC_KEY
        every { appInfoProvider.osVersion } returns "Android 16"
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
        return ApiConfig.ID.entries.mapTo(destination = hashSetOf()) {
            when (it) {
                ApiConfig.ID.Express -> {
                    Express(
                        environmentConfigStorage = environmentConfigStorage,
                        expressAuthProvider = expressAuthProvider,
                        appVersionProvider = appVersionProvider,
                        appInfoProvider = appInfoProvider,
                    )
                }
                ApiConfig.ID.TangemTech -> {
                    TangemTech(
                        appVersionProvider = appVersionProvider,
                        authProvider = appAuthProvider,
                        appInfoProvider = appInfoProvider,
                    )
                }
                ApiConfig.ID.StakeKit -> StakeKit(stakeKitAuthProvider = stakeKitAuthProvider)
                ApiConfig.ID.TangemPay -> TangemPay(appVersionProvider = appVersionProvider)
                ApiConfig.ID.BlockAid -> BlockAid(configStorage = environmentConfigStorage)
            }
        }
    }

    private fun provideTestModels() = ApiConfig.ID.entries.map {
        when (it) {
            ApiConfig.ID.Express -> createExpressModel()
            ApiConfig.ID.TangemTech -> createTangemTechModel()
            ApiConfig.ID.StakeKit -> createStakeKitModel()
            ApiConfig.ID.TangemPay -> createTangemPayModel()
            ApiConfig.ID.BlockAid -> createBlockAidSdkModel()
        }
    }

    private fun createExpressModel(): TestModel {
        val environment = when (BuildConfig.BUILD_TYPE) {
            DEBUG_BUILD_TYPE,
            -> ApiEnvironment.DEV
            INTERNAL_BUILD_TYPE,
            MOCKED_BUILD_TYPE,
            -> ApiEnvironment.STAGE
            EXTERNAL_BUILD_TYPE,
            RELEASE_BUILD_TYPE,
            -> ApiEnvironment.PROD
            else -> error("Unknown build type [${BuildConfig.BUILD_TYPE}]")
        }

        return TestModel(
            id = ApiConfig.ID.Express,
            expected = ApiEnvironmentConfig(
                environment = environment,
                baseUrl = when (BuildConfig.BUILD_TYPE) {
                    DEBUG_BUILD_TYPE,
                    -> "[REDACTED_ENV_URL]"
                    INTERNAL_BUILD_TYPE,
                    MOCKED_BUILD_TYPE,
                    -> "[REDACTED_ENV_URL]"
                    EXTERNAL_BUILD_TYPE,
                    RELEASE_BUILD_TYPE,
                    -> "[REDACTED_ENV_URL]"
                    else -> error("Unknown build type [${BuildConfig.BUILD_TYPE}]")
                },
                headers = mapOf(
                    "api-key" to ProviderSuspend {
                        if (environment == ApiEnvironment.PROD) {
                            MockEnvironmentConfigStorage.EXPRESS_API_KEY
                        } else {
                            MockEnvironmentConfigStorage.EXPRESS_DEV_API_KEY
                        }
                    },
                    "session-id" to ProviderSuspend { EXPRESS_SESSION_ID },
                    "version" to ProviderSuspend { VERSION_NAME },
                    "system_version" to ProviderSuspend { "Android 16" },
                    "platform" to ProviderSuspend { "android" },
                    "language" to ProviderSuspend { Locale.getDefault().language.checkHeaderValueOrEmpty() },
                    "timezone" to ProviderSuspend {
                        TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT).checkHeaderValueOrEmpty()
                    },
                    "device" to ProviderSuspend { "${Build.MANUFACTURER} ${Build.MODEL}".checkHeaderValueOrEmpty() },
                ),
            ),
        )
    }

    private fun createTangemTechModel(): TestModel {
        return TestModel(
            id = ApiConfig.ID.TangemTech,
            expected = ApiEnvironmentConfig(
                environment = ApiEnvironment.PROD,
                baseUrl = "https://api.tangem.org/",
                headers = mapOf(
                    "card_id" to ProviderSuspend { APP_CARD_ID },
                    "card_public_key" to ProviderSuspend { APP_CARD_PUBLIC_KEY },
                    "version" to ProviderSuspend { VERSION_NAME },
                    "platform" to ProviderSuspend { "android" },
                    "system_version" to ProviderSuspend { "Android 16" },
                    "language" to ProviderSuspend { Locale.getDefault().language.checkHeaderValueOrEmpty() },
                    "timezone" to ProviderSuspend {
                        TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT).checkHeaderValueOrEmpty()
                    },
                    "device" to ProviderSuspend { "${Build.MANUFACTURER} ${Build.MODEL}".checkHeaderValueOrEmpty() },
                ),
            ),
        )
    }

    private fun createStakeKitModel(): TestModel {
        return TestModel(
            id = ApiConfig.ID.StakeKit,
            expected = ApiEnvironmentConfig(
                environment = ApiEnvironment.PROD,
                baseUrl = "https://api.stakek.it/v1/",
                headers = mapOf(
                    "X-API-KEY" to ProviderSuspend { STAKE_KIT_API_KEY },
                    "accept" to ProviderSuspend { "application/json" },
                ),
            ),
        )
    }

    private fun createTangemPayModel(): TestModel {
        return TestModel(
            id = ApiConfig.ID.TangemPay,
            expected = ApiEnvironmentConfig(
                environment = ApiEnvironment.DEV,
                baseUrl = "[REDACTED_ENV_URL]",
                headers = mapOf(
                    "version" to ProviderSuspend { VERSION_NAME },
                    "platform" to ProviderSuspend { "Android" },
                ),
            ),
        )
    }

    private fun createBlockAidSdkModel(): TestModel {
        return TestModel(
            id = ApiConfig.ID.BlockAid,
            expected = ApiEnvironmentConfig(
                environment = ApiEnvironment.PROD,
                baseUrl = "https://api.blockaid.io/v0/",
                headers = mapOf(
                    "X-API-KEY" to ProviderSuspend { BLOCK_AID_API_KEY },
                    "accept" to ProviderSuspend { "application/json" },
                    "content-type" to ProviderSuspend { "application/json" },
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
        const val EXPRESS_SESSION_ID = "express_session_id"
        const val STAKE_KIT_API_KEY = "stake_kit_api_key"
        const val APP_CARD_ID = "app_card_id"
        const val APP_CARD_PUBLIC_KEY = "app_public_key"
    }
}