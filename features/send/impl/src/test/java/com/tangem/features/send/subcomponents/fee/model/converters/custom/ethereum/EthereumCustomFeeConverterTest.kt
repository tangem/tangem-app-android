package com.tangem.features.send.subcomponents.fee.model.converters.custom.ethereum

import androidx.compose.ui.text.input.ImeAction
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EthereumCustomFeeConverterTest {

    private val feeStatus = ethFeeStatus()

    private val converter = EthereumCustomFeeConverter(
        onCustomFeeValueChange = { _, _ -> },
        onNextClick = {},
        appCurrency = AppCurrency.Default,
        feeCryptoCurrencyStatus = feeStatus,
    )

    private fun legacyFee(amount: BigDecimal? = BigDecimal("0.01")) = Fee.Ethereum.Legacy(
        amount = ethAmount(amount),
        gasLimit = GAS_LIMIT,
        gasPrice = BigInteger.valueOf(1_000_000_000),
    )

    private fun eipFee(amount: BigDecimal? = BigDecimal("0.01")) = Fee.Ethereum.EIP1559(
        amount = ethAmount(amount),
        gasLimit = GAS_LIMIT,
        maxFeePerGas = BigInteger.valueOf(2_000_000_000),
        priorityFee = BigInteger.valueOf(1_000_000_000),
    )

    private fun tokenFee() = Fee.Ethereum.TokenCurrency(
        amount = ethAmount(BigDecimal("0.01")),
        gasLimit = GAS_LIMIT,
        coinPriceInToken = BigInteger.ONE,
        feeTransferGasLimit = BigInteger.ONE,
        baseGas = BigInteger.ONE,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @Test
        fun `GIVEN token currency fee WHEN convert THEN returns empty list`() {
            // Act
            val actual = converter.convert(tokenFee())

            // Assert
            assertThat(actual).isEmpty()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN ethereum fee WHEN convert THEN amount is first and gasLimit at reported index`(
            model: AssemblyModel,
        ) {
            // Act
            val actual = converter.convert(model.fee)

            // Assert
            assertThat(actual).hasSize(model.expectedFieldCount)
            assertThat(actual.first().value).isEqualTo("0.01")
            assertThat(actual[converter.getGasLimitIndex(model.fee)].value).isEqualTo(GAS_LIMIT.toString())
        }

        private fun provideTestModels() = listOf(
            AssemblyModel(fee = legacyFee(), expectedFieldCount = LEGACY_FIELD_COUNT), // [amount, gasPrice, gasLimit]
            AssemblyModel(fee = eipFee(), expectedFieldCount = EIP_FIELD_COUNT), // [amount, maxFee, priorityFee, gasLimit]
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GasLimitImeAction {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN fee compared to balance WHEN convert THEN gasLimit imeAction reflects affordability`(
            model: ImeActionModel,
        ) {
            // Act  (balance = 1 ETH)
            val actual = converter.convert(legacyFee(amount = model.feeAmount))

            // Assert
            assertThat(actual.last().keyboardOptions.imeAction).isEqualTo(model.expectedImeAction)
        }

        private fun provideTestModels() = listOf(
            ImeActionModel(feeAmount = BigDecimal("0.01"), expectedImeAction = ImeAction.Done), // within balance
            ImeActionModel(feeAmount = BigDecimal("2"), expectedImeAction = ImeAction.None), // exceeds balance
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertBack {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN ethereum fee WHEN convertBack THEN delegates to matching converter`(
            model: ConvertBackModel,
        ) {
            // Arrange
            val fields = converter.convert(model.fee)

            // Act
            val actual = converter.convertBack(model.fee, fields)

            // Assert
            assertThat(actual).isInstanceOf(model.expectedClazz)
        }

        private fun provideTestModels() = listOf(
            ConvertBackModel(fee = legacyFee(), expectedClazz = Fee.Ethereum.Legacy::class.java),
            ConvertBackModel(fee = eipFee(), expectedClazz = Fee.Ethereum.EIP1559::class.java),
        )
    }

    data class AssemblyModel(val fee: Fee.Ethereum, val expectedFieldCount: Int)
    data class ImeActionModel(val feeAmount: BigDecimal, val expectedImeAction: ImeAction)
    data class ConvertBackModel(val fee: Fee.Ethereum, val expectedClazz: Class<*>)

    private companion object {
        private val GAS_LIMIT: BigInteger = BigInteger.valueOf(21_000)

        // Router assembles [amount, ...type-specific, gasLimit]; Legacy adds 1 field, EIP adds 2.
        private const val LEGACY_FIELD_COUNT = 3
        private const val EIP_FIELD_COUNT = 4
    }
}