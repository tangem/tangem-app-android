package com.tangem.domain.marketing

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ObserveDemandSurveyCompletedUseCaseTest {

    private val repository: DemandSurveyRepository = mockk()
    private val useCase = ObserveDemandSurveyCompletedUseCase(repository)

    private val userWalletId = UserWalletId("0000000000000000000000000000000000000000000000000000000000000001")

    @BeforeEach
    fun reset() = clearMocks(repository)

    @Test
    fun `GIVEN repository flow WHEN invoke THEN emits repository values for the wallet`() = runTest {
        // Arrange
        every { repository.observeCompleted(userWalletId) } returns flowOf(false, true)

        // Act
        val actual = useCase(userWalletId).toList()

        // Assert
        assertThat(actual).containsExactly(false, true).inOrder()
    }
}