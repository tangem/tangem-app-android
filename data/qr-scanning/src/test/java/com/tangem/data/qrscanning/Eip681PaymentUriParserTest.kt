package com.tangem.data.qrscanning

import com.google.common.truth.Truth.assertThat
import com.tangem.data.qrscanning.parser.Eip681PaymentUriParser
import com.tangem.data.qrscanning.parser.PaymentUriParser
import com.tangem.data.qrscanning.parser.QrContentClassifierParser
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.qrscanning.models.ClassifiedQrContent
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.math.BigDecimal

internal class Eip681PaymentUriParserTest {

    private val blockchainDataProvider = mockk<QrContentClassifierParser.BlockchainDataProvider> {
        every { getChainId(any()) } returns null
        every { getBlockchainNameByChainId(any()) } returns null
        every { validateAddress(any(), any()) } returns true
    }
    private val parser = Eip681PaymentUriParser(blockchainDataProvider)

    // region Native transfer

    @Test
    fun `native transfer with chain_id and value`() {
        every { blockchainDataProvider.getChainId(ethereumCoin.network) } returns 1L

        val result = parser.parse(
            qrCode = "ethereum:0xRecipient@1?value=1500000000000000000",
            coins = listOf(ethereumCoin),
            allCurrencies = listOf(ethereumCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.address).isEqualTo("0xRecipient")
        assertThat(result.amount!!.compareTo(BigDecimal("1.5"))).isEqualTo(0)
        assertThat(result.memo).isNull()
        assertThat(result.matchingCurrencies).containsExactly(ethereumCoin)
    }

    @Test
    fun `native transfer without value`() {
        every { blockchainDataProvider.getChainId(ethereumCoin.network) } returns 1L

        val result = parser.parse(
            qrCode = "ethereum:0xRecipient@1",
            coins = listOf(ethereumCoin),
            allCurrencies = listOf(ethereumCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.address).isEqualTo("0xRecipient")
        assertThat(result.amount).isNull()
    }

    @Test
    fun `native transfer without chain_id falls back to chainId presence check`() {
        every { blockchainDataProvider.getChainId(ethereumCoin.network) } returns 1L

        val result = parser.parse(
            qrCode = "ethereum:0xRecipient?value=1000000000000000000",
            coins = listOf(ethereumCoin),
            allCurrencies = listOf(ethereumCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.address).isEqualTo("0xRecipient")
        assertThat(result.amount!!.compareTo(BigDecimal("1"))).isEqualTo(0)
    }

    @Test
    fun `native transfer with value returns only coins`() {
        every { blockchainDataProvider.getChainId(ethereumCoin.network) } returns 1L

        val usdcToken = buildToken("ethereum", "USDC", "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48")

        val result = parser.parse(
            qrCode = "ethereum:0xRecipient@1?value=1000000000000000000",
            coins = listOf(ethereumCoin),
            allCurrencies = listOf(ethereumCoin, usdcToken),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.matchingCurrencies).containsExactly(ethereumCoin)
    }

    @Test
    fun `native transfer without value returns all currencies on matching network`() {
        every { blockchainDataProvider.getChainId(ethereumCoin.network) } returns 1L

        val usdcToken = buildToken("ethereum", "USDC", "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48")

        val result = parser.parse(
            qrCode = "ethereum:0xRecipient@1",
            coins = listOf(ethereumCoin),
            allCurrencies = listOf(ethereumCoin, usdcToken),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.matchingCurrencies).containsExactly(ethereumCoin, usdcToken)
    }

    // endregion

    // region ERC-20 transfer

    @Test
    fun `ERC-20 transfer with contract, recipient and amount`() {
        every { blockchainDataProvider.getChainId(ethereumCoin.network) } returns 1L

        val usdcToken = buildToken("ethereum", "USDC", "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48")

        val result = parser.parse(
            qrCode = "ethereum:0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48@1/transfer?address=0xRecipient&uint256=1000000",
            coins = listOf(ethereumCoin),
            allCurrencies = listOf(ethereumCoin, usdcToken),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.address).isEqualTo("0xRecipient")
        assertThat(result.amount!!.compareTo(BigDecimal("1"))).isEqualTo(0)
        assertThat(result.matchingCurrencies).containsExactly(usdcToken)
    }

    @Test
    fun `ERC-20 transfer without amount`() {
        every { blockchainDataProvider.getChainId(ethereumCoin.network) } returns 1L

        val usdcToken = buildToken("ethereum", "USDC", "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48")

        val result = parser.parse(
            qrCode = "ethereum:0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48@1/transfer?address=0xRecipient",
            coins = listOf(ethereumCoin),
            allCurrencies = listOf(ethereumCoin, usdcToken),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.address).isEqualTo("0xRecipient")
        assertThat(result.amount).isNull()
        assertThat(result.matchingCurrencies).containsExactly(usdcToken)
    }

    @Test
    fun `ERC-20 transfer with unknown token returns UnsupportedNetwork error`() {
        every { blockchainDataProvider.getChainId(ethereumCoin.network) } returns 1L

        val result = parser.parse(
            qrCode = "ethereum:0xUnknownContract@1/transfer?address=0xRecipient&uint256=1000000",
            coins = listOf(ethereumCoin),
            allCurrencies = listOf(ethereumCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.RecognizedError::class.java)
        val error = (result as PaymentUriParser.ParseResult.RecognizedError).error
        assertThat(error).isInstanceOf(ClassifiedQrContent.Error.UnsupportedNetwork::class.java)
    }

    @Test
    fun `ERC-20 transfer without address param returns Unrecognized error`() {
        every { blockchainDataProvider.getChainId(ethereumCoin.network) } returns 1L

        val usdcToken = buildToken("ethereum", "USDC", "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48")

        val result = parser.parse(
            qrCode = "ethereum:0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48@1/transfer?uint256=1000000",
            coins = listOf(ethereumCoin),
            allCurrencies = listOf(ethereumCoin, usdcToken),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.RecognizedError::class.java)
        val error = (result as PaymentUriParser.ParseResult.RecognizedError).error
        assertThat(error).isInstanceOf(ClassifiedQrContent.Error.Unrecognized::class.java)
    }

    @Test
    fun `ERC-20 transfer with invalid recipient address returns Unrecognized error`() {
        every { blockchainDataProvider.getChainId(ethereumCoin.network) } returns 1L
        every { blockchainDataProvider.validateAddress(any(), eq("0xInvalidRecipient")) } returns false

        val usdcToken = buildToken("ethereum", "USDC", "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48")

        val result = parser.parse(
            qrCode = "ethereum:0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48@1/transfer?address=0xInvalidRecipient&uint256=1000000",
            coins = listOf(ethereumCoin),
            allCurrencies = listOf(ethereumCoin, usdcToken),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.RecognizedError::class.java)
        val error = (result as PaymentUriParser.ParseResult.RecognizedError).error
        assertThat(error).isInstanceOf(ClassifiedQrContent.Error.Unrecognized::class.java)
    }

    // endregion

    @Test
    fun `ERC-20 transfer with chainId as query param`() {
        every { blockchainDataProvider.getChainId(ethereumCoin.network) } returns 1L

        val usdtToken = buildToken("ethereum", "USDT", "0xdAC17F958D2ee523a2206206994597C13D831ec7")

        val result = parser.parse(
            qrCode = "ethereum:0xdAC17F958D2ee523a2206206994597C13D831ec7/transfer?address=0x3D709aC89d780312677519c3AfC13f390C819531&uint256=30000000&chainId=1",
            coins = listOf(ethereumCoin),
            allCurrencies = listOf(ethereumCoin, usdtToken),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.address).isEqualTo("0x3D709aC89d780312677519c3AfC13f390C819531")
        assertThat(result.amount!!.compareTo(BigDecimal("30"))).isEqualTo(0)
        assertThat(result.matchingCurrencies).containsExactly(usdtToken)
    }

    // region Chain ID matching

    @Test
    fun `chain_id mismatch returns UnsupportedNetwork error`() {
        every { blockchainDataProvider.getChainId(ethereumCoin.network) } returns 1L

        val result = parser.parse(
            qrCode = "ethereum:0xRecipient@137?value=1000",
            coins = listOf(ethereumCoin),
            allCurrencies = listOf(ethereumCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.RecognizedError::class.java)
        val error = (result as PaymentUriParser.ParseResult.RecognizedError).error
        assertThat(error).isInstanceOf(ClassifiedQrContent.Error.UnsupportedNetwork::class.java)
    }

    @Test
    fun `chain_id matches correct network among multiple`() {
        every { blockchainDataProvider.getChainId(ethereumCoin.network) } returns 1L
        every { blockchainDataProvider.getChainId(polygonCoin.network) } returns 137L

        val result = parser.parse(
            qrCode = "ethereum:0xRecipient@137?value=1000000000000000000",
            coins = listOf(ethereumCoin, polygonCoin),
            allCurrencies = listOf(ethereumCoin, polygonCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.matchingCurrencies).containsExactly(polygonCoin)
    }

    // endregion

    // region Non-ethereum schemes

    @Test
    fun `non-ethereum scheme returns NotRecognized`() {
        val result = parser.parse(
            qrCode = "bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa?amount=0.5",
            coins = listOf(bitcoinCoin),
            allCurrencies = listOf(bitcoinCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.NotRecognized::class.java)
    }

    @Test
    fun `empty qr code returns NotRecognized`() {
        val result = parser.parse(
            qrCode = "",
            coins = listOf(ethereumCoin),
            allCurrencies = listOf(ethereumCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.NotRecognized::class.java)
    }

    @Test
    fun `ethereum scheme with blank address returns NotRecognized`() {
        val result = parser.parse(
            qrCode = "ethereum:?value=1000",
            coins = listOf(ethereumCoin),
            allCurrencies = listOf(ethereumCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.NotRecognized::class.java)
    }

    // endregion

    // region Unsupported params

    @Test
    fun `native transfer with unknown param returns SuccessWithWarning`() {
        every { blockchainDataProvider.getChainId(ethereumCoin.network) } returns 1L

        val result = parser.parse(
            qrCode = "ethereum:0xRecipient@1?value=1000000000000000000&gasLimit=21000",
            coins = listOf(ethereumCoin),
            allCurrencies = listOf(ethereumCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.SuccessWithWarning::class.java)
        val warning = result as PaymentUriParser.ParseResult.SuccessWithWarning
        assertThat(warning.content.address).isEqualTo("0xRecipient")
        assertThat(warning.unsupportedParams).containsEntry("gasLimit", "21000")
    }

    @Test
    fun `ERC-20 transfer with unknown param returns SuccessWithWarning`() {
        every { blockchainDataProvider.getChainId(ethereumCoin.network) } returns 1L

        val usdcToken = buildToken("ethereum", "USDC", "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48")

        val result = parser.parse(
            qrCode = "ethereum:0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48@1/transfer?address=0xRecipient&uint256=1000000&gasPrice=20000000000",
            coins = listOf(ethereumCoin),
            allCurrencies = listOf(ethereumCoin, usdcToken),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.SuccessWithWarning::class.java)
        val warning = result as PaymentUriParser.ParseResult.SuccessWithWarning
        assertThat(warning.unsupportedParams).containsEntry("gasPrice", "20000000000")
    }

    @Test
    fun `native transfer with only known params returns Success`() {
        every { blockchainDataProvider.getChainId(ethereumCoin.network) } returns 1L

        val result = parser.parse(
            qrCode = "ethereum:0xRecipient@1?value=1000000000000000000",
            coins = listOf(ethereumCoin),
            allCurrencies = listOf(ethereumCoin),
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

    private val bitcoinCoin = buildCoin("bitcoin", decimals = 8)
    private val ethereumCoin = buildCoin("ethereum", decimals = 18)
    private val polygonCoin = buildCoin("polygon", decimals = 18)

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