package com.tangem.features.moneyaccount.main.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.model.IssueMoneyAccountError
import com.tangem.domain.moneyaccount.usecase.IssueMoneyAccountUseCase
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.moneyaccount.main.component.MoneyAccountActivationComponent
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class MoneyAccountActivationModelTest {

    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val issueMoneyAccountUseCase: IssueMoneyAccountUseCase = mockk()
    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val onDismiss: () -> Unit = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(getUserWalletUseCase, issueMoneyAccountUseCase, router, uiMessageSender, urlOpener, onDismiss)
        every { getUserWalletUseCase(USER_WALLET_ID) } returns mockk<UserWallet.Cold>().right()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Init {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN user wallet WHEN model created THEN cold wallet flag resolved`(model: WalletModel) = runTest {
            // Arrange
            every { getUserWalletUseCase(USER_WALLET_ID) } returns model.userWallet

            // Act
            val activationModel = createModel()

            // Assert
            assertThat(activationModel.uiState.value.isColdWallet).isEqualTo(model.expectedIsColdWallet)
            assertThat(activationModel.uiState.value.isActivationInProgress).isFalse()
            activationModel.onDestroy()
        }

        private fun provideTestModels() = listOf(
            WalletModel(name = "cold", userWallet = mockk<UserWallet.Cold>().right(), expectedIsColdWallet = true),
            WalletModel(name = "hot", userWallet = mockk<UserWallet.Hot>().right(), expectedIsColdWallet = false),
            WalletModel(
                name = "not found",
                userWallet = GetUserWalletError.UserWalletNotFound.left(),
                expectedIsColdWallet = false,
            ),
        )
    }

    @Nested
    inner class OnActivateClick {

        @Test
        fun `GIVEN issuing in progress WHEN activate clicked THEN progress shown`() = runTest {
            // Arrange
            val result = CompletableDeferred<Either<IssueMoneyAccountError, Unit>>()
            coEvery { issueMoneyAccountUseCase(USER_WALLET_ID) } coAnswers { result.await() }
            val activationModel = createModel()

            // Act
            activationModel.uiState.value.onActivateClick()
            advanceUntilIdle()

            // Assert
            assertThat(activationModel.uiState.value.isActivationInProgress).isTrue()
            result.complete(Unit.right())
            advanceUntilIdle()
            activationModel.onDestroy()
        }

        @Test
        fun `GIVEN issuing in progress WHEN activate clicked again THEN issued once`() = runTest {
            // Arrange
            val result = CompletableDeferred<Either<IssueMoneyAccountError, Unit>>()
            coEvery { issueMoneyAccountUseCase(USER_WALLET_ID) } coAnswers { result.await() }
            val activationModel = createModel()
            activationModel.uiState.value.onActivateClick()
            advanceUntilIdle()

            // Act
            activationModel.uiState.value.onActivateClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { issueMoneyAccountUseCase(USER_WALLET_ID) }
            result.complete(Unit.right())
            advanceUntilIdle()
            activationModel.onDestroy()
        }

        @Test
        fun `GIVEN issuing succeeds WHEN activate clicked THEN sheet dismissed and success screen opened`() = runTest {
            // Arrange
            coEvery { issueMoneyAccountUseCase(USER_WALLET_ID) } returns Unit.right()
            val activationModel = createModel()

            // Act
            activationModel.uiState.value.onActivateClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { onDismiss() }
            verify(exactly = 1) {
                router.push(
                    AppRoute.TangemPayOnboardingSuccess(
                        product = AppRoute.TangemPayOnboardingSuccess.Product.MONEY_ACCOUNT,
                    ),
                )
            }
            verify(exactly = 0) { uiMessageSender.send(any()) }
            activationModel.onDestroy()
        }

        @Test
        fun `GIVEN issuing fails WHEN activate clicked THEN progress hidden and error dialog shown`() = runTest {
            // Arrange
            coEvery { issueMoneyAccountUseCase(USER_WALLET_ID) } returns IssueMoneyAccountError.Failed.left()
            val activationModel = createModel()

            // Act
            activationModel.uiState.value.onActivateClick()
            advanceUntilIdle()

            // Assert
            assertThat(activationModel.uiState.value.isActivationInProgress).isFalse()
            verify(exactly = 1) { uiMessageSender.send(any<DialogMessage>()) }
            verify(exactly = 0) { router.push(any(), any()) }
            verify(exactly = 0) { onDismiss() }
            activationModel.onDestroy()
        }

        @Test
        fun `GIVEN user cancelled WHEN activate clicked THEN progress hidden and no dialog`() = runTest {
            // Arrange
            coEvery { issueMoneyAccountUseCase(USER_WALLET_ID) } returns IssueMoneyAccountError.UserCancelled.left()
            val activationModel = createModel()

            // Act
            activationModel.uiState.value.onActivateClick()
            advanceUntilIdle()

            // Assert
            assertThat(activationModel.uiState.value.isActivationInProgress).isFalse()
            verify(exactly = 0) { uiMessageSender.send(any()) }
            verify(exactly = 0) { router.push(any(), any()) }
            activationModel.onDestroy()
        }
    }

    @Test
    fun `GIVEN opened activation WHEN terms clicked THEN tangem pay terms opened`() = runTest {
        // Arrange
        val activationModel = createModel()

        // Act
        activationModel.uiState.value.onTermsClick()

        // Assert
        verify(exactly = 1) { urlOpener.openUrl(match { it.startsWith(TERMS_URL) }) }
        activationModel.onDestroy()
    }

    @Test
    fun `GIVEN opened activation WHEN dismissed THEN dismiss callback invoked`() = runTest {
        // Arrange
        val activationModel = createModel()

        // Act
        activationModel.uiState.value.onDismiss()

        // Assert
        verify(exactly = 1) { onDismiss() }
        activationModel.onDestroy()
    }

    data class WalletModel(
        val name: String,
        val userWallet: Either<GetUserWalletError, UserWallet>,
        val expectedIsColdWallet: Boolean,
    ) {
        override fun toString(): String = name
    }

    private fun TestScope.createModel() = MoneyAccountActivationModel(
        paramsContainer = MutableParamsContainer(
            MoneyAccountActivationComponent.Params(userWalletId = USER_WALLET_ID, onDismiss = onDismiss),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        getUserWalletUseCase = getUserWalletUseCase,
        issueMoneyAccountUseCase = issueMoneyAccountUseCase,
        router = router,
        uiMessageSender = uiMessageSender,
        urlOpener = urlOpener,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("0011")
        const val TERMS_URL = "https://paera.com/tangempay"
    }
}