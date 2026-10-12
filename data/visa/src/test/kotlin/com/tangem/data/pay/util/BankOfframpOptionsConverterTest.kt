package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.OfframpDestination
import com.tangem.domain.pay.model.bankofframp.OfframpEta
import com.tangem.domain.pay.model.bankofframp.OfframpFee
import com.tangem.domain.pay.model.bankofframp.OfframpSource
import com.tangem.domain.pay.model.bankofframp.OfframpToken
import com.tangem.spend.datasource.pay.models.response.BankOfframpOptionsResponse
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class BankOfframpOptionsConverterTest {

    @Test
    fun `GIVEN full options WHEN convert THEN sources and destinations mapped`() {
        // Act
        val actual = BankOfframpOptionsConverter.convert(result())

        // Assert
        assertThat(actual.sources).containsExactly(
            OfframpSource(
                networkName = "polygon-pos",
                isTestnet = false,
                chainId = 137L,
                tokens = listOf(OfframpToken(symbol = "USDC", contractAddress = CONTRACT)),
            ),
        )
        assertThat(actual.destinations).containsExactly(
            OfframpDestination(
                rail = BankRail.ACH,
                fee = OfframpFee(
                    name = "ACH",
                    amount = BigDecimal("1.50"),
                    currency = "USD",
                    description = "ACH offramp fee",
                ),
                eta = OfframpEta(minDays = 1, maxDays = 3),
            ),
        )
    }

    @Test
    fun `GIVEN lowercase rail WHEN convert THEN rail recognised`() {
        // Act
        val actual = BankOfframpOptionsConverter.convert(result(destinations = listOf(destination(rail = "wire"))))

        // Assert
        assertThat(actual.destinations.single().rail).isEqualTo(BankRail.WIRE)
    }

    @Test
    fun `GIVEN FEDWIRE rail WHEN convert THEN mapped to WIRE`() {
        // Act
        val actual = BankOfframpOptionsConverter.convert(result(destinations = listOf(destination(rail = "FEDWIRE"))))

        // Assert
        assertThat(actual.destinations.single().rail).isEqualTo(BankRail.WIRE)
    }

    @Test
    fun `GIVEN WIRE and FEDWIRE destinations WHEN convert THEN a single wire destination kept`() {
        // Arrange
        val destinations = listOf(destination(rail = "WIRE", feeName = "Wire"), destination(rail = "FEDWIRE"))

        // Act
        val actual = BankOfframpOptionsConverter.convert(result(destinations = destinations))

        // Assert
        assertThat(actual.destinations).containsExactly(
            OfframpDestination(
                rail = BankRail.WIRE,
                fee = OfframpFee(
                    name = "Wire",
                    amount = BigDecimal("1.50"),
                    currency = "USD",
                    description = "ACH offramp fee",
                ),
                eta = OfframpEta(minDays = 1, maxDays = 3),
            ),
        )
    }

    @Test
    fun `GIVEN blank fee name WHEN convert THEN fee name is null`() {
        // Act
        val actual = BankOfframpOptionsConverter.convert(result(destinations = listOf(destination(feeName = " "))))

        // Assert
        assertThat(actual.destinations.single().fee.name).isNull()
    }

    @Test
    fun `GIVEN unknown rail WHEN convert THEN destination dropped`() {
        // Act
        val actual = BankOfframpOptionsConverter.convert(result(destinations = listOf(destination(rail = "SEPA"))))

        // Assert
        assertThat(actual.destinations).isEmpty()
    }

    @Test
    fun `GIVEN destination without fee or eta WHEN convert THEN destination dropped`() {
        // Arrange
        val destinations = listOf(destination(hasFee = false), destination(hasEta = false))

        // Act
        val actual = BankOfframpOptionsConverter.convert(result(destinations = destinations))

        // Assert
        assertThat(actual.destinations).isEmpty()
    }

    @Test
    fun `GIVEN source whose tokens all lack a contract address WHEN convert THEN source dropped`() {
        // Arrange
        val source = source(tokens = listOf(BankOfframpOptionsResponse.Token(symbol = "USDC", contractAddress = null)))

        // Act
        val actual = BankOfframpOptionsConverter.convert(result(sources = listOf(source)))

        // Assert
        assertThat(actual.sources).isEmpty()
    }

    @Test
    fun `GIVEN source without chain id WHEN convert THEN source dropped`() {
        // Act
        val actual = BankOfframpOptionsConverter.convert(result(sources = listOf(source(chainId = null))))

        // Assert
        assertThat(actual.sources).isEmpty()
    }

    @Test
    fun `GIVEN empty result WHEN convert THEN empty options`() {
        // Act
        val actual = BankOfframpOptionsConverter.convert(BankOfframpOptionsResponse.Result(null, null))

        // Assert
        assertThat(actual.sources).isEmpty()
        assertThat(actual.destinations).isEmpty()
    }

    private fun result(
        sources: List<BankOfframpOptionsResponse.Source> = listOf(source()),
        destinations: List<BankOfframpOptionsResponse.Destination> = listOf(destination()),
    ) = BankOfframpOptionsResponse.Result(sources = sources, destinations = destinations)

    private fun source(
        chainId: Long? = 137L,
        tokens: List<BankOfframpOptionsResponse.Token> = listOf(
            BankOfframpOptionsResponse.Token(symbol = "USDC", contractAddress = CONTRACT),
        ),
    ) = BankOfframpOptionsResponse.Source(
        name = "polygon-pos",
        isTestnet = false,
        chainId = chainId,
        tokens = tokens,
    )

    private fun destination(
        rail: String? = "ACH",
        feeName: String? = "ACH",
        hasFee: Boolean = true,
        hasEta: Boolean = true,
    ) = BankOfframpOptionsResponse.Destination(
        rail = rail,
        fee = BankOfframpOptionsResponse.Fee(
            name = feeName,
            calculationType = "FIXED",
            amount = BigDecimal("1.50"),
            currency = "USD",
            description = "ACH offramp fee",
        ).takeIf { hasFee },
        eta = BankOfframpOptionsResponse.Eta(minDays = 1, maxDays = 3).takeIf { hasEta },
    )

    private companion object {
        const val CONTRACT = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359"
    }
}