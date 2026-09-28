package com.tangem.features.tangempay.withdraw.bank.choice.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.choice.TangemPayBankWithdrawChoiceComponent
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderUM
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class TangemPayBankWithdrawChoiceModelTest {

    private val router: Router = mockk(relaxed = true)
    private val onContinue: (String) -> Unit = mockk(relaxed = true)
    private val onCloseFlow: () -> Unit = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val draft = MutableStateFlow(BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal))

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, onContinue, onCloseFlow, analytics)
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
                onCloseClick = onCloseFlow,
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
                onCloseClick = onCloseFlow,
            ),
        )
        model.onDestroy()
    }

    @Test
    fun `GIVEN no preselection WHEN model created THEN no option selected and continue disabled`() {
        // Act
        val model = createModel(
            config = BankWithdrawChoices.transferType(isThirdParty = null),
            step = TangemPayBankWithdrawInnerRoute.TransferType,
        )

        // Assert
        val state = model.uiState.value
        assertThat(state.options.map { it.isSelected }).containsExactly(false, false).inOrder()
        assertThat(state.isContinueEnabled).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN no preselection WHEN continue clicked THEN nothing reported`() {
        // Arrange
        val model = createModel(
            config = BankWithdrawChoices.accountType(selected = null),
            step = TangemPayBankWithdrawInnerRoute.AccountType,
        )

        // Act
        model.uiState.value.onContinueClick()

        // Assert
        verify(exactly = 0) { onContinue(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN option clicked WHEN model updates THEN continue enabled`() {
        // Arrange
        val model = createModel(
            config = BankWithdrawChoices.accountType(selected = null),
            step = TangemPayBankWithdrawInnerRoute.AccountType,
        )

        // Act
        model.uiState.value.options[1].onClick()

        // Assert
        assertThat(model.uiState.value.isContinueEnabled).isTrue()
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

    @Test
    fun `GIVEN transfer type step WHEN model created THEN PersonalAccountTypeScreenOpened is sent`() {
        // Act
        val model = createModel(
            config = BankWithdrawChoices.transferType(isThirdParty = null),
            step = TangemPayBankWithdrawInnerRoute.TransferType,
        )

        // Assert
        verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.PersonalAccountTypeScreenOpened()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN account type step WHEN model created THEN no offramp event is sent`() {
        // Act
        val model = createModel(
            config = BankWithdrawChoices.accountType(selected = null),
            step = TangemPayBankWithdrawInnerRoute.AccountType,
        )

        // Assert
        verify(exactly = 0) { analytics.send(any()) }
        model.onDestroy()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ContinueClicked {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN transfer type selection WHEN continue clicked THEN PersonalAccountTypeContinueClicked is sent`(
            testModel: ContinueClickedModel,
        ) {
            // Arrange
            val model = createModel(
                config = BankWithdrawChoices.transferType(isThirdParty = null),
                step = TangemPayBankWithdrawInnerRoute.TransferType,
            )
            model.uiState.value.options[testModel.optionIndex].onClick()

            // Act
            model.uiState.value.onContinueClick()

            // Assert
            verify(exactly = 1) {
                analytics.send(
                    TangemPayAnalyticsEvents.Offramp.PersonalAccountTypeContinueClicked(
                        isThirdParty = testModel.isThirdParty,
                    ),
                )
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN no selection WHEN continue clicked THEN no continue event is sent`() {
            // Arrange
            val model = createModel(
                config = BankWithdrawChoices.transferType(isThirdParty = null),
                step = TangemPayBankWithdrawInnerRoute.TransferType,
            )

            // Act
            model.uiState.value.onContinueClick()

            // Assert
            verify(exactly = 0) {
                analytics.send(ofType<TangemPayAnalyticsEvents.Offramp.PersonalAccountTypeContinueClicked>())
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN account type step WHEN continue clicked THEN no offramp event is sent`() {
            // Arrange
            val model = createModel(
                config = BankWithdrawChoices.accountType(selected = null),
                step = TangemPayBankWithdrawInnerRoute.AccountType,
            )
            model.uiState.value.options[1].onClick()

            // Act
            model.uiState.value.onContinueClick()

            // Assert
            verify(exactly = 0) { analytics.send(any()) }
            model.onDestroy()
        }

        private fun provideTestModels() = listOf(
            ContinueClickedModel(optionIndex = 0, isThirdParty = false),
            ContinueClickedModel(optionIndex = 1, isThirdParty = true),
        )
    }

    internal data class ContinueClickedModel(val optionIndex: Int, val isThirdParty: Boolean)

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
                onCloseFlow = onCloseFlow,
            ),
        ),
        dispatchers = dispatchers,
        router = router,
        analytics = analytics,
    )
}