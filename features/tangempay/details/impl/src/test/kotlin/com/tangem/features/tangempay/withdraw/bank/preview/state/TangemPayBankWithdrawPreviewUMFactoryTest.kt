package com.tangem.features.tangempay.withdraw.bank.preview.state

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawSource
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.NewRecipientDraft
import com.tangem.features.tangempay.withdraw.bank.model.toFormValues
import com.tangem.features.tangempay.withdraw.bank.navigation.BankWithdrawEditTarget
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import com.tangem.features.tangempay.withdraw.bank.personalFormValues
import com.tangem.features.tangempay.withdraw.bank.preview.model.PreviewFailure
import com.tangem.features.tangempay.withdraw.bank.preview.model.PreviewState
import com.tangem.features.tangempay.withdraw.bank.preview.model.SendState
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.DetailRowUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.Failed
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.RecipientCardUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.Summary
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

private const val FORM_VALUES_KT = "com.tangem.features.tangempay.withdraw.bank.model.BankAccountDetailsFormValuesKt"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayBankWithdrawPreviewUMFactoryTest {

    private val edited = mutableListOf<BankWithdrawEditTarget>()
    private var railClicks = 0
    private val callbacks = TangemPayBankWithdrawPreviewUMFactory.Callbacks(
        onBackClick = {},
        onCloseClick = {},
        onSendClick = {},
        onRetryClick = {},
        onEditClick = { edited += it },
        onRailClick = { railClicks++ },
    )
    private val factory = TangemPayBankWithdrawPreviewUMFactory(callbacks)

    @BeforeEach
    fun resetState() {
        edited.clear()
        railClicks = 0
    }

    @Test
    fun `GIVEN saved individual recipient WHEN create THEN transfer summary and three recipient cards`() {
        // Act
        val actual = factory.create(draft = savedDraft(), state = PreviewState()) as Summary

        // Assert
        val transfer = requireNotNull(actual.transfer)
        assertThat(actual.header.title).isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_confirm_title))
        assertThat(transfer.amount.amount).isEqualTo("$500")
        assertThat(transfer.amount.balance).isEqualTo("$2,325.55")
        assertThat(transfer.amount.symbol).isEqualTo("USDC")
        assertThat(transfer.route.rail).isEqualTo(BankRail.ACH)
        assertThat(transfer.route.fee).isEqualTo("$1")
        assertThat(transfer.sendingSummary).isEqualTo(
            resourceReference(
                R.string.tangempay_withdraw_bank_sending_summary,
                wrappedList("$501", resourceReference(R.string.tangempay_bank_transfer_fee_ach), "$1"),
            ),
        )
        assertThat(actual.recipientCards.map { card -> card.rows.map { it.label } }).containsExactly(
            listOf(BankWithdrawFormField.AccountNumber.label, BankWithdrawFormField.RoutingNumber.label),
            listOf(
                BankWithdrawFormField.FirstName.label,
                BankWithdrawFormField.LastName.label,
                BankWithdrawFormField.Country.label,
                BankWithdrawFormField.Region.label,
                BankWithdrawFormField.City.label,
                BankWithdrawFormField.AddressLine1.label,
                BankWithdrawFormField.PostalCode.label,
            ),
            listOf(
                BankWithdrawFormField.BankName.label,
                BankWithdrawFormField.BankRegion.label,
                BankWithdrawFormField.BankCity.label,
                BankWithdrawFormField.BankAddressLine1.label,
                BankWithdrawFormField.BankPostalCode.label,
            ),
        ).inOrder()
        assertThat(actual.recipientCards[1].rows.map { it.value })
            .containsExactly("Andrei", "Melnikov", "US", "NY", "New York", "1 Main St", "10001")
            .inOrder()
        assertThat(actual.isSendEnabled).isTrue()
        assertThat(actual.isSending).isFalse()
    }

    @Test
    fun `GIVEN rail named by the catalog WHEN create THEN that name in the route and the summary`() {
        // Arrange
        val draft = savedDraft().copy(destination = offrampDestination(rail = BankRail.WIRE, feeName = "FedWire"))

        // Act
        val actual = factory.create(draft = draft, state = PreviewState()) as Summary

        // Assert
        val transfer = requireNotNull(actual.transfer)
        assertThat(transfer.route.title).isEqualTo(stringReference("FedWire"))
        assertThat(transfer.sendingSummary).isEqualTo(
            resourceReference(
                R.string.tangempay_withdraw_bank_sending_summary,
                wrappedList("$501", stringReference("FedWire"), "$1"),
            ),
        )
    }

    @Test
    fun `GIVEN business recipient WHEN create THEN person card starts with business name`() {
        // Arrange
        val draft = savedDraft().copy(recipient = bankAccount(beneficiary = Beneficiary.Business(name = "Arasaka")))

        // Act
        val actual = factory.create(draft = draft, state = PreviewState()) as Summary

        // Assert
        assertThat(actual.recipientCards[1].rows.first())
            .isEqualTo(TangemPayBankWithdrawPreviewUM.DetailRowUM(BankWithdrawFormField.BusinessName.label, "Arasaka"))
        assertThat(actual.recipientCards[1].rows).hasSize(6)
    }

    @Test
    fun `GIVEN recipient details map missing a field WHEN create THEN that row is dropped without throwing`() {
        // Arrange
        val recipient = bankAccount(id = "acc-1")
        val details = recipient.details
        val valuesMissingCity = details.toFormValues() - BankWithdrawFormField.City
        val draft = savedDraft().copy(recipient = recipient)

        // Act
        val actual = try {
            mockkStatic(FORM_VALUES_KT)
            every { details.toFormValues() } returns valuesMissingCity
            factory.create(draft = draft, state = PreviewState()) as Summary
        } finally {
            unmockkStatic(FORM_VALUES_KT)
        }

        // Assert
        assertThat(actual.recipientCards[1].rows.map { it.label }).doesNotContain(BankWithdrawFormField.City.label)
        assertThat(actual.recipientCards[1].rows).hasSize(6)
    }

    @Test
    fun `GIVEN saved recipient without a region WHEN create THEN the blank region row is dropped`() {
        // Arrange
        val recipient = bankAccount(id = "acc-1")
        val details = recipient.details
        val valuesWithoutRegion = details.toFormValues() + (BankWithdrawFormField.Region to "")
        val draft = savedDraft().copy(recipient = recipient)

        // Act
        val actual = try {
            mockkStatic(FORM_VALUES_KT)
            every { details.toFormValues() } returns valuesWithoutRegion
            factory.create(draft = draft, state = PreviewState()) as Summary
        } finally {
            unmockkStatic(FORM_VALUES_KT)
        }

        // Assert
        assertThat(actual.recipientCards[1].rows.map { it.label }).doesNotContain(BankWithdrawFormField.Region.label)
        assertThat(actual.recipientCards[1].rows).hasSize(6)
    }

    @Test
    fun `GIVEN saved recipient without bank details WHEN create THEN no empty card shown`() {
        // Arrange
        val recipient = bankAccount(id = "acc-1").let { account ->
            account.copy(details = account.details.copy(bankName = null, bankAddress = null))
        }

        // Act
        val actual = factory.create(draft = savedDraft().copy(recipient = recipient), state = PreviewState()) as Summary

        // Assert
        assertThat(actual.recipientCards).containsExactly(
            RecipientCardUM(
                rows = persistentListOf(
                    DetailRowUM(BankWithdrawFormField.AccountNumber.label, "0000000000"),
                    DetailRowUM(BankWithdrawFormField.RoutingNumber.label, "021000021"),
                ),
                onClick = null,
            ),
            RecipientCardUM(
                rows = persistentListOf(
                    DetailRowUM(BankWithdrawFormField.FirstName.label, "Andrei"),
                    DetailRowUM(BankWithdrawFormField.LastName.label, "Melnikov"),
                    DetailRowUM(BankWithdrawFormField.Country.label, "US"),
                    DetailRowUM(BankWithdrawFormField.Region.label, "NY"),
                    DetailRowUM(BankWithdrawFormField.City.label, "New York"),
                    DetailRowUM(BankWithdrawFormField.AddressLine1.label, "1 Main St"),
                    DetailRowUM(BankWithdrawFormField.PostalCode.label, "10001"),
                ),
                onClick = null,
            ),
        ).inOrder()
    }

    @Test
    fun `GIVEN recipient not loaded WHEN create THEN no recipient cards and send disabled`() {
        // Act
        val actual = factory.create(draft = savedDraft().copy(recipient = null), state = PreviewState()) as Summary

        // Assert
        assertThat(actual.transfer).isNotNull()
        assertThat(actual.recipientCards).isEmpty()
        assertThat(actual.isSendEnabled).isFalse()
    }

    @Test
    fun `GIVEN draft without amount step data WHEN create THEN no transfer and send disabled`() {
        // Act
        val actual = factory.create(
            draft = BankWithdrawDraft(entry = BankWithdrawEntry.Saved("acc-1"), recipient = bankAccount()),
            state = PreviewState(),
        ) as Summary

        // Assert
        assertThat(actual.transfer).isNull()
        assertThat(actual.recipientCards).hasSize(3)
        assertThat(actual.isSendEnabled).isFalse()
    }

    @Test
    fun `GIVEN sending WHEN create THEN summary is sending and send still enabled`() {
        // Act
        val actual = factory.create(draft = savedDraft(), state = PreviewState(send = SendState.CreatingOrder)) as Summary

        // Assert
        assertThat(actual.isSending).isTrue()
        assertThat(actual.isSendEnabled).isTrue()
    }

    @Test
    fun `GIVEN general failure WHEN create THEN error result with retry`() {
        // Act
        val actual = factory.create(draft = savedDraft(), state = PreviewState(failure = PreviewFailure.General))

        // Assert
        assertThat(actual).isEqualTo(
            Failed(
                title = resourceReference(R.string.tangempay_withdraw_bank_error_general_title),
                icon = TangemStatusIcon.Kind.Warning,
                onCloseClick = callbacks.onCloseClick,
                onRetryClick = callbacks.onRetryClick,
            ),
        )
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN unsupported failure WHEN create THEN unavailable result without retry`(model: UnavailableModel) {
        // Act
        val actual = factory.create(draft = savedDraft(), state = PreviewState(failure = model.failure))

        // Assert
        assertThat(actual).isEqualTo(
            Failed(
                title = resourceReference(model.titleRes),
                icon = TangemStatusIcon.Kind.HeartBroken,
                onCloseClick = callbacks.onCloseClick,
                onRetryClick = null,
            ),
        )
    }

    private fun provideTestModels() = listOf(
        UnavailableModel(PreviewFailure.RecipientRegion, R.string.tangempay_withdraw_bank_error_recipient_region),
        UnavailableModel(PreviewFailure.SenderRegion, R.string.tangempay_withdraw_bank_error_sender_region),
        UnavailableModel(PreviewFailure.Business, R.string.tangempay_withdraw_bank_error_business),
        UnavailableModel(
            failure = PreviewFailure.UnsupportedConfiguration,
            titleRes = R.string.tangempay_withdraw_bank_error_region_title,
        ),
    )

    internal data class UnavailableModel(val failure: PreviewFailure, val titleRes: Int)

    @Test
    fun `GIVEN new recipient WHEN create THEN amount and every recipient card is tappable`() {
        // Act
        val actual = factory.create(draft = newRecipientDraft(), state = PreviewState()) as Summary

        // Assert
        assertThat(actual.transfer?.amount?.onClick).isNotNull()
        assertThat(actual.recipientCards.map { it.onClick != null }).containsExactly(true, true, true)
    }

    @Test
    fun `GIVEN new recipient WHEN card tapped THEN edit target matches the card`() {
        // Arrange
        val actual = factory.create(draft = newRecipientDraft(), state = PreviewState()) as Summary

        // Act
        actual.transfer?.amount?.onClick?.invoke()
        actual.recipientCards.forEach { card -> card.onClick?.invoke() }

        // Assert
        assertThat(edited).containsExactly(
            BankWithdrawEditTarget.Amount,
            BankWithdrawEditTarget.RecipientDetails,
            BankWithdrawEditTarget.RecipientDetails,
            BankWithdrawEditTarget.BankDetails,
        ).inOrder()
    }

    @Test
    fun `GIVEN new recipient not sending WHEN rail row tapped THEN the rail callback fires`() {
        // Arrange
        val actual = factory.create(draft = newRecipientDraft(), state = PreviewState()) as Summary

        // Act
        actual.transfer?.route?.onClick?.invoke()

        // Assert
        assertThat(railClicks).isEqualTo(1)
        assertThat(edited).isEmpty()
    }

    @Test
    fun `GIVEN sending WHEN create THEN the rail row is read-only`() {
        // Arrange
        val state = PreviewState(send = SendState.CreatingOrder)

        // Act
        val actual = factory.create(draft = newRecipientDraft(), state = state) as Summary

        // Assert
        assertThat(actual.transfer?.route?.onClick).isNull()
    }

    @Test
    fun `GIVEN saved recipient not sending WHEN create THEN the rail row is read-only`() {
        // Act
        val actual = factory.create(draft = savedDraft(), state = PreviewState()) as Summary

        // Assert
        assertThat(actual.transfer?.route?.onClick).isNull()
    }

    @Test
    fun `GIVEN saved recipient WHEN create THEN the recipient cards are read-only`() {
        // Act
        val actual = factory.create(draft = savedDraft(), state = PreviewState()) as Summary

        // Assert
        assertThat(actual.transfer?.amount?.onClick).isNotNull()
        assertThat(actual.recipientCards.map { it.onClick }).containsExactly(null, null, null)
    }

    @Test
    fun `GIVEN sending WHEN create THEN amount and every recipient card is read-only`() {
        // Act
        val actual = factory.create(draft = newRecipientDraft(), state = PreviewState(send = SendState.CreatingOrder)) as Summary

        // Assert
        assertThat(actual.transfer?.amount?.onClick).isNull()
        assertThat(actual.recipientCards.map { it.onClick }).containsExactly(null, null, null)
    }

    @Test
    fun `GIVEN new recipient with names WHEN create THEN recipient card shows names`() {
        // Arrange
        val draft = newRecipientDraft(
            displayNames = mapOf(
                BankWithdrawFormField.Country to "United States",
                BankWithdrawFormField.Region to "California",
                BankWithdrawFormField.BankRegion to "California",
            ),
        )

        // Act
        val actual = factory.create(draft = draft, state = PreviewState()) as Summary

        // Assert
        assertThat(actual.recipientCards[1].rows.map { it.value })
            .containsExactly(
                "Saburo",
                "Arasaka",
                "United States",
                "California",
                "Night City",
                "Megatower H10, 322F",
                "90001",
            )
            .inOrder()
        assertThat(actual.recipientCards[2].rows.map { it.value })
            .containsExactly("Arasaka Bank", "California", "Night City", "Ebunike st., 32A", "90002")
            .inOrder()
    }

    @Test
    fun `GIVEN saved recipient WHEN create THEN recipient card shows codes`() {
        // Act
        val actual = factory.create(draft = savedDraft(), state = PreviewState()) as Summary

        // Assert
        assertThat(actual.recipientCards[1].rows.map { it.value })
            .containsExactly("Andrei", "Melnikov", "US", "NY", "New York", "1 Main St", "10001")
            .inOrder()
        assertThat(actual.recipientCards[2].rows.map { it.value })
            .containsExactly("Test Bank", "NY", "New York", "1 Main St", "10001")
            .inOrder()
    }

    @Test
    fun `GIVEN new recipient with a typed region WHEN create THEN the typed text shown`() {
        // Arrange
        val draft = newRecipientDraft(
            fields = personalFormValues + mapOf(
                BankWithdrawFormField.Country to "MC",
                BankWithdrawFormField.Region to "Bavaria",
            ),
            displayNames = mapOf(
                BankWithdrawFormField.Country to "Monaco",
                BankWithdrawFormField.Region to "",
            ),
        )

        // Act
        val actual = factory.create(draft = draft, state = PreviewState()) as Summary

        // Assert
        assertThat(actual.recipientCards[1].rows.map { it.value })
            .containsExactly("Saburo", "Arasaka", "Monaco", "Bavaria", "Night City", "Megatower H10, 322F", "90001")
            .inOrder()
    }

    @Test
    fun `GIVEN cold wallet WHEN preview created THEN send button carries the Tangem icon`() {
        // Act
        val actual = factory.create(draft = savedDraft(), state = PreviewState(isColdWallet = true)) as Summary

        // Assert
        assertThat(actual.isColdWallet).isTrue()
    }

    @Test
    fun `GIVEN hot wallet WHEN preview created THEN send button has no icon`() {
        // Act
        val actual = factory.create(draft = savedDraft(), state = PreviewState(isColdWallet = false)) as Summary

        // Assert
        assertThat(actual.isColdWallet).isFalse()
    }

    private fun savedDraft() = BankWithdrawDraft(
        entry = BankWithdrawEntry.Saved("acc-1"),
        recipient = bankAccount(id = "acc-1"),
        amount = BigDecimal("500"),
        source = bankWithdrawSource(),
        destination = offrampDestination(),
    )

    private fun newRecipientDraft(
        fields: Map<BankWithdrawFormField, String> = personalFormValues,
        displayNames: Map<BankWithdrawFormField, String> = emptyMap(),
    ) = BankWithdrawDraft(
        entry = BankWithdrawEntry.NewPersonal,
        recipient = null,
        newRecipient = NewRecipientDraft(
            isThirdParty = false,
            accountType = BankAccountType.CHECKING,
            fields = fields,
            displayNames = displayNames,
        ),
        amount = BigDecimal("500"),
        source = bankWithdrawSource(),
        destination = offrampDestination(),
    )
}