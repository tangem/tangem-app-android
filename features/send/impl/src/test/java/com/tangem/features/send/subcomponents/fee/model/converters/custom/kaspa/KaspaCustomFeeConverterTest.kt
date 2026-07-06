package com.tangem.features.send.subcomponents.fee.model.converters.custom.kaspa

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.send.loadedStatus
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KaspaCustomFeeConverterTest {

    private val currencyFactory = MockCryptoCurrencyFactory()

    private val feeStatus = loadedStatus(
        currency = currencyFactory.createCoin(Blockchain.Kaspa),
        fiatRate = BigDecimal("0.1"),
    )

    private val converter = KaspaCustomFeeConverter(
        onCustomFeeValueChange = { _, _ -> },
        appCurrency = AppCurrency.Default,
        feeCryptoCurrencyStatus = feeStatus,
    )

    private fun kaspaAmount(value: BigDecimal?) = Amount(currencySymbol = "KAS", value = value, decimals = KAS_DECIMALS)

    private fun kaspaFee(
        amount: BigDecimal? = BigDecimal("0.0001"),
        mass: BigInteger = BigInteger.valueOf(2000),
        feeRate: BigInteger = BigInteger.valueOf(5),
        revealTransactionFee: Amount? = null,
    ) = Fee.Kaspa(amount = kaspaAmount(amount), mass = mass, feeRate = feeRate, revealTransactionFee = revealTransactionFee)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN kaspa fee WHEN convert THEN single amount field`(model: ConvertModel) {
            // Act
            val actual = converter.convert(kaspaFee(amount = model.amount))

            // Assert
            assertThat(actual).hasSize(1)
            assertThat(actual[FEE_AMOUNT_INDEX].symbol).isEqualTo("KAS")
            assertThat(actual[FEE_AMOUNT_INDEX].value).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ConvertModel(amount = BigDecimal("0.0001"), expected = "0.0001"),
            ConvertModel(amount = null, expected = ""),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertBack {

        @Test
        fun `GIVEN fields WHEN convertBack THEN amount kept mass kept and feeRate recomputed`() {
            // Arrange  (feeRate seed 999 must be overwritten: 0.0001 / 2000 = 5e-8 -> *1e8 = 5)
            val normalFee = kaspaFee(amount = BigDecimal("0.0001"), mass = BigInteger.valueOf(2000), feeRate = BigInteger.valueOf(999))
            val fields = converter.convert(normalFee)

            // Act
            val actual = converter.convertBack(normalFee, fields)

            // Assert
            assertThat(actual.amount.value!!.compareTo(BigDecimal("0.0001"))).isEqualTo(0)
            assertThat(actual.mass).isEqualTo(BigInteger.valueOf(2000))
            assertThat(actual.feeRate).isEqualTo(BigInteger.valueOf(5))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnValueChange {

        @Test
        fun `GIVEN amount changed WHEN onValueChange THEN field value updated`() {
            // Arrange
            val fields = converter.convert(kaspaFee(amount = BigDecimal("0.0001")))

            // Act
            val actual = converter.onValueChange(fields, FEE_AMOUNT_INDEX, "0.0002")

            // Assert
            assertThat(actual[FEE_AMOUNT_INDEX].value).isEqualTo("0.0002")
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TryAutoFixValue {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN minimum fee WHEN tryAutoFixValue THEN value clamped only for krc-20 below minimum`(
            model: AutoFixModel,
        ) {
            // Arrange
            val fields = converter.convert(kaspaFee(amount = model.currentValue))

            // Act
            val actual = converter.tryAutoFixValue(model.minimumFee, fields)

            // Assert
            assertThat(actual[FEE_AMOUNT_INDEX].value).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            // not a krc-20 transfer (revealTransactionFee == null) -> never clamps, even below minimum
            AutoFixModel(
                currentValue = BigDecimal("0.0001"),
                minimumFee = kaspaFee(amount = BigDecimal("0.0005"), revealTransactionFee = null),
                expected = "0.0001",
            ),
            // krc-20 transfer, value below minimum -> clamped up to minimum
            AutoFixModel(
                currentValue = BigDecimal("0.0001"),
                minimumFee = kaspaFee(amount = BigDecimal("0.0005"), revealTransactionFee = kaspaAmount(BigDecimal("0.0001"))),
                expected = "0.0005",
            ),
            // krc-20 transfer, value at/above minimum -> unchanged
            AutoFixModel(
                currentValue = BigDecimal("0.001"),
                minimumFee = kaspaFee(amount = BigDecimal("0.0005"), revealTransactionFee = kaspaAmount(BigDecimal("0.0001"))),
                expected = "0.001",
            ),
        )
    }

    data class ConvertModel(val amount: BigDecimal?, val expected: String)
    data class AutoFixModel(val currentValue: BigDecimal, val minimumFee: Fee.Kaspa, val expected: String)

    private companion object {
        private const val KAS_DECIMALS = 8
        private const val FEE_AMOUNT_INDEX = 0
    }
}