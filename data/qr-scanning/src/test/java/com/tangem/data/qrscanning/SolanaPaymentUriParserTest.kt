package com.tangem.data.qrscanning

import com.google.common.truth.Truth.assertThat
import com.tangem.data.qrscanning.parser.PaymentUriParser
import com.tangem.data.qrscanning.parser.SolanaPaymentUriParser
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.qrscanning.models.ClassifiedQrContent
import org.junit.Test
import java.math.BigDecimal

internal class SolanaPaymentUriParserTest {

    private val parser = SolanaPaymentUriParser()

    // region Scheme matching

    @Test
    fun `solana URI recognized`() {
        val result = parser.parse(
            qrCode = "solana:SolAddress",
            coins = listOf(solanaCoin),
            allCurrencies = listOf(solanaCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.address).isEqualTo("SolAddress")
    }

    @Test
    fun `non-solana URI returns NotRecognized`() {
        val result = parser.parse(
            qrCode = "bitcoin:1Address",
            coins = listOf(solanaCoin),
            allCurrencies = listOf(solanaCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.NotRecognized::class.java)
    }

    @Test
    fun `case insensitive scheme`() {
        val result = parser.parse(
            qrCode = "Solana:SolAddress",
            coins = listOf(solanaCoin),
            allCurrencies = listOf(solanaCoin),
        ).asSuccess()

        assertThat(result).isNotNull()
    }

    // endregion

    // region Native transfer

    @Test
    fun `native transfer with amount`() {
        val result = parser.parse(
            qrCode = "solana:SolAddress?amount=2.5",
            coins = listOf(solanaCoin),
            allCurrencies = listOf(solanaCoin, usdcToken),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.amount!!.compareTo(BigDecimal("2.5"))).isEqualTo(0)
        assertThat(result.matchingCurrencies).containsExactly(solanaCoin)
    }

    @Test
    fun `no amount returns all currencies on network`() {
        val result = parser.parse(
            qrCode = "solana:SolAddress",
            coins = listOf(solanaCoin),
            allCurrencies = listOf(solanaCoin, usdcToken),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.amount).isNull()
        assertThat(result.matchingCurrencies).containsExactly(solanaCoin, usdcToken)
    }

    // endregion

    // region SPL token transfer

    @Test
    fun `spl-token param resolves to matching token`() {
        val result = parser.parse(
            qrCode = "solana:SolAddress?spl-token=EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v&amount=10",
            coins = listOf(solanaCoin),
            allCurrencies = listOf(solanaCoin, usdcToken),
        ).asSuccess()

        assertThat(result).isNotNull()
        assertThat(result!!.address).isEqualTo("SolAddress")
        assertThat(result.amount!!.compareTo(BigDecimal("10"))).isEqualTo(0)
        assertThat(result.matchingCurrencies).containsExactly(usdcToken)
    }

    @Test
    fun `spl-token not found returns UnsupportedNetwork`() {
        val result = parser.parse(
            qrCode = "solana:SolAddress?spl-token=UnknownMint",
            coins = listOf(solanaCoin),
            allCurrencies = listOf(solanaCoin),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.RecognizedError::class.java)
    }

    // endregion

    // region Unsupported network

    @Test
    fun `no matching solana coin returns UnsupportedNetwork`() {
        val result = parser.parse(
            qrCode = "solana:SolAddress",
            coins = emptyList(),
            allCurrencies = emptyList(),
        )

        assertThat(result).isInstanceOf(PaymentUriParser.ParseResult.RecognizedError::class.java)
    }

    // endregion

    // region Helpers

    private fun PaymentUriParser.ParseResult.asSuccess(): ClassifiedQrContent.PaymentUri? {
        return (this as? PaymentUriParser.ParseResult.Success)?.content
    }

    private val solanaNetwork = buildNetwork("SOLANA", "Solana", "SOL")

    private val solanaCoin = CryptoCurrency.Coin(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId("SOLANA"),
            suffix = CryptoCurrency.ID.Suffix.RawID("SOLANA"),
        ),
        network = solanaNetwork,
        name = "Solana",
        symbol = "SOL",
        decimals = 9,
        iconUrl = null,
        isCustom = false,
    )

    private val usdcToken = CryptoCurrency.Token(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId("SOLANA"),
            suffix = CryptoCurrency.ID.Suffix.RawID("EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"),
        ),
        network = solanaNetwork,
        name = "USD Coin",
        symbol = "USDC",
        decimals = 6,
        iconUrl = null,
        isCustom = false,
        contractAddress = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
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