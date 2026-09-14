package com.tangem.features.tangempay.withdraw.hub.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRoute.Swap.AccountFlow
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.BottomSheetMessage
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOptions
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.domain.pay.repository.TangemPayWithdrawRepository
import com.tangem.domain.pay.usecase.bankofframp.GetBankAccountsUseCase
import com.tangem.domain.pay.usecase.bankofframp.GetBankOfframpOptionsUseCase
import com.tangem.domain.pay.usecase.bankofframp.GetRecentBankAccountsUseCase
import com.tangem.domain.pay.usecase.bankofframp.MarkBankAccountUsedUseCase
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.availableNetwork
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawOptions
import com.tangem.features.tangempay.withdraw.bank.offrampOptions
import com.tangem.features.tangempay.withdraw.bank.paymentToken
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.BankSectionUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.RecentsUM
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class TangemPayWithdrawHubModelTest {

    private val userWalletId = UserWalletId("123")
    private val token: CryptoCurrency.Token = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)
    private val currencyFactory: TangemPayCurrencyFactory = mockk()
    private val withdrawRepository: TangemPayWithdrawRepository = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val getRecentBankAccountsUseCase: GetRecentBankAccountsUseCase = mockk()
    private val getBankAccountsUseCase: GetBankAccountsUseCase = mockk()
    private val markBankAccountUsedUseCase: MarkBankAccountUsedUseCase = mockk(relaxed = true)
    private val getBankOfframpOptionsUseCase: GetBankOfframpOptionsUseCase = mockk()
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val statusFlow = MutableSharedFlow<AccountStatus.Payment>(replay = 1)
    private val opened = mutableListOf<Pair<BankWithdrawEntry, StateFlow<BankWithdrawOptions>>>()

    private val loadFailure: Either<BankOfframpError, List<BankAccount>> = BankOfframpError.Unknown(cause = null).left()
    private val optionsFailure: Either<BankOfframpError, BankOfframpOptions> =
        BankOfframpError.Unknown(cause = null).left()

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            currencyFactory,
            withdrawRepository,
            uiMessageSender,
            getRecentBankAccountsUseCase,
            getBankAccountsUseCase,
            markBankAccountUsedUseCase,
            getBankOfframpOptionsUseCase,
            paymentAccountStatusSupplier,
        )
        statusFlow.resetReplayCache()
        opened.clear()
        every { currencyFactory.create(userWalletId) } returns token
        coEvery { withdrawRepository.hasWithdrawOrder(userWalletId) } returns false
        coEvery { getRecentBankAccountsUseCase(userWalletId) } returns emptyList<BankAccount>().right()
        coEvery { getBankAccountsUseCase(userWalletId) } returns emptyList<BankAccount>().right()
        coEvery { getBankOfframpOptionsUseCase(userWalletId) } returns offrampOptions().right()
        every { paymentAccountStatusSupplier(userWalletId) } returns statusFlow
        emitStatus()
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

    @Test
    fun `GIVEN model WHEN within portfolio clicked THEN hub popped and withdraw swap pushed`() {
        // Arrange
        val model = createModel()
        val expectedRoute = AppRoute.Swap(
            userWalletId = userWalletId,
            fromCryptoCurrency = token,
            screenSource = AnalyticsParam.ScreensSources.TangemPay.value,
            fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.FROM,
            accountFlow = AccountFlow.Withdraw,
        )

        // Act
        model.uiState.value.swapSection.withinPortfolio.onClick()

        // Assert
        verifyOrder {
            router.pop()
            router.push(expectedRoute)
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN active withdrawal WHEN within portfolio clicked THEN in progress message shown and no navigation`() {
        // Arrange
        coEvery { withdrawRepository.hasWithdrawOrder(userWalletId) } returns true
        val model = createModel()

        // Act
        model.uiState.value.swapSection.withinPortfolio.onClick()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(any<BottomSheetMessage>()) }
        verify(exactly = 0) { router.pop() }
        verify(exactly = 0) { router.push(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN offramp options fail WHEN model created THEN bank section error`() {
        // Arrange
        coEvery { getBankOfframpOptionsUseCase(userWalletId) } returns optionsFailure

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.bankSection).isInstanceOf(BankSectionUM.Error::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN no token with balance WHEN model created THEN bank section error`() {
        // Arrange
        statusFlow.resetReplayCache()
        emitStatus(balance = BigDecimal.ZERO)

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.bankSection).isInstanceOf(BankSectionUM.Error::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN bank section error WHEN retry clicked THEN options and recents reloaded`() {
        // Arrange
        coEvery { getBankOfframpOptionsUseCase(userWalletId) } returnsMany listOf(
            optionsFailure,
            offrampOptions().right(),
        )
        val model = createModel()

        // Act
        (model.uiState.value.bankSection as BankSectionUM.Error).onRetryClick()

        // Assert
        assertThat(model.uiState.value.bankSection).isInstanceOf(BankSectionUM.Content::class.java)
        coVerify(exactly = 2) { getBankOfframpOptionsUseCase(userWalletId) }
        coVerify(exactly = 2) { getRecentBankAccountsUseCase(userWalletId) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN wizard opened WHEN balances change THEN its options keep up`() {
        // Arrange
        val model = createModel()
        model.bankContent().toPersonalAccount.onClick()
        val options = opened.single().second
        assertThat(options.value.sources.single().fiatBalance).isEqualTo(BigDecimal("2325.55"))

        // Act
        emitStatus(balance = BigDecimal("100"))

        // Assert
        assertThat(options.value.sources.single().fiatBalance).isEqualTo(BigDecimal("100"))
        model.onDestroy()
    }

    @Test
    fun `GIVEN the account cannot be resolved WHEN status emitted THEN bank section error`() {
        // Arrange
        emitStatusValue(PaymentAccountStatusValue.Error.Unavailable)

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.bankSection).isInstanceOf(BankSectionUM.Error::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN the account is still loading WHEN status emitted THEN bank section keeps loading`() {
        // Arrange
        emitStatusValue(PaymentAccountStatusValue.Loading)

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.bankSection).isEqualTo(BankSectionUM.Loading)
        model.onDestroy()
    }

    @Test
    fun `GIVEN last token spent WHEN account status updates THEN bank section error`() {
        // Arrange
        val model = createModel()
        assertThat(model.uiState.value.bankSection).isInstanceOf(BankSectionUM.Content::class.java)

        // Act
        emitStatus(balance = BigDecimal.ZERO)

        // Assert
        assertThat(model.uiState.value.bankSection).isInstanceOf(BankSectionUM.Error::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN recent accounts WHEN model created THEN recents content in usage order`() {
        // Arrange
        coEvery { getRecentBankAccountsUseCase(userWalletId) } returns listOf(
            bankAccount(id = "acc-2", beneficiary = Beneficiary.Business(name = "Raxanax")),
            bankAccount(id = "acc-1"),
        ).right()

        // Act
        val model = createModel()

        // Assert
        val recents = model.recents()
        assertThat(recents).isInstanceOf(RecentsUM.Content::class.java)
        assertThat((recents as RecentsUM.Content).recipients.map { it.id }).containsExactly("acc-2", "acc-1").inOrder()
        coVerify(exactly = 0) { getBankAccountsUseCase(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN no recent and no saved accounts WHEN model created THEN recents empty without see all`() {
        // Act
        val model = createModel()

        // Assert
        assertThat(model.recents()).isEqualTo(RecentsUM.Empty(onSeeAllClick = null))
        model.onDestroy()
    }

    @Test
    fun `GIVEN no recent but saved accounts WHEN model created THEN recents empty with see all`() {
        // Arrange
        coEvery { getBankAccountsUseCase(userWalletId) } returns listOf(bankAccount(id = "acc-1")).right()
        val model = createModel()

        // Act
        requireNotNull((model.recents() as RecentsUM.Empty).onSeeAllClick).invoke()

        // Assert
        assertThat(opened.single().first).isEqualTo(BankWithdrawEntry.SeeAll)
        model.onDestroy()
    }

    @Test
    fun `GIVEN no recent accounts and saved accounts fail WHEN model created THEN recents empty without see all`() {
        // Arrange
        coEvery { getBankAccountsUseCase(userWalletId) } returns loadFailure

        // Act
        val model = createModel()

        // Assert
        assertThat(model.recents()).isEqualTo(RecentsUM.Empty(onSeeAllClick = null))
        model.onDestroy()
    }

    @Test
    fun `GIVEN recents load fails WHEN model created THEN recents error`() {
        // Arrange
        coEvery { getRecentBankAccountsUseCase(userWalletId) } returns loadFailure

        // Act
        val model = createModel()

        // Assert
        assertThat(model.recents()).isInstanceOf(RecentsUM.Error::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN recents error WHEN retry clicked THEN recents reloaded`() {
        // Arrange
        coEvery { getRecentBankAccountsUseCase(userWalletId) } returnsMany listOf(
            loadFailure,
            listOf(bankAccount(id = "acc-1")).right(),
        )
        val model = createModel()

        // Act
        (model.recents() as RecentsUM.Error).onRetryClick()

        // Assert
        assertThat(model.recents()).isInstanceOf(RecentsUM.Content::class.java)
        coVerify(exactly = 2) { getRecentBankAccountsUseCase(userWalletId) }
        coVerify(exactly = 1) { getBankOfframpOptionsUseCase(userWalletId) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN model WHEN to personal account clicked THEN wizard opened with prepared options`() {
        // Arrange
        val model = createModel()

        // Act
        model.bankContent().toPersonalAccount.onClick()

        // Assert
        val (entry, options) = opened.single()
        assertThat(entry).isEqualTo(BankWithdrawEntry.NewPersonal)
        assertThat(options.value.sources.map { it.token }).containsExactly(paymentToken())
        model.onDestroy()
    }

    @Test
    fun `GIVEN model WHEN to business account clicked THEN wizard opened with new business entry`() {
        // Arrange
        val model = createModel()

        // Act
        model.bankContent().toBusinessAccount.onClick()

        // Assert
        assertThat(opened.single().first).isEqualTo(BankWithdrawEntry.NewBusiness)
        model.onDestroy()
    }

    @Test
    fun `GIVEN recents content WHEN recipient clicked THEN account marked used and saved entry opened`() {
        // Arrange
        coEvery { getRecentBankAccountsUseCase(userWalletId) } returns listOf(bankAccount(id = "acc-1")).right()
        val model = createModel()

        // Act
        (model.recents() as RecentsUM.Content).onRecipientClick("acc-1")

        // Assert
        coVerify(exactly = 1) { markBankAccountUsedUseCase(userWalletId = userWalletId, bankAccountId = "acc-1") }
        assertThat(opened.single().first).isEqualTo(BankWithdrawEntry.Saved(bankAccountId = "acc-1"))
        model.onDestroy()
    }

    @Test
    fun `GIVEN recents content WHEN see all clicked THEN wizard opened with see all entry`() {
        // Arrange
        coEvery { getRecentBankAccountsUseCase(userWalletId) } returns listOf(bankAccount(id = "acc-1")).right()
        val model = createModel()

        // Act
        (model.recents() as RecentsUM.Content).onSeeAllClick()

        // Assert
        assertThat(opened.single().first).isEqualTo(BankWithdrawEntry.SeeAll)
        model.onDestroy()
    }

    private fun emitStatusValue(value: PaymentAccountStatusValue) {
        statusFlow.resetReplayCache()
        val status = AccountStatus.Payment(account = Account.Payment(userWalletId), value = value)
        assertThat(statusFlow.tryEmit(status)).isTrue()
    }

    private fun emitStatus(balance: BigDecimal = BigDecimal("2325.55")) {
        val status = MockAccounts.createPaymentAccountStatus(
            userWalletId = userWalletId,
            networks = listOf(availableNetwork(fiatBalance = balance)),
        )
        assertThat(statusFlow.tryEmit(status)).isTrue()
    }

    private fun TangemPayWithdrawHubModel.bankContent(): BankSectionUM.Content =
        uiState.value.bankSection as BankSectionUM.Content

    private fun TangemPayWithdrawHubModel.recents(): RecentsUM = bankContent().recents

    private fun createModel(): TangemPayWithdrawHubModel = TangemPayWithdrawHubModel(
        paramsContainer = MutableParamsContainer(
            TangemPayWithdrawHubModel.Params(
                userWalletId = userWalletId,
                onOpenBankWithdraw = { entry, options -> opened += entry to options },
            ),
        ),
        dispatchers = dispatchers,
        router = router,
        tangemPayCurrencyFactory = currencyFactory,
        tangemPayWithdrawRepository = withdrawRepository,
        uiMessageSender = uiMessageSender,
        getRecentBankAccountsUseCase = getRecentBankAccountsUseCase,
        getBankAccountsUseCase = getBankAccountsUseCase,
        markBankAccountUsedUseCase = markBankAccountUsedUseCase,
        getBankOfframpOptionsUseCase = getBankOfframpOptionsUseCase,
        paymentAccountStatusSupplier = paymentAccountStatusSupplier,
    )
}