package com.tangem.tap.domain.scanCard

import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.card.ScanFailsRequester
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultScanFailsCounterTest {

    private val scanFailsRequester = mockk<ScanFailsRequester>()
    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val appScope = object : AppCoroutineScope {
        override val coroutineContext = dispatchers.main
    }

    private lateinit var counter: DefaultScanFailsCounter

    @BeforeEach
    fun setup() {
        clearMocks(scanFailsRequester)
        counter = DefaultScanFailsCounter(
            scanFailsRequester = scanFailsRequester,
            appScope = appScope,
        )
    }

    @Test
    fun `single user cancellation does not show dialog`() = runTest {
        // Arrange
        val source = AnalyticsParam.ScreensSources.Main
        coEvery { scanFailsRequester.show(source) } returns ScanFailsRequester.Result.Dismissed

        // Act
        counter.onScanFailure(isUserCancelled = true, source = source)

        // Assert
        coVerify(exactly = 0) { scanFailsRequester.show(source) }
    }

    @Test
    fun `two consecutive user cancellations show dialog`() = runTest {
        // Arrange
        val source = AnalyticsParam.ScreensSources.Main
        coEvery { scanFailsRequester.show(source) } returns ScanFailsRequester.Result.Dismissed

        // Act
        counter.onScanFailure(isUserCancelled = true, source = source)
        counter.onScanFailure(isUserCancelled = true, source = source)

        // Assert
        coVerify(exactly = 1) { scanFailsRequester.show(source) }
    }

    @Test
    fun `non-cancelled failure resets counter`() = runTest {
        // Arrange
        val source = AnalyticsParam.ScreensSources.Main
        coEvery { scanFailsRequester.show(source) } returns ScanFailsRequester.Result.Dismissed

        // Act
        counter.onScanFailure(isUserCancelled = true, source = source)
        counter.onScanFailure(isUserCancelled = false, source = source)
        counter.onScanFailure(isUserCancelled = true, source = source)

        // Assert
        coVerify(exactly = 0) { scanFailsRequester.show(source) }
    }

    @Test
    fun `reset clears counter`() = runTest {
        // Arrange
        val source = AnalyticsParam.ScreensSources.Main
        coEvery { scanFailsRequester.show(source) } returns ScanFailsRequester.Result.Dismissed

        // Act
        counter.onScanFailure(isUserCancelled = true, source = source)
        counter.reset()
        counter.onScanFailure(isUserCancelled = true, source = source)

        // Assert
        coVerify(exactly = 0) { scanFailsRequester.show(source) }
    }

    @Test
    fun `after reset two new cancellations show dialog again`() = runTest {
        // Arrange
        val source = AnalyticsParam.ScreensSources.SignIn
        coEvery { scanFailsRequester.show(source) } returns ScanFailsRequester.Result.Dismissed

        // Act
        counter.onScanFailure(isUserCancelled = true, source = source)
        counter.onScanFailure(isUserCancelled = true, source = source)
        counter.reset()
        counter.onScanFailure(isUserCancelled = true, source = source)
        counter.onScanFailure(isUserCancelled = true, source = source)

        // Assert
        coVerify(exactly = 2) { scanFailsRequester.show(source) }
    }

    @Test
    fun `dialog receives correct source`() = runTest {
        // Arrange
        val source = AnalyticsParam.ScreensSources.Settings
        coEvery { scanFailsRequester.show(source) } returns ScanFailsRequester.Result.Dismissed

        // Act
        counter.onScanFailure(isUserCancelled = true, source = source)
        counter.onScanFailure(isUserCancelled = true, source = source)

        // Assert
        coVerify(exactly = 1) { scanFailsRequester.show(source) }
    }

    @Test
    fun `third consecutive cancellation also triggers dialog`() = runTest {
        // Arrange
        val source = AnalyticsParam.ScreensSources.Intro
        coEvery { scanFailsRequester.show(source) } returns ScanFailsRequester.Result.Dismissed

        // Act
        counter.onScanFailure(isUserCancelled = true, source = source)
        counter.onScanFailure(isUserCancelled = true, source = source)
        counter.onScanFailure(isUserCancelled = true, source = source)

        // Assert
        coVerify(exactly = 2) { scanFailsRequester.show(source) }
    }
}