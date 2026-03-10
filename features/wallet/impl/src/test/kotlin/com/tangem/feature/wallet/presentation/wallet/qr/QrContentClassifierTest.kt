package com.tangem.feature.wallet.presentation.wallet.qr

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.math.BigDecimal

internal class QrContentClassifierTest {

    private val blockchainDataProvider = mockk<QrContentClassifier.BlockchainDataProvider> {
        every { getShareSchemes(any()) } returns emptyList()
        every { validateAddress(any(), any()) } returns false
    }
    private val classifier = QrContentClassifier(blockchainDataProvider)

    // region WalletConnect

    @Test
    fun `WalletConnect URI is classified correctly`() {
        val uri = "wc:a4f86d5-72ac-46ad-a1aa-9e@1-f5c0c@2"
        val result = classifier.classify(uri, listOf(bitcoinCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.WalletConnect::class.java)
        assertThat((result as ClassifiedQrContent.WalletConnect).uri).isEqualTo(uri)
    }

    @Test
    fun `WalletConnect URI takes priority over address matching`() {
        val uri = "wc:something"
        every { blockchainDataProvider.validateAddress(any(), uri) } returns true

        val result = classifier.classify(uri, listOf(bitcoinCoin, ethereumCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.WalletConnect::class.java)
    }

    @Test
    fun `dApp URL with wc uri query param is classified as WalletConnect`() {
        val dAppUrl = "https://uniswap.org/app/wc?uri=wc:6ea45@2?relay-protocol=irn&symKey=f2de&expiryTimestamp=123"

        val result = classifier.classify(dAppUrl, listOf(bitcoinCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.WalletConnect::class.java)
        assertThat((result as ClassifiedQrContent.WalletConnect).uri).isEqualTo(dAppUrl)
    }

    @Test
    fun `HTTP URL without wc uri param is not classified as WalletConnect`() {
        val url = "https://example.com/page?foo=bar"

        val result = classifier.classify(url, listOf(bitcoinCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.Unknown::class.java)
    }

    @Test
    fun `HTTP URL with uri param not starting with wc is not WalletConnect`() {
        val url = "https://example.com/page?uri=https://other.com"

        val result = classifier.classify(url, listOf(bitcoinCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.Unknown::class.java)
    }

    // endregion

    // region PaymentUri

    @Test
    fun `Bitcoin BIP-021 URI with amount is parsed`() {
        every { blockchainDataProvider.getShareSchemes(bitcoinCoin.network) } returns listOf("bitcoin:")

        val qr = "bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa?amount=0.5"
        val result = classifier.classify(qr, listOf(bitcoinCoin, ethereumCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.PaymentUri::class.java)
        val paymentUri = result as ClassifiedQrContent.PaymentUri
        assertThat(paymentUri.currency).isEqualTo(bitcoinCoin)
        assertThat(paymentUri.address).isEqualTo("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")
        assertThat(paymentUri.amount).isEqualTo(BigDecimal("0.5"))
        assertThat(paymentUri.memo).isNull()
    }

    @Test
    fun `Bitcoin URI without params returns address only`() {
        every { blockchainDataProvider.getShareSchemes(bitcoinCoin.network) } returns listOf("bitcoin:")

        val qr = "bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"
        val result = classifier.classify(qr, listOf(bitcoinCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.PaymentUri::class.java)
        val paymentUri = result as ClassifiedQrContent.PaymentUri
        assertThat(paymentUri.address).isEqualTo("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")
        assertThat(paymentUri.amount).isNull()
        assertThat(paymentUri.memo).isNull()
    }

    @Test
    fun `Bitcoin URI with message param is parsed as memo`() {
        every { blockchainDataProvider.getShareSchemes(bitcoinCoin.network) } returns listOf("bitcoin:")

        val qr = "bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa?amount=1.0&message=test%20memo"
        val result = classifier.classify(qr, listOf(bitcoinCoin))

        val paymentUri = result as ClassifiedQrContent.PaymentUri
        assertThat(paymentUri.amount).isEqualTo(BigDecimal("1.0"))
        assertThat(paymentUri.memo).isEqualTo("test memo")
    }

    @Test
    fun `URI with memo parameter is parsed`() {
        every { blockchainDataProvider.getShareSchemes(bitcoinCoin.network) } returns listOf("bitcoin:")

        val qr = "bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa?memo=hello"
        val result = classifier.classify(qr, listOf(bitcoinCoin))

        val paymentUri = result as ClassifiedQrContent.PaymentUri
        assertThat(paymentUri.memo).isEqualTo("hello")
    }

    @Test
    fun `Ethereum ERC-681 URI with chain_id and function is parsed`() {
        every { blockchainDataProvider.getShareSchemes(ethereumCoin.network) } returns listOf("ethereum:")

        val qr = "ethereum:0x1234567890abcdef1234567890abcdef12345678@1/transfer?amount=1.5"
        val result = classifier.classify(qr, listOf(ethereumCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.PaymentUri::class.java)
        val paymentUri = result as ClassifiedQrContent.PaymentUri
        assertThat(paymentUri.address).isEqualTo("0x1234567890abcdef1234567890abcdef12345678")
        assertThat(paymentUri.amount).isEqualTo(BigDecimal("1.5"))
    }

    @Test
    fun `URI scheme not matching user currencies falls through`() {
        val qr = "bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"
        val result = classifier.classify(qr, listOf(ethereumCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.Unknown::class.java)
    }

    @Test
    fun `Longest matching scheme is preferred`() {
        every { blockchainDataProvider.getShareSchemes(bitcoinCoin.network) } returns
            listOf("bitcoin:", "bitcoin://")

        val qr = "bitcoin://1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"
        val result = classifier.classify(qr, listOf(bitcoinCoin))

        val paymentUri = result as ClassifiedQrContent.PaymentUri
        assertThat(paymentUri.address).isEqualTo("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")
    }

    // endregion

    // region PlainAddress

    @Test
    fun `Plain address matches single currency`() {
        val address = "0x1234567890abcdef1234567890abcdef12345678"
        every { blockchainDataProvider.validateAddress(ethereumCoin.network, address) } returns true

        val result = classifier.classify(address, listOf(ethereumCoin))

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

        val result = classifier.classify(address, listOf(ethereumCoin, bscCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.PlainAddress::class.java)
        val plain = result as ClassifiedQrContent.PlainAddress
        assertThat(plain.matchingCurrencies).hasSize(2)
    }

    // endregion

    // region Unknown

    @Test
    fun `Random string returns Unknown`() {
        val result = classifier.classify("hello world", listOf(bitcoinCoin, ethereumCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.Unknown::class.java)
        assertThat((result as ClassifiedQrContent.Unknown).raw).isEqualTo("hello world")
    }

    @Test
    fun `Empty string returns Unknown`() {
        val result = classifier.classify("", listOf(bitcoinCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.Unknown::class.java)
    }

    @Test
    fun `Empty currencies list returns Unknown`() {
        val result = classifier.classify("0x1234567890abcdef1234567890abcdef12345678", emptyList())

        assertThat(result).isInstanceOf(ClassifiedQrContent.Unknown::class.java)
    }

    // endregion

    // region Edge cases

    @Test
    fun `Tokens are filtered out, only Coins are used`() {
        val token = CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId("ethereum"),
                suffix = CryptoCurrency.ID.Suffix.RawID("ethereum"),
            ),
            network = buildNetwork("ethereum"),
            name = "USDT",
            symbol = "USDT",
            decimals = 6,
            iconUrl = null,
            isCustom = false,
            contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
        )

        val result = classifier.classify("0x1234", listOf(token))

        assertThat(result).isInstanceOf(ClassifiedQrContent.Unknown::class.java)
    }

    @Test
    fun `Duplicate coins with same network are deduplicated`() {
        val address = "0x1234567890abcdef1234567890abcdef12345678"
        every { blockchainDataProvider.validateAddress(ethereumCoin.network, address) } returns true

        val result = classifier.classify(address, listOf(ethereumCoin, ethereumCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.PlainAddress::class.java)
        val plain = result as ClassifiedQrContent.PlainAddress
        assertThat(plain.matchingCurrencies).hasSize(1)
    }

    @Test
    fun `Payment URI takes priority over plain address match`() {
        every { blockchainDataProvider.getShareSchemes(bitcoinCoin.network) } returns listOf("bitcoin:")
        every { blockchainDataProvider.validateAddress(bitcoinCoin.network, any()) } returns true

        val qr = "bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa?amount=0.1"
        val result = classifier.classify(qr, listOf(bitcoinCoin))

        assertThat(result).isInstanceOf(ClassifiedQrContent.PaymentUri::class.java)
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