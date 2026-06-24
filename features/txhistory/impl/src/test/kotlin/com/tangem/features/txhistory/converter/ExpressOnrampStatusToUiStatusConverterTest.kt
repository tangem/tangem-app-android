package com.tangem.features.txhistory.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.domain.express.models.ExpressOnrampStatus
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExpressOnrampStatusToUiStatusConverterTest {

    private val converter = ExpressOnrampStatusToUiStatusConverter()

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: Model) {
        // Act
        val actual = converter.convert(model.status)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN every onramp status WHEN listing test models THEN all enum entries are covered`() {
        // Asserts the mapping table below stays in lockstep with the enum, so a newly added status
        // can never silently fall through with an untested UI bucket.
        val covered = provideTestModels().map { it.status }.toSet()

        assertThat(covered).containsExactlyElementsIn(ExpressOnrampStatus.entries)
    }

    private fun provideTestModels() = listOf(
        Model(status = ExpressOnrampStatus.Finished, expected = Status.Confirmed),
        Model(status = ExpressOnrampStatus.Failed, expected = Status.Failed),
        Model(status = ExpressOnrampStatus.Expired, expected = Status.Failed),
        Model(status = ExpressOnrampStatus.Unknown, expected = Status.Failed),
        Model(status = ExpressOnrampStatus.Created, expected = Status.Unconfirmed),
        Model(status = ExpressOnrampStatus.WaitingForPayment, expected = Status.Unconfirmed),
        Model(status = ExpressOnrampStatus.PaymentProcessing, expected = Status.Unconfirmed),
        Model(status = ExpressOnrampStatus.Verifying, expected = Status.Unconfirmed),
        Model(status = ExpressOnrampStatus.Paid, expected = Status.Unconfirmed),
        Model(status = ExpressOnrampStatus.Sending, expected = Status.Unconfirmed),
        Model(status = ExpressOnrampStatus.Paused, expected = Status.Unconfirmed),
    )

    internal data class Model(val status: ExpressOnrampStatus, val expected: Status)
}