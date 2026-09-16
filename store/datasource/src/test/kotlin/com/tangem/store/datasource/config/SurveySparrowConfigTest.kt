package com.tangem.store.datasource.config

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.config.ApiEnvironment
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SurveySparrowConfigTest {

    private val storeEnvironmentConfig = mockk<StoreEnvironmentConfig>()

    private val config = SurveySparrow(storeEnvironmentConfig)

    @BeforeEach
    fun setup() {
        clearMocks(storeEnvironmentConfig)

        every { storeEnvironmentConfig.surveySparrowToken } returns SURVEY_SPARROW_TOKEN
    }

    @Test
    fun `GIVEN config WHEN read id THEN SurveySparrow id`() {
        // Assert
        assertThat(config.id).isEqualTo(SurveySparrow.ID)
    }

    @Test
    fun `GIVEN config WHEN read environments THEN expected base urls`() {
        // Act
        val actual = config.environmentConfigs.associate { it.environment to it.baseUrl }

        // Assert
        assertThat(actual).containsExactlyEntriesIn(
            mapOf(
                ApiEnvironment.PROD to "https://eu-api.surveysparrow.com/",
            ),
        )
    }

    @Test
    fun `GIVEN config WHEN read PROD headers THEN expected headers`() {
        // Arrange
        val prod = config.environmentConfigs.first { it.environment == ApiEnvironment.PROD }

        // Act
        val actual = prod.headers.mapValues { runBlocking { it.value() } }

        // Assert
        assertThat(actual).containsExactlyEntriesIn(
            mapOf(
                "Authorization" to "Bearer $SURVEY_SPARROW_TOKEN",
            ),
        )
    }

    private companion object {
        const val SURVEY_SPARROW_TOKEN = "survey_sparrow_token"
    }
}