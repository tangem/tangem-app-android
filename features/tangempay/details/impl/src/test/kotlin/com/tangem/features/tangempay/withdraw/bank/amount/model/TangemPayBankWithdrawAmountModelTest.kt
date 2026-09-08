package com.tangem.features.tangempay.withdraw.bank.amount.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.BottomSheetMessage
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.usecase.bankofframp.GetBankOfframpOptionsUseCase
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BASE_CHAIN_ID
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.USDC_BASE
import com.tangem.features.tangempay.withdraw.bank.amount.TangemPayBankWithdrawAmountComponent
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.RouteRowUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.SheetUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.SourceRowUM
import com.tangem.features.tangempay.withdraw.bank.availableNetwork
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawSource
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawAmountResult
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import com.tangem.features.tangempay.withdraw.bank.offrampOptions
import com.tangem.features.tangempay.withdraw.bank.offrampSource
import com.tangem.features.tangempay.withdraw.bank.paymentToken
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Locale

internal class TangemPayBankWithdrawAmountModelTest {

    private val userWalletId = UserWalletId("011")
    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()
    private val getBankOfframpOptionsUseCase: GetBankOfframpOptionsUseCase = mockk()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val statusFlow = MutableSharedFlow<AccountStatus.Payment>(replay = 1)
    private val usdcPolygon = paymentToken()
    private val usdcBase = paymentToken(networkId = "base", networkName = "Base", contractAddress = USDC_BASE)
    private val confirmed = mutableListOf<BankWithdrawAmountResult>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, uiMessageSender, paymentAccountStatusSupplier, getBankOfframpOptionsUseCase)
        statusFlow.resetReplayCache()
        confirmed.clear()
        every { paymentAccountStatusSupplier(userWalletId) } returns statusFlow
        coEvery { getBankOfframpOptionsUseCase(userWalletId) } returns offrampOptions(
            sources = listOf(offrampSource(), offrampSource("Base", BASE_CHAIN_ID, USDC_BASE)),
        ).right()
    }

    @Test
    fun `GIVEN status not loaded WHEN model created THEN rows loading and continue disabled`() {
        // Act
        val model = createModel()

        // Assert
        val state = model.uiState.value
        assertThat(state.source).isEqualTo(SourceRowUM.Loading)
        assertThat(state.route).isEqualTo(RouteRowUM.Loading)
        assertThat(state.isContinueEnabled).isFalse()
        assertThat(state.header.subtitle)
            .isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_step_counter, wrappedList(1, 5)))
        model.onDestroy()
    }

    @Test
    fun `GIVEN options and status WHEN loaded THEN first available source and first rail preselected`() {
        // Arrange
        val model = createModel()

        // Act
        emitStatus()

        // Assert
        val state = model.uiState.value
        val source = state.source as SourceRowUM.Content
        assertThat(source.symbol).isEqualTo("USDC")
        assertThat(source.subtitle).isEqualTo(
            resourceReference(R.string.tangempay_withdraw_bank_source_subtitle, wrappedList("Polygon", "$2,325.55")),
        )
        val route = state.route as RouteRowUM.Content
        assertThat(route.rail).isEqualTo(BankRail.ACH)
        assertThat(route.fee).isEqualTo("$1")
        assertThat(route.onClick).isNotNull()
        assertThat(state.amount.currencySymbol).isEqualTo("$")
        assertThat(state.amount.decimals).isEqualTo(2)
        assertThat(state.isContinueEnabled).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN single destination WHEN loaded THEN route row not clickable`() {
        // Arrange
        coEvery { getBankOfframpOptionsUseCase(userWalletId) } returns offrampOptions(
            destinations = listOf(offrampDestination()),
        ).right()
        val model = createModel()

        // Act
        emitStatus()

        // Assert
        assertThat((model.uiState.value.route as RouteRowUM.Content).onClick).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN amount below minimum WHEN value changed THEN range error and continue disabled`() {
        // Arrange
        val model = createModel()
        emitStatus()

        // Act
        model.uiState.value.amount.onValueChange("0.5")

        // Assert
        val state = model.uiState.value
        assertThat(state.amount.error).isEqualTo(
            resourceReference(R.string.tangempay_withdraw_bank_amount_error_range, wrappedList("$1", "$10,000")),
        )
        assertThat(state.isContinueEnabled).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN amount plus fee above balance WHEN value changed THEN not enough funds`() {
        // Arrange
        val model = createModel()
        emitStatus()

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
        emitStatus()

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
            emitStatus()

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
    fun `GIVEN loaded WHEN max clicked THEN balance minus fee`() {
        // Arrange
        val model = createModel()
        emitStatus()

        // Act
        (model.uiState.value.source as SourceRowUM.Content).onMaxClick()

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
        emitStatus()
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
        emitStatus()

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
        emitStatus()
        (model.uiState.value.source as SourceRowUM.Content).onClick()
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
        assertThat((state.source as SourceRowUM.Content).subtitle).isEqualTo(
            resourceReference(R.string.tangempay_withdraw_bank_source_subtitle, wrappedList("Base", "$12,325.55")),
        )
        model.onDestroy()
    }

    @Test
    fun `GIVEN token not accepted by offramp WHEN sheet opened THEN marked unavailable`() {
        // Arrange
        coEvery { getBankOfframpOptionsUseCase(userWalletId) } returns offrampOptions(
            sources = listOf(offrampSource()),
        ).right()
        val model = createModel()
        emitStatus()

        // Act
        (model.uiState.value.source as SourceRowUM.Content).onClick()

        // Assert
        val sheet = model.uiState.value.sheet as SheetUM.Tokens
        val unavailable = sheet.items.last()
        assertThat(unavailable.isAvailable).isFalse()
        assertThat(unavailable.subtitle).isEqualTo(
            resourceReference(
                R.string.tangempay_withdraw_bank_source_subtitle,
                wrappedList("Base", resourceReference(R.string.tangempay_withdraw_bank_offramp_unavailable)),
            ),
        )
        model.onDestroy()
    }

    @Test
    fun `GIVEN route row clicked WHEN wire selected THEN route row updates and sheet closes`() {
        // Arrange
        val model = createModel()
        emitStatus()
        (model.uiState.value.route as RouteRowUM.Content).onClick?.invoke()
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
        val route = state.route as RouteRowUM.Content
        assertThat(route.rail).isEqualTo(BankRail.WIRE)
        assertThat(route.fee).isEqualTo("$11")
        assertThat(route.eta).isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_eta_same_business_day))
        model.onDestroy()
    }

    @Test
    fun `GIVEN sheet opened WHEN dismissed THEN sheet closes`() {
        // Arrange
        val model = createModel()
        emitStatus()
        (model.uiState.value.source as SourceRowUM.Content).onClick()

        // Act
        model.uiState.value.sheet.onDismiss()

        // Assert
        assertThat(model.uiState.value.sheet).isEqualTo(SheetUM.Hidden)
        model.onDestroy()
    }

    @Test
    fun `GIVEN draft with source and rail WHEN loaded THEN preselected from draft`() {
        // Arrange
        val draft = BankWithdrawDraft(
            entry = BankWithdrawEntry.NewPersonal,
            source = bankWithdrawSource(
                token = usdcBase,
                chainId = BASE_CHAIN_ID,
            ),
            destination = offrampDestination(rail = BankRail.WIRE, fee = BigDecimal("11.00"), minDays = 0, maxDays = 0),
        )
        val model = createModel(draft = draft)

        // Act
        emitStatus()

        // Assert
        val state = model.uiState.value
        assertThat((state.source as SourceRowUM.Content).subtitle).isEqualTo(
            resourceReference(R.string.tangempay_withdraw_bank_source_subtitle, wrappedList("Base", "$12,325.55")),
        )
        assertThat((state.route as RouteRowUM.Content).rail).isEqualTo(BankRail.WIRE)
        model.onDestroy()
    }

    @Test
    fun `GIVEN options fail WHEN model created THEN error message shown and wizard closed`() {
        // Arrange
        coEvery { getBankOfframpOptionsUseCase(userWalletId) } returns BankOfframpError.Unknown(null).left()

        // Act
        val model = createModel()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(any<BottomSheetMessage>()) }
        verify(exactly = 1) { router.pop() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN options without destinations WHEN model created THEN error message shown and wizard closed`() {
        // Arrange
        coEvery { getBankOfframpOptionsUseCase(userWalletId) } returns offrampOptions(
            destinations = emptyList(),
        ).right()

        // Act
        val model = createModel()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(any<BottomSheetMessage>()) }
        verify(exactly = 1) { router.pop() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN draft source not accepted by offramp WHEN valid amount entered THEN continue stays disabled`() {
        // Arrange
        coEvery { getBankOfframpOptionsUseCase(userWalletId) } returns offrampOptions(
            sources = listOf(offrampSource()),
        ).right()
        val draft = BankWithdrawDraft(
            entry = BankWithdrawEntry.NewPersonal,
            source = bankWithdrawSource(token = usdcBase, chainId = BASE_CHAIN_ID),
        )
        val model = createModel(draft = draft)
        emitStatus()

        // Act
        model.uiState.value.amount.onValueChange("500")
        model.uiState.value.onContinueClick()

        // Assert
        val state = model.uiState.value
        assertThat(state.amount.error).isNull()
        assertThat(state.isContinueEnabled).isFalse()
        assertThat(confirmed).isEmpty()
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

    private fun emitStatus() {
        val status = MockAccounts.createPaymentAccountStatus(
            userWalletId = userWalletId,
            networks = listOf(
                availableNetwork(token = usdcPolygon, fiatBalance = BigDecimal("2325.55")),
                availableNetwork(token = usdcBase, chainId = BASE_CHAIN_ID, fiatBalance = BigDecimal("12325.55")),
            ),
        )
        assertThat(statusFlow.tryEmit(status)).isTrue()
    }

    private fun createModel(
        draft: BankWithdrawDraft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal),
    ): TangemPayBankWithdrawAmountModel = TangemPayBankWithdrawAmountModel(
        paramsContainer = MutableParamsContainer(
            TangemPayBankWithdrawAmountComponent.Params(
                userWalletId = userWalletId,
                draft = MutableStateFlow(draft),
                onContinue = confirmed::add,
            ),
        ),
        dispatchers = dispatchers,
        router = router,
        uiMessageSender = uiMessageSender,
        paymentAccountStatusSupplier = paymentAccountStatusSupplier,
        getBankOfframpOptionsUseCase = getBankOfframpOptionsUseCase,
    )
}