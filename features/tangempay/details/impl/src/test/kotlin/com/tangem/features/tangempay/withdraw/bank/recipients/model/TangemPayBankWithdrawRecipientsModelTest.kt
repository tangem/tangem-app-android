package com.tangem.features.tangempay.withdraw.bank.recipients.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.BottomSheetMessage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.domain.pay.usecase.bankofframp.GetBankAccountsUseCase
import com.tangem.domain.pay.usecase.bankofframp.MarkBankAccountUsedUseCase
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.recipients.TangemPayBankWithdrawRecipientsComponent
import com.tangem.features.tangempay.withdraw.bank.recipients.state.TangemPayBankWithdrawRecipientsUM.ContentUM
import com.tangem.features.tangempay.withdraw.bank.recipients.state.TangemPayBankWithdrawRecipientsUM.RecipientUM
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TangemPayBankWithdrawRecipientsModelTest {

    private val userWalletId = UserWalletId("011")
    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val getBankAccountsUseCase: GetBankAccountsUseCase = mockk()
    private val markBankAccountUsedUseCase: MarkBankAccountUsedUseCase = mockk(relaxed = true)
    private val onRecipientSelected: (BankAccount) -> Unit = mockk(relaxed = true)
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val basit = bankAccount(
        id = "acc-1",
        beneficiary = Beneficiary.Individual(firstName = "Basit", lastName = "Didenko"),
        accountNumber = "4830019271",
    )
    private val raxanax = bankAccount(
        id = "acc-2",
        beneficiary = Beneficiary.Business(name = "Raxanax"),
        accountNumber = "1120938475",
    )
    private val saburo = bankAccount(
        id = "acc-3",
        beneficiary = Beneficiary.Individual(firstName = "Saburo", lastName = "Arasaka"),
        accountNumber = "7719004432",
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, uiMessageSender, getBankAccountsUseCase, markBankAccountUsedUseCase, onRecipientSelected)
        coEvery { getBankAccountsUseCase(userWalletId) } returns listOf(basit, raxanax, saburo).right()
    }

    @Test
    fun `GIVEN accounts not loaded yet WHEN model created THEN content loading and search inactive`() {
        // Arrange
        val pending = CompletableDeferred<List<BankAccount>>()
        coEvery { getBankAccountsUseCase(userWalletId) } coAnswers { pending.await().right() }

        // Act
        val model = createModel()

        // Assert
        val state = model.uiState.value
        assertThat(state.content).isEqualTo(ContentUM.Loading)
        assertThat(state.search.query).isEmpty()
        assertThat(state.search.isActive).isFalse()
        assertThat(state.search.placeholderText).isEqualTo(resourceReference(R.string.common_search))
        model.onDestroy()
    }

    @Test
    fun `GIVEN accounts WHEN loaded THEN recipients keep use case order with initial name and account number`() {
        // Act
        val model = createModel()

        // Assert
        val items = (model.uiState.value.content as ContentUM.Recipients).items
        assertThat(items.map { it.id }).containsExactly("acc-1", "acc-2", "acc-3").inOrder()
        assertThat(items[0]).isEqualTo(
            RecipientUM(
                id = "acc-1",
                initial = "B",
                name = "Basit Didenko",
                subtitle = stringReference("4830019271"),
                onClick = items[0].onClick,
            ),
        )
        assertThat(items[1]).isEqualTo(
            RecipientUM(
                id = "acc-2",
                initial = "R",
                name = "Raxanax",
                subtitle = combinedReference(
                    stringReference("1120938475 · "),
                    resourceReference(R.string.tangempay_withdraw_bank_recipient_business),
                ),
                onClick = items[1].onClick,
            ),
        )
        model.onDestroy()
    }

    @Test
    fun `GIVEN accounts WHEN query typed THEN only matching recipients shown`() {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.search.onQueryChange("sab")

        // Assert
        val state = model.uiState.value
        assertThat(state.search.query).isEqualTo("sab")
        assertThat((state.content as ContentUM.Recipients).items.map { it.id }).containsExactly("acc-3")
        model.onDestroy()
    }

    @Test
    fun `GIVEN accounts WHEN query matches nothing THEN no results`() {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.search.onQueryChange("zzz")

        // Assert
        assertThat(model.uiState.value.content).isEqualTo(ContentUM.NoResults)
        model.onDestroy()
    }

    @Test
    fun `GIVEN active search with query WHEN close clicked THEN query cleared and search inactive`() {
        // Arrange
        val model = createModel()
        model.uiState.value.search.onActiveChange(true)
        model.uiState.value.search.onQueryChange("zzz")

        // Act
        requireNotNull(model.uiState.value.search.onCloseClick).invoke()

        // Assert
        val state = model.uiState.value
        assertThat(state.search.query).isEmpty()
        assertThat(state.search.isActive).isFalse()
        assertThat(state.content).isInstanceOf(ContentUM.Recipients::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN accounts fail to load WHEN model created THEN error message shown and step closed`() {
        // Arrange
        coEvery { getBankAccountsUseCase(userWalletId) } returns BankOfframpError.Unknown(null).left()

        // Act
        val model = createModel()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(any<BottomSheetMessage>()) }
        verify(exactly = 1) { router.pop() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN recipients WHEN recipient clicked THEN account marked used then selection reported`() {
        // Arrange
        val model = createModel()

        // Act
        (model.uiState.value.content as ContentUM.Recipients).items.first().onClick()

        // Assert
        coVerifyOrder {
            markBankAccountUsedUseCase(userWalletId = userWalletId, bankAccountId = "acc-1")
            onRecipientSelected(basit)
        }
        coVerify(exactly = 1) { markBankAccountUsedUseCase(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN model WHEN back clicked THEN router pops`() {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.onBackClick()

        // Assert
        verify(exactly = 1) { router.pop() }
        model.onDestroy()
    }

    private fun createModel(): TangemPayBankWithdrawRecipientsModel = TangemPayBankWithdrawRecipientsModel(
        paramsContainer = MutableParamsContainer(
            TangemPayBankWithdrawRecipientsComponent.Params(
                userWalletId = userWalletId,
                onRecipientSelected = onRecipientSelected,
            ),
        ),
        dispatchers = dispatchers,
        router = router,
        uiMessageSender = uiMessageSender,
        getBankAccountsUseCase = getBankAccountsUseCase,
        markBankAccountUsedUseCase = markBankAccountUsedUseCase,
    )
}