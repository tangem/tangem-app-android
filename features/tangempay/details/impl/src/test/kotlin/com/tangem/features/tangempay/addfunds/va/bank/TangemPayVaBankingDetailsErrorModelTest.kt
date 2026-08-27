package com.tangem.features.tangempay.addfunds.va.bank

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.usecase.GetBankCredentialsUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TangemPayVaBankingDetailsErrorModelTest {

    private val userWalletId = UserWalletId("1234567890ABCDEF")
    private val productInstanceId = "pi_1"

    private val getBankCredentialsUseCase: GetBankCredentialsUseCase = mockk()
    private val onDismiss: () -> Unit = mockk(relaxed = true)
    private val onContactSupport: () -> Unit = mockk(relaxed = true)
    private val onResolved: (BankCredentials) -> Unit = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(getBankCredentialsUseCase, onDismiss, onContactSupport, onResolved)
    }

    @Test
    fun `GIVEN refetch succeeds WHEN retry THEN onResolved called with credentials`() = runTest {
        // Arrange
        val credentials = bankCredentials()
        coEvery {
            getBankCredentialsUseCase(userWalletId, productInstanceId)
        } returns credentials.right()
        val model = createModel()

        // Act
        model.uiState.value.onRetryClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { onResolved(credentials) }
    }

    @Test
    fun `GIVEN refetch still fails WHEN retry THEN onResolved not called and loading reset`() = runTest {
        // Arrange
        coEvery {
            getBankCredentialsUseCase(userWalletId, productInstanceId)
        } returns VisaApiError.Unspecified.left()
        val model = createModel()

        // Act
        model.uiState.value.onRetryClick()
        advanceUntilIdle()

        // Assert
        verify { onResolved wasNot Called }
        assertThat(model.uiState.value.isRetryLoading).isFalse()
    }

    @Test
    fun `GIVEN refetch in progress WHEN retry twice THEN fetch invoked once and loading shown`() = runTest {
        // Arrange
        val pending = CompletableDeferred<Either<VisaApiError, BankCredentials>>()
        coEvery {
            getBankCredentialsUseCase(userWalletId, productInstanceId)
        } coAnswers { pending.await() }
        val model = createModel()

        // Act
        model.uiState.value.onRetryClick() // starts loading, fetch suspends
        advanceUntilIdle()
        model.uiState.value.onRetryClick() // gated by isRetryLoading — must be ignored
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isRetryLoading).isTrue()
        coVerify(exactly = 1) { getBankCredentialsUseCase(userWalletId, productInstanceId) }

        pending.complete(bankCredentials().right()) // let the in-flight call finish cleanly
        advanceUntilIdle()
    }

    private fun bankCredentials() = BankCredentials(
        type = "ACH",
        beneficiaryName = "Test Beneficiary",
        beneficiaryAddress = "Addr",
        beneficiaryBankName = "Bank",
        beneficiaryBankAddress = "Bank Addr",
        accountNumber = "123",
        routingNumber = "456",
    )

    private fun TestScope.createModel() = TangemPayVaBankingDetailsErrorModel(
        paramsContainer = MutableParamsContainer(
            TangemPayVaBankingDetailsErrorComponent.Params(
                userWalletId = userWalletId,
                productInstanceId = productInstanceId,
                onDismiss = onDismiss,
                onContactSupport = onContactSupport,
                onResolved = onResolved,
            ),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        getBankCredentialsUseCase = getBankCredentialsUseCase,
    )

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