package com.tangem.domain.transaction.models.tron

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TronFeeExtensionsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun isCoveredByOwnTronEnergy(model: CoverageModel) {
        // Act
        val actual = TransactionFee.Single(normal = model.fee).isCoveredByOwnTronEnergy()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class CoverageModel(val fee: Fee, val expected: Boolean)

    private fun provideTestModels() = listOf(
        CoverageModel(fee = tronFee(remainingEnergy = 70_000, feeEnergy = 65_000), expected = true),
        CoverageModel(fee = tronFee(remainingEnergy = 65_000, feeEnergy = 65_000), expected = true),
        CoverageModel(fee = tronFee(remainingEnergy = 10_000, feeEnergy = 65_000), expected = false),
        CoverageModel(fee = tronFee(remainingEnergy = 0, feeEnergy = 65_000), expected = false),
        CoverageModel(fee = tronFee(remainingEnergy = 0, feeEnergy = 0), expected = false),
        CoverageModel(fee = tronFee(remainingEnergy = 70_000, feeEnergy = 0), expected = false),
        CoverageModel(fee = Fee.Common(Amount(BigDecimal.ZERO, Blockchain.Tron)), expected = false),
    )

    private fun tronFee(remainingEnergy: Long, feeEnergy: Long) = Fee.Tron(
        amount = Amount(BigDecimal("0.35"), Blockchain.Tron),
        remainingEnergy = remainingEnergy,
        feeEnergy = feeEnergy,
    )
}