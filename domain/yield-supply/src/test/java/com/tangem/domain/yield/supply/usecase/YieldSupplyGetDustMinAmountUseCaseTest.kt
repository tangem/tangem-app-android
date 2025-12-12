package com.tangem.domain.yield.supply.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.appcurrency.model.AppCurrency
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class YieldSupplyGetDustMinAmountUseCaseTest {

    private val useCase = YieldSupplyGetDustMinAmountUseCase()

    @Test
    fun `GIVEN supported currency WHEN invoke THEN return dust min amount`() {
        val minAmount = BigDecimal("123.456")
        val appCurrency = AppCurrency(code = "EUR", name = "Euro", symbol = "€")

        val result = useCase(minAmount, appCurrency)

        assertThat(result).isEqualTo(BigDecimal("0.1"))
    }

    @Test
    fun `GIVEN unsupported currency WHEN invoke THEN return min amount stripped`() {
        val minAmount = BigDecimal("1.2300")
        val appCurrency = AppCurrency(code = "JPY", name = "Japanese Yen", symbol = "¥")

        val result = useCase(minAmount, appCurrency)

        assertThat(result).isEqualTo(BigDecimal("1.23"))
    }
}