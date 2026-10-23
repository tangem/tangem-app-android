package com.tangem.features.tangempay.addfunds.va.bank

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.bankCredentials
import com.tangem.features.tangempay.components.TangemPayBankingDetailsErrorComponent
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.Called
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

    private val retry: suspend () -> Either<VisaApiError, BankCredentials> = mockk()
    private val onDismiss: () -> Unit = mockk(relaxed = true)
    private val onContactSupport: () -> Unit = mockk(relaxed = true)
    private val onResolved: (BankCredentials) -> Unit = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(retry, onDismiss, onContactSupport, onResolved)
    }

    @Test
    fun `GIVEN retry succeeds WHEN retry clicked THEN credentials handed to onResolved`() = runTest {
        // Arrange
        val credentials = bankCredentials()
        coEvery { retry() } returns credentials.right()
        val model = createModel()

        // Act
        model.uiState.value.onRetryClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { onResolved(credentials) }
    }

    @Test
    fun `GIVEN retry fails WHEN retry clicked THEN onResolved not called and loading reset`() = runTest {
        // Arrange
        coEvery { retry() } returns VisaApiError.Unspecified.left()
        val model = createModel()

        // Act
        model.uiState.value.onRetryClick()
        advanceUntilIdle()

        // Assert
        verify { onResolved wasNot Called }
        assertThat(model.uiState.value.isRetryLoading).isFalse()
    }

    @Test
    fun `GIVEN retry in flight WHEN retry clicked twice THEN retried once and loading shown`() = runTest {
        // Arrange
        val pending = CompletableDeferred<Either<VisaApiError, BankCredentials>>()
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
        pending.complete(bankCredentials().right())
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
                onResolved = onResolved,
            ),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
    )
}