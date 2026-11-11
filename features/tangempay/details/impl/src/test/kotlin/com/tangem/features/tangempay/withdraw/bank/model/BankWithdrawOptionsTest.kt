package com.tangem.features.tangempay.withdraw.bank.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.tangempay.model.AmountLimits
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.model.bankofframp.OfframpDestination
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawOptions
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankWithdrawOptionsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN rail limits and the account rule WHEN amountLimitsFor THEN rail limits only when followed`(
        model: LimitsModel,
    ) {
        // Arrange
        val options = bankWithdrawOptions(isLimitedPerRail = model.isLimitedPerRail)
        val destination = offrampDestination(amountLimits = model.railLimits)

        // Act
        val actual = options.amountLimitsFor(destination)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("provideDestinationModels")
    fun `GIVEN recipient rail WHEN destinationsFor THEN a CLABE takes SPEI only and a US account all but SPEI`(
        model: DestinationsModel,
    ) {
        // Arrange
        val options = bankWithdrawOptions(destinations = model.offered)

        // Act
        val actual = options.destinationsFor(model.recipientRail)

        // Assert
        assertThat(actual).containsExactlyElementsIn(model.expected).inOrder()
    }

    @ParameterizedTest
    @MethodSource("provideDestinationModels")
    fun `GIVEN recipient rail WHEN canPay THEN true only when a rail is left for it`(model: DestinationsModel) {
        // Arrange
        val options = bankWithdrawOptions(destinations = model.offered)

        // Act
        val actual = options.canPay(bankAccount(rail = model.recipientRail))

        // Assert
        assertThat(actual).isEqualTo(model.expected.isNotEmpty())
    }

    private fun provideDestinationModels() = listOf(
        DestinationsModel(recipientRail = BankRail.SPEI, offered = listOf(ACH, WIRE, SPEI), expected = listOf(SPEI)),
        DestinationsModel(recipientRail = BankRail.ACH, offered = listOf(ACH, WIRE, SPEI), expected = listOf(ACH, WIRE)),
        DestinationsModel(recipientRail = null, offered = listOf(SPEI, ACH), expected = listOf(ACH)),
        DestinationsModel(recipientRail = BankRail.SPEI, offered = listOf(ACH, WIRE), expected = emptyList()),
        DestinationsModel(recipientRail = BankRail.ACH, offered = listOf(SPEI), expected = emptyList()),
    )

    data class DestinationsModel(
        val recipientRail: BankRail?,
        val offered: List<OfframpDestination>,
        val expected: List<OfframpDestination>,
    )

    private fun provideTestModels() = listOf(
        LimitsModel(isLimitedPerRail = true, railLimits = MONEY_ACCOUNT_LIMITS, expected = MONEY_ACCOUNT_LIMITS),
        LimitsModel(isLimitedPerRail = true, railLimits = null, expected = DEFAULT_LIMITS),
        LimitsModel(
            isLimitedPerRail = true,
            railLimits = MONEY_ACCOUNT_LIMITS.copy(currency = "usd"),
            expected = MONEY_ACCOUNT_LIMITS.copy(currency = "usd"),
        ),
        LimitsModel(
            isLimitedPerRail = true,
            railLimits = MONEY_ACCOUNT_LIMITS.copy(currency = "MXN"),
            expected = DEFAULT_LIMITS,
        ),
        LimitsModel(isLimitedPerRail = false, railLimits = MONEY_ACCOUNT_LIMITS, expected = DEFAULT_LIMITS),
    )

    data class LimitsModel(
        val isLimitedPerRail: Boolean,
        val railLimits: AmountLimits?,
        val expected: AmountLimits,
    )

    private companion object {
        val ACH = offrampDestination(rail = BankRail.ACH)
        val WIRE = offrampDestination(rail = BankRail.WIRE)
        val SPEI = offrampDestination(rail = BankRail.SPEI)
        val MONEY_ACCOUNT_LIMITS = AmountLimits(min = BigDecimal("10"), max = null, currency = "USD")
        val DEFAULT_LIMITS = AmountLimits(min = BigDecimal("1.00"), max = BigDecimal("10000.00"), currency = "USD")
    }
}