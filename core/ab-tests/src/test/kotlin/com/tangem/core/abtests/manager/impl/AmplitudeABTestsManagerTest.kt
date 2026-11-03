package com.tangem.core.abtests.manager.impl

import android.app.Application
import com.amplitude.experiment.Experiment
import com.amplitude.experiment.ExperimentClient
import com.amplitude.experiment.ExperimentConfig
import com.amplitude.experiment.ExperimentUser
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.clearStaticMockk
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.Future

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AmplitudeABTestsManagerTest {

    private val application: Application = mockk()
    private val client: ExperimentClient = mockk()
    private val fetchFuture: Future<ExperimentClient> = mockk()
    private val config = slot<ExperimentConfig>()
    private var currentUser: ExperimentUser? = null

    private val walletUser = ExperimentUser.builder()
        .userId(USER_ID)
        .userProperties(
            mapOf(
                AnalyticsParam.BATCH to BATCH,
                AnalyticsParam.PRODUCT_TYPE to PRODUCT_TYPE,
                AnalyticsParam.FIRMWARE to FIRMWARE,
            ),
        )
        .build()

    @BeforeAll
    fun mockExperiment() {
        mockkStatic(Experiment::class)
    }

    @AfterAll
    fun unmockExperiment() {
        unmockkStatic(Experiment::class)
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(client, fetchFuture)
        clearStaticMockk(Experiment::class)
        config.clear()
        currentUser = null
        every { Experiment.initializeWithAmplitudeAnalytics(application, API_KEY, capture(config)) } returns client
        every { client.fetch(any()) } answers {
            currentUser = firstArg<ExperimentUser?>() ?: currentUser
            fetchFuture
        }
        every { client.getUser() } answers { currentUser }
        every { fetchFuture.get() } returns client
        every { client.all() } returns emptyMap()
    }

    @Test
    fun `GIVEN new manager WHEN init THEN automatic fetch on identity change is disabled`() {
        // Act
        createManager().init()

        // Assert
        assertThat(config.captured.automaticFetchOnAmplitudeIdentityChange).isFalse()
    }

    @Test
    fun `GIVEN new manager WHEN init THEN variants are fetched once`() {
        // Act
        createManager().init()

        // Assert
        verify(exactly = 1) { client.fetch(any()) }
    }

    @Test
    fun `GIVEN initialized manager WHEN init again THEN client is not recreated`() {
        // Arrange
        val manager = createManager()
        manager.init()

        // Act
        manager.init()

        // Assert
        verify(exactly = 1) { Experiment.initializeWithAmplitudeAnalytics(any(), any(), any()) }
        verify(exactly = 1) { client.fetch(any()) }
    }

    @Test
    fun `GIVEN initialized manager WHEN setUserProperties THEN variants are fetched for the user`() {
        // Arrange
        val manager = createManager()
        manager.init()

        // Act
        manager.setWalletUser()

        // Assert
        verify(exactly = 1) { client.fetch(walletUser) }
    }

    @Test
    fun `GIVEN user already set WHEN setUserProperties with the same user THEN variants are not fetched again`() {
        // Arrange
        val manager = createManager()
        manager.init()
        manager.setWalletUser()

        // Act
        manager.setWalletUser()

        // Assert
        verify(exactly = 1) { client.fetch(walletUser) }
    }

    @Test
    fun `GIVEN user set WHEN removeUserProperties THEN variants are fetched for an empty user`() {
        // Arrange
        val manager = createManager()
        manager.init()
        manager.setWalletUser()

        // Act
        manager.removeUserProperties()

        // Assert
        verify(exactly = 1) { client.fetch(ExperimentUser()) }
    }

    @Test
    fun `GIVEN no user set WHEN removeUserProperties THEN variants are not fetched again`() {
        // Arrange
        val manager = createManager()
        manager.init()

        // Act
        manager.removeUserProperties()

        // Assert
        verify(exactly = 1) { client.fetch(any()) }
    }

    private fun AmplitudeABTestsManager.setWalletUser() {
        setUserProperties(userId = USER_ID, batch = BATCH, productType = PRODUCT_TYPE, firmware = FIRMWARE)
    }

    private fun createManager() = AmplitudeABTestsManager(
        application = application,
        apiKey = API_KEY,
        scope = TestAppCoroutineScope(),
    )

    private companion object {
        const val API_KEY = "api_key"
        const val USER_ID = "user_id"
        const val BATCH = "AC03"
        const val PRODUCT_TYPE = "Wallet"
        const val FIRMWARE = "4.52r"
    }
}