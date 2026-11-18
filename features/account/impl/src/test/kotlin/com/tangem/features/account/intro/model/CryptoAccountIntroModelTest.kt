package com.tangem.features.account.intro.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.account.accountLimitReachedDialog
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.commonfeatures.api.portfolioselector.AccountWalletPicker
import com.tangem.features.commonfeatures.api.portfolioselector.AccountWalletPicker.Pick
import com.tangem.features.commonfeatures.api.portfolioselector.AccountWalletPicker.Resolution
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CryptoAccountIntroModelTest {

    private val router: Router = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val walletPicker: AccountWalletPicker = mockk(relaxed = true)
    private val walletPickerFactory: AccountWalletPicker.Factory = mockk()

    private val picks = MutableSharedFlow<Pick>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, messageSender, walletPicker, walletPickerFactory)
        every { walletPickerFactory.create(any()) } returns walletPicker
        every { walletPicker.picks } returns picks
    }

    @Test
    fun `GIVEN the only wallet has room WHEN continue THEN creation opens in it in place of the intro`() = runTest {
        // Arrange
        coEvery { walletPicker.resolve(any()) } returns Resolution.Single(WALLET_A)
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.onContinueClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.replaceCurrent(AppRoute.CreateAccount(userWalletId = WALLET_A)) }
    }

    @Test
    fun `GIVEN several wallets WHEN continue THEN chooser opens with the full ones marked`() = runTest {
        // Arrange
        coEvery { walletPicker.resolve(any()) } returns Resolution.Choose(fullWalletIds = setOf(WALLET_B))
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.onContinueClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { walletPicker.openChooser(setOf(WALLET_B)) }
        verify(exactly = 0) { router.replaceCurrent(any()) }
    }

    @Test
    fun `GIVEN chooser open WHEN a wallet with room picked THEN creation opens in it`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        picks.emit(Pick.Wallet(WALLET_B))
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.replaceCurrent(AppRoute.CreateAccount(userWalletId = WALLET_B)) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN chooser open WHEN a full wallet picked THEN limit dialog shown`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        picks.emit(Pick.Full)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { messageSender.send(match(::isLimitDialog)) }
        verify(exactly = 0) { router.replaceCurrent(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the only wallet is full WHEN continue THEN limit dialog shown`() = runTest {
        // Arrange
        coEvery { walletPicker.resolve(any()) } returns Resolution.LimitReached
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.onContinueClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { messageSender.send(match(::isLimitDialog)) }
        verify(exactly = 0) { router.replaceCurrent(any()) }
    }

    @Test
    fun `GIVEN account lists unavailable WHEN continue THEN error dialog shown`() = runTest {
        // Arrange
        coEvery { walletPicker.resolve(any()) } returns Resolution.Unavailable
        val model = createModel()
        advanceUntilIdle()
        val message = slot<DialogMessage>()

        // Act
        model.uiState.value.onContinueClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { messageSender.send(capture(message)) }
        assertThat(isLimitDialog(message.captured)).isFalse()
        verify(exactly = 0) { router.replaceCurrent(any()) }
    }

    @Test
    fun `GIVEN resolution pending WHEN continue tapped twice THEN button loads and resolves once`() = runTest {
        // Arrange
        val resolution = CompletableDeferred<Resolution>()
        coEvery { walletPicker.resolve(any()) } coAnswers { resolution.await() }
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.onContinueClick()
        advanceUntilIdle()
        val isLoadingWhilePending = model.uiState.value.isContinueLoading
        model.uiState.value.onContinueClick()
        resolution.complete(Resolution.Single(WALLET_A))
        advanceUntilIdle()

        // Assert
        assertThat(isLoadingWhilePending).isTrue()
        assertThat(model.uiState.value.isContinueLoading).isFalse()
        coVerify(exactly = 1) { walletPicker.resolve(any()) }
    }

    private fun isLimitDialog(message: UiMessage): Boolean = (message as? DialogMessage)?.message ==
        accountLimitReachedDialog(limit = AccountList.MAX_CRYPTO_PORTFOLIO_ACCOUNTS_COUNT).message

    private fun TestScope.createModel() = CryptoAccountIntroModel(
        dispatchers = createTestingCoroutineDispatcherProvider(),
        router = router,
        messageSender = messageSender,
        accountWalletPickerFactory = walletPickerFactory,
    )

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")
    }
}