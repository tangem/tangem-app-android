package com.tangem.data.qrscanning

import com.google.common.truth.Truth.assertThat
import com.tangem.data.qrscanning.parser.PaymentUriParser
import com.tangem.data.qrscanning.parser.QrContentClassifierParser
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.qrscanning.models.ClassifiedQrContent
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.math.BigDecimal

internal class QrContentClassifierTest {

    private val blockchainDataProvider = mockk<QrContentClassifierParser.BlockchainDataProvider> {
        every { validateAddress(any(), any()) } returns false
        every { getChainId(any()) } returns null
        every { findSupportedBlockchainName(any()) } returns null
        every { getBlockchainNameByChainId(any()) } returns null
    }
    private val paymentUriParser = mockk<PaymentUriParser> {
        every { parse(any(), any(), any()) } returns PaymentUriParser.ParseResult.NotRecognized
    }
    private val classifier = QrContentClassifierParser(
        blockchainDataProvider = blockchainDataProvider,
        paymentUriParsers = setOf(paymentUriParser),
    )

    // region WalletConnect

    @Test
    fun `WalletConnect URI is classified correctly`() {
        val uri = "wc:a4f86d5-72ac-46ad-a1aa-9e@1-f5c0c@2"
        val result = classifier.parse(uri, listOf(bitcoinCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.WalletConnect::class.java)
        assertThat((result as ClassifiedQrContent.WalletConnect).uri).isEqualTo(uri)
    }

    @Test
    fun `WalletConnect URI takes priority over address matching`() {
        val uri = "wc:something"
        every { blockchainDataProvider.validateAddress(any(), uri) } returns true

        val result = classifier.parse(uri, listOf(bitcoinCoin, ethereumCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.WalletConnect::class.java)
    }

    @Test
    fun `dApp URL with wc uri query param is classified as WalletConnect`() {
        val dAppUrl = "https://uniswap.org/app/wc?uri=wc:6ea45@2?relay-protocol=irn&symKey=f2de&expiryTimestamp=123"

        val result = classifier.parse(dAppUrl, listOf(bitcoinCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.WalletConnect::class.java)
        assertThat((result as ClassifiedQrContent.WalletConnect).uri).isEqualTo(dAppUrl)
    }

    @Test
    fun `HTTP URL without wc uri param is not classified as WalletConnect`() {
        val url = "https://example.com/page?foo=bar"

        val result = classifier.parse(url, listOf(bitcoinCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.Error.Unrecognized::class.java)
    }

    @Test
    fun `HTTP URL with uri param not starting with wc is not WalletConnect`() {
        val url = "https://example.com/page?uri=https://other.com"

        val result = classifier.parse(url, listOf(bitcoinCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.Error.Unrecognized::class.java)
    }

    // endregion

    // region PaymentUri delegation

    @Test
    fun `PaymentUri is returned when parser matches`() {
        val expectedUri = ClassifiedQrContent.PaymentUri(
            address = "0xRecipient",
            amount = BigDecimal("1.5"),
            memo = null,
            matchingCurrencies = listOf(ethereumCoin),
        )
        every { paymentUriParser.parse(any(), any(), any()) } returns PaymentUriParser.ParseResult.Success(expectedUri)

        val result = classifier.parse("ethereum:0xRecipient@1?value=1500000000000000000", listOf(ethereumCoin))

        assertThat(result).isEqualTo(expectedUri)
    }

    @Test
    fun `PaymentUri takes priority over plain address match`() {
        val expectedUri = ClassifiedQrContent.PaymentUri(
            address = "0xRecipient",
            amount = null,
            memo = null,
            matchingCurrencies = listOf(ethereumCoin),
        )
        every { paymentUriParser.parse(any(), any(), any()) } returns PaymentUriParser.ParseResult.Success(expectedUri)
        every { blockchainDataProvider.validateAddress(ethereumCoin.network, any()) } returns true

        val result = classifier.parse("ethereum:0xRecipient", listOf(ethereumCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.PaymentUri::class.java)
    }

    // endregion

    // region PlainAddress

    @Test
    fun `Plain address matches single currency`() {
        val address = "0x1234567890abcdef1234567890abcdef12345678"
        every { blockchainDataProvider.validateAddress(ethereumCoin.network, address) } returns true

        val result = classifier.parse(address, listOf(ethereumCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.PlainAddress::class.java)
        val plain = result as ClassifiedQrContent.PlainAddress
        assertThat(plain.address).isEqualTo(address)
        assertThat(plain.matchingCurrencies).containsExactly(ethereumCoin)
    }

    @Test
    fun `Plain address matches multiple currencies`() {
        val address = "0x1234567890abcdef1234567890abcdef12345678"
        every { blockchainDataProvider.validateAddress(ethereumCoin.network, address) } returns true
        every { blockchainDataProvider.validateAddress(bscCoin.network, address) } returns true

        val result = classifier.parse(address, listOf(ethereumCoin, bscCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.PlainAddress::class.java)
        val plain = result as ClassifiedQrContent.PlainAddress
        assertThat(plain.matchingCurrencies).hasSize(2)
    }

    @Test
    fun `PlainAddress includes tokens on matching networks`() {
        val address = "0x1234567890abcdef1234567890abcdef12345678"
        every { blockchainDataProvider.validateAddress(ethereumCoin.network, address) } returns true

        val usdcToken = buildToken("ethereum", "USDC", "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48")

        val result = classifier.parse(address, listOf(ethereumCoin, usdcToken))

        assertThat(result).isInstanceOf(ClassifiedQrContent.PlainAddress::class.java)
        val plain = result as ClassifiedQrContent.PlainAddress
        assertThat(plain.matchingCurrencies).containsExactly(ethereumCoin, usdcToken)
    }

    // endregion

    // region Unknown

    @Test
    fun `Random string returns Unknown`() {
        val result = classifier.parse("hello world", listOf(bitcoinCoin, ethereumCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.Error.Unrecognized::class.java)
        assertThat((result as ClassifiedQrContent.Error.Unrecognized).raw).isEqualTo("hello world")
    }

    @Test
    fun `Empty string returns Unknown`() {
        val result = classifier.parse("", listOf(bitcoinCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.Error.Unrecognized::class.java)
    }

    @Test
    fun `Empty currencies list returns Unknown`() {
        val result = classifier.parse("0x1234567890abcdef1234567890abcdef12345678", emptyList())

        assertThat(result).isInstanceOf(ClassifiedQrContent.Error.Unrecognized::class.java)
    }

    @Test
    fun `Tokens alone without Coins cannot match addresses`() {
        val token = buildToken("ethereum", "USDT", "0xdAC17F958D2ee523a2206206994597C13D831ec7")

        val result = classifier.parse("0x1234", listOf(token))

        assertThat(result).isInstanceOf(ClassifiedQrContent.Error.Unrecognized::class.java)
    }

    // endregion

    // region Helpers

    private val bitcoinCoin = buildCoin("bitcoin")
    private val ethereumCoin = buildCoin("ethereum")
    private val bscCoin = buildCoin("bsc")

    private fun buildCoin(rawNetworkId: String): CryptoCurrency.Coin {
        return CryptoCurrency.Coin(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(rawNetworkId),
                suffix = CryptoCurrency.ID.Suffix.RawID(rawNetworkId),
            ),
            network = buildNetwork(rawNetworkId),
            name = rawNetworkId,
            symbol = rawNetworkId.take(3).uppercase(),
            decimals = 8,
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