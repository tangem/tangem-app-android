package com.tangem.features.introduction.impl.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.SelectWalletError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.domain.sociallogin.usecase.GetAvailableSocialLoginProvidersUseCase
import com.tangem.features.hotwallet.SocialLoginUseCase
import com.tangem.features.hotwallet.SocialLoginOutcome
import com.tangem.features.hotwallet.SocialLoginStage
import com.tangem.features.introduction.impl.ui.state.SocialLoginProgressUM
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IntroductionSocialLoginHandlerTest {

    private val socialLoginUseCase: SocialLoginUseCase = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)

    private val getAvailableSocialLoginProvidersUseCase: GetAvailableSocialLoginProvidersUseCase = mockk()

    private val handler = IntroductionSocialLoginHandler(
        getAvailableSocialLoginProvidersUseCase = getAvailableSocialLoginProvidersUseCase,
        socialLoginUseCase = socialLoginUseCase,
        userWalletsListRepository = userWalletsListRepository,
        router = router,
        uiMessageSender = uiMessageSender,
    )

    private val host = RecordingHost()

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            getAvailableSocialLoginProvidersUseCase,
            socialLoginUseCase,
            userWalletsListRepository,
            router,
            uiMessageSender,
        )
        host.reset()
    }

    @Test
    fun `GIVEN wallet created WHEN signIn THEN wallet is selected and the main screen is opened`() = runTest {
        // Arrange
        coEvery {
            socialLoginUseCase.signIn(PROVIDER, any())
        } returns SocialLoginOutcome.WalletCreated(WALLET_ID).right()

        // Act
        handler.signIn(PROVIDER, host)

        // Assert
        coVerify(exactly = 1) { userWalletsListRepository.select(WALLET_ID) }
        verify(exactly = 1) { router.replaceAll(AppRoute.Wallet) }
        verify(exactly = 1) { uiMessageSender.send(any<SnackbarMessage>()) }
        assertThat(host.finishedCount).isEqualTo(1)
    }

    @Test
    fun `GIVEN the wallet cannot be selected WHEN signIn THEN the main screen stays closed`() = runTest {
        // Arrange — opening the portfolio of whichever wallet was selected before would read as success
        coEvery {
            socialLoginUseCase.signIn(PROVIDER, any())
        } returns SocialLoginOutcome.WalletCreated(WALLET_ID).right()
        coEvery { userWalletsListRepository.select(WALLET_ID) } returns SELECT_ERROR.left()

        // Act
        handler.signIn(PROVIDER, host)

        // Assert
        verify { router wasNot Called }
        verify(exactly = 1) { uiMessageSender.send(any<SnackbarMessage>()) }
    }

    @Test
    fun `GIVEN viewer backed out WHEN signIn THEN provider is signed out and a message is shown`() = runTest {
        // Arrange
        coEvery { socialLoginUseCase.signIn(PROVIDER, any()) } returns SocialLoginError.SignInCanceled.left()

        // Act
        handler.signIn(PROVIDER, host)

        // Assert
        coVerify(exactly = 1) { socialLoginUseCase.signOut(PROVIDER) }
        verify { router wasNot Called }
        coVerify(exactly = 0) { userWalletsListRepository.select(any()) }
    }

    @Test
    fun `GIVEN sign-in failed WHEN signIn THEN the entry points reopen only after the sign-out`() = runTest {
        // Arrange — a retry entering signIn while the sign-out still runs would read a half-cleared account
        val order = mutableListOf<String>()
        coEvery { socialLoginUseCase.signIn(PROVIDER, any()) } returns SocialLoginError.NetworkError.left()
        coEvery { socialLoginUseCase.signOut(PROVIDER) } answers { order += "signOut" }
        host.onFinishedCallback = { order += "finished" }

        // Act
        handler.signIn(PROVIDER, host)

        // Assert
        assertThat(order).containsExactly("signOut", "finished").inOrder()
    }

    @Test
    fun `GIVEN stages reported WHEN signIn THEN progress is shown and then cleared`() = runTest {
        // Arrange
        val onStageChanged = slot<(SocialLoginStage) -> Unit>()
        coEvery { socialLoginUseCase.signIn(PROVIDER, capture(onStageChanged)) } answers {
            onStageChanged.captured(SocialLoginStage.SigningIn)
            onStageChanged.captured(SocialLoginStage.VerifyingAccount)
            onStageChanged.captured(SocialLoginStage.RestoringWallet)
            SocialLoginOutcome.WalletRestored(WALLET_ID).right()
        }

        // Act
        handler.signIn(PROVIDER, host)

        // Assert — the provider owns the screen during SigningIn, hence the leading null
        assertThat(host.progress).hasSize(4)
        assertThat(host.progress[0]).isNull()
        assertThat(host.progress[1]).isNotNull()
        assertThat(host.progress[2]).isNotNull()
        assertThat(host.progress[3]).isNull()
    }

    private class RecordingHost : IntroductionSocialLoginHandler.Host {

        val progress = mutableListOf<SocialLoginProgressUM?>()
        var finishedCount = 0
            private set

        var onFinishedCallback: () -> Unit = {}

        override fun onProgress(progress: SocialLoginProgressUM?) {
            this.progress += progress
        }

        override fun onFinished() {
            finishedCount++
            onFinishedCallback()
        }

        fun reset() {
            progress.clear()
            finishedCount = 0
            onFinishedCallback = {}
        }
    }

    private companion object {
        val PROVIDER = SocialLoginProvider.Google
        val WALLET_ID = UserWalletId(stringValue = "011")
        val SELECT_ERROR = SelectWalletError.UnableToSelectUserWallet
    }
}