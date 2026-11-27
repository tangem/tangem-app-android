package com.tangem.features.tangempay.withdraw.bank.amount.state

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.tangempay.model.FeeCalculationType
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankWithdrawRailTextTest {

    @Test
    fun `GIVEN destination with a fee name WHEN toTitleReference THEN the catalog name shown`() {
        // Act
        val actual = offrampDestination(feeName = "FedWire").toTitleReference()

        // Assert
        assertThat(actual).isEqualTo(stringReference("FedWire"))
    }

    @ParameterizedTest
    @MethodSource("provideBuiltInLabelModels")
    fun `GIVEN destination without a fee name WHEN toTitleReference THEN the built-in label shown`(
        model: BuiltInLabelModel,
    ) {
        // Act
        val actual = offrampDestination(rail = model.rail).toTitleReference()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN blank fee name WHEN toTitleReference THEN the built-in label shown`() {
        // Act
        val actual = offrampDestination(rail = BankRail.WIRE, feeName = " ").toTitleReference()

        // Assert
        assertThat(actual).isEqualTo(resourceReference(R.string.tangempay_bank_transfer_fee_fedwire))
    }

    @ParameterizedTest
    @MethodSource("provideEtaModels")
    fun `GIVEN rail and delivery days WHEN toEtaReference THEN the rail's delivery time shown`(model: EtaModel) {
        // Arrange
        val destination = offrampDestination(rail = model.rail, minDays = model.minDays, maxDays = model.maxDays)

        // Act
        val actual = destination.toEtaReference()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("provideFeeTextModels")
    fun `GIVEN rail fee and amount WHEN feeText THEN a percentage fee becomes one figure once an amount is entered`(
        model: FeeTextModel,
    ) {
        // Arrange
        val destination = offrampDestination(
            fee = model.railFee,
            calculationType = model.calculationType,
            fixedFeeAmount = model.fixedFeeAmount,
        )

        // Act
        val actual = destination.feeText(amount = model.amount, networkFee = BigDecimal("0.01"))

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("provideTotalFeeModels")
    fun `GIVEN rail and network fees WHEN totalFeeText THEN fixed parts summed and a percentage kept apart`(
        model: TotalFeeModel,
    ) {
        // Arrange
        val destination = offrampDestination(
            fee = model.railFee,
            calculationType = model.calculationType,
            fixedFeeAmount = model.fixedFeeAmount,
        )

        // Act
        val actual = destination.totalFeeText(model.networkFee)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("provideRailTotalFeeModels")
    fun `GIVEN rail WHEN totalFee THEN a route fee goes on top and a SPEI fee comes out of the amount`(
        model: RailTotalFeeModel,
    ) {
        // Act
        val actual = offrampDestination(rail = model.rail, fee = BigDecimal("1.00")).totalFee(BigDecimal("0.03"))

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideEtaModels() = listOf(
        EtaModel(
            rail = BankRail.SPEI,
            minDays = 0,
            maxDays = 0,
            expected = resourceReference(R.string.tangempay_withdraw_bank_eta_within_minutes),
        ),
        EtaModel(
            rail = BankRail.WIRE,
            minDays = 0,
            maxDays = 0,
            expected = resourceReference(R.string.tangempay_withdraw_bank_eta_same_business_day),
        ),
        EtaModel(
            rail = BankRail.SPEI,
            minDays = 1,
            maxDays = 1,
            expected = pluralReference(
                id = R.plurals.tangempay_withdraw_bank_eta_business_days,
                count = 1,
                formatArgs = wrappedList(1),
            ),
        ),
        EtaModel(
            rail = BankRail.ACH,
            minDays = 2,
            maxDays = 2,
            expected = pluralReference(
                id = R.plurals.tangempay_withdraw_bank_eta_business_days,
                count = 2,
                formatArgs = wrappedList(2),
            ),
        ),
        EtaModel(
            rail = BankRail.ACH,
            minDays = 1,
            maxDays = 3,
            expected = resourceReference(R.string.tangempay_order_type_delivery_eta_range, wrappedList(1, 3)),
        ),
    )

    internal data class EtaModel(val rail: BankRail, val minDays: Int, val maxDays: Int, val expected: TextReference)

    private fun provideFeeTextModels() = listOf(
        FeeTextModel(
            railFee = BigDecimal("1"),
            calculationType = FeeCalculationType.PERCENTAGE,
            fixedFeeAmount = BigDecimal("0.50"),
            amount = BigDecimal("500"),
            expected = "$5.51",
        ),
        FeeTextModel(
            railFee = BigDecimal("1"),
            calculationType = FeeCalculationType.PERCENTAGE,
            amount = BigDecimal("500"),
            expected = "$5.01",
        ),
        FeeTextModel(
            railFee = BigDecimal("1"),
            calculationType = FeeCalculationType.PERCENTAGE,
            fixedFeeAmount = BigDecimal("0.50"),
            amount = null,
            expected = "$0.51 + 1%",
        ),
        FeeTextModel(
            railFee = BigDecimal("1"),
            calculationType = FeeCalculationType.PERCENTAGE,
            fixedFeeAmount = BigDecimal("0.50"),
            amount = BigDecimal.ZERO,
            expected = "$0.51 + 1%",
        ),
        FeeTextModel(
            railFee = BigDecimal("1.00"),
            calculationType = FeeCalculationType.FIXED,
            amount = BigDecimal("500"),
            expected = "$1.01",
        ),
    )

    internal data class FeeTextModel(
        val railFee: BigDecimal,
        val calculationType: FeeCalculationType,
        val amount: BigDecimal?,
        val expected: String,
        val fixedFeeAmount: BigDecimal? = null,
    )

    private fun provideRailTotalFeeModels() = listOf(
        RailTotalFeeModel(rail = BankRail.ACH, expected = BigDecimal("1.03")),
        RailTotalFeeModel(rail = BankRail.WIRE, expected = BigDecimal("1.03")),
        RailTotalFeeModel(rail = BankRail.SPEI, expected = BigDecimal("0.03")),
    )

    internal data class RailTotalFeeModel(val rail: BankRail, val expected: BigDecimal)

    private fun provideBuiltInLabelModels() = listOf(
        BuiltInLabelModel(rail = BankRail.ACH, expected = resourceReference(R.string.tangempay_bank_transfer_fee_ach)),
        BuiltInLabelModel(
            rail = BankRail.WIRE,
            expected = resourceReference(R.string.tangempay_bank_transfer_fee_fedwire),
        ),
        BuiltInLabelModel(rail = BankRail.SPEI, expected = stringReference("SPEI")),
    )

    internal data class BuiltInLabelModel(val rail: BankRail, val expected: TextReference)

    private fun provideTotalFeeModels() = listOf(
        TotalFeeModel(railFee = BigDecimal("11.00"), networkFee = BigDecimal.ZERO, expected = "$11"),
        TotalFeeModel(railFee = BigDecimal("1.00"), networkFee = BigDecimal("0.03"), expected = "$1.03"),
        TotalFeeModel(railFee = BigDecimal("1.50"), networkFee = BigDecimal("0.50"), expected = "$2"),
        TotalFeeModel(
            railFee = BigDecimal("1"),
            networkFee = BigDecimal.ZERO,
            expected = "$0.50 + 1%",
            calculationType = FeeCalculationType.PERCENTAGE,
            fixedFeeAmount = BigDecimal("0.50"),
        ),
        TotalFeeModel(
            railFee = BigDecimal("1"),
            networkFee = BigDecimal.ZERO,
            expected = "1%",
            calculationType = FeeCalculationType.PERCENTAGE,
        ),
        TotalFeeModel(
            railFee = BigDecimal("1"),
            networkFee = BigDecimal("0.03"),
            expected = "$0.53 + 1%",
            calculationType = FeeCalculationType.PERCENTAGE,
            fixedFeeAmount = BigDecimal("0.50"),
        ),
        TotalFeeModel(
            railFee = BigDecimal("1"),
            networkFee = BigDecimal("0.03"),
            expected = "$0.03 + 1%",
            calculationType = FeeCalculationType.PERCENTAGE,
        ),
    )

    internal data class TotalFeeModel(
        val railFee: BigDecimal,
        val networkFee: BigDecimal,
        val expected: String,
        val calculationType: FeeCalculationType = FeeCalculationType.FIXED,
        val fixedFeeAmount: BigDecimal? = null,
    )
}