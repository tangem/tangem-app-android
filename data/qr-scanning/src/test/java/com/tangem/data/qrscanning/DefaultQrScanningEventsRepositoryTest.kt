package com.tangem.data.qrscanning

import com.google.common.truth.Truth
import com.tangem.data.qrscanning.parser.QrContentClassifierParser
import com.tangem.data.qrscanning.repository.DefaultQrScanningEventsRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.qrscanning.models.QrResult
import io.mockk.every
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class DefaultQrScanningEventsRepositoryTest {

    private val qrContentClassifier: QrContentClassifierParser = mockk()
    private val repository = DefaultQrScanningEventsRepository(qrContentClassifier)

    private val cryptoCurrencyId = mockk<CryptoCurrency.ID>()
    private val network = mockk<Network>()
    private val cryptoCurrency = CryptoCurrency.Coin(
        id = cryptoCurrencyId,
        network = network,
        name = "blockchain",
        symbol = "symbol",
        decimals = 18,
        iconUrl = null,
        isCustom = false,
    )
    private val tokenCryptoCurrency = CryptoCurrency.Token(
        id = cryptoCurrencyId,
        network = network,
        name = "blockchain",
        symbol = "symbol",
        decimals = 7,
        iconUrl = null,
        isCustom = false,
        contractAddress = "0x89205A3A3b2A69De6Dbf7f01ED13B2108B2c43e7",
    )

    private val garbage = "some_garbage="

    private val schema1 = "bitcoin"
    private val schema2 = "ethereum"

    private val address1 = "bc1pw83rs5s75na2g7ec8yqgekr3ae209ye7ck2ftakjnh8tv3xzw8ls6wgt62"
    private val address2 = "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb"
    private val address3 = "pay-0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb"
    private val address4 = "0x89205A3A3b2A69De6Dbf7f01ED13B2108B2c43e7"

    private val function = "/transfer"

    private val someParam = "someParam"
    private val someParamValue = "someParamValue"

    private val addressParam = "address"
    private val addressParamValue = "0xc00f86ab93cd0bd3a60213583d0fe35aaa1ace23"

    private val amountParam = "amount"
    private val someAmountParamValue = "amount"
    private val amountParamValue = "123.123"

    private val valueParam = "value"
    private val someValueParamValue = "amount"
    private val valueParamValue2 = "1.88e10"
    private val valueParamValue3 = "1.68E11"
    private val valueParamValue4 = "23000000000"

    private val memoParam = "memo"
    private val memoParamValue = "a%20random%20memo"
    private val memoParamValueUtf8 = "a random memo"

    private val messageParam = "message"
    private val messageParamValue = "some%20message"
    private val messageParamValueUft8 = "some message"
    private val messageParamValue2 = "message"

    @Test
    fun testBip021() {
        every { network.id } returns Network.ID(value = "bitcoin", derivationPath = Network.DerivationPath.None)
        every { network.rawId } returns "bitcoin"
        positiveCase(
            "$schema1:$address1",
            QrResult(address = address1),
            cryptoCurrency,
        )
        positiveCase(
            "$garbage$schema1:$address1",
            QrResult(address = address1),
            cryptoCurrency,
        )
        positiveCase(
            "$address1?$someParam=$someParamValue",
            QrResult(address = address1),
            cryptoCurrency,
        )
        positiveCase(
            "$address1?$someParam=$someParamValue&$amountParam=$someAmountParamValue",
            QrResult(address = address1),
            cryptoCurrency,
        )
        positiveCase(
            "$address1?$someParam=$someParamValue&$amountParam=$amountParamValue",
            QrResult(address = address1, amount = BigDecimal(amountParamValue)),
            cryptoCurrency,
        )
        positiveCase(
            "$address1?$someParam=$someParamValue&$amountParam=$amountParamValue&$memoParam=$memoParamValue",
            QrResult(address = address1, amount = BigDecimal(amountParamValue), memo = memoParamValueUtf8),
            cryptoCurrency,
        )
        positiveCase(
            "$address1?$someParam=$someParamValue&$messageParam=$messageParamValue",
            QrResult(address = address1, memo = messageParamValueUft8),
            cryptoCurrency,
        )
        positiveCase(
            "$address1?$someParam=$someParamValue&$messageParam=$messageParamValue2",
            QrResult(address = address1, memo = messageParamValue2),
            cryptoCurrency,
        )
        negativeCase(
            "$address1?$someParam=$someParamValue&$amountParam=$amountParamValue",
            QrResult(address = address1, memo = messageParamValue2),
            cryptoCurrency,
        )
    }

    @Test
    fun testErc681Coin() {
        every { network.id } returns Network.ID(value = "ethereum", derivationPath = Network.DerivationPath.None)
        every { network.rawId } returns "ethereum"
        positiveCase(
            address2,
            QrResult(address = address2),
            cryptoCurrency,
        )
        positiveCase(
            "$address2?$someParam=$someParamValue",
            QrResult(address = address2),
            cryptoCurrency,
        )
        positiveCase(
            "$schema2:$address2",
            QrResult(address = address2),
            cryptoCurrency,
        )
        positiveCase(
            "$schema2:$address3",
            QrResult(address = address2),
            cryptoCurrency,
        )
        positiveCase(
            "$garbage$schema2:$address2",
            QrResult(address = address2),
            cryptoCurrency,
        )
        positiveCase(
            "$garbage$schema2:$address2?$someParam=$someParamValue&$valueParam=$someAmountParamValue",
            QrResult(address = address2),
            cryptoCurrency,
        )
        positiveCase(
            "$garbage$schema2:$address2?$someParam=$someParamValue&$valueParam=$valueParamValue2",
            QrResult(address = address2, amount = BigDecimal("0.0000000188")),
            cryptoCurrency,
        )
        positiveCase(
            "$garbage$schema2:$address2?$someParam=$someParamValue&$valueParam=$valueParamValue3",
            QrResult(address = address2, amount = BigDecimal("0.000000168")),
            cryptoCurrency,
        )
        positiveCase(
            "$garbage$schema2:$address2?$someParam=$someParamValue&$valueParam=$valueParamValue4",
            QrResult(address = address2, amount = BigDecimal("0.000000023")),
            cryptoCurrency,
        )
        negativeCase(
            "$garbage$schema2:$address2$function?$addressParam=$addressParamValue",
            QrResult(address = address2),
            cryptoCurrency,
        )
    }

    @Test
    fun testErc681Token() {
        every { network.id } returns Network.ID(value = "ethereum", derivationPath = Network.DerivationPath.None)
        every { network.rawId } returns "ethereum"
        positiveCase(
            address2,
            QrResult(address = address2),
            tokenCryptoCurrency,
        )
        positiveCase(
            "$address2?$someParam=$someParamValue",
            QrResult(address = address2),
            tokenCryptoCurrency,
        )
        positiveCase(
            "$schema2:$address2",
            QrResult(address = address2),
            tokenCryptoCurrency,
        )
        positiveCase(
            "$schema2:$address3",
            QrResult(address = address2),
            tokenCryptoCurrency,
        )
        positiveCase(
            "$garbage$schema2:$address2",
            QrResult(address = address2),
            tokenCryptoCurrency,
        )
        positiveCase(
            "$schema2:$address4$function?$addressParam=$addressParamValue",
            QrResult(address = addressParamValue),
            tokenCryptoCurrency,
        )
        positiveCase(
            "$address2?$someParam=$someParamValue&$valueParam=$someValueParamValue",
            QrResult(address = address2),
            tokenCryptoCurrency,
        )
        positiveCase(
            "$address2?$someParam=$someParamValue&$valueParam=$valueParamValue2",
            QrResult(address = address2, amount = BigDecimal("1880")),
            tokenCryptoCurrency,
        )
        positiveCase(
            "$address2?$someParam=$someParamValue&$valueParam=$valueParamValue3",
            QrResult(address = address2, amount = BigDecimal("16800")),
            tokenCryptoCurrency,
        )
        positiveCase(
            "$address2?$someParam=$someParamValue&$valueParam=$valueParamValue4",
            QrResult(address = address2, amount = BigDecimal("2300")),
            tokenCryptoCurrency,
        )
        positiveCase(
            "$address4?$addressParam=$addressParamValue",
            QrResult(address = addressParamValue),
            tokenCryptoCurrency,
        )
        negativeCase(
            "$address2?$someParam=$someParamValue&$amountParam=$amountParamValue",
            QrResult(address = address2, amount = BigDecimal("123.123"), memo = memoParamValueUtf8),
            tokenCryptoCurrency,
        )
        negativeCase(
            "$address4?$addressParam=$addressParamValue",
            QrResult(address = addressParamValue),
            cryptoCurrency,
        )
    }

    private fun positiveCase(input: String, expected: QrResult, cryptoCurrency: CryptoCurrency) {
        val actual = repository.parseQrCode(input, cryptoCurrency)
        Truth.assertThat(actual.address).isEqualTo(expected.address)
        Truth.assertThat(actual.amount).isEqualTo(expected.amount)
        Truth.assertThat(actual.memo).isEqualTo(expected.memo)
    }

    private fun negativeCase(input: String, expected: QrResult, cryptoCurrency: CryptoCurrency) {
        val actual = repository.parseQrCode(input, cryptoCurrency)
        Truth.assertThat(actual).isNotEqualTo(expected)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ExtractRecipientAddress {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN scanned QR code WHEN extractRecipientAddress THEN returns expected recipient`(
            model: ExtractRecipientModel,
        ) {
            // Act
            val actual = repository.extractRecipientAddress(model.qrCode)

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ExtractRecipientModel(qrCode = EVM_RECIPIENT, expected = EVM_RECIPIENT),
            ExtractRecipientModel(qrCode = "  $EVM_RECIPIENT\n", expected = EVM_RECIPIENT),
            ExtractRecipientModel(qrCode = "ethereum:$EVM_RECIPIENT@1?value=1e18", expected = EVM_RECIPIENT),
            ExtractRecipientModel(
                qrCode = "ethereum:$TOKEN_CONTRACT@1/transfer?address=$EVM_RECIPIENT&uint256=1000000",
                expected = EVM_RECIPIENT,
            ),
            ExtractRecipientModel(
                qrCode = "ethereum:$TOKEN_CONTRACT/transfer?address=$EVM_RECIPIENT&uint256=1000000",
                expected = EVM_RECIPIENT,
            ),
            ExtractRecipientModel(
                qrCode = "  ethereum:$TOKEN_CONTRACT@56/transfer?address=$EVM_RECIPIENT  ",
                expected = EVM_RECIPIENT,
            ),
            ExtractRecipientModel(
                qrCode = "ethereum:$TOKEN_CONTRACT?address=$EVM_RECIPIENT&amount=1",
                expected = EVM_RECIPIENT,
            ),
            ExtractRecipientModel(qrCode = "ethereum:$TOKEN_CONTRACT@1/transfer?uint256=1000000", expected = ""),
            ExtractRecipientModel(qrCode = "ethereum:$TOKEN_CONTRACT@1/transfer", expected = ""),
            ExtractRecipientModel(
                qrCode = "ethereum:$TOKEN_CONTRACT@1/approve?address=$EVM_RECIPIENT&uint256=1",
                expected = "",
            ),
            ExtractRecipientModel(qrCode = "ethereum:$TOKEN_CONTRACT@1/approve", expected = ""),
            ExtractRecipientModel(qrCode = "bitcoin:$BTC_ADDRESS?amount=0.1&label=Shop", expected = BTC_ADDRESS),
            ExtractRecipientModel(qrCode = "solana:$SOLANA_ADDRESS", expected = SOLANA_ADDRESS),
            ExtractRecipientModel(qrCode = "ethereum:", expected = ""),
            ExtractRecipientModel(
                qrCode = "ethereum:pay-$TOKEN_CONTRACT@1/transfer?address=$EVM_RECIPIENT",
                expected = EVM_RECIPIENT,
            ),
            ExtractRecipientModel(qrCode = "ethereum:pay-$EVM_RECIPIENT@1", expected = EVM_RECIPIENT),
            ExtractRecipientModel(
                qrCode = "$EVM_RECIPIENT?message=invoice:$TOKEN_CONTRACT",
                expected = EVM_RECIPIENT,
            ),
            ExtractRecipientModel(qrCode = "bitcoin:$BTC_ADDRESS?address=$EVM_RECIPIENT", expected = BTC_ADDRESS),
            ExtractRecipientModel(
                qrCode = "polygon:$TOKEN_CONTRACT@137/transfer?address=$EVM_RECIPIENT&uint256=1",
                expected = EVM_RECIPIENT,
            ),
            ExtractRecipientModel(
                qrCode = "polygon:$TOKEN_CONTRACT@137/approve?address=$EVM_RECIPIENT&uint256=1",
                expected = "",
            ),
            ExtractRecipientModel(qrCode = TON_ADDRESS_WITH_SLASHES, expected = TON_ADDRESS_WITH_SLASHES),
            ExtractRecipientModel(qrCode = TON_ADDRESS_URL_SAFE, expected = TON_ADDRESS_URL_SAFE),
            ExtractRecipientModel(qrCode = TON_ADDRESS_WITH_PLUS, expected = TON_ADDRESS_WITH_PLUS),
            ExtractRecipientModel(
                qrCode = "ton://transfer/$TON_ADDRESS_WITH_SLASHES?amount=1000000000&text=hi",
                expected = TON_ADDRESS_WITH_SLASHES,
            ),
            ExtractRecipientModel(qrCode = "ton://transfer/$TON_ADDRESS_URL_SAFE", expected = TON_ADDRESS_URL_SAFE),
            ExtractRecipientModel(qrCode = "ton:$TON_ADDRESS_WITH_SLASHES", expected = TON_ADDRESS_WITH_SLASHES),
            ExtractRecipientModel(qrCode = TON_RAW_ADDRESS, expected = TON_RAW_ADDRESS),
            ExtractRecipientModel(qrCode = HEDERA_ACCOUNT_ID, expected = HEDERA_ACCOUNT_ID),
            ExtractRecipientModel(
                qrCode = HEDERA_ACCOUNT_ID_WITH_CHECKSUM,
                expected = HEDERA_ACCOUNT_ID_WITH_CHECKSUM,
            ),
            ExtractRecipientModel(qrCode = "hedera:$HEDERA_ACCOUNT_ID", expected = HEDERA_ACCOUNT_ID),
            ExtractRecipientModel(qrCode = "$HEDERA_ACCOUNT_ID?memo=invoice", expected = HEDERA_ACCOUNT_ID),
            ExtractRecipientModel(qrCode = HEDERA_EVM_ALIAS, expected = HEDERA_EVM_ALIAS),
        )
    }

    internal data class ExtractRecipientModel(val qrCode: String, val expected: String)

    private companion object {
        const val EVM_RECIPIENT = "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb"
        const val TOKEN_CONTRACT = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
        const val BTC_ADDRESS = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"
        const val SOLANA_ADDRESS = "7xKXtg2CW87d97TXJSDpbD5jBkheTqA83TZRuJosgAsU"
        const val TON_ADDRESS_WITH_SLASHES = "EQD//////////////////////////////////////////0vo"
        const val TON_ADDRESS_URL_SAFE = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB-_"
        const val TON_ADDRESS_WITH_PLUS = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB+/"
        const val TON_RAW_ADDRESS = "0:83dfd552e63729b472fcbcc8c45ebcc6691702558b68ec7527e1ba403a0f31a8"
        const val HEDERA_ACCOUNT_ID = "0.0.123456"
        const val HEDERA_ACCOUNT_ID_WITH_CHECKSUM = "0.0.123456-vfmkw"
        const val HEDERA_EVM_ALIAS = "0x000000000000000000000000000000000001e240"
    }
}