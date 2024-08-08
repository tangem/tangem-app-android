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
import com.tangem.datasource.api.common.config.TangemTech
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
* [REDACTED_AUTHOR]
 */
@RunWith(Parameterized::class)
internal class ProdApiConfigsManagerTest(private val model: Model) {

    private val manager = ProdApiConfigsManager()

    @Test
    fun test_getBaseUrl() {
        val actual = manager.getBaseUrl(id = model.id)

        Truth.assertThat(actual).isEqualTo(model.expected)
    }

    data class Model(val id: ApiConfig.ID, val expected: String)

    private companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Model> = ApiConfig.values().map {
            when (it) {
                is Express -> createExpressModel()
                is TangemTech -> createTangemTechModel()
            }
        }

        private fun createExpressModel(): Model {
            return Model(
                id = ApiConfig.ID.Express,
                expected = when (BuildConfig.BUILD_TYPE) {
                    DEBUG_BUILD_TYPE -> "https://express.tangem.org/v1/"
                    INTERNAL_BUILD_TYPE,
                    MOCKED_BUILD_TYPE,
                    -> "https://express-stage.tangem.com/v1/"
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
                expected = when (BuildConfig.BUILD_TYPE) {
                    DEBUG_BUILD_TYPE,
                    INTERNAL_BUILD_TYPE,
                    -> "https://devapi.tangem-tech.com/v1/"
                    MOCKED_BUILD_TYPE,
                    EXTERNAL_BUILD_TYPE,
                    RELEASE_BUILD_TYPE,
                    -> "https://api.tangem-tech.com/v1/"
                    else -> error("Unknown build type [${BuildConfig.BUILD_TYPE}]")
                },
            )
        }
    }
}
