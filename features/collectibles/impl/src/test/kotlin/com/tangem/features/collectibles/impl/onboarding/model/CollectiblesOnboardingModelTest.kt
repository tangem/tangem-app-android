package com.tangem.features.collectibles.impl.onboarding.model

import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CollectiblesOnboardingModelTest {

    private val router: Router = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, urlOpener)
    }

    @Test
    fun `GIVEN model created WHEN create account clicked THEN stories open on top of main`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onCreateAccountClick()

        // Assert
        verify(exactly = 1) {
            router.replaceAll(CollectiblesRoute.Main, CollectiblesRoute.Stories, onComplete = any())
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN model created WHEN close clicked THEN pops the stack`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onCloseClick()

        // Assert
        verify(exactly = 1) { router.pop(onComplete = any()) }
        model.onDestroy()
    }

    private fun createModel(testScope: TestScope): CollectiblesOnboardingModel {
        return CollectiblesOnboardingModel(
            router = router,
            urlOpener = urlOpener,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        )
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }
}