package com.tangem.domain.card

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.test.core.assertEitherLeft
import com.tangem.test.core.assertEitherRight
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeleteSavedAccessCodesUseCaseTest {

    private val tangemSdkManager = mockk<TangemSdkManager>()

    private lateinit var useCase: DeleteSavedAccessCodesUseCase

    @BeforeEach
    fun setup() {
        clearMocks(tangemSdkManager)
        useCase = DeleteSavedAccessCodesUseCase(tangemSdkManager = tangemSdkManager)
    }

    @Test
    fun `returns Right Unit when sdk deletes codes successfully`() = runTest {
        // Arrange
        val cardId = "AA00000000000001"
        coEvery { tangemSdkManager.deleteSavedUserCodes(setOf(cardId)) } returns CompletionResult.Success(Unit)

        // Act
        val actual = useCase(cardId = cardId)

        // Assert
        assertEitherRight(actual)
    }

    @Test
    fun `returns Left with sdk error when sdk fails`() = runTest {
        // Arrange
        val cardId = "AA00000000000002"
        val sdkError = TangemSdkError.ExceptionError(RuntimeException("boom"))
        coEvery { tangemSdkManager.deleteSavedUserCodes(setOf(cardId)) } returns CompletionResult.Failure(sdkError)

        // Act
        val actual = useCase(cardId = cardId)

        // Assert
        assertEitherLeft(actual, sdkError)
    }

    @Test
    fun `passes exactly the given cardId as a singleton set to sdk`() = runTest {
        // Arrange
        val cardId = "AA00000000000003"
        coEvery { tangemSdkManager.deleteSavedUserCodes(any()) } returns CompletionResult.Success(Unit)

        // Act
        useCase(cardId = cardId)

        // Assert
        coVerify(exactly = 1) { tangemSdkManager.deleteSavedUserCodes(setOf(cardId)) }
    }
}