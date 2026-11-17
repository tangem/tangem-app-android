package com.tangem.features.account.createedit

import arrow.core.getOrElse
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountNameUM
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.account.usecase.AddCryptoPortfolioUseCase
import com.tangem.domain.account.usecase.GetUnoccupiedAccountIndexUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.account.AccountCreateEditComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AccountCreateEditModelTest {

    private val router: Router = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val getUnoccupiedAccountIndexUseCase: GetUnoccupiedAccountIndexUseCase = mockk()
    private val addCryptoPortfolioUseCase: AddCryptoPortfolioUseCase = mockk()
    private val portfolioSelectorController: PortfolioSelectorController = mockk(relaxed = true)

    private val selectedAccounts = MutableSharedFlow<Set<AccountId>>()
    private val isEnabled = MutableStateFlow<(UserWallet, AccountStatus) -> Boolean>({ _, _ -> true })

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            userWalletsListRepository,
            singleAccountListSupplier,
            getUnoccupiedAccountIndexUseCase,
            addCryptoPortfolioUseCase,
            portfolioSelectorController,
        )
        every { portfolioSelectorController.selectedAccounts } returns selectedAccounts
        every { portfolioSelectorController.isEnabled } returns isEnabled
        coEvery { getUnoccupiedAccountIndexUseCase(WALLET_A) } returns derivationIndex(1).right()
        coEvery { getUnoccupiedAccountIndexUseCase(WALLET_B) } returns derivationIndex(2).right()
        coEvery { addCryptoPortfolioUseCase(any(), any(), any(), any()) } returns
            mockk<Account.CryptoPortfolio>(relaxed = true).right()
        stubAccountLists(
            MockAccounts.createAccountList(activeAccounts = 1, userWalletId = WALLET_A),
            MockAccounts.createAccountList(activeAccounts = 1, userWalletId = WALLET_B),
        )
    }

    @Test
    fun `GIVEN single wallet WHEN model created THEN wallet row hidden`() = runTest {
        // Arrange
        stubWallets(hotWallet(WALLET_A, name = "A"))

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.wallet).isNull()
    }

    @Test
    fun `GIVEN several wallets WHEN model created THEN wallet row shows the opening wallet`() = runTest {
        // Arrange
        stubWallets(hotWallet(WALLET_A, name = "A"), hotWallet(WALLET_B, name = "B"))

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.wallet?.name).isEqualTo("A")
    }

    @Test
    fun `GIVEN a wallet at the cap WHEN wallet row clicked THEN that wallet disabled in the chooser`() = runTest {
        // Arrange
        stubWallets(hotWallet(WALLET_A, name = "A"), hotWallet(WALLET_B, name = "B"))
        stubAccountLists(
            MockAccounts.createAccountList(activeAccounts = 1, userWalletId = WALLET_A),
            MockAccounts.createAccountList(activeAccounts = 20, userWalletId = WALLET_B),
        )
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.wallet?.onClick?.invoke()

        // Assert
        val status: AccountStatus = mockk()
        assertThat(isEnabled.value(hotWallet(WALLET_A, name = "A"), status)).isTrue()
        assertThat(isEnabled.value(hotWallet(WALLET_B, name = "B"), status)).isFalse()
    }

    @Test
    fun `GIVEN another wallet picked WHEN confirm THEN account created in the picked wallet`() = runTest {
        // Arrange
        stubWallets(hotWallet(WALLET_A, name = "A"), hotWallet(WALLET_B, name = "B"))
        val model = createModel()
        advanceUntilIdle()
        model.uiState.value.wallet?.onClick?.invoke()
        selectedAccounts.emit(setOf(Account.Personal.createMainAccount(WALLET_B).accountId))
        advanceUntilIdle()
        model.uiState.value.account.onNameChange(AccountNameUM.Custom(raw = "Savings"))

        // Act
        model.uiState.value.buttonState.onConfirmClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.wallet?.name).isEqualTo("B")
        coVerify(exactly = 1) {
            addCryptoPortfolioUseCase(
                userWalletId = WALLET_B,
                accountName = any(),
                icon = any(),
                derivationIndex = derivationIndex(2),
            )
        }
    }

    @Test
    fun `GIVEN a wallet whose accounts never load WHEN model created THEN wallet row still shown`() = runTest {
        // Arrange
        stubWallets(hotWallet(WALLET_A, name = "A"), hotWallet(WALLET_B, name = "B"))
        every { singleAccountListSupplier(WALLET_B) } returns emptyFlow()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.wallet?.name).isEqualTo("A")
    }

    @Test
    fun `GIVEN picked wallet index loading WHEN name valid THEN confirm stays disabled`() = runTest {
        // Arrange
        stubWallets(hotWallet(WALLET_A, name = "A"), hotWallet(WALLET_B, name = "B"))
        val walletBIndex = CompletableDeferred<DerivationIndex>()
        coEvery { getUnoccupiedAccountIndexUseCase(WALLET_B) } coAnswers { walletBIndex.await().right() }
        val model = createModel()
        advanceUntilIdle()
        model.uiState.value.account.onNameChange(AccountNameUM.Custom(raw = "Savings"))

        // Act
        model.uiState.value.wallet?.onClick?.invoke()
        selectedAccounts.emit(setOf(Account.Personal.createMainAccount(WALLET_B).accountId))
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.buttonState.isButtonEnabled).isFalse()
        walletBIndex.complete(derivationIndex(2))
        advanceUntilIdle()
        assertThat(model.uiState.value.buttonState.isButtonEnabled).isTrue()
    }

    private fun stubWallets(vararg wallets: UserWallet) {
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(wallets.toList())
    }

    private fun stubAccountLists(vararg accountLists: AccountList) {
        accountLists.forEach { list -> every { singleAccountListSupplier(list.userWalletId) } returns flowOf(list) }
    }

    private fun TestScope.createModel() = AccountCreateEditModel(
        paramsContainer = MutableParamsContainer(value = AccountCreateEditComponent.Params.Create(WALLET_A)),
        messageSender = mockk(relaxed = true),
        router = router,
        dispatchers = createTestingCoroutineDispatcherProvider(),
        updateCryptoPortfolioUseCase = mockk(),
        addCryptoPortfolioUseCase = addCryptoPortfolioUseCase,
        getUnoccupiedAccountIndexUseCase = getUnoccupiedAccountIndexUseCase,
        analyticsExceptionHandler = mockk(relaxed = true),
        analyticsEventHandler = mockk(relaxed = true),
        userWalletsListRepository = userWalletsListRepository,
        singleAccountListSupplier = singleAccountListSupplier,
        portfolioFetcherFactory = mockk(),
        portfolioSelectorController = portfolioSelectorController,
    )

    private fun derivationIndex(value: Int) = DerivationIndex(value).getOrElse { error("Invalid index $value") }

    private fun hotWallet(walletId: UserWalletId, name: String) = UserWallet.Hot(
        name = name,
        walletId = walletId,
        hotWalletId = HotWalletId(value = "hot", authType = HotWalletId.AuthType.NoPassword),
        wallets = emptyList(),
        isBackedUp = false,
    )

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")
    }
}