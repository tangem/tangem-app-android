package com.tangem.domain.collectibles.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.CollectiblesModelFactory
import com.tangem.domain.collectibles.models.CollectibleTransaction
import com.tangem.domain.collectibles.models.CollectibleTurboMode
import com.tangem.domain.collectibles.models.CollectiblesError
import com.tangem.domain.collectibles.models.GenerateTxParams
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GenerateCollectibleTxUseCaseTest {

    private val repository: CollectiblesRepository = mockk()

    private val useCase = GenerateCollectibleTxUseCase(repository)

    private val userWalletId = CollectiblesModelFactory.userWalletId

    private val params = GenerateTxParams(
        fromAddress = "from",
        collectibleType = "pokemon_50",
        toAddress = "to",
        collectibleTurboMode = CollectibleTurboMode(isEnabled = true, buybackAddress = "0xbuyback"),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @Test
    fun `GIVEN repository returns a transaction WHEN invoke THEN returns it on the right`() = runTest {
        // Arrange
        val transaction = CollectibleTransaction(id = "tx-id", txData = "base64")
        coEvery { repository.generateTx(userWalletId, params) } returns transaction

        // Act
        val actual = useCase(userWalletId = userWalletId, params = params)

        // Assert
        assertThat(actual).isEqualTo(transaction.right())
        coVerify(exactly = 1) { repository.generateTx(userWalletId, params) }
    }

    @Test
    fun `GIVEN repository throws WHEN invoke THEN returns an Api error carrying the cause`() = runTest {
        // Arrange
        val cause = IllegalStateException("cannot build the tx")
        coEvery { repository.generateTx(any(), any()) } throws cause

        // Act
        val actual = useCase(userWalletId = userWalletId, params = params)

        // Assert
        assertThat(actual).isEqualTo(CollectiblesError.Api(cause).left())
    }
}