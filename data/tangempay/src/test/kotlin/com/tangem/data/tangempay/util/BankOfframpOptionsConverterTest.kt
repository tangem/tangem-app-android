package com.tangem.data.tangempay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.tangempay.model.AccountFee
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.model.bankofframp.OfframpDestination
import com.tangem.domain.tangempay.model.bankofframp.OfframpEta
import com.tangem.domain.tangempay.model.bankofframp.OfframpFee
import com.tangem.domain.tangempay.model.bankofframp.OfframpSource
import com.tangem.domain.tangempay.model.bankofframp.OfframpToken
import com.tangem.spend.datasource.tangempay.models.response.BankOfframpOptionsResponse
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
                currency = "USD",
                fee = OfframpFee(
                    name = "ACH",
                    calculationType = AccountFee.CalculationType.FIXED,
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
                currency = "USD",
                fee = OfframpFee(
                    name = "Wire",
                    calculationType = AccountFee.CalculationType.FIXED,
                    amount = BigDecimal("1.50"),
                    currency = "USD",
                    description = "ACH offramp fee",
                ),
                eta = OfframpEta(minDays = 1, maxDays = 3),
            ),
        )
    }

    @Test
    fun `GIVEN SPEI destination in MXN with a percentage fee WHEN convert THEN SPEI destination kept`() {
        // Arrange
        val spei = destination(
            rail = "SPEI",
            currency = "MXN",
            feeName = "SPEI",
            calculationType = "PERCENTAGE",
            feeDescription = "SPEI offramp fee",
        )

        // Act
        val actual = BankOfframpOptionsConverter.convert(result(destinations = listOf(destination(), spei)))

        // Assert
        assertThat(actual.destinations).containsExactly(
            OfframpDestination(
                rail = BankRail.ACH,
                currency = "USD",
                fee = OfframpFee(
                    name = "ACH",
                    calculationType = AccountFee.CalculationType.FIXED,
                    amount = BigDecimal("1.50"),
                    currency = "USD",
                    description = "ACH offramp fee",
                ),
                eta = OfframpEta(minDays = 1, maxDays = 3),
            ),
            OfframpDestination(
                rail = BankRail.SPEI,
                currency = "MXN",
                fee = OfframpFee(
                    name = "SPEI",
                    calculationType = AccountFee.CalculationType.PERCENTAGE,
                    amount = BigDecimal("1.50"),
                    currency = "USD",
                    description = "SPEI offramp fee",
                ),
                eta = OfframpEta(minDays = 1, maxDays = 3),
            ),
        ).inOrder()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN fee calculation type WHEN convert THEN known types mapped and unknown destination dropped`(
        model: CalculationTypeModel,
    ) {
        // Arrange
        val destinations = listOf(destination(calculationType = model.calculationType))

        // Act
        val actual = BankOfframpOptionsConverter.convert(result(destinations = destinations))

        // Assert
        assertThat(actual.destinations.map { it.fee.calculationType }).isEqualTo(listOfNotNull(model.expected))
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["", " "])
    fun `GIVEN destination without currency WHEN convert THEN destination kept without currency`(currency: String?) {
        // Act
        val actual = BankOfframpOptionsConverter.convert(
            result(destinations = listOf(destination(currency = currency))),
        )

        // Assert
        assertThat(actual.destinations.single().currency).isNull()
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
        currency: String? = "USD",
        feeName: String? = "ACH",
        calculationType: String? = "FIXED",
        feeDescription: String? = "ACH offramp fee",
        hasFee: Boolean = true,
        hasEta: Boolean = true,
    ) = BankOfframpOptionsResponse.Destination(
        rail = rail,
        currency = currency,
        fee = BankOfframpOptionsResponse.Fee(
            name = feeName,
            calculationType = calculationType,
            amount = BigDecimal("1.50"),
            currency = "USD",
            description = feeDescription,
        ).takeIf { hasFee },
        eta = BankOfframpOptionsResponse.Eta(minDays = 1, maxDays = 3).takeIf { hasEta },
    )

    private fun provideTestModels() = listOf(
        CalculationTypeModel(calculationType = "FIXED", expected = AccountFee.CalculationType.FIXED),
        CalculationTypeModel(calculationType = "percentage", expected = AccountFee.CalculationType.PERCENTAGE),
        CalculationTypeModel(calculationType = null, expected = AccountFee.CalculationType.FIXED),
        CalculationTypeModel(calculationType = "TIERED", expected = null),
    )

    internal data class CalculationTypeModel(
        val calculationType: String?,
        val expected: AccountFee.CalculationType?,
    )

    private companion object {
        const val CONTRACT = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359"
    }
}