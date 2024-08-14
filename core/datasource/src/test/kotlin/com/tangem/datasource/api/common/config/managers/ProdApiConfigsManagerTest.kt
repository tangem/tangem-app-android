package com.tangem.datasource.api.common.config.managers

import com.google.common.truth.Truth
import com.tangem.datasource.BuildConfig
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiConfig.Companion.DEBUG_BUILD_TYPE
import com.tangem.datasource.api.common.config.ApiConfig.Companion.EXTERNAL_BUILD_TYPE
import com.tangem.datasource.api.common.config.ApiConfig.Companion.INTERNAL_BUILD_TYPE
import com.tangem.datasource.api.common.config.ApiConfig.Companion.MOCKED_BUILD_TYPE
import com.tangem.datasource.api.common.config.ApiConfig.Companion.RELEASE_BUILD_TYPE
import com.tangem.datasource.api.common.config.Express
import com.tangem.datasource.api.common.config.StakeKit
import com.tangem.datasource.api.common.config.TangemTech
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
[REDACTED_AUTHOR]
 */
@RunWith(Parameterized::class)
internal class ProdApiConfigsManagerTest(private val model: Model) {

    private val manager = ProdApiConfigsManager(API_CONFIGS)

    @Test
    fun test_getBaseUrl() {
        val actual = manager.getEnvironmentConfig(id = model.id).baseUrl

        Truth.assertThat(actual).isEqualTo(model.expected)
    }

    data class Model(val id: ApiConfig.ID, val expected: String)

    private companion object {

        val API_CONFIGS = setOf(
            Express(mockk(), mockk(), mockk()),
            TangemTech(mockk()),
            StakeKit(mockk()),

            // Don't forget to add new config
        )

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
            return Model(
                id = ApiConfig.ID.Express,
                expected = when (BuildConfig.BUILD_TYPE) {
                    DEBUG_BUILD_TYPE -> "[REDACTED_ENV_URL]"
                    INTERNAL_BUILD_TYPE,
                    MOCKED_BUILD_TYPE,
                    -> "[REDACTED_ENV_URL]"
                    EXTERNAL_BUILD_TYPE,
                    RELEASE_BUILD_TYPE,
                    -> "https://express.tangem.com/v1/"
                    else -> error("Unknown build type [${BuildConfig.BUILD_TYPE}]")
                },
            )
        }

        private fun createTangemTechModel(): Model {
            return Model(
                id = ApiConfig.ID.TangemTech,
                expected = "https://api.tangem-tech.com/v1/",
            )
        }

        private fun createStakeKitModel(): Model {
            return Model(
                id = ApiConfig.ID.StakeKit,
                expected = "https://api.stakek.it/v1/",
            )
        }
    }
}