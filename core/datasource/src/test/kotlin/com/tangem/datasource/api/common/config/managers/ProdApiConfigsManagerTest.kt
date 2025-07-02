package com.tangem.datasource.api.common.config.managers

import android.os.Build
import com.google.common.truth.Truth
import com.tangem.datasource.BuildConfig
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.api.common.config.*
import com.tangem.datasource.api.common.config.ApiConfig.Companion.DEBUG_BUILD_TYPE
import com.tangem.datasource.api.common.config.ApiConfig.Companion.EXTERNAL_BUILD_TYPE
import com.tangem.datasource.api.common.config.ApiConfig.Companion.INTERNAL_BUILD_TYPE
import com.tangem.datasource.api.common.config.ApiConfig.Companion.MOCKED_BUILD_TYPE
import com.tangem.datasource.api.common.config.ApiConfig.Companion.RELEASE_BUILD_TYPE
import com.tangem.lib.auth.ExpressAuthProvider
import com.tangem.lib.auth.StakeKitAuthProvider
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.info.AppInfoProvider
import com.tangem.utils.version.AppVersionProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale
import java.util.TimeZone

private val configManager = MockEnvironmentConfigStorage()
private val appVersionProvider = mockk<AppVersionProvider>()
private val expressAuthProvider = mockk<ExpressAuthProvider>()
private val stakeKitAuthProvider = mockk<StakeKitAuthProvider>()
private val appAuthProvider = mockk<AuthProvider>()
private val appInfoProvider = mockk<AppInfoProvider>()

// Don't forget to add new config !!!
private val API_CONFIGS = setOf(
    Express(configManager, expressAuthProvider, appVersionProvider, appInfoProvider),
    TangemTech(appVersionProvider, appAuthProvider, appInfoProvider),
    StakeKit(stakeKitAuthProvider),
    TangemVisa(appVersionProvider),
)

/**
* [REDACTED_AUTHOR]
 */
@RunWith(Parameterized::class)
internal class ProdApiConfigsManagerTest(private val model: Model) {

    private val manager = ProdApiConfigsManager(API_CONFIGS)

    @Before
    fun setup() {
        every { appVersionProvider.versionName } returns VERSION_NAME
        every { expressAuthProvider.getSessionId() } returns EXPRESS_SESSION_ID
        every { stakeKitAuthProvider.getApiKey() } returns STAKE_KIT_API_KEY
        every { appAuthProvider.getCardId() } returns APP_CARD_ID
        every { appAuthProvider.getCardPublicKey() } returns APP_CARD_PUBLIC_KEY
        every { appInfoProvider.osVersion } returns "Android 16"
    }

    @Test
    fun test_getEnvironmentConfig() {
        val actual = manager.getEnvironmentConfig(id = model.id)

        Truth.assertThat(actual.environment).isEqualTo(model.expected.environment)
        Truth.assertThat(actual.baseUrl).isEqualTo(model.expected.baseUrl)

        Truth.assertThat(actual.headers.mapValues { runBlocking { it.value() } })
            .isEqualTo(model.expected.headers.mapValues { runBlocking { it.value() } })
    }

    data class Model(val id: ApiConfig.ID, val expected: ApiEnvironmentConfig)

    internal companion object {

        const val VERSION_NAME = "debug"
        const val EXPRESS_USER_ID = "express_user_id"
        const val EXPRESS_SESSION_ID = "express_session_id"
        const val EXPRESS_REF_CODE = "express_ref_code"
        const val STAKE_KIT_API_KEY = "stake_kit_api_key"
        const val APP_CARD_ID = "app_card_id"
        const val APP_CARD_PUBLIC_KEY = "app_public_key"

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Model> = API_CONFIGS.map {
            when (it) {
                is Express -> createExpressModel()
                is TangemTech -> createTangemTechModel()
                is StakeKit -> createStakeKitModel()
                is TangemVisa -> createVisaModel()
                is Attestation -> createAttestationModel()
                is BlockAid -> createBlockAidSdkModel()
            }
        }

        private fun createExpressModel(): Model {
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

            return Model(
                id = ApiConfig.ID.Express,
                expected = ApiEnvironmentConfig(
                    environment = environment,
                    baseUrl = when (BuildConfig.BUILD_TYPE) {
                        DEBUG_BUILD_TYPE,
                        -> "https://express.tangem.org/v1/"
                        INTERNAL_BUILD_TYPE,
                        MOCKED_BUILD_TYPE,
                        -> "https://express-stage.tangem.com/v1/"
                        EXTERNAL_BUILD_TYPE,
                        RELEASE_BUILD_TYPE,
                        -> "https://express.tangem.com/v1/"
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

        private fun createTangemTechModel(): Model {
            return Model(
                id = ApiConfig.ID.TangemTech,
                expected = ApiEnvironmentConfig(
                    environment = ApiEnvironment.PROD,
                    baseUrl = "https://api.tangem.org/v1/",
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

        private fun createStakeKitModel(): Model {
            return Model(
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

        private fun createVisaModel(): Model {
            return Model(
                id = ApiConfig.ID.TangemVisa,
                expected = ApiEnvironmentConfig(
                    environment = ApiEnvironment.DEV,
                    baseUrl = "https://api.dev.paera.com/bff/",
                    headers = mapOf(
                        "version" to ProviderSuspend { VERSION_NAME },
                        "platform" to ProviderSuspend { "Android" },
                    ),
                ),
            )
        }

        private fun createAttestationModel(): Model {
            return Model(
                id = ApiConfig.ID.Attestation,
                expected = ApiEnvironmentConfig(
                    environment = ApiEnvironment.PROD,
                    baseUrl = "https://api.tangem-tech.com/",
                ),
            )
        }

        private fun createBlockAidSdkModel(): Model {
            return Model(
                id = ApiConfig.ID.BlockAid,
                expected = ApiEnvironmentConfig(
                    environment = ApiEnvironment.PROD,
                    baseUrl = "https://api.blockaid.io/v0/",
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
    }
}
