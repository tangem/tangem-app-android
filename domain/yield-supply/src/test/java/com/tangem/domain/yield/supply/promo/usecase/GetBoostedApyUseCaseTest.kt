package com.tangem.domain.yield.supply.promo.usecase

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class GetBoostedApyUseCaseTest {

    private val useCase = GetBoostedApyUseCase()

    @Test
    fun `GIVEN base apy 5_1 WHEN invoke THEN returns 15_3`() {
        val result = useCase(BigDecimal("5.1"))

        assertThat(result).isEqualTo(BigDecimal("15.3"))
    }

    @Test
    fun `GIVEN base apy 0 WHEN invoke THEN returns 0`() {
        val result = useCase(BigDecimal.ZERO)

        assertThat(result).isEqualTo(BigDecimal.ZERO.multiply(BigDecimal(3)))
    }

    @Test
    fun `GIVEN base apy 4_99 WHEN invoke THEN returns 14_97`() {
        val result = useCase(BigDecimal("4.99"))

        assertThat(result).isEqualTo(BigDecimal("14.97"))
    }

    @Test
    fun `GIVEN base apy 100 WHEN invoke THEN returns 300`() {
        val result = useCase(BigDecimal("100"))

        assertThat(result).isEqualTo(BigDecimal("300"))
    }
}