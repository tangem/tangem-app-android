package com.tangem.feature.swap.model

import androidx.compose.runtime.MutableState
import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.models.currency.CryptoCurrency
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class SwapModelTronReapproveTest : SwapModelTestBase() {

    @BeforeEach
    fun setUp() {
        clearMocks(swapInteractor)
        setUpBase()
    }

    @Test
    fun `GIVEN from token selected WHEN tron reapprove clicked THEN reapproval required and amount reduced`() =
        runTest {
            // Arrange
            coEvery { getMinimumTransactionAmountSyncUseCase(any(), any()) } returns Either.Right(null)
            val token: CryptoCurrency.Token = mockk(relaxed = true) { every { decimals } returns 6 }
            val fromStatus = swapCurrencyStatus(currency = token)
            val model = createModel().apply {
                dataState = dataState.copy(fromSwapCurrencyStatus = fromStatus)
            }
            val amount = BigDecimal("35.7")

            // Act
            model.onTronReapproveClick(spenderAddress = SPENDER, amount = amount)

            // Assert
            verify(exactly = 1) {
                swapInteractor.requireTronReapproval(
                    fromSwapCurrencyStatus = fromStatus,
                    spenderAddress = SPENDER,
                    maxAllowance = amount,
                )
            }
            assertThat(model.lastAmount()).isEqualTo(amount.parseBigDecimal(6))
        }

    @Test
    fun `GIVEN no from token WHEN tron reapprove clicked THEN nothing happens`() = runTest {
        // Arrange
        val model = createModel()

        // Act
        model.onTronReapproveClick(spenderAddress = SPENDER, amount = BigDecimal("35.7"))

        // Assert
        verify(exactly = 0) { swapInteractor.requireTronReapproval(any(), any(), any()) }
    }

    @Test
    fun `GIVEN swap screen WHEN model destroyed THEN tron reapprovals cleared`() = runTest {
        // Arrange
        val model = createModel()

        // Act
        model.onDestroy()

        // Assert
        verify(exactly = 1) { swapInteractor.clearTronReapprovals() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun SwapModel.lastAmount(): String {
        val field = SwapModel::class.java.getDeclaredField("lastAmount").apply { isAccessible = true }
        return (field.get(this) as MutableState<String>).value
    }

    private companion object {
        const val SPENDER = "TSpender"
    }
}