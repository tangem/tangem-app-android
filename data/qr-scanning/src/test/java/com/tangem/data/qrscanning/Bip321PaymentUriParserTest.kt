package com.tangem.data.qrscanning

import com.google.common.truth.Truth.assertThat
import com.tangem.data.qrscanning.parser.Bip321PaymentUriParser
import com.tangem.data.qrscanning.parser.PaymentUriParser
import com.tangem.data.qrscanning.parser.QrContentClassifierParser
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.qrscanning.models.ClassifiedQrContent
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.math.BigDecimal

internal class Bip321PaymentUriParserTest {

    private val blockchainDataProvider = mockk<QrContentClassifierParser.BlockchainDataProvider> {
        every { validateAddress(any(), any()) } returns true
    }
    private val parser = Bip321PaymentUriParser(blockchainDataProvider)

    // region Basic parsing

    @Test
    fun `bitcoin URI with address and amount`() {
        val result = parser.parse(
            qrCode = "bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa?amount=0.5",
            coins = listOf(bitcoinCoin),
            allCurrencies = listOf(bitcoinCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.address).isEqualTo("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")
        assertThat(result.amount!!.compareTo(BigDecimal("0.5"))).isEqualTo(0)
        assertThat(result.memo).isNull()
        assertThat(result.matchingCurrencies).containsExactly(bitcoinCoin)
    }

    @Test
    fun `bitcoin URI with address only`() {
        val result = parser.parse(
            qrCode = "bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa",
            coins = listOf(bitcoinCoin),
            allCurrencies = listOf(bitcoinCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.address).isEqualTo("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")
        assertThat(result.amount).isNull()
        assertThat(result.memo).isNull()
    }

    @Test
    fun `bitcoin URI with amount and message`() {
        val result = parser.parse(
            qrCode = "bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa?amount=1.23&message=Donation",
            coins = listOf(bitcoinCoin),
            allCurrencies = listOf(bitcoinCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.address).isEqualTo("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")
        assertThat(result.amount!!.compareTo(BigDecimal("1.23"))).isEqualTo(0)
        assertThat(result.memo).isEqualTo("Donation")
    }

    @Test
    fun `bitcoin URI with label and message`() {
        val result = parser.parse(
            qrCode = "bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa?label=Satoshi&message=Payment",
            coins = listOf(bitcoinCoin),
            allCurrencies = listOf(bitcoinCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.memo).isEqualTo("Payment")
    }

    // endregion

    // region Scheme matching

    @Test
    fun `litecoin URI matches litecoin coin`() {
        val result = parser.parse(
            qrCode = "litecoin:LcHKx4Tt97hnGgR3CRUiB1gSQ3F8wMozLj?amount=10",
            coins = listOf(litecoinCoin),
            allCurrencies = listOf(litecoinCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.address).isEqualTo("LcHKx4Tt97hnGgR3CRUiB1gSQ3F8wMozLj")
        assertThat(result.amount!!.compareTo(BigDecimal("10"))).isEqualTo(0)
        assertThat(result.matchingCurrencies).containsExactly(litecoinCoin)
    }

    @Test
    fun `dogecoin URI matches dogecoin coin`() {
        val result = parser.parse(
            qrCode = "doge:DAddress?amount=100",
            coins = listOf(dogecoinCoin),
            allCurrencies = listOf(dogecoinCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.address).isEqualTo("DAddress")
    }

    @Test
    fun `no matching scheme returns NotRecognized`() {
        val result = parser.parse(
            qrCode = "solana:SomeAddress?amount=100",
            coins = listOf(bitcoinCoin),
            allCurrencies = listOf(bitcoinCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.NotRecognized::class.java)
    }

    @Test
    fun `ethereum scheme returns NotRecognized`() {
        val result = parser.parse(
            qrCode = "ethereum:0xRecipient?value=1000",
            coins = listOf(bitcoinCoin),
            allCurrencies = listOf(bitcoinCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.NotRecognized::class.java)
    }

    @Test
    fun `case insensitive scheme matching`() {
        val result = parser.parse(
            qrCode = "Bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa?amount=0.1",
            coins = listOf(bitcoinCoin),
            allCurrencies = listOf(bitcoinCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.address).isEqualTo("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")
    }

    // endregion

    // region Unsupported network

    @Test
    fun `invalid address returns Unrecognized error`() {
        every { blockchainDataProvider.validateAddress(any(), eq("InvalidBtcAddress")) } returns false

        val result = parser.parse(
            qrCode = "bitcoin:InvalidBtcAddress?amount=0.5",
            coins = listOf(bitcoinCoin),
            allCurrencies = listOf(bitcoinCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.RecognizedError::class.java)
        val error = (result as PaymentUriParser.ParseResult.RecognizedError).error
        assertThat(error).isInstanceOf(ClassifiedQrContent.Error.Unrecognized::class.java)
    }

    @Test
    fun `bitcoin URI with no matching coin returns UnsupportedNetwork`() {
        val result = parser.parse(
            qrCode = "bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa?amount=0.5",
            coins = listOf(litecoinCoin),
            allCurrencies = listOf(litecoinCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.RecognizedError::class.java)
    }

    // endregion

    // region Includes tokens on matching network

    @Test
    fun `includes tokens on matching network`() {
        val btcToken = buildToken("BTC", "RUNE", "contractAddr")

        val result = parser.parse(
            qrCode = "bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa?amount=0.01",
            coins = listOf(bitcoinCoin),
            allCurrencies = listOf(bitcoinCoin, btcToken),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.matchingCurrencies).containsExactly(bitcoinCoin, btcToken)
    }

    // endregion

    // region Edge cases

    @Test
    fun `empty qr code returns NotRecognized`() {
        val result = parser.parse(
            qrCode = "",
            coins = listOf(bitcoinCoin),
            allCurrencies = listOf(bitcoinCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.NotRecognized::class.java)
    }

    @Test
    fun `plain address returns NotRecognized`() {
        val result = parser.parse(
            qrCode = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa",
            coins = listOf(bitcoinCoin),
            allCurrencies = listOf(bitcoinCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.NotRecognized::class.java)
    }

    @Test
    fun `bitcoin URI with memo param returns warning for unsupported memo`() {
        val result = parser.parse(
            qrCode = "bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa?amount=1&memo=TestMemo",
            coins = listOf(bitcoinCoin),
            allCurrencies = listOf(bitcoinCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.SuccessWithWarning::class.java)
        val warning = result as PaymentUriParser.ParseResult.SuccessWithWarning
        assertThat(warning.content.memo).isEqualTo("TestMemo")
        assertThat(warning.unsupportedParams).containsEntry("memo", "TestMemo")
    }

    // endregion

    // region Unsupported params

    @Test
    fun `unknown parameter returns SuccessWithWarning`() {
        val result = parser.parse(
            qrCode = "bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa?amount=0.5&req-unknownparam=bar",
            coins = listOf(bitcoinCoin),
            allCurrencies = listOf(bitcoinCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.SuccessWithWarning::class.java)
        val warning = result as PaymentUriParser.ParseResult.SuccessWithWarning
        assertThat(warning.content.address).isEqualTo("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")
        assertThat(warning.unsupportedParams).containsEntry("req-unknownparam", "bar")
    }

    @Test
    fun `multiple unknown parameters all reported`() {
        val result = parser.parse(
            qrCode = "bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa?amount=1&foo=1&bar=2",
            coins = listOf(bitcoinCoin),
            allCurrencies = listOf(bitcoinCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.SuccessWithWarning::class.java)
        val warning = result as PaymentUriParser.ParseResult.SuccessWithWarning
        assertThat(warning.unsupportedParams).hasSize(2)
        assertThat(warning.unsupportedParams).containsEntry("foo", "1")
        assertThat(warning.unsupportedParams).containsEntry("bar", "2")
    }

    @Test
    fun `memo on network with memo support is not unsupported`() {
        val xrpCoin = buildCoin("XRP", "XRP", "XRP", decimals = 6, extrasType = Network.TransactionExtrasType.DESTINATION_TAG)

        val result = parser.parse(
            qrCode = "ripple:rAddress?dt=12345",
            coins = listOf(xrpCoin),
            allCurrencies = listOf(xrpCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.memo).isEqualTo("12345")
    }

    @Test
    fun `only known params returns Success`() {
        val result = parser.parse(
            qrCode = "bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa?amount=0.5&label=Satoshi",
            coins = listOf(bitcoinCoin),
            allCurrencies = listOf(bitcoinCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.address).isEqualTo("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")
    }

    // endregion

    // region Helpers

    private fun PaymentUriParser.ParseResult.asSuccess(): ClassifiedQrContent.PaymentUri? {
        return when (this) {
            is PaymentUriParser.ParseResult.Success -> content
            is PaymentUriParser.ParseResult.SuccessWithWarning -> content
            else -> null
        }
    }

    private val bitcoinCoin = buildCoin("BTC", "Bitcoin", "BTC", decimals = 8)
    private val litecoinCoin = buildCoin("LTC", "Litecoin", "LTC", decimals = 8)
    private val dogecoinCoin = buildCoin("DOGE", "Dogecoin", "DOGE", decimals = 8)

    private fun buildCoin(
        rawNetworkId: String,
        name: String,
        symbol: String,
        decimals: Int,
        extrasType: Network.TransactionExtrasType = Network.TransactionExtrasType.NONE,
    ): CryptoCurrency.Coin {
        return CryptoCurrency.Coin(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(rawNetworkId),
                suffix = CryptoCurrency.ID.Suffix.RawID(rawNetworkId),
            ),
            network = buildNetwork(rawNetworkId, name, symbol, extrasType),
            name = name,
            symbol = symbol,
            decimals = decimals,
            iconUrl = null,
            isCustom = false,
        )
    }

    private fun buildToken(rawNetworkId: String, symbol: String, contractAddress: String): CryptoCurrency.Token {
        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(rawNetworkId),
                suffix = CryptoCurrency.ID.Suffix.RawID(contractAddress),
            ),
            network = buildNetwork(rawNetworkId, rawNetworkId, symbol),
            name = symbol,
            symbol = symbol,
            decimals = 6,
            iconUrl = null,
            isCustom = false,
            contractAddress = contractAddress,
        )
    }

    private fun buildNetwork(
        rawNetworkId: String,
        name: String,
        symbol: String,
        extrasType: Network.TransactionExtrasType = Network.TransactionExtrasType.NONE,
    ): Network {
        return Network(
            id = Network.ID(Network.RawID(rawNetworkId), Network.DerivationPath.None),
            backendId = rawNetworkId,
            name = name,
            currencySymbol = symbol,
            derivationPath = Network.DerivationPath.None,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified("UNSPECIFIED"),
            hasFiatFeeRate = false,
            canHandleTokens = false,
            transactionExtrasType = extrasType,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
    }

    // endregion
}