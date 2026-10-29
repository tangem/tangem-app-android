package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.models.domain.PreparedSwapConfigState
import com.tangem.feature.swap.domain.models.domain.SwapBalanceStatus
import com.tangem.feature.swap.domain.models.ui.SwapFee
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SolanaGaslessSendStatusTest {

    private val swapInteractor: SwapInteractor = mockk()
    private val fee: SwapFee = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(swapInteractor, fee)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN quote status WHEN resolveSendBalanceStatus THEN only a live sponsored result is sent sponsored`(
        model: SendStatusModel,
    ) = runTest {
        // Arrange
        val quoteState = quoteState(model.quoteStatus)
        every { fee.isSponsored } returns true
        coEvery { swapInteractor.applySwapFee(quoteState, fee, BigDecimal.ZERO, model.isSuppressed) } returns
            quoteState(model.appliedStatus)

        // Act
        val actual = swapInteractor.resolveSendBalanceStatus(
            quoteState = quoteState,
            fee = fee.takeIf { model.hasFee },
            lastReducedBalanceBy = BigDecimal.ZERO,
            isSolanaGaslessSuppressed = model.isSuppressed,
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN resolved quote status WHEN resolveSendBalanceStatus THEN fee is not re-applied`() = runTest {
        // Act
        swapInteractor.resolveSendBalanceStatus(
            quoteState = quoteState(SwapBalanceStatus.Sufficient),
            fee = fee,
            lastReducedBalanceBy = BigDecimal.ZERO,
            isSolanaGaslessSuppressed = false,
        )

        // Assert
        coVerify(exactly = 0) { swapInteractor.applySwapFee(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN pending quote status AND fee not sponsored WHEN resolveSendBalanceStatus THEN fee is not re-applied`() =
        runTest {
            // Arrange
            every { fee.isSponsored } returns false

            // Act
            val actual = swapInteractor.resolveSendBalanceStatus(
                quoteState = quoteState(SwapBalanceStatus.Pending),
                fee = fee,
                lastReducedBalanceBy = BigDecimal.ZERO,
                isSolanaGaslessSuppressed = false,
            )

            // Assert
            assertThat(actual).isEqualTo(SwapBalanceStatus.Pending)
            coVerify(exactly = 0) { swapInteractor.applySwapFee(any(), any(), any(), any()) }
        }

    data class SendStatusModel(
        val quoteStatus: SwapBalanceStatus,
        val appliedStatus: SwapBalanceStatus,
        val hasFee: Boolean,
        val expected: SwapBalanceStatus,
        val isSuppressed: Boolean = false,
    )

    private fun provideTestModels() = listOf(
        SendStatusModel(
            quoteStatus = SwapBalanceStatus.Pending,
            appliedStatus = SPONSORED,
            hasFee = true,
            expected = SPONSORED,
        ),
        SendStatusModel(
            quoteStatus = SwapBalanceStatus.Pending,
            appliedStatus = SwapBalanceStatus.Sufficient,
            hasFee = true,
            expected = SwapBalanceStatus.Pending,
        ),
        SendStatusModel(
            quoteStatus = SwapBalanceStatus.Pending,
            appliedStatus = SwapBalanceStatus.InsufficientFee(feeCurrencyName = "Solana", feeCurrencySymbol = "SOL"),
            hasFee = true,
            expected = SwapBalanceStatus.Pending,
        ),
        SendStatusModel(
            quoteStatus = SwapBalanceStatus.Pending,
            appliedStatus = SPONSORED,
            hasFee = false,
            expected = SwapBalanceStatus.Pending,
        ),
        SendStatusModel(
            quoteStatus = SPONSORED,
            appliedStatus = SPONSORED,
            hasFee = true,
            expected = SPONSORED,
        ),
        SendStatusModel(
            quoteStatus = SPONSORED,
            appliedStatus = SPONSORED,
            hasFee = true,
            expected = SwapBalanceStatus.Pending,
            isSuppressed = true,
        ),
        SendStatusModel(
            quoteStatus = SwapBalanceStatus.InsufficientAmount,
            appliedStatus = SPONSORED,
            hasFee = true,
            expected = SwapBalanceStatus.InsufficientAmount,
        ),
    )

    private fun quoteState(status: SwapBalanceStatus): SwapState.QuotesLoadedState = mockk {
        every { preparedSwapConfigState } returns PreparedSwapConfigState(
            balanceStatus = status,
            hasOutgoingTransaction = false,
        )
    }

    private companion object {
        val SPONSORED = SwapBalanceStatus.SponsoredFee(feePayer = "HQf2XoPd")
    }
}