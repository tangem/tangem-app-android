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
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.domain.pay.repository.TangemPayWithdrawRepository
import com.tangem.domain.pay.usecase.bankofframp.GetBankAccountsUseCase
import com.tangem.domain.pay.usecase.bankofframp.GetRecentBankAccountsUseCase
import com.tangem.domain.pay.usecase.bankofframp.MarkBankAccountUsedUseCase
import com.tangem.features.tangempay.account.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.hub.TangemPayWithdrawHubComponent
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.RecentsUM
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val loadFailure: Either<BankOfframpError, List<BankAccount>> = BankOfframpError.Unknown(cause = null).left()

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
        )
        every { currencyFactory.create(userWalletId) } returns token
        coEvery { withdrawRepository.hasWithdrawOrder(userWalletId) } returns false
        coEvery { getRecentBankAccountsUseCase(userWalletId) } returns emptyList<BankAccount>().right()
        coEvery { getBankAccountsUseCase(userWalletId) } returns emptyList<BankAccount>().right()
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
    fun `GIVEN recent accounts WHEN model created THEN recents content in usage order`() {
        // Arrange
        coEvery { getRecentBankAccountsUseCase(userWalletId) } returns listOf(
            bankAccount(id = "acc-2", beneficiary = Beneficiary.Business(name = "Raxanax")),
            bankAccount(id = "acc-1"),
        ).right()

        // Act
        val model = createModel()

        // Assert
        val recents = model.uiState.value.bankSection.recents
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
        assertThat(model.uiState.value.bankSection.recents).isEqualTo(RecentsUM.Empty(onSeeAllClick = null))
        model.onDestroy()
    }

    @Test
    fun `GIVEN no recent but saved accounts WHEN model created THEN recents empty with see all`() {
        // Arrange
        coEvery { getBankAccountsUseCase(userWalletId) } returns listOf(bankAccount(id = "acc-1")).right()
        val model = createModel()

        // Act
        requireNotNull((model.uiState.value.bankSection.recents as RecentsUM.Empty).onSeeAllClick).invoke()

        // Assert
        verify(exactly = 1) {
            router.push(TangemPayAccountDetailsInnerRoute.BankWithdraw(entry = BankWithdrawEntry.SeeAll))
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN no recent accounts and saved accounts fail WHEN model created THEN recents empty without see all`() {
        // Arrange
        coEvery { getBankAccountsUseCase(userWalletId) } returns loadFailure

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.bankSection.recents).isEqualTo(RecentsUM.Empty(onSeeAllClick = null))
        model.onDestroy()
    }

    @Test
    fun `GIVEN recents load fails WHEN model created THEN recents error`() {
        // Arrange
        coEvery { getRecentBankAccountsUseCase(userWalletId) } returns loadFailure

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.bankSection.recents).isInstanceOf(RecentsUM.Error::class.java)
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
        (model.uiState.value.bankSection.recents as RecentsUM.Error).onRetryClick()

        // Assert
        assertThat(model.uiState.value.bankSection.recents).isInstanceOf(RecentsUM.Content::class.java)
        coVerify(exactly = 2) { getRecentBankAccountsUseCase(userWalletId) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN model WHEN to personal account clicked THEN bank withdraw pushed with new personal entry`() {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.bankSection.toPersonalAccount.onClick()

        // Assert
        verify(exactly = 1) {
            router.push(TangemPayAccountDetailsInnerRoute.BankWithdraw(entry = BankWithdrawEntry.NewPersonal))
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN model WHEN to business account clicked THEN bank withdraw pushed with new business entry`() {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.bankSection.toBusinessAccount.onClick()

        // Assert
        verify(exactly = 1) {
            router.push(TangemPayAccountDetailsInnerRoute.BankWithdraw(entry = BankWithdrawEntry.NewBusiness))
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN recents content WHEN recipient clicked THEN account marked used then saved entry pushed`() {
        // Arrange
        coEvery { getRecentBankAccountsUseCase(userWalletId) } returns listOf(bankAccount(id = "acc-1")).right()
        val model = createModel()

        // Act
        (model.uiState.value.bankSection.recents as RecentsUM.Content).onRecipientClick("acc-1")

        // Assert
        coVerifyOrder {
            markBankAccountUsedUseCase(userWalletId = userWalletId, bankAccountId = "acc-1")
            router.push(
                TangemPayAccountDetailsInnerRoute.BankWithdraw(entry = BankWithdrawEntry.Saved(bankAccountId = "acc-1")),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN recents content WHEN see all clicked THEN bank withdraw pushed with see all entry`() {
        // Arrange
        coEvery { getRecentBankAccountsUseCase(userWalletId) } returns listOf(bankAccount(id = "acc-1")).right()
        val model = createModel()

        // Act
        (model.uiState.value.bankSection.recents as RecentsUM.Content).onSeeAllClick()

        // Assert
        verify(exactly = 1) {
            router.push(TangemPayAccountDetailsInnerRoute.BankWithdraw(entry = BankWithdrawEntry.SeeAll))
        }
        model.onDestroy()
    }

    private fun createModel(): TangemPayWithdrawHubModel = TangemPayWithdrawHubModel(
        paramsContainer = MutableParamsContainer(TangemPayWithdrawHubComponent.Params(userWalletId = userWalletId)),
        dispatchers = dispatchers,
        router = router,
        tangemPayCurrencyFactory = currencyFactory,
        tangemPayWithdrawRepository = withdrawRepository,
        uiMessageSender = uiMessageSender,
        getRecentBankAccountsUseCase = getRecentBankAccountsUseCase,
        getBankAccountsUseCase = getBankAccountsUseCase,
        markBankAccountUsedUseCase = markBankAccountUsedUseCase,
    )
}