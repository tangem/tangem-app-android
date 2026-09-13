package com.tangem.features.collectibles.impl.stories.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.navigation.Router
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CollectiblesStoriesModelTest {

    private val router: Router = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(router)
    }

    @Test
    fun `GIVEN stories opened WHEN next slide clicked THEN index moves forward`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onNextSlideClick()

        // Assert
        assertThat(model.uiState.value.currentIndex).isEqualTo(1)
        model.onDestroy()
    }

    @Test
    fun `GIVEN first slide WHEN previous slide clicked THEN index stays at zero`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onPreviousSlideClick()

        // Assert
        assertThat(model.uiState.value.currentIndex).isEqualTo(0)
        model.onDestroy()
    }

    @Test
    fun `GIVEN last slide WHEN next slide clicked THEN index holds`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        val lastIndex = model.uiState.value.slides.lastIndex
        repeat(lastIndex) { model.uiState.value.onNextSlideClick() }

        // Act
        model.uiState.value.onNextSlideClick()

        // Assert
        assertThat(model.uiState.value.currentIndex).isEqualTo(lastIndex)
        model.onDestroy()
    }

    @Test
    fun `GIVEN intermediate slide WHEN continue clicked THEN index moves forward and no navigation`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onContinueClick()

        // Assert
        assertThat(model.uiState.value.currentIndex).isEqualTo(1)
        verify(exactly = 0) { router.pop(onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN last slide WHEN continue clicked THEN pops the stack`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        val lastIndex = model.uiState.value.slides.lastIndex
        repeat(lastIndex) { model.uiState.value.onNextSlideClick() }

        // Act
        model.uiState.value.onContinueClick()

        // Assert
        verify(exactly = 1) { router.pop(onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN stories opened WHEN close clicked THEN pops the stack`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onCloseClick()

        // Assert
        verify(exactly = 1) { router.pop(onComplete = any()) }
        model.onDestroy()
    }

    private fun createModel(testScope: TestScope): CollectiblesStoriesModel {
        return CollectiblesStoriesModel(
            router = router,
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