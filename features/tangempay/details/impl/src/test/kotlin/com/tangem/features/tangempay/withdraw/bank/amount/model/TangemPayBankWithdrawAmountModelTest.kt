package com.tangem.features.tangempay.withdraw.bank.amount.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BASE_CHAIN_ID
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.USDC_BASE
import com.tangem.features.tangempay.withdraw.bank.amount.TangemPayBankWithdrawAmountComponent
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.SheetUM
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawOptions
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawSource
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawAmountResult
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawOptions
import com.tangem.features.tangempay.withdraw.bank.navigation.BankWithdrawEditTarget
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import com.tangem.features.tangempay.withdraw.bank.paymentToken
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Locale

internal class TangemPayBankWithdrawAmountModelTest {

    private val router: Router = mockk(relaxed = true)
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val usdcPolygon = paymentToken()
    private val usdcBase = paymentToken(networkId = "base", networkName = "Base", contractAddress = USDC_BASE)
    private val baseSource = bankWithdrawSource(
        token = usdcBase,
        chainId = BASE_CHAIN_ID,
        fiatBalance = BigDecimal("12325.55"),
    )
    private val twoSourceOptions = bankWithdrawOptions(sources = listOf(bankWithdrawSource(), baseSource))

    private val confirmed = mutableListOf<BankWithdrawAmountResult>()
    private lateinit var draftFlow: MutableStateFlow<BankWithdrawDraft>
    private lateinit var optionsFlow: MutableStateFlow<BankWithdrawOptions>

    @BeforeEach
    fun resetMocks() {
        clearMocks(router)
        confirmed.clear()
    }

    @Test
    fun `GIVEN prepared options WHEN model created THEN first source and first rail preselected`() {
        // Act
        val model = createModel()

        // Assert
        val state = model.uiState.value
        assertThat(state.source.symbol).isEqualTo("USDC")
        assertThat(state.source.subtitle).isEqualTo(
            resourceReference(R.string.tangempay_withdraw_bank_source_subtitle, wrappedList("Polygon", "$2,325.55")),
        )
        assertThat(state.route.rail).isEqualTo(BankRail.ACH)
        assertThat(state.route.fee).isEqualTo("$1")
        assertThat(state.route.onClick).isNotNull()
        assertThat(state.amount.currencySymbol).isEqualTo("$")
        assertThat(state.amount.decimals).isEqualTo(2)
        assertThat(state.isContinueEnabled).isFalse()
        assertThat(state.header.subtitle)
            .isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_step_counter, wrappedList(1, 5)))
        model.onDestroy()
    }

    @Test
    fun `GIVEN single destination WHEN model created THEN route row not clickable`() {
        // Act
        val model = createModel(options = bankWithdrawOptions(destinations = listOf(offrampDestination())))

        // Assert
        assertThat(model.uiState.value.route.onClick).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN amount below minimum WHEN value changed THEN range error and continue disabled`() {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.amount.onValueChange("0.5")

        // Assert
        val state = model.uiState.value
        assertThat(state.amount.error).isEqualTo(
            resourceReference(R.string.tangempay_withdraw_bank_amount_error_range, wrappedList("$1", "$2,324.55")),
        )
        assertThat(state.isContinueEnabled).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN amount plus fee above balance WHEN value changed THEN not enough funds`() {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.amount.onValueChange("2325")

        // Assert
        val state = model.uiState.value
        assertThat(state.amount.error)
            .isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_amount_not_enough_funds))
        assertThat(state.isContinueEnabled).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN non numeric input WHEN value changed THEN ignored`() {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.amount.onValueChange("abc")

        // Assert
        assertThat(model.uiState.value.amount.value).isEmpty()
        model.onDestroy()
    }

    @Test
    fun `GIVEN comma decimal locale WHEN comma separated value entered THEN accepted and validated`() {
        val previousLocale = Locale.getDefault()
        try {
            // Arrange
            Locale.setDefault(Locale.GERMANY)
            val model = createModel()

            // Act
            model.uiState.value.amount.onValueChange("1,5")

            // Assert
            val state = model.uiState.value
            assertThat(state.amount.value).isEqualTo("1,5")
            assertThat(state.amount.error).isNull()
            assertThat(state.isContinueEnabled).isTrue()
            model.onDestroy()
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    fun `GIVEN prepared options WHEN max clicked THEN balance minus fee`() {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.source.onMaxClick()

        // Assert
        val state = model.uiState.value
        assertThat(state.amount.value).isEqualTo("2324.55")
        assertThat(state.amount.error).isNull()
        assertThat(state.isContinueEnabled).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN valid amount WHEN continue clicked THEN result passed to container`() {
        // Arrange
        val model = createModel()
        model.uiState.value.amount.onValueChange("500")

        // Act
        model.uiState.value.onContinueClick()

        // Assert
        val result = confirmed.single()
        assertThat(result.amount).isEqualTo(BigDecimal("500"))
        assertThat(result.source.token).isEqualTo(usdcPolygon)
        assertThat(result.destination.rail).isEqualTo(BankRail.ACH)
        model.onDestroy()
    }

    @Test
    fun `GIVEN invalid amount WHEN continue clicked THEN nothing passed`() {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.onContinueClick()

        // Assert
        assertThat(confirmed).isEmpty()
        model.onDestroy()
    }

    @Test
    fun `GIVEN source row clicked WHEN token selected THEN source changes and sheet closes`() {
        // Arrange
        val model = createModel()
        model.uiState.value.source.onClick()
        val sheet = model.uiState.value.sheet as SheetUM.Tokens
        assertThat(sheet.items.map { it.symbol to it.isSelected }).containsExactly(
            "USDC" to true,
            "USDC" to false,
        ).inOrder()

        // Act
        sheet.items.last().onClick()

        // Assert
        val state = model.uiState.value
        assertThat(state.sheet).isEqualTo(SheetUM.Hidden)
        assertThat(state.source.subtitle).isEqualTo(
            resourceReference(R.string.tangempay_withdraw_bank_source_subtitle, wrappedList("Base", "$12,325.55")),
        )
        model.onDestroy()
    }

    @Test
    fun `GIVEN route row clicked WHEN wire selected THEN route row updates and sheet closes`() {
        // Arrange
        val model = createModel()
        model.uiState.value.route.onClick?.invoke()
        val sheet = model.uiState.value.sheet as SheetUM.Routes
        assertThat(sheet.items.map { it.rail to it.isSelected }).containsExactly(
            BankRail.ACH to true,
            BankRail.WIRE to false,
        ).inOrder()

        // Act
        sheet.items.last().onClick()

        // Assert
        val state = model.uiState.value
        assertThat(state.sheet).isEqualTo(SheetUM.Hidden)
        assertThat(state.route.rail).isEqualTo(BankRail.WIRE)
        assertThat(state.route.fee).isEqualTo("$11")
        assertThat(state.route.eta).isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_eta_same_business_day))
        model.onDestroy()
    }

    @Test
    fun `GIVEN sheet opened WHEN dismissed THEN sheet closes`() {
        // Arrange
        val model = createModel()
        model.uiState.value.source.onClick()

        // Act
        model.uiState.value.sheet.onDismiss()

        // Assert
        assertThat(model.uiState.value.sheet).isEqualTo(SheetUM.Hidden)
        model.onDestroy()
    }

    @Test
    fun `GIVEN draft with source and rail WHEN model created THEN preselected from draft`() {
        // Arrange
        val draft = BankWithdrawDraft(
            entry = BankWithdrawEntry.NewPersonal,
            source = baseSource,
            destination = offrampDestination(rail = BankRail.WIRE, fee = BigDecimal("11.00"), minDays = 0, maxDays = 0),
        )

        // Act
        val model = createModel(draft = draft)

        // Assert
        val state = model.uiState.value
        assertThat(state.source.subtitle).isEqualTo(
            resourceReference(R.string.tangempay_withdraw_bank_source_subtitle, wrappedList("Base", "$12,325.55")),
        )
        assertThat(state.route.rail).isEqualTo(BankRail.WIRE)
        model.onDestroy()
    }

    @Test
    fun `GIVEN draft source no longer withdrawable WHEN model created THEN falls back to the first source`() {
        // Arrange
        val draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal, source = baseSource)

        // Act
        val model = createModel(draft = draft, options = bankWithdrawOptions())

        // Assert
        assertThat(model.uiState.value.source.subtitle).isEqualTo(
            resourceReference(R.string.tangempay_withdraw_bank_source_subtitle, wrappedList("Polygon", "$2,325.55")),
        )
        model.onDestroy()
    }

    @Test
    fun `GIVEN draft with amount WHEN reopened for editing THEN amount prefilled and continue enabled`() {
        // Arrange
        val draft = BankWithdrawDraft(
            entry = BankWithdrawEntry.NewPersonal,
            amount = BigDecimal("500"),
            source = bankWithdrawSource(),
            destination = offrampDestination(),
        )

        // Act
        val model = createModel(
            draft = draft,
            step = TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.Amount),
        )

        // Assert
        val state = model.uiState.value
        assertThat(state.amount.value).isEqualTo("500")
        assertThat(state.isContinueEnabled).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN prefilled amount WHEN cleared THEN stays empty instead of reverting to the draft`() {
        // Arrange
        val draft = BankWithdrawDraft(
            entry = BankWithdrawEntry.NewPersonal,
            amount = BigDecimal("500"),
            source = bankWithdrawSource(),
            destination = offrampDestination(),
        )
        val model = createModel(
            draft = draft,
            step = TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.Amount),
        )

        // Act
        model.uiState.value.amount.onValueChange("")

        // Assert
        assertThat(model.uiState.value.amount.value).isEmpty()
        model.onDestroy()
    }

    @Test
    fun `GIVEN back clicked THEN router pops`() {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.onBackClick()

        // Assert
        verify(exactly = 1) { router.pop() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN edit step WHEN model created THEN header has no step counter`() {
        // Act
        val model = createModel(step = TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.Amount))

        // Assert
        assertThat(model.uiState.value.header.subtitle).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN continue committed WHEN draft amount changes afterwards THEN new amount shown and reported`() {
        // Arrange
        val model = createModel()
        model.uiState.value.amount.onValueChange("500")
        model.uiState.value.onContinueClick()

        // Act
        draftFlow.value = draftFlow.value.copy(amount = BigDecimal("700"))

        // Assert
        assertThat(model.uiState.value.amount.value).isEqualTo("700")
        model.uiState.value.onContinueClick()
        assertThat(confirmed.last().amount).isEqualTo(BigDecimal("700"))
        model.onDestroy()
    }

    @Test
    fun `GIVEN balance drops while the step is open WHEN options update THEN the amount is revalidated`() {
        // Arrange
        val model = createModel()
        model.uiState.value.amount.onValueChange("2000")
        assertThat(model.uiState.value.isContinueEnabled).isTrue()

        // Act
        optionsFlow.value = bankWithdrawOptions(
            sources = listOf(bankWithdrawSource(fiatBalance = BigDecimal("10")), baseSource),
        )

        // Assert
        val state = model.uiState.value
        assertThat(state.amount.error)
            .isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_amount_not_enough_funds))
        assertThat(state.isContinueEnabled).isFalse()
        model.onDestroy()
    }

    private fun createModel(
        draft: BankWithdrawDraft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal),
        step: TangemPayBankWithdrawInnerRoute = TangemPayBankWithdrawInnerRoute.Amount,
        options: BankWithdrawOptions = twoSourceOptions,
    ): TangemPayBankWithdrawAmountModel {
        draftFlow = MutableStateFlow(draft)
        optionsFlow = MutableStateFlow(options)
        return TangemPayBankWithdrawAmountModel(
            paramsContainer = MutableParamsContainer(
                TangemPayBankWithdrawAmountComponent.Params(
                    draft = draftFlow,
                    options = optionsFlow,
                    step = step,
                    onContinue = confirmed::add,
                ),
            ),
            dispatchers = dispatchers,
            router = router,
        )
    }
}