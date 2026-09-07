package com.tangem.features.collectibles.impl.main.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

internal class CollectiblesMainModelTest {

    private val router: Router = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(router)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Chrome {

        @Test
        fun `GIVEN model created WHEN state read THEN balance comes from the stub data`() = runTest {
            // Act
            val model = createModel(testScope = this)

            // Assert
            assertThat(model.uiState.value.balance.crypto)
                .isEqualTo(stringReference(CollectiblesMainStubData.BALANCE.crypto))
            model.onDestroy()
        }

        @Test
        fun `GIVEN model created WHEN back clicked THEN the feature is popped`() = runTest {
            // Arrange
            val model = createModel(testScope = this)

            // Act
            model.uiState.value.onBackClick()

            // Assert
            verify(exactly = 1) { router.pop(onComplete = any()) }
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Menu {

        @Test
        fun `GIVEN model created WHEN menu clicked THEN menu is shown`() = runTest {
            // Arrange
            val model = createModel(testScope = this)

            // Act
            model.uiState.value.menu.onMenuClick()

            // Assert
            assertThat(model.uiState.value.menu.isShown).isTrue()
            model.onDestroy()
        }

        @Test
        fun `GIVEN menu shown WHEN dismissed THEN menu is hidden`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            model.uiState.value.menu.onMenuClick()

            // Act
            model.uiState.value.menu.onDismiss()

            // Assert
            assertThat(model.uiState.value.menu.isShown).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN menu shown WHEN how it works clicked THEN stories open`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            model.uiState.value.menu.onMenuClick()

            // Act
            model.uiState.value.menu.items.single { it.id == "how_it_works" }.onClick()

            // Assert
            verify(exactly = 1) { router.push(CollectiblesRoute.Stories, onComplete = any()) }
            assertThat(model.uiState.value.menu.isShown).isFalse()
            model.onDestroy()
        }
    }

    private fun createModel(testScope: TestScope): CollectiblesMainModel {
        return CollectiblesMainModel(
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