package com.tangem.data.walletconnect.sdk

import android.app.Application
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.data.walletconnect.utils.WcSdkObserver
import com.tangem.domain.walletconnect.config.WalletConnectConfig
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.google.common.truth.Truth.assertThat

/**
 * Covers the single-flight start-up: the SDK must come up once even under concurrent callers, survive a
 * caller being cancelled, and allow a retry after a failure.
 */
internal class WcSdkTest {

    private val application: Application = mockk(relaxed = true)
    private val observer: WcSdkObserver = mockk(relaxed = true)

    private val onSuccess = slot<() -> Unit>()
    private val onError = slot<(Wallet.Model.Error) -> Unit>()

    @BeforeEach
    fun setUp() {
        mockkObject(CoreClient)
        mockkObject(WalletKit)
        every {
            CoreClient.initialize(
                application = any(),
                relayServerUrl = any(),
                connectionType = any(),
                metaData = any(),
                onError = any(),
            )
        } just runs
        every { WalletKit.setWalletDelegate(any()) } just runs
        every { WalletKit.initialize(any(), capture(onSuccess), capture(onError)) } just runs
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(CoreClient, WalletKit)
    }

    @Test
    fun `GIVEN concurrent callers WHEN ensureInitialized THEN sdk starts once`() = runTest {
        // Arrange
        val wcSdk = createSdk(this)

        // Act
        val first = async { wcSdk.ensureInitialized() }
        val second = async { wcSdk.ensureInitialized() }
        runCurrent()
        onSuccess.captured.invoke()
        first.await()
        second.await()

        // Assert
        verify(exactly = 1) { WalletKit.initialize(any(), any(), any()) }
        verify(exactly = 1) { observer.onWcSdkInit() }
        assertThat(wcSdk.isInitialized).isTrue()
    }

    @Test
    fun `GIVEN a cancelled caller WHEN another one arrives THEN the same start-up is awaited`() = runTest {
        // Arrange
        val wcSdk = createSdk(this)

        // Act
        val cancelled = async { wcSdk.ensureInitialized() }
        runCurrent()
        cancelled.cancel()
        val second = async { wcSdk.ensureInitialized() }
        runCurrent()
        onSuccess.captured.invoke()
        second.await()

        // Assert
        verify(exactly = 1) { WalletKit.initialize(any(), any(), any()) }
        verify(exactly = 1) { observer.onWcSdkInit() }
    }

    @Test
    fun `GIVEN a failed start-up WHEN called again THEN a new attempt is made`() = runTest {
        // Arrange
        val wcSdk = createSdk(this)

        // Act
        // Caught inside the coroutine: a throwing async would fail the test scope itself
        val failing = async { runCatching { wcSdk.ensureInitialized() } }
        runCurrent()
        onError.captured.invoke(Wallet.Model.Error(IllegalStateException("relay is down")))
        val failure = failing.await()

        val retrying = async { wcSdk.ensureInitialized() }
        runCurrent()
        onSuccess.captured.invoke()
        retrying.await()

        // Assert
        assertThat(failure.isFailure).isTrue()
        verify(exactly = 2) { WalletKit.initialize(any(), any(), any()) }
        assertThat(wcSdk.isInitialized).isTrue()
    }

    @Test
    fun `GIVEN sdk already started WHEN ensureInitialized THEN it does not start again`() = runTest {
        // Arrange
        val wcSdk = createSdk(this)
        val first = async { wcSdk.ensureInitialized() }
        runCurrent()
        onSuccess.captured.invoke()
        first.await()

        // Act
        wcSdk.ensureInitialized()

        // Assert
        verify(exactly = 1) { WalletKit.initialize(any(), any(), any()) }
    }

    private fun createSdk(testScope: TestScope) = WcSdk(
        application = application,
        config = WalletConnectConfig(projectId = "project-id"),
        observers = { setOf(observer) },
        scope = TestAppCoroutineScope(testScope),
        dispatchers = TestingCoroutineDispatcherProvider(),
    )
}