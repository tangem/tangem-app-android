package com.tangem.domain.swap.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.swap.models.PredefinedPercentAmount
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class CalculateAmountUseCaseTest {

    private val useCase = CalculateAmountUseCase()

    @Test
    fun `GIVEN balance and PERCENT_25 WHEN invoke THEN return one quarter of balance`() {
        val balance = BigDecimal("100")
        val decimals = 2

        val result = useCase(
            balance = balance,
            decimals = decimals,
            percent = PredefinedPercentAmount.PERCENT_25,
        )

        assertThat(result).isEqualTo(BigDecimal("25.00"))
    }

    @Test
    fun `GIVEN balance and PERCENT_50 WHEN invoke THEN return half of balance`() {
        val balance = BigDecimal("100")
        val decimals = 2

        val result = useCase(
            balance = balance,
            decimals = decimals,
            percent = PredefinedPercentAmount.PERCENT_50,
        )

        assertThat(result).isEqualTo(BigDecimal("50.00"))
    }

    @Test
    fun `GIVEN balance and PERCENT_75 WHEN invoke THEN return three quarters of balance`() {
        val balance = BigDecimal("100")
        val decimals = 2

        val result = useCase(
            balance = balance,
            decimals = decimals,
            percent = PredefinedPercentAmount.PERCENT_75,
        )

        assertThat(result).isEqualTo(BigDecimal("75.00"))
    }

    @Test
    fun `GIVEN balance and MAX WHEN invoke THEN return full balance`() {
        val balance = BigDecimal("100")
        val decimals = 2

        val result = useCase(
            balance = balance,
            decimals = decimals,
            percent = PredefinedPercentAmount.MAX,
        )

        assertThat(result).isEqualTo(BigDecimal("100.00"))
    }

    @Test
    fun `GIVEN zero balance WHEN invoke THEN return zero with decimals scale`() {
        val balance = BigDecimal.ZERO
        val decimals = 6

        val result = useCase(
            balance = balance,
            decimals = decimals,
            percent = PredefinedPercentAmount.PERCENT_50,
        )

        assertThat(result).isEqualTo(BigDecimal("0.000000"))
    }

    @Test
    fun `GIVEN balance with more precision than decimals WHEN invoke THEN truncate result with rounding down`() {
        val balance = BigDecimal("1.999999999999999999")
        val decimals = 6

        val result = useCase(
            balance = balance,
            decimals = decimals,
            percent = PredefinedPercentAmount.PERCENT_25,
        )

        assertThat(result).isEqualTo(BigDecimal("0.499999"))
    }

    @Test
    fun `GIVEN fractional percent product WHEN invoke THEN round down to decimals scale`() {
        val balance = BigDecimal("1")
        val decimals = 1

        val result = useCase(
            balance = balance,
            decimals = decimals,
            percent = PredefinedPercentAmount.PERCENT_75,
        )

        assertThat(result).isEqualTo(BigDecimal("0.7"))
    }

    @Test
    fun `GIVEN zero decimals WHEN invoke THEN return integer value rounded down`() {
        val balance = BigDecimal("9")
        val decimals = 0

        val result = useCase(
            balance = balance,
            decimals = decimals,
            percent = PredefinedPercentAmount.PERCENT_75,
        )

        assertThat(result).isEqualTo(BigDecimal("6"))
    }

    @Test
    fun `GIVEN high-precision balance and MAX WHEN invoke THEN preserve balance truncated to decimals`() {
        val balance = BigDecimal("12.3456789012345678")
        val decimals = 8

        val result = useCase(
            balance = balance,
            decimals = decimals,
            percent = PredefinedPercentAmount.MAX,
        )

        assertThat(result).isEqualTo(BigDecimal("12.34567890"))
    }

    @Test
    fun `GIVEN large balance and PERCENT_50 WHEN invoke THEN return correctly scaled half`() {
        val balance = BigDecimal("123456789.987654321")
        val decimals = 4

        val result = useCase(
            balance = balance,
            decimals = decimals,
            percent = PredefinedPercentAmount.PERCENT_50,
        )

        assertThat(result).isEqualTo(BigDecimal("61728394.9938"))
    }
}