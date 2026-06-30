package com.tangem.features.send.subcomponents.fee.model.converters.custom.bitcoin

import androidx.compose.ui.text.input.ImeAction
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.loadedStatus
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BitcoinCustomFeeConverterTest {

    private val currencyFactory = MockCryptoCurrencyFactory()

    private val feeStatus = loadedStatus(
        currency = currencyFactory.createCoin(Blockchain.Bitcoin),
        fiatRate = BigDecimal("50000"),
    )

    private val converter = bitcoinConverter(feeStatus)

    private fun bitcoinConverter(status: CryptoCurrencyStatus) = BitcoinCustomFeeConverter(
        onCustomFeeValueChange = { _, _ -> },
        onNextClick = {},
        appCurrency = AppCurrency.Default,
        feeCryptoCurrencyStatus = status,
    )

    private fun amount(amount: BigDecimal?) = Amount(
        currencySymbol = "BTC",
        value = amount,
        decimals = BTC_DECIMALS,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN bitcoin fee WHEN convert THEN amount readonly and satoshiPerByte computed`(
            model: ConvertModel,
        ) {
            // Act
            val actual = converter.convert(model.fee)

            // Assert
            assertThat(actual).hasSize(2)
            assertThat(actual[FEE_AMOUNT_INDEX].isReadonly).isTrue()
            assertThat(actual[FEE_AMOUNT_INDEX].value).isEqualTo(model.expectedAmount)
            assertThat(actual[FEE_SATOSHI_INDEX].value).isEqualTo(model.expectedSatoshi)
        }

        private fun provideTestModels() = listOf(
            ConvertModel(
                fee = Fee.Bitcoin(
                    amount(BigDecimal("0.000025")),
                    BigDecimal("10"),
                    BigDecimal("250")
                ),
                expectedAmount = "0.000025",
                expectedSatoshi = "10",
            ), // exact: 2500 sat / 250 byte
            ConvertModel(
                fee = Fee.Bitcoin(
                    amount(BigDecimal("0.00002875")),
                    BigDecimal("10"),
                    BigDecimal("250")
                ),
                expectedAmount = "0.00002875",
                expectedSatoshi = "12",
            ), // 2875 sat / 250 byte = 11.5 -> HALF_UP -> 12
            ConvertModel(
                fee = Fee.Bitcoin(
                    amount(null),
                    BigDecimal("10"),
                    BigDecimal("250")
                ),
                expectedAmount = "",
                expectedSatoshi = "",
            ), // null amount -> both fields empty
        )

        @Test
        fun `GIVEN non-bitcoin network WHEN convert THEN returns empty list`() {
            // Arrange
            val ethStatus = feeStatus.copy(currency = currencyFactory.createCoin(Blockchain.Ethereum))

            // Act
            val actual = bitcoinConverter(ethStatus).convert(
                Fee.Bitcoin(
                    amount(BigDecimal("0.000025")),
                    BigDecimal("10"),
                    BigDecimal("250")
                ),
            )

            // Assert
            assertThat(actual).isEmpty()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Affordability {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN fee compared to balance WHEN convert THEN satoshi field imeAction reflects affordability`(
            model: ImeActionModel,
        ) {
            // Act  (balance = 1 BTC)
            val actual = converter.convert(model.fee)

            // Assert
            assertThat(actual[FEE_SATOSHI_INDEX].keyboardOptions.imeAction).isEqualTo(model.expectedImeAction)
        }

        private fun provideTestModels() = listOf(
            ImeActionModel(
                fee = Fee.Bitcoin(
                    amount(BigDecimal("0.000025")),
                    BigDecimal("10"),
                    BigDecimal("250")
                ),
                expectedImeAction = ImeAction.Done,
            ), // within balance
            ImeActionModel(
                fee = Fee.Bitcoin(
                    amount(BigDecimal("2")),
                    BigDecimal("10"),
                    BigDecimal("250")
                ),
                expectedImeAction = ImeAction.None,
            ), // exceeds balance
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertBack {

        @Test
        fun `GIVEN custom fields WHEN convertBack THEN amount and satoshiPerByte parsed back`() {
            // Arrange
            val normalFee = Fee.Bitcoin(amount(BigDecimal("0.000025")), BigDecimal("10"), BigDecimal("250"))
            val fields = converter.convert(normalFee)

            // Act
            val actual = converter.convertBack(normalFee, fields)

            // Assert
            assertThat(actual.amount.value!!.compareTo(BigDecimal("0.000025"))).isEqualTo(0)
            assertThat(actual.satoshiPerByte.compareTo(BigDecimal("10"))).isEqualTo(0)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnValueChange {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN satoshi changed WHEN onValueChange THEN fee amount recalculated`(
            model: OnValueChangeModel,
        ) {
            // Arrange
            val fields = converter.convert(
                Fee.Bitcoin(
                    amount(BigDecimal("0.000025")),
                    BigDecimal("10"),
                    BigDecimal("250")
                ),
            )

            // Act
            val actual = converter.onValueChange(fields, FEE_SATOSHI_INDEX, model.inputSatoshi, model.txSize)

            // Assert
            assertThat(actual[FEE_AMOUNT_INDEX].value).isEqualTo(model.expectedAmount)
            assertThat(actual[FEE_SATOSHI_INDEX].value).isEqualTo(model.inputSatoshi)
        }

        private fun provideTestModels() = listOf(
            OnValueChangeModel(
                inputSatoshi = "20",
                txSize = BigDecimal("250"),
                expectedAmount = "0.00005",
            ), // 20 * 250 = 5000 sat = 0.00005 BTC
            OnValueChangeModel(
                inputSatoshi = "11",
                txSize = BigDecimal("250.5"),
                expectedAmount = "0.00002755",
            ), // 11 * 250.5 = 2755.5 sat -> 0.000027555 -> DOWN to 8 decimals
        )

        @Test
        fun `GIVEN non-satoshi index WHEN onValueChange THEN values unchanged`() {
            // Arrange
            val fields = converter.convert(
                Fee.Bitcoin(
                    amount(BigDecimal("0.000025")),
                    BigDecimal("10"),
                    BigDecimal("250")
                ),
            )

            // Act
            val actual = converter.onValueChange(fields, FEE_AMOUNT_INDEX, "999", BigDecimal("250"))

            // Assert
            assertThat(actual).isEqualTo(fields)
        }
    }

    data class ConvertModel(val fee: Fee.Bitcoin, val expectedAmount: String, val expectedSatoshi: String)
    data class ImeActionModel(val fee: Fee.Bitcoin, val expectedImeAction: ImeAction)
    data class OnValueChangeModel(val inputSatoshi: String, val txSize: BigDecimal, val expectedAmount: String)

    private companion object {
        private const val BTC_DECIMALS = 8
        private const val FEE_AMOUNT_INDEX = 0
        private const val FEE_SATOSHI_INDEX = 1
    }
}