package com.tangem.features.account.createedit

import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountNameUM
import com.tangem.common.ui.account.accountLimitReachedDialog
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.usecase.AddCryptoPortfolioUseCase
import com.tangem.domain.account.usecase.GetUnoccupiedAccountIndexUseCase
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.account.AccountCreateEditComponent
import com.tangem.features.commonfeatures.api.portfolioselector.AccountWalletPicker
import com.tangem.features.commonfeatures.api.portfolioselector.AccountWalletPicker.Pick
import com.tangem.features.commonfeatures.api.portfolioselector.AccountWalletPicker.Resolution
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AccountCreateEditModelTest {

    private val router: Router = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val getUnoccupiedAccountIndexUseCase: GetUnoccupiedAccountIndexUseCase = mockk()
    private val addCryptoPortfolioUseCase: AddCryptoPortfolioUseCase = mockk()
    private val walletPicker: AccountWalletPicker = mockk(relaxed = true)
    private val walletPickerFactory: AccountWalletPicker.Factory = mockk()

    private val wallets = MutableStateFlow<List<UserWallet>?>(null)
    private val picks = MutableSharedFlow<Pick>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            messageSender,
            getUnoccupiedAccountIndexUseCase,
            addCryptoPortfolioUseCase,
            walletPicker,
            walletPickerFactory,
        )
        every { walletPickerFactory.create(any()) } returns walletPicker
        every { walletPicker.wallets } returns wallets
        every { walletPicker.picks } returns picks
        wallets.value = listOf(hotWallet(WALLET_A, name = "A"), hotWallet(WALLET_B, name = "B"))
        coEvery { getUnoccupiedAccountIndexUseCase(WALLET_A) } returns derivationIndex(1).right()
        coEvery { getUnoccupiedAccountIndexUseCase(WALLET_B) } returns derivationIndex(2).right()
        coEvery { addCryptoPortfolioUseCase(any(), any(), any(), any()) } returns
            mockk<Account.CryptoPortfolio>(relaxed = true).right()
    }

    @Test
    fun `GIVEN single wallet WHEN model created THEN wallet row hidden`() = runTest {
        // Arrange
        wallets.value = listOf(hotWallet(WALLET_A, name = "A"))

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.wallet).isNull()
    }

    @Test
    fun `GIVEN several wallets WHEN model created THEN wallet row shows the opening wallet`() = runTest {
        // Arrange & Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.wallet?.name).isEqualTo("A")
    }

    @Test
    fun `GIVEN a wallet at the cap WHEN wallet row clicked THEN chooser opens with it marked full`() = runTest {
        // Arrange
        coEvery { walletPicker.resolve(any()) } returns Resolution.Choose(fullWalletIds = setOf(WALLET_B))
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.wallet?.onClick?.invoke()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { walletPicker.openChooser(setOf(WALLET_B)) }
    }

    @Test
    fun `GIVEN chooser open WHEN a full wallet picked THEN limit dialog shown and wallet kept`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        picks.emit(Pick.Full)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { messageSender.send(match(::isLimitDialog)) }
        assertThat(model.uiState.value.wallet?.name).isEqualTo("A")
    }

    @Test
    fun `GIVEN another wallet picked WHEN confirm THEN account created in the picked wallet`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()
        picks.emit(Pick.Wallet(WALLET_B))
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
    fun `GIVEN picked wallet index loading WHEN name valid THEN confirm stays disabled until it loads`() = runTest {
        // Arrange
        val walletBIndex = CompletableDeferred<DerivationIndex>()
        coEvery { getUnoccupiedAccountIndexUseCase(WALLET_B) } coAnswers { walletBIndex.await().right() }
        val model = createModel()
        advanceUntilIdle()
        model.uiState.value.account.onNameChange(AccountNameUM.Custom(raw = "Savings"))

        // Act
        picks.emit(Pick.Wallet(WALLET_B))
        advanceUntilIdle()
        val isEnabledWhileLoading = model.uiState.value.buttonState.isButtonEnabled
        walletBIndex.complete(derivationIndex(2))
        advanceUntilIdle()

        // Assert
        assertThat(isEnabledWhileLoading).isFalse()
        assertThat(model.uiState.value.buttonState.isButtonEnabled).isTrue()
    }

    @Test
    fun `GIVEN previous wallet index pending WHEN another wallet picked THEN stale index never applied`() = runTest {
        // Arrange
        val walletAIndex = CompletableDeferred<DerivationIndex>()
        val walletBIndex = CompletableDeferred<DerivationIndex>()
        coEvery { getUnoccupiedAccountIndexUseCase(WALLET_A) } coAnswers { walletAIndex.await().right() }
        coEvery { getUnoccupiedAccountIndexUseCase(WALLET_B) } coAnswers { walletBIndex.await().right() }
        val model = createModel()
        advanceUntilIdle()
        model.uiState.value.account.onNameChange(AccountNameUM.Custom(raw = "Savings"))

        // Act
        picks.emit(Pick.Wallet(WALLET_B))
        advanceUntilIdle()
        walletAIndex.complete(derivationIndex(1))
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.account.derivationInfo.index).isNull()
        assertThat(model.uiState.value.buttonState.isButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN switched wallet index fails WHEN error dismissed THEN previous wallet restored and screen kept`() =
        runTest {
            // Arrange
            coEvery { getUnoccupiedAccountIndexUseCase(WALLET_B) } returns indexNotFound
            val model = createModel()
            advanceUntilIdle()
            val message = slot<UiMessage>()
            every { messageSender.send(capture(message)) } just Runs

            // Act
            picks.emit(Pick.Wallet(WALLET_B))
            advanceUntilIdle()
            (message.captured as DialogMessage).onDismissRequest()
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { router.pop(any()) }
            assertThat(model.uiState.value.wallet?.name).isEqualTo("A")
            assertThat(model.uiState.value.account.derivationInfo.index).isEqualTo(1)
        }

    @Test
    fun `GIVEN opening wallet index fails WHEN error dismissed THEN screen closed`() = runTest {
        // Arrange
        coEvery { getUnoccupiedAccountIndexUseCase(WALLET_A) } returns indexNotFound
        val message = slot<UiMessage>()
        every { messageSender.send(capture(message)) } just Runs
        createModel()
        advanceUntilIdle()

        // Act
        (message.captured as DialogMessage).onDismissRequest()

        // Assert
        verify(exactly = 1) { router.pop(any()) }
    }

    @Test
    fun `GIVEN valid name and index loading WHEN close THEN unsaved changes dialog shown`() = runTest {
        // Arrange
        coEvery { getUnoccupiedAccountIndexUseCase(WALLET_B) } coAnswers { CompletableDeferred<Nothing>().await() }
        val model = createModel()
        advanceUntilIdle()
        model.uiState.value.account.onNameChange(AccountNameUM.Custom(raw = "Savings"))
        picks.emit(Pick.Wallet(WALLET_B))
        advanceUntilIdle()

        // Act
        model.uiState.value.onCloseClick()

        // Assert
        assertThat(model.uiState.value.hasUnsavedChanges).isTrue()
        verify(exactly = 1) { messageSender.send(any()) }
        verify(exactly = 0) { router.pop(any()) }
        model.onDestroy()
    }

    private fun isLimitDialog(message: UiMessage): Boolean = (message as? DialogMessage)?.message ==
        accountLimitReachedDialog(limit = AccountList.MAX_CRYPTO_PORTFOLIO_ACCOUNTS_COUNT).message

    private fun TestScope.createModel() = AccountCreateEditModel(
        paramsContainer = MutableParamsContainer(value = AccountCreateEditComponent.Params.Create(WALLET_A)),
        messageSender = messageSender,
        router = router,
        dispatchers = createTestingCoroutineDispatcherProvider(),
        updateCryptoPortfolioUseCase = mockk(),
        addCryptoPortfolioUseCase = addCryptoPortfolioUseCase,
        getUnoccupiedAccountIndexUseCase = getUnoccupiedAccountIndexUseCase,
        analyticsExceptionHandler = mockk(relaxed = true),
        analyticsEventHandler = mockk(relaxed = true),
        accountWalletPickerFactory = walletPickerFactory,
    )

    private val indexNotFound = GetUnoccupiedAccountIndexUseCase.Error.DataNotFound.left()

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