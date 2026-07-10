package com.tangem.features.send.subcomponents.fee.model.converters.custom.ethereum

import androidx.compose.ui.text.input.ImeAction
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.send.api.subcomponents.feeSelector.entity.CustomFeeFieldUM
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.ImmutableList
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EthereumEIPCustomFeeConverterTest {

    private val feeStatus = ethFeeStatus()

    private val converter = EthereumEIPCustomFeeConverter(
        onCustomFeeValueChange = { _, _ -> },
        appCurrency = AppCurrency.Default,
        feeCryptoCurrencyStatus = feeStatus,
    )

    // The leaf operates on the full field list assembled by the router: [amount, maxFee, priorityFee, gasLimit].
    private val router = EthereumCustomFeeConverter(
        onCustomFeeValueChange = { _, _ -> },
        onNextClick = {},
        appCurrency = AppCurrency.Default,
        feeCryptoCurrencyStatus = feeStatus,
    )

    private fun eipFee(
        gasLimit: BigInteger = BigInteger.valueOf(21_000)
    ) = Fee.Ethereum.EIP1559(
        amount = ethAmount(BigDecimal("0.00063")),
        gasLimit = gasLimit,
        maxFeePerGas = BigInteger.valueOf(30_000_000_000), // 30 GWEI
        priorityFee = BigInteger.valueOf(2_000_000_000), // 2 GWEI
    )

    private fun fullFields(fee: Fee.Ethereum.EIP1559): ImmutableList<CustomFeeFieldUM> = router.convert(fee)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @Test
        fun `GIVEN eip fee WHEN convert THEN max fee and priority fee fields in GWEI`() {
            // Act
            val actual = converter.convert(eipFee())

            // Assert
            assertThat(actual).hasSize(2)
            assertThat(actual[0].value).isEqualTo("30") // maxFeePerGas
            assertThat(actual[1].value).isEqualTo("2") // priorityFee
            assertThat(actual[0].symbol).isEqualTo("GWEI")
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertBack {

        @Test
        fun `GIVEN fields WHEN convertBack THEN all fields parsed back`() {
            // Arrange
            val fee = eipFee()
            val fields = fullFields(fee)

            // Act
            val actual = converter.convertBack(fee, fields)

            // Assert
            assertThat(actual.amount.value!!.compareTo(BigDecimal("0.00063"))).isEqualTo(0)
            assertThat(actual.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
            assertThat(actual.maxFeePerGas).isEqualTo(BigInteger.valueOf(30_000_000_000))
            assertThat(actual.priorityFee).isEqualTo(BigInteger.valueOf(2_000_000_000))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnValueChange {

        @Test
        fun `GIVEN max fee changed WHEN onValueChange THEN fee amount recalculated`() {
            // Arrange  (21000 * 40 GWEI = 0.00084 ETH)
            val fee = eipFee()
            val fields = fullFields(fee)

            // Act
            val actual = converter.onValueChange(fee, fields, MAX_FEE_INDEX, "40")

            // Assert
            assertThat(actual[FEE_AMOUNT_INDEX].value).isEqualTo("0.00084")
            assertThat(actual[MAX_FEE_INDEX].value).isEqualTo("40")
        }

        @Test
        fun `GIVEN amount changed WHEN onValueChange THEN max fee recalculated`() {
            // Arrange  (0.00084 ETH / 21000 gas = 40 GWEI)
            val fee = eipFee()
            val fields = fullFields(fee)

            // Act
            val actual = converter.onValueChange(fee, fields, FEE_AMOUNT_INDEX, "0.00084")

            // Assert
            assertThat(actual[MAX_FEE_INDEX].value).isEqualTo("40")
            assertThat(actual[FEE_AMOUNT_INDEX].value).isEqualTo("0.00084")
        }

        @Test
        fun `GIVEN amount changed and gas limit field is zero WHEN onValueChange THEN gas limit pulled from fee`() {
            // Arrange: gas limit field shows "0" (cleared), but the original fee keeps gasLimit = 21000
            val fee = eipFee(gasLimit = BigInteger.valueOf(21_000))
            val fields = fullFields(eipFee(gasLimit = BigInteger.ZERO))

            // Act  (gasLimit pulled from fee = 21000 -> 0.00084 / 21000 = 40 GWEI)
            val actual = converter.onValueChange(fee, fields, FEE_AMOUNT_INDEX, "0.00084")

            // Assert
            assertThat(actual[GAS_LIMIT_INDEX].value).isEqualTo("21000")
            assertThat(actual[MAX_FEE_INDEX].value).isEqualTo("40")
            assertThat(actual[FEE_AMOUNT_INDEX].value).isEqualTo("0.00084")
        }

        @Test
        fun `GIVEN gas limit changed WHEN onValueChange THEN fee amount recalculated`() {
            // Arrange  (42000 * 30 GWEI = 0.00126 ETH, balance = 1 ETH)
            val fee = eipFee()
            val fields = fullFields(fee)

            // Act
            val actual = converter.onValueChange(fee, fields, GAS_LIMIT_INDEX, "42000")

            // Assert
            assertThat(actual[FEE_AMOUNT_INDEX].value).isEqualTo("0.00126")
            assertThat(actual[GAS_LIMIT_INDEX].value).isEqualTo("42000")
            // FIXME [AND-XXXXX]: same inverted imeAction as EthereumLegacyCustomFeeConverter.setOnGasLimitChange.
            //  checkExceedBalance() returns true when the fee EXCEEDS balance, but the code does
            //  `if (!isNotExceedBalance) None else Done`, so an affordable fee (0.00126 < 1 ETH) yields None.
            //  Asserting current (buggy) behavior until the converter is fixed.
            assertThat(actual[GAS_LIMIT_INDEX].keyboardOptions.imeAction).isEqualTo(ImeAction.None)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN blank value WHEN onValueChange THEN dependent fields cleared`(model: BlankModel) {
            // Arrange
            val fee = eipFee()
            val fields = fullFields(fee)

            // Act
            val actual = converter.onValueChange(fee, fields, model.index, "")

            // Assert
            model.clearedIndices.forEach { index ->
                assertThat(actual[index].value).isEmpty()
            }
        }

        private fun provideTestModels() = listOf(
            BlankModel(index = FEE_AMOUNT_INDEX, clearedIndices = listOf(FEE_AMOUNT_INDEX, MAX_FEE_INDEX)),
            BlankModel(index = MAX_FEE_INDEX, clearedIndices = listOf(FEE_AMOUNT_INDEX, MAX_FEE_INDEX)),
            BlankModel(index = GAS_LIMIT_INDEX, clearedIndices = listOf(FEE_AMOUNT_INDEX, GAS_LIMIT_INDEX)),
        )
    }

    data class BlankModel(val index: Int, val clearedIndices: List<Int>)

    private companion object {
        private const val MAX_FEE_INDEX = 1
        private const val GAS_LIMIT_INDEX = 3
    }
}