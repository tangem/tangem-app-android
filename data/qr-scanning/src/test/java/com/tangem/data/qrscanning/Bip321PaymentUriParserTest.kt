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
        every { getShareSchemes(any()) } returns emptyList()
    }
    private val parser = Bip321PaymentUriParser(blockchainDataProvider)

    // region Basic parsing

    @Test
    fun `bitcoin URI with address and amount`() {
        every { blockchainDataProvider.getShareSchemes(bitcoinCoin.network) } returns listOf("bitcoin:")

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
        every { blockchainDataProvider.getShareSchemes(bitcoinCoin.network) } returns listOf("bitcoin:")

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
        every { blockchainDataProvider.getShareSchemes(bitcoinCoin.network) } returns listOf("bitcoin:")

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
        every { blockchainDataProvider.getShareSchemes(bitcoinCoin.network) } returns listOf("bitcoin:")

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
        every { blockchainDataProvider.getShareSchemes(litecoinCoin.network) } returns listOf("litecoin:")

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
    fun `no matching scheme returns NotRecognized`() {
        every { blockchainDataProvider.getShareSchemes(bitcoinCoin.network) } returns listOf("bitcoin:")

        val result = parser.parse(
            qrCode = "dogecoin:DAddress?amount=100",
            coins = listOf(bitcoinCoin),
            allCurrencies = listOf(bitcoinCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.NotRecognized::class.java)
    }

    @Test
    fun `ethereum scheme matches as Success`() {
        every { blockchainDataProvider.getShareSchemes(ethereumCoin.network) } returns listOf("ethereum:")

        val result = parser.parse(
            qrCode = "ethereum:0xRecipient?value=1000",
            coins = listOf(ethereumCoin),
            allCurrencies = listOf(ethereumCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
    }

    @Test
    fun `case insensitive scheme matching`() {
        every { blockchainDataProvider.getShareSchemes(bitcoinCoin.network) } returns listOf("bitcoin:")

        val result = parser.parse(
            qrCode = "Bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa?amount=0.1",
            coins = listOf(bitcoinCoin),
            allCurrencies = listOf(bitcoinCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.address).isEqualTo("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")
    }

    // endregion

    // region Includes tokens on matching network

    @Test
    fun `includes tokens on matching network`() {
        every { blockchainDataProvider.getShareSchemes(bitcoinCoin.network) } returns listOf("bitcoin:")

        val btcToken = buildToken("bitcoin", "RUNE", "contractAddr")

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
    fun `bitcoin URI with memo param`() {
        every { blockchainDataProvider.getShareSchemes(bitcoinCoin.network) } returns listOf("bitcoin:")

        val result = parser.parse(
            qrCode = "bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa?amount=1&memo=TestMemo",
            coins = listOf(bitcoinCoin),
            allCurrencies = listOf(bitcoinCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.memo).isEqualTo("TestMemo")
    }

    // endregion

    // region Helpers

    private fun PaymentUriParser.ParseResult.asSuccess(): ClassifiedQrContent.PaymentUri? {
        return (this as? PaymentUriParser.ParseResult.Success)?.content
    }

    private val bitcoinCoin = buildCoin("bitcoin", decimals = 8)
    private val litecoinCoin = buildCoin("litecoin", decimals = 8)
    private val ethereumCoin = buildCoin("ethereum", decimals = 18)

    private fun buildCoin(rawNetworkId: String, decimals: Int): CryptoCurrency.Coin {
        return CryptoCurrency.Coin(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(rawNetworkId),
                suffix = CryptoCurrency.ID.Suffix.RawID(rawNetworkId),
            ),
            network = buildNetwork(rawNetworkId),
            name = rawNetworkId,
            symbol = rawNetworkId.take(3).uppercase(),
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
            network = buildNetwork(rawNetworkId),
            name = symbol,
            symbol = symbol,
            decimals = 6,
            iconUrl = null,
            isCustom = false,
            contractAddress = contractAddress,
        )
    }

    private fun buildNetwork(rawNetworkId: String): Network {
        return Network(
            id = Network.ID(Network.RawID(rawNetworkId), Network.DerivationPath.None),
            backendId = rawNetworkId,
            name = rawNetworkId,
            currencySymbol = rawNetworkId.take(3).uppercase(),
            derivationPath = Network.DerivationPath.None,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified("UNSPECIFIED"),
            hasFiatFeeRate = false,
            canHandleTokens = false,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
    }

    // endregion
}