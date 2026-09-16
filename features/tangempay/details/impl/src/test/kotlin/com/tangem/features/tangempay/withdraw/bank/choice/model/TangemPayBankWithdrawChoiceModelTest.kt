package com.tangem.features.tangempay.withdraw.bank.choice.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.choice.TangemPayBankWithdrawChoiceComponent
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderUM
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TangemPayBankWithdrawChoiceModelTest {

    private val router: Router = mockk(relaxed = true)
    private val onContinue: (String) -> Unit = mockk(relaxed = true)
    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val draft = MutableStateFlow(BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal))

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, onContinue)
        draft.value = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal)
    }

    @Test
    fun `GIVEN transfer type step WHEN model created THEN header caption and preselected option`() {
        // Act
        val model = createModel(
            config = BankWithdrawChoices.transferType(isThirdParty = false),
            step = TangemPayBankWithdrawInnerRoute.TransferType,
        )

        // Assert
        val state = model.uiState.value
        assertThat(state.header).isEqualTo(
            BankWithdrawHeaderUM(
                title = resourceReference(R.string.tangempay_withdraw_hub_to_personal_account),
                subtitle = resourceReference(R.string.tangempay_withdraw_bank_step_counter, wrappedList(2, 5)),
            ),
        )
        assertThat(state.sectionTitle).isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_transfer_type_title))
        assertThat(state.options.map { it.isSelected }).containsExactly(true, false).inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN model WHEN other option clicked THEN selection moves`() {
        // Arrange
        val model = createModel(
            config = BankWithdrawChoices.transferType(isThirdParty = false),
            step = TangemPayBankWithdrawInnerRoute.TransferType,
        )

        // Act
        model.uiState.value.options[1].onClick()

        // Assert
        assertThat(model.uiState.value.options.map { it.isSelected }).containsExactly(false, true).inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN option clicked WHEN continue clicked THEN selected id reported`() {
        // Arrange
        val config = BankWithdrawChoices.accountType(selected = BankAccountType.CHECKING)
        val model = createModel(config = config, step = TangemPayBankWithdrawInnerRoute.AccountType)
        model.uiState.value.options[1].onClick()

        // Act
        model.uiState.value.onContinueClick()

        // Assert
        verify(exactly = 1) { onContinue(config.options[1].id) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN account type step and business draft WHEN model created THEN select account type title 2 of 4`() {
        // Arrange
        draft.value = BankWithdrawDraft(entry = BankWithdrawEntry.NewBusiness)

        // Act
        val model = createModel(
            config = BankWithdrawChoices.accountType(selected = BankAccountType.CHECKING),
            step = TangemPayBankWithdrawInnerRoute.AccountType,
        )

        // Assert
        assertThat(model.uiState.value.header).isEqualTo(
            BankWithdrawHeaderUM(
                title = resourceReference(R.string.tangempay_withdraw_bank_account_type_screen_title),
                subtitle = resourceReference(R.string.tangempay_withdraw_bank_step_counter, wrappedList(2, 4)),
            ),
        )
        model.onDestroy()
    }

    @Test
    fun `GIVEN model WHEN back clicked THEN router pops`() {
        // Arrange
        val model = createModel(
            config = BankWithdrawChoices.transferType(isThirdParty = false),
            step = TangemPayBankWithdrawInnerRoute.TransferType,
        )

        // Act
        model.uiState.value.onBackClick()

        // Assert
        verify(exactly = 1) { router.pop() }
        model.onDestroy()
    }

    private fun createModel(
        config: BankWithdrawChoiceConfig,
        step: TangemPayBankWithdrawInnerRoute,
    ): TangemPayBankWithdrawChoiceModel = TangemPayBankWithdrawChoiceModel(
        paramsContainer = MutableParamsContainer(
            TangemPayBankWithdrawChoiceComponent.Params(
                draft = draft,
                step = step,
                config = config,
                onContinue = onContinue,
            ),
        ),
        dispatchers = dispatchers,
        router = router,
    )
}