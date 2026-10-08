package com.tangem.google.auth

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GoogleAuthActivityResultBridgeTest {

    private val lifecycleOwner: LifecycleOwner = mockk()

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN host resumed WHEN result delivered THEN result returned`(model: LaunchModel) = runTest {
        // Arrange
        val bridge = createBridge()
        val registry = FakeActivityResultRegistry()
        val lifecycle = createLifecycle(Lifecycle.State.RESUMED)
        bridge.attach(registry, lifecycle)
        val data: Intent = mockk()

        // Act
        val result = async { model.launch(bridge) }
        runCurrent()
        registry.dispatchResult(registry.launchedRequestCodes.single(), Activity.RESULT_OK, data)
        advanceUntilIdle()

        // Assert
        assertResult(result.await(), expectedCode = Activity.RESULT_OK, expectedData = data)
    }

    @Test
    fun `GIVEN host resumed at launch WHEN paused and resumed without result THEN completes as canceled`() = runTest {
        // Arrange
        val bridge = createBridge()
        val lifecycle = createLifecycle(Lifecycle.State.RESUMED)
        bridge.attach(FakeActivityResultRegistry(), lifecycle)

        // Act
        val result = async { bridge.launch(mockk<IntentSender>()) }
        runCurrent()
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        advanceUntilIdle()

        // Assert
        assertResult(result.await(), expectedCode = Activity.RESULT_CANCELED, expectedData = null)
    }

    @Test
    fun `GIVEN host paused WHEN result delivered before resume THEN delivered result returned`() = runTest {
        // Arrange
        val bridge = createBridge()
        val registry = FakeActivityResultRegistry()
        val lifecycle = createLifecycle(Lifecycle.State.RESUMED)
        bridge.attach(registry, lifecycle)
        val data: Intent = mockk()

        // Act
        val result = async { bridge.launch(mockk<IntentSender>()) }
        runCurrent()
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        registry.dispatchResult(registry.launchedRequestCodes.single(), Activity.RESULT_OK, data)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        advanceUntilIdle()

        // Assert
        assertResult(result.await(), expectedCode = Activity.RESULT_OK, expectedData = data)
    }

    @Test
    fun `GIVEN host started at launch WHEN resumed without prior pause THEN request stays pending until result`() =
        runTest {
            // Arrange
            val bridge = createBridge()
            val registry = FakeActivityResultRegistry()
            val lifecycle = createLifecycle(Lifecycle.State.STARTED)
            bridge.attach(registry, lifecycle)
            val data: Intent = mockk()

            // Act
            val result = async { bridge.launch(mockk<IntentSender>()) }
            runCurrent()
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            advanceUntilIdle()
            val isPendingAfterResume = result.isActive
            registry.dispatchResult(registry.launchedRequestCodes.single(), Activity.RESULT_OK, data)
            advanceUntilIdle()

            // Assert
            assertThat(isPendingAfterResume).isTrue()
            assertResult(result.await(), expectedCode = Activity.RESULT_OK, expectedData = data)
        }

    @Test
    fun `GIVEN host stopped at launch WHEN started and resumed without result THEN completes as canceled`() =
        runTest {
            // Arrange
            val bridge = createBridge()
            val lifecycle = createLifecycle(Lifecycle.State.CREATED)
            bridge.attach(FakeActivityResultRegistry(), lifecycle)

            // Act
            val result = async { bridge.launch(mockk<IntentSender>()) }
            runCurrent()
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            advanceUntilIdle()

            // Assert
            assertResult(result.await(), expectedCode = Activity.RESULT_CANCELED, expectedData = null)
        }

    @Test
    fun `GIVEN first request canceled WHEN its stale result delivered THEN next request is not completed by it`() =
        runTest {
            // Arrange
            val bridge = createBridge()
            val registry = FakeActivityResultRegistry()
            val lifecycle = createLifecycle(Lifecycle.State.RESUMED)
            bridge.attach(registry, lifecycle)
            val first = async { bridge.launch(mockk<IntentSender>()) }
            runCurrent()
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            advanceUntilIdle()
            val firstResult = first.await()
            val second = async { bridge.launch(mockk<IntentSender>()) }
            runCurrent()
            val (firstRequestCode, secondRequestCode) = registry.launchedRequestCodes
            val secondData: Intent = mockk()

            // Act
            registry.dispatchResult(firstRequestCode, Activity.RESULT_OK, mockk<Intent>())
            advanceUntilIdle()
            val isSecondPendingAfterStale = second.isActive
            registry.dispatchResult(secondRequestCode, Activity.RESULT_OK, secondData)
            advanceUntilIdle()

            // Assert
            assertResult(firstResult, expectedCode = Activity.RESULT_CANCELED, expectedData = null)
            assertThat(isSecondPendingAfterStale).isTrue()
            assertResult(second.await(), expectedCode = Activity.RESULT_OK, expectedData = secondData)
        }

    @Test
    fun `GIVEN result delivered inline WHEN next request launched THEN previous key unregistered`() = runTest {
        // Arrange
        val bridge = GoogleAuthActivityResultBridge(
            TestingCoroutineDispatcherProvider(mainImmediate = UnconfinedTestDispatcher(testScheduler)),
        )
        val registry = FakeActivityResultRegistry()
        bridge.attach(registry, createLifecycle(Lifecycle.State.RESUMED))
        val first = async { bridge.launch(mockk<IntentSender>()) }
        runCurrent()
        val firstRequestCode = registry.launchedRequestCodes.single()
        registry.dispatchResult(firstRequestCode, Activity.RESULT_OK, mockk<Intent>())
        first.await()

        // Act
        backgroundScope.async { bridge.launch(mockk<IntentSender>()) }
        runCurrent()

        // Assert
        assertThat(registry.dispatchResult(firstRequestCode, Activity.RESULT_OK, null)).isFalse()
    }

    @Test
    fun `GIVEN request canceled WHEN late result delivered and next request launched THEN key unregistered`() =
        runTest {
            // Arrange
            val bridge = createBridge()
            val registry = FakeActivityResultRegistry()
            val lifecycle = createLifecycle(Lifecycle.State.RESUMED)
            bridge.attach(registry, lifecycle)
            val first = async { bridge.launch(mockk<IntentSender>()) }
            runCurrent()
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            advanceUntilIdle()
            first.await()
            val firstRequestCode = registry.launchedRequestCodes.single()

            // Act
            val isLateResultClaimed = registry.dispatchResult(firstRequestCode, Activity.RESULT_OK, mockk<Intent>())
            backgroundScope.async { bridge.launch(mockk<IntentSender>()) }
            runCurrent()

            // Assert
            assertThat(isLateResultClaimed).isTrue()
            assertThat(registry.dispatchResult(firstRequestCode, Activity.RESULT_OK, null)).isFalse()
        }

    @Test
    fun `GIVEN pending request WHEN attached registry detached THEN completes as canceled`() = runTest {
        // Arrange
        val bridge = createBridge()
        val registry = FakeActivityResultRegistry()
        bridge.attach(registry, createLifecycle(Lifecycle.State.RESUMED))

        // Act
        val result = async { bridge.launch(mockk<IntentSender>()) }
        runCurrent()
        bridge.detach(registry)
        advanceUntilIdle()

        // Assert
        assertResult(result.await(), expectedCode = Activity.RESULT_CANCELED, expectedData = null)
    }

    @Test
    fun `GIVEN pending request WHEN other registry detached THEN request stays pending until result`() = runTest {
        // Arrange
        val bridge = createBridge()
        val registry = FakeActivityResultRegistry()
        bridge.attach(registry, createLifecycle(Lifecycle.State.RESUMED))
        val data: Intent = mockk()

        // Act
        val result = async { bridge.launch(mockk<IntentSender>()) }
        runCurrent()
        bridge.detach(FakeActivityResultRegistry())
        advanceUntilIdle()
        val isPendingAfterDetach = result.isActive
        registry.dispatchResult(registry.launchedRequestCodes.single(), Activity.RESULT_OK, data)
        advanceUntilIdle()

        // Assert
        assertThat(isPendingAfterDetach).isTrue()
        assertResult(result.await(), expectedCode = Activity.RESULT_OK, expectedData = data)
    }

    @Test
    fun `GIVEN no host attached WHEN launch THEN throws launcher unavailable after wait`() = runTest {
        // Arrange
        val bridge = createBridge()

        // Act
        val exception = runCatching { bridge.launch(mockk<IntentSender>()) }.exceptionOrNull()

        // Assert
        assertThat(exception).isInstanceOf(GoogleAuthLauncherUnavailableException::class.java)
        assertThat(currentTime).isEqualTo(HOST_WAIT_TIMEOUT_MS)
    }

    @Test
    fun `GIVEN pending request WHEN new host attached THEN old request completes as canceled`() = runTest {
        // Arrange
        val bridge = createBridge()
        bridge.attach(FakeActivityResultRegistry(), createLifecycle(Lifecycle.State.RESUMED))

        // Act
        val result = async { bridge.launch(mockk<IntentSender>()) }
        runCurrent()
        bridge.attach(FakeActivityResultRegistry(), createLifecycle(Lifecycle.State.RESUMED))
        advanceUntilIdle()

        // Assert
        assertResult(result.await(), expectedCode = Activity.RESULT_CANCELED, expectedData = null)
    }

    private fun TestScope.createBridge() = GoogleAuthActivityResultBridge(createTestingCoroutineDispatcherProvider())

    private fun createLifecycle(state: Lifecycle.State): LifecycleRegistry =
        LifecycleRegistry.createUnsafe(lifecycleOwner).apply { currentState = state }

    private fun assertResult(actual: ActivityResult, expectedCode: Int, expectedData: Intent?) {
        assertThat(actual.resultCode).isEqualTo(expectedCode)
        assertThat(actual.data).isSameInstanceAs(expectedData)
    }

    private fun provideTestModels() = listOf(
        LaunchModel(name = "launch") { it.launch(mockk<IntentSender>()) },
        LaunchModel(name = "launchAccountPicker") { it.launchAccountPicker(mockk<Intent>()) },
    )

    data class LaunchModel(
        val name: String,
        val launch: suspend (GoogleAuthActivityResultBridge) -> ActivityResult,
    ) {
        override fun toString(): String = name
    }

    private class FakeActivityResultRegistry : ActivityResultRegistry() {

        val launchedRequestCodes = mutableListOf<Int>()

        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            launchedRequestCodes += requestCode
        }
    }

    private companion object {
        const val HOST_WAIT_TIMEOUT_MS = 1000L
    }
}