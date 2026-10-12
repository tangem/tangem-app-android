package com.tangem.features.tangempay.withdraw.bank.success.state

import android.text.format.DateFormat
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawSource
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.NewRecipientDraft
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import com.tangem.features.tangempay.withdraw.bank.personalFormValues
import com.tangem.features.tangempay.withdraw.bank.success.state.TangemPayBankWithdrawSuccessUM.RowUM
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.collections.immutable.persistentListOf
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

private const val DATE_PATTERN = "MMM d, yyyy, h:mm a"
private const val FORMATTED_DATE = "May 2, 2028, 9:41 AM"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayBankWithdrawSuccessUMFactoryTest {

    private val defaultLocale = Locale.getDefault()
    private val defaultZone: DateTimeZone = DateTimeZone.getDefault()
    private val onCloseClick: () -> Unit = {}
    private val shownAt = DateTime(2028, 5, 2, 9, 41, DateTimeZone.UTC).millis
    private val businessFormValues = personalFormValues -
        BankWithdrawFormField.FirstName -
        BankWithdrawFormField.LastName +
        (BankWithdrawFormField.BusinessName to "Arasaka Corporation")

    @BeforeEach
    fun setUp() {
        Locale.setDefault(Locale.US)
        DateTimeZone.setDefault(DateTimeZone.UTC)
        mockkStatic(DateFormat::class)
        every { DateFormat.getBestDateTimePattern(any(), any()) } returns DATE_PATTERN
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(DateFormat::class)
        DateTimeZone.setDefault(defaultZone)
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `GIVEN saved recipient WHEN built THEN recipient, bank name and account number rows in order`() {
        // Act
        val actual = create(savedDraft())

        // Assert
        assertThat(actual).isEqualTo(
            TangemPayBankWithdrawSuccessUM(
                title = resourceReference(R.string.tangempay_withdraw_bank_success_title, wrappedList("$500")),
                subtitle = stringReference(FORMATTED_DATE),
                rows = persistentListOf(
                    RowUM(resourceReference(R.string.send_recipient), "Andrei Melnikov"),
                    RowUM(resourceReference(R.string.tangempay_withdraw_bank_field_bank_name), "Test Bank"),
                    RowUM(resourceReference(R.string.tangempay_withdraw_bank_field_account_number), "0000000000"),
                ),
                onCloseClick = onCloseClick,
            ),
        )
    }

    @Test
    fun `GIVEN details with a blank bank name WHEN built THEN the bank row is dropped`() {
        // Arrange
        val recipient = bankAccount()
        val draft = savedDraft().copy(recipient = recipient.withBankName(" "))

        // Act
        val actual = create(draft)

        // Assert
        assertThat(actual.rows).containsExactly(
            RowUM(resourceReference(R.string.send_recipient), "Andrei Melnikov"),
            RowUM(resourceReference(R.string.tangempay_withdraw_bank_field_account_number), "0000000000"),
        ).inOrder()
    }

    @Test
    fun `GIVEN a fixed instant WHEN built THEN subtitle is the formatted date and time`() {
        // Act
        val actual = create(savedDraft())

        // Assert
        assertThat(actual.subtitle).isEqualTo(stringReference(FORMATTED_DATE))
    }

    @Test
    fun `GIVEN draft without amount WHEN built THEN title has no amount`() {
        // Act
        val actual = create(savedDraft().copy(amount = null))

        // Assert
        assertThat(actual.title).isEqualTo(resourceReference(R.string.common_sending))
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN recipient WHEN built THEN recipient row carries their full name`(model: RecipientModel) {
        // Act
        val actual = create(model.draft)

        // Assert
        assertThat(actual.rows.first())
            .isEqualTo(RowUM(resourceReference(R.string.send_recipient), model.expectedName))
    }

    private fun provideTestModels() = listOf(
        RecipientModel(
            draft = savedDraft(),
            expectedName = "Andrei Melnikov",
            description = "saved individual -> display name",
        ),
        RecipientModel(
            draft = savedDraft().copy(recipient = bankAccount(beneficiary = Beneficiary.Business("Arasaka"))),
            expectedName = "Arasaka",
            description = "saved business -> display name",
        ),
        RecipientModel(
            draft = newRecipientDraft(),
            expectedName = "Saburo Arasaka",
            description = "new individual -> first and last name",
        ),
        RecipientModel(
            draft = newRecipientDraft(entry = BankWithdrawEntry.NewBusiness, fields = businessFormValues),
            expectedName = "Arasaka Corporation",
            description = "new business -> registered name",
        ),
    )

    internal class RecipientModel(
        val draft: BankWithdrawDraft,
        val expectedName: String,
        private val description: String,
    ) {
        override fun toString(): String = description
    }

    private fun create(draft: BankWithdrawDraft) = TangemPayBankWithdrawSuccessUMFactory.create(
        draft = draft,
        shownAt = shownAt,
        onCloseClick = onCloseClick,
    )

    private fun BankAccount.withBankName(bankName: String) = copy(details = details.copy(bankName = bankName))

    private fun savedDraft() = BankWithdrawDraft(
        entry = BankWithdrawEntry.Saved("acc-1"),
        recipient = bankAccount(id = "acc-1"),
        amount = BigDecimal("500"),
        source = bankWithdrawSource(),
        destination = offrampDestination(),
    )

    private fun newRecipientDraft(
        entry: BankWithdrawEntry = BankWithdrawEntry.NewPersonal,
        fields: Map<BankWithdrawFormField, String> = personalFormValues,
    ) = BankWithdrawDraft(
        entry = entry,
        recipient = null,
        newRecipient = NewRecipientDraft(
            isThirdParty = false,
            accountType = BankAccountType.CHECKING,
            fields = fields,
        ),
        amount = BigDecimal("500"),
        source = bankWithdrawSource(),
        destination = offrampDestination(),
    )
}