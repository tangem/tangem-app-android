package com.tangem.data.qrscanning

import com.google.common.truth.Truth.assertThat
import com.tangem.data.qrscanning.parser.PaymentUriParser
import com.tangem.data.qrscanning.parser.QrContentClassifierParser
import com.tangem.data.qrscanning.parser.TronPaymentUriParser
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.qrscanning.models.ClassifiedQrContent
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.math.BigDecimal

internal class TronPaymentUriParserTest {

    private val blockchainDataProvider = mockk<QrContentClassifierParser.BlockchainDataProvider> {
        every { validateAddress(any(), any()) } returns true
    }
    private val parser = TronPaymentUriParser(blockchainDataProvider)

    // region Scheme matching

    @Test
    fun `tron URI recognized`() {
        val result = parser.parse(
            qrCode = "tron:TAddress",
            coins = listOf(tronCoin),
            allCurrencies = listOf(tronCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.address).isEqualTo("TAddress")
    }

    @Test
    fun `non-tron URI returns NotRecognized`() {
        val result = parser.parse(
            qrCode = "bitcoin:1Address",
            coins = listOf(tronCoin),
            allCurrencies = listOf(tronCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.NotRecognized::class.java)
    }

    @Test
    fun `case insensitive scheme`() {
        val result = parser.parse(
            qrCode = "Tron:TAddress",
            coins = listOf(tronCoin),
            allCurrencies = listOf(tronCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
    }

    // endregion

    // region Native transfer

    @Test
    fun `native transfer with amount`() {
        val result = parser.parse(
            qrCode = "tron:TAddress?amount=100",
            coins = listOf(tronCoin),
            allCurrencies = listOf(tronCoin, usdtToken),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.amount!!.compareTo(BigDecimal("100"))).isEqualTo(0)
        assertThat(result.matchingCurrencies).containsExactly(tronCoin)
    }

    @Test
    fun `no amount returns all currencies on network`() {
        val result = parser.parse(
            qrCode = "tron:TAddress",
            coins = listOf(tronCoin),
            allCurrencies = listOf(tronCoin, usdtToken),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.amount).isNull()
        assertThat(result.matchingCurrencies).containsExactly(tronCoin, usdtToken)
    }

    // endregion

    // region Token transfer

    @Test
    fun `token transfer with token param`() {
        val result = parser.parse(
            qrCode = "tron:TAddress?token=TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t&amount=50",
            coins = listOf(tronCoin),
            allCurrencies = listOf(tronCoin, usdtToken),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.address).isEqualTo("TAddress")
        assertThat(result.matchingCurrencies).containsExactly(usdtToken)
    }

    @Test
    fun `token not found returns UnsupportedNetwork`() {
        val result = parser.parse(
            qrCode = "tron:TAddress?token=TUnknownContract",
            coins = listOf(tronCoin),
            allCurrencies = listOf(tronCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.RecognizedError::class.java)
    }

    // endregion

    // region Amount interpretation

    @Test
    fun `amount with decimal point used as-is`() {
        val result = parser.parse(
            qrCode = "tron:TAddress?amount=1.5",
            coins = listOf(tronCoin),
            allCurrencies = listOf(tronCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.amount!!.compareTo(BigDecimal("1.5"))).isEqualTo(0)
    }

    @Test
    fun `integer amount less or equal 100000 treated as normal`() {
        val result = parser.parse(
            qrCode = "tron:TAddress?amount=100000",
            coins = listOf(tronCoin),
            allCurrencies = listOf(tronCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.amount!!.compareTo(BigDecimal("100000"))).isEqualTo(0)
    }

    @Test
    fun `integer amount greater than 100000 treated as smallest unit`() {
        // 1_000_000 with 6 decimals (TRX) = 1.0
        val result = parser.parse(
            qrCode = "tron:TAddress?amount=1000000",
            coins = listOf(tronCoin),
            allCurrencies = listOf(tronCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.amount!!.compareTo(BigDecimal("1"))).isEqualTo(0)
    }

    // endregion

    // region Unsupported network

    @Test
    fun `invalid address returns Unrecognized error`() {
        every { blockchainDataProvider.validateAddress(any(), eq("InvalidTronAddress")) } returns false

        val result = parser.parse(
            qrCode = "tron:InvalidTronAddress?amount=1",
            coins = listOf(tronCoin),
            allCurrencies = listOf(tronCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.RecognizedError::class.java)
        val error = (result as PaymentUriParser.ParseResult.RecognizedError).error
        assertThat(error).isInstanceOf(ClassifiedQrContent.Error.Unrecognized::class.java)
    }

    @Test
    fun `no matching tron coin returns UnsupportedNetwork`() {
        val result = parser.parse(
            qrCode = "tron:TAddress",
            coins = emptyList(),
            allCurrencies = emptyList(),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.RecognizedError::class.java)
    }

    // endregion

    // region Unsupported params

    @Test
    fun `unknown parameter returns SuccessWithWarning`() {
        val result = parser.parse(
            qrCode = "tron:TAddress?amount=100&gasLimit=21000",
            coins = listOf(tronCoin),
            allCurrencies = listOf(tronCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.SuccessWithWarning::class.java)
        val warning = result as PaymentUriParser.ParseResult.SuccessWithWarning
        assertThat(warning.content.address).isEqualTo("TAddress")
        assertThat(warning.unsupportedParams).containsEntry("gaslimit", "21000")
    }

    @Test
    fun `memo on non-memo network returns SuccessWithWarning`() {
        val result = parser.parse(
            qrCode = "tron:TAddress?amount=100&memo=hello",
            coins = listOf(tronCoin),
            allCurrencies = listOf(tronCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.SuccessWithWarning::class.java)
        val warning = result as PaymentUriParser.ParseResult.SuccessWithWarning
        assertThat(warning.unsupportedParams).containsEntry("memo", "hello")
    }

    @Test
    fun `only known params returns Success`() {
        val result = parser.parse(
            qrCode = "tron:TAddress?amount=100",
            coins = listOf(tronCoin),
            allCurrencies = listOf(tronCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.Success::class.java)
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

    private val tronNetwork = buildNetwork("TRON", "Tron", "TRX")

    private val tronCoin = CryptoCurrency.Coin(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId("TRON"),
            suffix = CryptoCurrency.ID.Suffix.RawID("TRON"),
        ),
        network = tronNetwork,
        name = "Tron",
        symbol = "TRX",
        decimals = 6,
        iconUrl = null,
        isCustom = false,
    )

    private val usdtToken = CryptoCurrency.Token(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId("TRON"),
            suffix = CryptoCurrency.ID.Suffix.RawID("TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"),
        ),
        network = tronNetwork,
        name = "Tether USD",
        symbol = "USDT",
        decimals = 6,
        iconUrl = null,
        isCustom = false,
        contractAddress = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t",
    )

    private fun buildNetwork(rawNetworkId: String, name: String, symbol: String): Network {
        return Network(
            id = Network.ID(Network.RawID(rawNetworkId), Network.DerivationPath.None),
            backendId = rawNetworkId,
            name = name,
            currencySymbol = symbol,
            derivationPath = Network.DerivationPath.None,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified("UNSPECIFIED"),
            hasFiatFeeRate = false,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
    }

    // endregion
}