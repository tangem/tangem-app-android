package com.tangem.features.tangempay.addfunds.va.bank

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.features.tangempay.components.TangemPayBankingDetailsErrorComponent
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayBankingDetailsErrorModelTest {

    private val retry: suspend () -> Either<TangemPayApiError, Unit> = mockk()
    private val onDismiss: () -> Unit = mockk(relaxed = true)
    private val onContactSupport: () -> Unit = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(retry, onDismiss, onContactSupport)
    }

    @Test
    fun `GIVEN retry succeeds WHEN retry clicked THEN retried once and loading kept until the sheet closes`() =
        runTest {
            // Arrange
            coEvery { retry() } returns Unit.right()
            val model = createModel()

            // Act
            model.uiState.value.onRetryClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { retry() }
            assertThat(model.uiState.value.isRetryLoading).isTrue()
        }

    @Test
    fun `GIVEN retry fails WHEN retry clicked THEN loading reset`() = runTest {
        // Arrange
        coEvery { retry() } returns TangemPayApiError.Unspecified.left()
        val model = createModel()

        // Act
        model.uiState.value.onRetryClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isRetryLoading).isFalse()
    }

    @Test
    fun `GIVEN retry in flight WHEN retry clicked twice THEN retried once and loading shown`() = runTest {
        // Arrange
        val pending = CompletableDeferred<Either<TangemPayApiError, Unit>>()
        coEvery { retry() } coAnswers { pending.await() }
        val model = createModel()

        // Act
        model.uiState.value.onRetryClick()
        advanceUntilIdle()
        model.uiState.value.onRetryClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isRetryLoading).isTrue()
        coVerify(exactly = 1) { retry() }
        pending.complete(Unit.right())
        advanceUntilIdle()
    }

    @Test
    fun `GIVEN sheet shown WHEN dismissed or support clicked THEN callbacks invoked`() = runTest {
        // Arrange
        val model = createModel()

        // Act
        model.onDismiss()
        model.uiState.value.onContactSupportClick()

        // Assert
        verify(exactly = 1) { onDismiss() }
        verify(exactly = 1) { onContactSupport() }
    }

    private fun TestScope.createModel() = TangemPayBankingDetailsErrorModel(
        paramsContainer = MutableParamsContainer(
            TangemPayBankingDetailsErrorComponent.Params(
                retry = retry,
                onDismiss = onDismiss,
                onContactSupport = onContactSupport,
            ),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
    )
}