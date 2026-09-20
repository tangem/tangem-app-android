package com.tangem.features.collectibles.impl.onboarding.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.collectibles.usecase.CreateOrReactivateCollectiblesAccountUseCase
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.onboarding.CollectiblesOnboardingComponent
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class CollectiblesOnboardingModelTest {

    private val router: Router = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val createOrReactivateCollectiblesAccountUseCase: CreateOrReactivateCollectiblesAccountUseCase = mockk()

    private val coldWallet = MockUserWalletFactory.create()

    private lateinit var walletsFlow: MutableStateFlow<List<UserWallet>?>

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            urlOpener,
            messageSender,
            userWalletsListRepository,
            createOrReactivateCollectiblesAccountUseCase,
        )
        walletsFlow = MutableStateFlow(listOf(coldWallet))
        every { userWalletsListRepository.userWallets } returns walletsFlow
    }

    @Test
    fun `GIVEN account prepared WHEN create account clicked THEN stories open on top of main`() = runTest {
        // Arrange
        coEvery { createOrReactivateCollectiblesAccountUseCase(any()) } returns Unit.right()
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onCreateAccountClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            router.replaceAll(
                CollectiblesRoute.Main(userWalletId = coldWallet.walletId),
                CollectiblesRoute.Stories,
                onComplete = any(),
            )
        }
        assertThat(model.uiState.value.isCreatingAccount).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN the settled wallet is gone WHEN create account clicked THEN a dialog is shown over the screen`() =
        runTest {
            // Arrange
            walletsFlow.value = emptyList()
            val model = createModel(testScope = this)
            val message = slot<UiMessage>()

            // Act
            model.uiState.value.onCreateAccountClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { messageSender.send(capture(message)) }
            assertThat(message.captured).isInstanceOf(DialogMessage::class.java)
            verify { router wasNot Called }
            coVerify(exactly = 0) { createOrReactivateCollectiblesAccountUseCase(any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN card prompt dismissed WHEN create account clicked THEN the screen is kept for a retry`() = runTest {
        // Arrange
        coEvery { createOrReactivateCollectiblesAccountUseCase(any()) } returns CollectiblesAccountError.Cancelled.left()
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onCreateAccountClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { messageSender.send(any()) }
        verify { router wasNot Called }
        assertThat(model.uiState.value.isCreatingAccount).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN the card reported the failure WHEN create account clicked THEN no message is shown`() = runTest {
        // Arrange
        coEvery { createOrReactivateCollectiblesAccountUseCase(any()) } returns
            CollectiblesAccountError.CardError(code = 40002, message = "wallet not found").left()
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onCreateAccountClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { messageSender.send(any()) }
        verify { router wasNot Called }
        assertThat(model.uiState.value.isCreatingAccount).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN the chain refuses WHEN create account clicked THEN a message is shown and the screen is kept`() =
        runTest {
            // Arrange
                coEvery { createOrReactivateCollectiblesAccountUseCase(any()) } returns
                CollectiblesAccountError.Vault(GachaVaultError.WalletUnavailable).left()
            val model = createModel(testScope = this)

            // Act
            model.uiState.value.onCreateAccountClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { messageSender.send(any()) }
            verify { router wasNot Called }
            model.onDestroy()
        }

    @Test
    fun `GIVEN a click is in flight WHEN clicked again THEN the use case runs once`() = runTest {
        // Arrange
        coEvery { createOrReactivateCollectiblesAccountUseCase(any()) } returns Unit.right()
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onCreateAccountClick()
        model.uiState.value.onCreateAccountClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { createOrReactivateCollectiblesAccountUseCase(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN model created WHEN close clicked THEN pops the stack`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onCloseClick()

        // Assert
        verify(exactly = 1) { router.pop(onComplete = any()) }
        model.onDestroy()
    }

    private fun createModel(testScope: TestScope): CollectiblesOnboardingModel {
        return CollectiblesOnboardingModel(
            paramsContainer = MutableParamsContainer(
                CollectiblesOnboardingComponent.Params(userWalletId = coldWallet.walletId),
            ),
            router = router,
            urlOpener = urlOpener,
            messageSender = messageSender,
            userWalletsListRepository = userWalletsListRepository,
            createOrReactivateCollectiblesAccountUseCase = createOrReactivateCollectiblesAccountUseCase,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        )
    }
}