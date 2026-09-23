package com.tangem.features.collectibles.impl.main.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusFetcher
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusSupplier
import com.tangem.domain.collectibles.usecase.CloseCollectiblesVaultUseCase
import com.tangem.domain.collectibles.usecase.GetCollectiblesVaultTopUpAddressUseCase
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.StatusSource
import com.tangem.test.mock.collectibles.MockCollectibles
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.main.CollectiblesMainComponent
import com.tangem.features.collectibles.impl.main.model.converter.CollectiblesBalanceConverter
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.utils.StringsSigns.DASH_SIGN
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@OptIn(ExperimentalCoroutinesApi::class)
internal class CollectiblesMainModelTest {

    private val router: Router = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val closeCollectiblesVaultUseCase: CloseCollectiblesVaultUseCase = mockk()
    private val getTopUpAddressUseCase: GetCollectiblesVaultTopUpAddressUseCase = mockk()
    private val clipboardManager: ClipboardManager = mockk(relaxUnitFun = true)
    private val accountStatusSupplier: CollectiblesAccountStatusSupplier = mockk()
    private val accountStatusFetcher: CollectiblesAccountStatusFetcher = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()

    private val coldWallet = MockUserWalletFactory.create()

    /** The app-selected wallet, deliberately not the one the screen was opened for. */
    private val selectedWallet = coldWallet.copy(walletId = UserWalletId("bb"))

    private lateinit var walletsFlow: MutableStateFlow<List<UserWallet>?>
    private lateinit var statusFlow: MutableStateFlow<CollectiblesAccountStatusValue>

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            messageSender,
            userWalletsListRepository,
            closeCollectiblesVaultUseCase,
            getTopUpAddressUseCase,
            clipboardManager,
            accountStatusSupplier,
            accountStatusFetcher,
            getSelectedAppCurrencyUseCase,
        )
        walletsFlow = MutableStateFlow(listOf(coldWallet, selectedWallet))
        statusFlow = MutableStateFlow(CollectiblesAccountStatusValue.Loading)
        every { userWalletsListRepository.userWallets } returns walletsFlow
        every { accountStatusSupplier(coldWallet.walletId) } returns statusFlow
        every { getSelectedAppCurrencyUseCase.invokeOrDefault() } returns MutableStateFlow(AppCurrency.Default)
        coEvery { accountStatusFetcher(any()) } returns Unit.right()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Chrome {

        @Test
        fun `GIVEN an unread vault WHEN state read THEN the balance is a dash`() = runTest {
            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.balance.crypto).isEqualTo(stringReference(DASH_SIGN))
            assertThat(model.uiState.value.balance.fiatPlain).isEqualTo(stringReference(DASH_SIGN))
            model.onDestroy()
        }

        @Test
        fun `GIVEN a priced vault WHEN state read THEN the balance is the account's own`() = runTest {
            // Arrange
            val model = createModel(testScope = this)

            // Act
            statusFlow.value = PRICED_VAULT
            advanceUntilIdle()

            // Assert
            val expected = CollectiblesBalanceConverter().convert(
                CollectiblesBalanceConverter.Input(status = PRICED_VAULT, appCurrency = AppCurrency.Default),
            )
            assertThat(model.uiState.value.balance).isEqualTo(expected)
            assertThat(model.uiState.value.balance.crypto).isNotEqualTo(stringReference(DASH_SIGN))
            model.onDestroy()
        }

        @Test
        fun `GIVEN model created WHEN back clicked THEN the feature is popped`() = runTest {
            // Arrange
            val model = createModel(testScope = this)

            // Act
            model.uiState.value.onBackClick()

            // Assert
            verify(exactly = 1) { router.pop(onComplete = any()) }
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Menu {

        @Test
        fun `GIVEN model created WHEN menu clicked THEN menu is shown`() = runTest {
            // Arrange
            val model = createModel(testScope = this)

            // Act
            model.uiState.value.menu.onMenuClick()

            // Assert
            assertThat(model.uiState.value.menu.isShown).isTrue()
            model.onDestroy()
        }

        @Test
        fun `GIVEN menu shown WHEN dismissed THEN menu is hidden`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            model.uiState.value.menu.onMenuClick()

            // Act
            model.uiState.value.menu.onDismiss()

            // Assert
            assertThat(model.uiState.value.menu.isShown).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN menu shown WHEN how it works clicked THEN stories open`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            model.uiState.value.menu.onMenuClick()

            // Act
            model.uiState.value.menu.items.single { it.id == "how_it_works" }.onClick()

            // Assert
            verify(exactly = 1) { router.push(CollectiblesRoute.Stories, onComplete = any()) }
            assertThat(model.uiState.value.menu.isShown).isFalse()
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CopyTopUpAddress {

        @Test
        fun `GIVEN the address is read WHEN copy clicked THEN it is put on the clipboard`() = runTest {
            // Arrange
            coEvery { getTopUpAddressUseCase(coldWallet) } returns TOP_UP_ADDRESS.right()
            val model = createModel(testScope = this)
            model.uiState.value.menu.onMenuClick()

            // Act
            model.uiState.value.menu.items.single { it.id == "copy_top_up_address" }.onClick()
            advanceUntilIdle()

            // Assert — the address is public, so it is not a sensitive copy
            verify(exactly = 1) { clipboardManager.setText(text = TOP_UP_ADDRESS, isSensitive = false, label = any()) }
            verify(exactly = 1) { messageSender.send(any()) }
            assertThat(model.uiState.value.menu.isShown).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN the address cannot be read WHEN copy clicked THEN nothing is copied`() = runTest {
            // Arrange
            coEvery { getTopUpAddressUseCase(coldWallet) } returns
                CollectiblesAccountError.Vault(GachaVaultError.WalletUnavailable).left()
            val model = createModel(testScope = this)

            // Act
            model.uiState.value.menu.items.single { it.id == "copy_top_up_address" }.onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { clipboardManager.setText(any(), any(), any()) }
            verify(exactly = 1) { messageSender.send(any()) }
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CloseAccount {

        @Test
        fun `GIVEN the vault closes WHEN close account clicked THEN the feature is popped`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            coEvery { closeCollectiblesVaultUseCase(coldWallet) } returns Unit.right()

            // Act
            model.uiState.value.menu.items.single { it.id == "close_account" }.onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { router.pop(onComplete = any()) }
            assertThat(model.uiState.value.menu.isShown).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN another wallet is selected WHEN close account clicked THEN the screen's wallet is closed`() =
            runTest {
                // Arrange
                val model = createModel(testScope = this)
                coEvery { closeCollectiblesVaultUseCase(any()) } returns Unit.right()

                // Act
                model.uiState.value.menu.items.single { it.id == "close_account" }.onClick()
                advanceUntilIdle()

                // Assert
                coVerify(exactly = 1) { closeCollectiblesVaultUseCase(coldWallet) }
                coVerify(exactly = 0) { closeCollectiblesVaultUseCase(selectedWallet) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN the card refuses WHEN close account clicked THEN the account is kept`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            coEvery { closeCollectiblesVaultUseCase(coldWallet) } returns
                CollectiblesAccountError.CardError(code = 50002, message = "50002").left()

            // Act
            model.uiState.value.menu.items.single { it.id == "close_account" }.onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { router.pop(onComplete = any()) }
            verify(exactly = 0) { messageSender.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the chain refuses WHEN close account clicked THEN a message is shown and the account is kept`() =
            runTest {
                // Arrange
                val model = createModel(testScope = this)
                coEvery { closeCollectiblesVaultUseCase(coldWallet) } returns
                    CollectiblesAccountError.Vault(
                        GachaVaultError.NetworkUnsupported,
                    ).left()

                // Act
                model.uiState.value.menu.items.single { it.id == "close_account" }.onClick()
                advanceUntilIdle()

                // Assert
                verify(exactly = 0) { router.pop(onComplete = any()) }
                verify(exactly = 1) { messageSender.send(any()) }
                model.onDestroy()
            }
    }

    private fun createModel(testScope: TestScope): CollectiblesMainModel {
        return CollectiblesMainModel(
            paramsContainer = MutableParamsContainer(
                CollectiblesMainComponent.Params(userWalletId = coldWallet.walletId),
            ),
            router = router,
            messageSender = messageSender,
            userWalletsListRepository = userWalletsListRepository,
            closeCollectiblesVaultUseCase = closeCollectiblesVaultUseCase,
            getCollectiblesVaultTopUpAddressUseCase = getTopUpAddressUseCase,
            clipboardManager = clipboardManager,
            collectiblesAccountStatusSupplier = accountStatusSupplier,
            collectiblesAccountStatusFetcher = accountStatusFetcher,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        )
    }

    private companion object {

        const val TOP_UP_ADDRESS = "9vJc1bnfHRLbGeDdQCrmJFAsEtR9SEbNJVi8QeHfAo8f"

        val PRICED_VAULT = CollectiblesAccountStatusValue.Onboarded(
            source = StatusSource.ACTUAL,
            vaultBalance = MockCollectibles.vaultBalance(amount = BigDecimal("25.5"), fiatRate = BigDecimal.ONE),
        )
    }
}