package com.tangem.features.tangempay.withdraw.bank.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.pay.usecase.bankofframp.GetBankAccountsUseCase
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawOptions
import com.tangem.features.tangempay.withdraw.bank.TangemPayBankWithdrawComponent
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawSource
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class TangemPayBankWithdrawModelTest {

    private val userWalletId = UserWalletId("011")
    private val getBankAccountsUseCase: GetBankAccountsUseCase = mockk()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    @BeforeEach
    fun resetMocks() {
        clearMocks(getBankAccountsUseCase)
    }

    @Test
    fun `GIVEN saved entry WHEN model created THEN recipient loaded into draft`() {
        // Arrange
        val saved = bankAccount(id = "acc-2")
        coEvery { getBankAccountsUseCase(userWalletId) } returns listOf(bankAccount(id = "acc-1"), saved).right()

        // Act
        val model = createModel(BankWithdrawEntry.Saved("acc-2"))

        // Assert
        assertThat(model.draft.value).isEqualTo(BankWithdrawDraft(entry = BankWithdrawEntry.Saved("acc-2"), recipient = saved))
        model.onDestroy()
    }

    @Test
    fun `GIVEN saved entry WHEN accounts fail THEN recipient stays null`() {
        // Arrange
        coEvery { getBankAccountsUseCase(userWalletId) } returns BankOfframpError.Unknown(null).left()

        // Act
        val model = createModel(BankWithdrawEntry.Saved("acc-2"))

        // Assert
        assertThat(model.draft.value.recipient).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN new personal entry WHEN model created THEN accounts not requested`() {
        // Act
        val model = createModel(BankWithdrawEntry.NewPersonal)

        // Assert
        coVerify(exactly = 0) { getBankAccountsUseCase(any()) }
        assertThat(model.draft.value).isEqualTo(BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal))
        model.onDestroy()
    }

    @Test
    fun `GIVEN see all entry WHEN recipient selected THEN draft keeps recipient`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.SeeAll)
        val recipient = bankAccount(id = "acc-3")

        // Act
        model.onRecipientSelected(recipient)

        // Assert
        assertThat(model.draft.value)
            .isEqualTo(BankWithdrawDraft(entry = BankWithdrawEntry.SeeAll, recipient = recipient))
        coVerify(exactly = 0) { getBankAccountsUseCase(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN draft WHEN amount confirmed THEN draft keeps amount source and destination`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.NewBusiness)
        val source = bankWithdrawSource()
        val destination = offrampDestination()

        // Act
        model.onAmountConfirmed(
            BankWithdrawAmountResult(amount = BigDecimal("500"), source = source, destination = destination),
        )

        // Assert
        assertThat(model.draft.value).isEqualTo(
            BankWithdrawDraft(
                entry = BankWithdrawEntry.NewBusiness,
                amount = BigDecimal("500"),
                source = source,
                destination = destination,
            ),
        )
        model.onDestroy()
    }

    @Test
    fun `GIVEN new personal entry WHEN transfer type selected THEN draft keeps third party flag`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.NewPersonal)

        // Act
        model.onTransferTypeSelected(isThirdParty = true)

        // Assert
        assertThat(model.draft.value.newRecipient).isEqualTo(NewRecipientDraft(isThirdParty = true))
        model.onDestroy()
    }

    @Test
    fun `GIVEN new personal entry WHEN account type selected THEN draft keeps account type`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.NewPersonal)

        // Act
        model.onAccountTypeSelected(BankAccountType.SAVINGS)

        // Assert
        assertThat(model.draft.value.newRecipient).isEqualTo(NewRecipientDraft(accountType = BankAccountType.SAVINGS))
        model.onDestroy()
    }

    @Test
    fun `GIVEN transfer type selected WHEN account type selected THEN draft keeps both answers`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.NewPersonal)
        model.onTransferTypeSelected(isThirdParty = true)

        // Act
        model.onAccountTypeSelected(BankAccountType.SAVINGS)

        // Assert
        assertThat(model.draft.value.newRecipient)
            .isEqualTo(NewRecipientDraft(isThirdParty = true, accountType = BankAccountType.SAVINGS))
        model.onDestroy()
    }

    @Test
    fun `GIVEN filled recipient details WHEN bank details filled THEN draft merges both forms`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.NewPersonal)
        model.onDetailsFilled(mapOf(BankWithdrawFormField.AccountNumber to "4830019271"))

        // Act
        model.onDetailsFilled(mapOf(BankWithdrawFormField.BankName to "Test Bank"))

        // Assert
        assertThat(model.draft.value.newRecipient.fields).containsExactly(
            BankWithdrawFormField.AccountNumber, "4830019271",
            BankWithdrawFormField.BankName, "Test Bank",
        )
        model.onDestroy()
    }

    @Test
    fun `GIVEN draft WHEN order created THEN draft keeps order`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.NewPersonal)
        val order = BankOfframpOrderResult(orderId = "order-1", bankAccountId = "acc-9")

        // Act
        model.onOrderCreated(order)

        // Assert
        assertThat(model.draft.value.order).isEqualTo(order)
        model.onDestroy()
    }

    private fun createModel(entry: BankWithdrawEntry): TangemPayBankWithdrawModel = TangemPayBankWithdrawModel(
        paramsContainer = MutableParamsContainer(
            TangemPayBankWithdrawComponent.Params(
                userWalletId = userWalletId,
                entry = entry,
                options = MutableStateFlow(bankWithdrawOptions()),
            ),
        ),
        dispatchers = dispatchers,
        getBankAccountsUseCase = getBankAccountsUseCase,
    )
}