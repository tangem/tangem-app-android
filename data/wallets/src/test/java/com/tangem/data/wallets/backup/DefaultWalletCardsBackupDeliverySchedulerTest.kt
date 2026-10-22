package com.tangem.data.wallets.backup

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.connection.NetworkConnectionManager
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultWalletCardsBackupDeliverySchedulerTest {

    private val workManager: WorkManager = mockk(relaxed = true)
    private val networkConnectionManager: NetworkConnectionManager = mockk()
    private val isOnlineFlow = MutableStateFlow(false)
    private val enqueuedRequests = mutableListOf<OneTimeWorkRequest>()

    @BeforeEach
    fun setUp() {
        clearMocks(workManager, networkConnectionManager)
        enqueuedRequests.clear()
        isOnlineFlow.value = false
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(any()) } returns workManager
        every { networkConnectionManager.isOnlineFlow } returns isOnlineFlow
        every {
            workManager.enqueueUniqueWork(any<String>(), ExistingWorkPolicy.REPLACE, capture(enqueuedRequests))
        } returns mockk()
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(WorkManager::class)
    }

    @Test
    fun `GIVEN offline WHEN schedule THEN enqueues delivery waiting for a connected network`() = runTest {
        // Arrange
        val scheduler = createScheduler()

        // Act
        scheduler.schedule()
        runCurrent()

        // Assert
        assertThat(enqueuedRequests.map { it.requiredNetworkType() }).containsExactly(NetworkType.CONNECTED)
    }

    @Test
    fun `GIVEN scheduled while offline WHEN connection comes back THEN enqueues delivery not requiring a network`() =
        runTest {
            // Arrange
            val scheduler = createScheduler()
            scheduler.schedule()
            runCurrent()

            // Act
            isOnlineFlow.value = true
            runCurrent()

            // Assert
            assertThat(enqueuedRequests.map { it.requiredNetworkType() })
                .containsExactly(NetworkType.CONNECTED, NetworkType.NOT_REQUIRED)
                .inOrder()
        }

    @Test
    fun `GIVEN scheduled twice WHEN connection comes back THEN enqueues one delivery not requiring a network`() =
        runTest {
            // Arrange
            val scheduler = createScheduler()
            scheduler.schedule()
            scheduler.schedule()
            runCurrent()

            // Act
            isOnlineFlow.value = true
            runCurrent()

            // Assert
            assertThat(enqueuedRequests.count { it.requiredNetworkType() == NetworkType.NOT_REQUIRED }).isEqualTo(1)
        }

    @Test
    fun `GIVEN connection came back once WHEN it drops and comes back again THEN enqueues delivery again`() =
        runTest {
            // Arrange
            val scheduler = createScheduler()
            scheduler.schedule()
            isOnlineFlow.value = true
            runCurrent()

            // Act
            isOnlineFlow.value = false
            runCurrent()
            isOnlineFlow.value = true
            runCurrent()

            // Assert
            assertThat(enqueuedRequests.count { it.requiredNetworkType() == NetworkType.NOT_REQUIRED }).isEqualTo(2)
        }

    private fun TestScope.createScheduler() = DefaultWalletCardsBackupDeliveryScheduler(
        context = mockk<Context>(relaxed = true),
        networkConnectionManager = networkConnectionManager,
        appScope = TestAppCoroutineScope(backgroundScope.coroutineContext),
    )

    private fun OneTimeWorkRequest.requiredNetworkType(): NetworkType = workSpec.constraints.requiredNetworkType
}