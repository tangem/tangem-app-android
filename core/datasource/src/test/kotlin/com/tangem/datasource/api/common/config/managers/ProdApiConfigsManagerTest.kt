package com.tangem.datasource.api.common.config.managers

import com.google.common.truth.Truth
import com.tangem.datasource.BuildConfig
import com.tangem.datasource.api.common.config.*
import com.tangem.datasource.api.common.config.ApiConfig.Companion.DEBUG_BUILD_TYPE
import com.tangem.datasource.api.common.config.ApiConfig.Companion.EXTERNAL_BUILD_TYPE
import com.tangem.datasource.api.common.config.ApiConfig.Companion.INTERNAL_BUILD_TYPE
import com.tangem.datasource.api.common.config.ApiConfig.Companion.MOCKED_BUILD_TYPE
import com.tangem.datasource.api.common.config.ApiConfig.Companion.RELEASE_BUILD_TYPE
import com.tangem.lib.auth.ExpressAuthProvider
import com.tangem.lib.auth.StakeKitAuthProvider
import com.tangem.utils.Provider
import com.tangem.utils.version.AppVersionProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private val configManager = MockConfigManager()
private val appVersionProvider = mockk<AppVersionProvider>()
private val expressAuthProvider = mockk<ExpressAuthProvider>()
private val stakeKitAuthProvider = mockk<StakeKitAuthProvider>()

// Don't forget to add new config !!!
private val API_CONFIGS = setOf(
    Express(configManager, expressAuthProvider, appVersionProvider),
    TangemTech(appVersionProvider),
    StakeKit(stakeKitAuthProvider),
)

/**
[REDACTED_AUTHOR]
 */
@RunWith(Parameterized::class)
internal class ProdApiConfigsManagerTest(private val model: Model) {

    private val manager = ProdApiConfigsManager(API_CONFIGS)

    @Before
    fun setup() {
        every { appVersionProvider.versionName } returns VERSION_NAME
        every { expressAuthProvider.getUserId() } returns EXPRESS_USER_ID
        every { expressAuthProvider.getSessionId() } returns EXPRESS_SESSION_ID
        every { stakeKitAuthProvider.getApiKey() } returns STAKE_KIT_API_KEY
    }

    @Test
    fun test_getEnvironmentConfig() {
        val actual = manager.getEnvironmentConfig(id = model.id)

        Truth.assertThat(actual.environment).isEqualTo(model.expected.environment)
        Truth.assertThat(actual.baseUrl).isEqualTo(model.expected.baseUrl)

        Truth.assertThat(actual.headers.mapValues { it.value() })
            .isEqualTo(model.expected.headers.mapValues { it.value() })
    }

    data class Model(val id: ApiConfig.ID, val expected: ApiEnvironmentConfig)

    internal companion object {

        const val VERSION_NAME = "debug"
        const val EXPRESS_USER_ID = "express_user_id"
        const val EXPRESS_SESSION_ID = "express_session_id"
        const val EXPRESS_API_KEY = "express_api_key"
        const val EXPRESS_DEV_API_KEY = "express_dev_api_key"
        const val STAKE_KIT_API_KEY = "stake_kit_api_key"

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Model> = API_CONFIGS.map {
            when (it) {
                is Express -> createExpressModel()
                is TangemTech -> createTangemTechModel()
                is StakeKit -> createStakeKitModel()
            }
        }

        private fun createExpressModel(): Model {
            val environment = when (BuildConfig.BUILD_TYPE) {
                DEBUG_BUILD_TYPE -> ApiEnvironment.DEV
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
                        DEBUG_BUILD_TYPE -> "[REDACTED_ENV_URL]"
                        INTERNAL_BUILD_TYPE,
                        MOCKED_BUILD_TYPE,
                        -> "[REDACTED_ENV_URL]"
                        EXTERNAL_BUILD_TYPE,
                        RELEASE_BUILD_TYPE,
                        -> "[REDACTED_ENV_URL]"
                        else -> error("Unknown build type [${BuildConfig.BUILD_TYPE}]")
                    },
                    headers = mapOf(
                        "api-key" to Provider {
                            if (environment == ApiEnvironment.PROD) EXPRESS_API_KEY else EXPRESS_DEV_API_KEY
                        },
                        "user-id" to Provider { EXPRESS_USER_ID },
                        "session-id" to Provider { EXPRESS_SESSION_ID },
                        "version" to Provider { VERSION_NAME },
                        "platform" to Provider { "android" },
                    ),
                ),
            )
        }

        private fun createTangemTechModel(): Model {
            return Model(
                id = ApiConfig.ID.TangemTech,
                expected = ApiEnvironmentConfig(
                    environment = ApiEnvironment.PROD,
                    baseUrl = "https://api.tangem-tech.com/v1/",
                    headers = mapOf(
                        "version" to Provider { VERSION_NAME },
                        "platform" to Provider { "android" },
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
                        "X-API-KEY" to Provider { STAKE_KIT_API_KEY },
                        "accept" to Provider { "application/json" },
                    ),
                ),
            )
        }
    }
}