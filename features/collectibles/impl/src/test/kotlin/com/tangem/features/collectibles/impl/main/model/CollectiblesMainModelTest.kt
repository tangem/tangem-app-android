package com.tangem.features.collectibles.impl.main.model

import com.arkivanov.decompose.router.slot.SlotNavigation
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
import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.collectibles.usecase.CloseCollectiblesVaultUseCase
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.delegate.CollectiblesHotDelegate
import com.tangem.domain.collectibles.delegate.HotDelegatePublicKey
import com.tangem.domain.collectibles.usecase.CreateOrReactivateCollectiblesAccountUseCase
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.StatusSource
import com.tangem.test.mock.collectibles.MockCollectibles
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.common.routing.AppRoute
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.addfunds.CollectiblesReceiveConfigFactory
import com.tangem.features.collectibles.impl.main.CollectiblesMainBottomSheetConfig
import com.tangem.features.collectibles.impl.main.CollectiblesMainComponent
import com.tangem.features.collectibles.impl.main.model.converter.CollectiblesBalanceConverter
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.utils.StringsSigns.DASH_SIGN
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@OptIn(ExperimentalCoroutinesApi::class)
internal class CollectiblesMainModelTest {

    private val router: Router = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val closeCollectiblesVaultUseCase: CloseCollectiblesVaultUseCase = mockk()
    private val createOrReactivateUseCase: CreateOrReactivateCollectiblesAccountUseCase = mockk()
    private val accountStatusSupplier: CollectiblesAccountStatusSupplier = mockk()
    private val accountRepository: CollectiblesAccountRepository = mockk()
    private val accountStatusFetcher: CollectiblesAccountStatusFetcher = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val hotDelegate: CollectiblesHotDelegate = mockk()
    private val clipboardManager: ClipboardManager = mockk(relaxed = true)

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
            createOrReactivateUseCase,
            accountStatusSupplier,
            accountRepository,
            accountStatusFetcher,
            getSelectedAppCurrencyUseCase,
            hotDelegate,
            clipboardManager,
        )
        walletsFlow = MutableStateFlow(listOf(coldWallet, selectedWallet))
        statusFlow = MutableStateFlow(CollectiblesAccountStatusValue.Loading)
        every { userWalletsListRepository.userWallets } returns walletsFlow
        every { accountStatusSupplier(coldWallet.walletId) } returns statusFlow
        every { accountRepository.getStatus(coldWallet.walletId) } returns statusFlow
        every { getSelectedAppCurrencyUseCase.invokeOrDefault() } returns MutableStateFlow(AppCurrency.Default)
        coEvery { accountStatusFetcher(any()) } returns Unit.right()
        coEvery { hotDelegate.getPublicKeyOrNull(any()) } returns HotDelegatePublicKey(HOT_DELEGATE)
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
    inner class AddFunds {

        @Test
        fun `GIVEN the vault is read WHEN from another wallet clicked THEN the sheet shows its token account`() =
            runTest {
                // Arrange
                val model = createModel(testScope = this)
                val slot = FakeSlotHost(model.bottomSheetNavigation)
                statusFlow.value = PRICED_VAULT
                advanceUntilIdle()

                // Act
                model.onFromAnotherWalletClick()
                advanceUntilIdle()

                // Assert
                val expected = CollectiblesMainBottomSheetConfig.Receive(
                    config = CollectiblesReceiveConfigFactory.create(
                        vault = requireNotNull(PRICED_VAULT.balance),
                        userWalletId = coldWallet.walletId,
                    ),
                )
                assertThat(slot.configuration).isEqualTo(expected)
                model.onDestroy()
            }

        @Test
        fun `GIVEN the vault has not been read WHEN from another wallet clicked THEN no sheet is shown`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            val slot = FakeSlotHost(model.bottomSheetNavigation)
            advanceUntilIdle()

            // Act
            model.onFromAnotherWalletClick()
            advanceUntilIdle()

            // Assert
            assertThat(slot.configuration).isNull()
            verify(exactly = 1) { messageSender.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the vault is read WHEN from tangem wallet clicked THEN the swap opens on its token`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            statusFlow.value = PRICED_VAULT
            advanceUntilIdle()

            // Act
            model.onFromTangemWalletClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                router.push(
                    match<AppRoute.Swap> { it.fromCryptoCurrency == MockCollectibles.usdc },
                    onComplete = any(),
                )
            }
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class HotDelegate {

        @Test
        fun `GIVEN a hot delegate WHEN its menu item clicked THEN the full address is copied`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.uiState.value.menu.items.single { it.id == "hot_delegate" }.onClick()

            // Assert
            verify(exactly = 1) { clipboardManager.setText(text = HOT_DELEGATE, isSensitive = false) }
            assertThat(model.uiState.value.menu.isShown).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN no hot delegate WHEN menu built THEN the item is absent`() = runTest {
            // Arrange
            coEvery { hotDelegate.getPublicKeyOrNull(any()) } returns null

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.menu.items.none { it.id == "hot_delegate" }).isTrue()
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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Relink {

        @Test
        fun `GIVEN the screen WHEN it is resumed twice THEN the vault is re-read each time`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onScreenResumed()
            model.onScreenResumed()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 2) {
                accountStatusFetcher(CollectiblesAccountStatusFetcher.Params(coldWallet.walletId))
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the vault answers to another phone WHEN state read THEN the relink banner is shown`() = runTest {
            // Arrange
            statusFlow.value = CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.ACTUAL)

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.shownRelink()?.isLoading).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN a relink is required and the balance is unpriced WHEN state read THEN the banner is shown`() =
            runTest {
                // Arrange
                every { accountRepository.getStatus(coldWallet.walletId) } returns MutableStateFlow(
                    CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.ACTUAL),
                )

                // Act
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Assert — the priced status is still Loading
                assertThat(model.shownRelink()?.isLoading).isFalse()
                model.onDestroy()
            }

        @Test
        fun `GIVEN an onboarded account WHEN state read THEN there is no relink banner`() = runTest {
            // Arrange
            statusFlow.value = CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL)

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.relink).isEqualTo(CollectiblesMainUM.RelinkUM.Hidden)
            model.onDestroy()
        }

        @Test
        fun `GIVEN the relink banner WHEN relink clicked THEN this wallet is relinked`() = runTest {
            // Arrange
            statusFlow.value = CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.ACTUAL)
            coEvery { createOrReactivateUseCase(coldWallet) } returns Unit.right()
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.shownRelink()?.onRelinkClick?.invoke()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { createOrReactivateUseCase(coldWallet) }
            verify(exactly = 0) { messageSender.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN relink succeeds WHEN the account is onboarded during it THEN the banner is hidden`() = runTest {
            // Arrange
            statusFlow.value = CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.ACTUAL)
            val gate = CompletableDeferred<Unit>()
            coEvery { createOrReactivateUseCase(coldWallet) } coAnswers {
                statusFlow.value = CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL)
                gate.await()
                Unit.right()
            }
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.shownRelink()?.onRelinkClick?.invoke()
            advanceUntilIdle()
            gate.complete(Unit)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.relink).isEqualTo(CollectiblesMainUM.RelinkUM.Hidden)
            model.onDestroy()
        }

        /** A card refusal is on the NFC sheet already and a cancellation is the user's choice: neither needs a word. */
        @ParameterizedTest
        @MethodSource("relinkFailures")
        fun `GIVEN relink fails WHEN relink clicked THEN only an unexplained failure is reported`(
            failure: CollectiblesAccountError,
            isReported: Boolean,
        ) = runTest {
            // Arrange
            statusFlow.value = CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.ACTUAL)
            coEvery { createOrReactivateUseCase(coldWallet) } returns failure.left()
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.shownRelink()?.onRelinkClick?.invoke()
            advanceUntilIdle()

            // Assert
            verify(exactly = if (isReported) 1 else 0) { messageSender.send(any()) }
            assertThat(model.shownRelink()?.isLoading).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN relink in progress WHEN relink clicked again THEN it runs once and shows loading`() = runTest {
            // Arrange
            statusFlow.value = CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.ACTUAL)
            val gate = CompletableDeferred<Unit>()
            coEvery { createOrReactivateUseCase(coldWallet) } coAnswers {
                gate.await()
                Unit.right()
            }
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.shownRelink()?.onRelinkClick?.invoke()
            advanceUntilIdle()
            model.shownRelink()?.onRelinkClick?.invoke()
            advanceUntilIdle()

            // Assert
            assertThat(model.shownRelink()?.isLoading).isTrue()
            gate.complete(Unit)
            advanceUntilIdle()
            coVerify(exactly = 1) { createOrReactivateUseCase(coldWallet) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the wallet is gone WHEN relink clicked THEN a failure is reported`() = runTest {
            // Arrange
            statusFlow.value = CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.ACTUAL)
            val model = createModel(testScope = this)
            advanceUntilIdle()
            walletsFlow.value = listOf(selectedWallet)

            // Act
            model.shownRelink()?.onRelinkClick?.invoke()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { messageSender.send(any()) }
            coVerify(exactly = 0) { createOrReactivateUseCase(any()) }
            model.onDestroy()
        }

        private fun relinkFailures() = listOf(
            Arguments.of(CollectiblesAccountError.DelegateKeyCreationFailed(IllegalStateException("keystore")), true),
            Arguments.of(CollectiblesAccountError.CardError(code = 1, message = "refused"), false),
            Arguments.of(CollectiblesAccountError.Cancelled, false),
        )
    }

    private fun CollectiblesMainModel.shownRelink() = uiState.value.relink as? CollectiblesMainUM.RelinkUM.Shown

    private fun createModel(testScope: TestScope): CollectiblesMainModel {
        return CollectiblesMainModel(
            paramsContainer = MutableParamsContainer(
                CollectiblesMainComponent.Params(userWalletId = coldWallet.walletId),
            ),
            router = router,
            messageSender = messageSender,
            userWalletsListRepository = userWalletsListRepository,
            closeCollectiblesVaultUseCase = closeCollectiblesVaultUseCase,
            createOrReactivateCollectiblesAccountUseCase = createOrReactivateUseCase,
            collectiblesAccountStatusSupplier = accountStatusSupplier,
            collectiblesAccountRepository = accountRepository,
            collectiblesAccountStatusFetcher = accountStatusFetcher,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            hotDelegate = hotDelegate,
            clipboardManager = clipboardManager,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        )
    }

    /**
     * Minimal stand-in for `childSlot`: tracks the active configuration and invokes each navigation
     * event's completion callback, which `DefaultSlotNavigation` leaves to its host.
     */
    private class FakeSlotHost<C : Any>(navigation: SlotNavigation<C>) {

        var configuration: C? = null
            private set

        init {
            navigation.subscribe { event ->
                val oldConfiguration = configuration
                configuration = event.transformer(oldConfiguration)
                event.onComplete(configuration, oldConfiguration)
            }
        }
    }

    private companion object {

        const val HOT_DELEGATE = "3hTCy6rJu8iXNWnFMB741HrVnbS5bViyfguEAMF5iT2P"

        val PRICED_VAULT = CollectiblesAccountStatusValue.Onboarded(
            source = StatusSource.ACTUAL,
            vaultBalance = MockCollectibles.vaultBalance(amount = BigDecimal("25.5"), fiatRate = BigDecimal.ONE),
        )
    }
}