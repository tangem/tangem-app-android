package com.tangem.domain.models.account

import com.google.common.truth.Truth
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectiblesVaultBalanceTest {

    @ParameterizedTest
    @ProvideTestModels
    fun fiatAmount(model: FiatAmountModel) {
        // Act
        val actual = model.balance.fiatAmount

        // Assert
        Truth.assertThat(actual).isEqualTo(model.expected)
    }

    internal data class FiatAmountModel(val balance: CollectiblesVaultBalance, val expected: BigDecimal?)

    private fun provideTestModels() = listOf(
        FiatAmountModel(balance = vaultBalance(amount = "10", rate = "1"), expected = BigDecimal("10")),
        // An empty vault needs no quote at all: requiring one would blank the wallet total over a rate that
        // could not change the answer
        FiatAmountModel(balance = vaultBalance(amount = "0", rate = null), expected = BigDecimal.ZERO),
        // A vault that holds something and cannot be priced is unpriceable
        FiatAmountModel(balance = vaultBalance(amount = "10", rate = null), expected = null),
    )

    private companion object {

    }
}