package com.tangem.features.collectibles.impl.main.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.collectibles.usecase.CloseCollectiblesVaultUseCase
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.main.CollectiblesMainComponent
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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

    private val coldWallet = MockUserWalletFactory.create()

    /** The app-selected wallet, deliberately not the one the screen was opened for. */
    private val selectedWallet = coldWallet.copy(walletId = UserWalletId("bb"))

    private lateinit var walletsFlow: MutableStateFlow<List<UserWallet>?>

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, messageSender, userWalletsListRepository, closeCollectiblesVaultUseCase)
        walletsFlow = MutableStateFlow(listOf(coldWallet, selectedWallet))
        every { userWalletsListRepository.userWallets } returns walletsFlow
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Chrome {

        @Test
        fun `GIVEN model created WHEN state read THEN balance comes from the stub data`() = runTest {
            // Act
            val model = createModel(testScope = this)

            // Assert
            assertThat(model.uiState.value.balance.crypto)
                .isEqualTo(stringReference(CollectiblesMainStubData.BALANCE.crypto))
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
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        )
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }
}