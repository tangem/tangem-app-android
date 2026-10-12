package com.tangem.domain.collectibles.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.CollectiblesModelFactory
import com.tangem.domain.collectibles.models.CollectiblesError
import com.tangem.domain.collectibles.models.SubmittedCollectibleTransaction
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SubmitCollectibleTxUseCaseTest {

    private val repository: CollectiblesRepository = mockk()

    private val useCase = SubmitCollectibleTxUseCase(repository)

    private val userWalletId = CollectiblesModelFactory.userWalletId

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @Test
    fun `GIVEN repository accepts the tx WHEN invoke THEN returns the submitted tx on the right`() = runTest {
        // Arrange
        val submitted = SubmittedCollectibleTransaction(id = "tx-id")
        coEvery { repository.submitTx(userWalletId, "signed") } returns submitted

        // Act
        val actual = useCase(userWalletId = userWalletId, signedTxData = "signed")

        // Assert
        assertThat(actual).isEqualTo(submitted.right())
        coVerify(exactly = 1) { repository.submitTx(userWalletId, "signed") }
    }

    @Test
    fun `GIVEN repository throws WHEN invoke THEN returns an Api error carrying the cause`() = runTest {
        // Arrange
        val cause = IllegalStateException("tx rejected")
        coEvery { repository.submitTx(any(), any()) } throws cause

        // Act
        val actual = useCase(userWalletId = userWalletId, signedTxData = "signed")

        // Assert
        assertThat(actual).isEqualTo(CollectiblesError.Api(cause).left())
    }
}