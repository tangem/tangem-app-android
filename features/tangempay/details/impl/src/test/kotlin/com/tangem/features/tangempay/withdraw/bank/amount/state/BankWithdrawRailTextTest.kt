package com.tangem.features.tangempay.withdraw.bank.amount.state

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.OfframpEta
import com.tangem.domain.pay.model.bankofframp.OfframpFee
import com.tangem.features.tangempay.details.impl.R
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class BankWithdrawRailTextTest {

    @Test
    fun `GIVEN rails WHEN toTitleReference THEN existing onramp fee labels reused`() {
        // Act & Assert
        assertThat(BankRail.ACH.toTitleReference()).isEqualTo(resourceReference(R.string.tangempay_bank_transfer_fee_ach))
        assertThat(BankRail.WIRE.toTitleReference())
            .isEqualTo(resourceReference(R.string.tangempay_bank_transfer_fee_fedwire))
    }

    @Test
    fun `GIVEN zero days WHEN toTextReference THEN same business day`() {
        // Act
        val actual = OfframpEta(minDays = 0, maxDays = 0).toTextReference()

        // Assert
        assertThat(actual).isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_eta_same_business_day))
    }

    @Test
    fun `GIVEN equal days WHEN toTextReference THEN plural`() {
        // Act
        val actual = OfframpEta(minDays = 2, maxDays = 2).toTextReference()

        // Assert
        assertThat(actual).isEqualTo(
            pluralReference(R.plurals.tangempay_withdraw_bank_eta_business_days, count = 2, formatArgs = wrappedList(2)),
        )
    }

    @Test
    fun `GIVEN range WHEN toTextReference THEN range string`() {
        // Act
        val actual = OfframpEta(minDays = 1, maxDays = 3).toTextReference()

        // Assert
        assertThat(actual).isEqualTo(resourceReference(R.string.tangempay_order_type_delivery_eta_range, wrappedList(1, 3)))
    }

    @Test
    fun `GIVEN whole fee WHEN toText THEN no decimals`() {
        // Act
        val actual = OfframpFee(amount = BigDecimal("11.00"), currency = "USD", description = null).toText()

        // Assert
        assertThat(actual).isEqualTo("$11")
    }
}