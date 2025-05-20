package com.tangem.data.qrscanning

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.data.qrscanning.repository.DefaultQrScanningEventsRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.qrscanning.models.QrResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.math.BigDecimal

internal class DefaultQrScanningEventsRepositoryTest {

    private val repository = DefaultQrScanningEventsRepository()

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
        every { network.rawId } returns Blockchain.Bitcoin.id
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
        every { network.rawId } returns Blockchain.Ethereum.id
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
        every { network.rawId } returns Blockchain.Ethereum.id
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
}