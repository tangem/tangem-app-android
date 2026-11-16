package com.tangem.feature.wallet.link

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.ds2.modal.TangemModalActionScope
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.registration.WalletLinkError
import com.tangem.domain.wallets.registration.WalletLinkRequester
import com.tangem.domain.wallets.usecase.LinkWalletToDeviceUseCase
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletLinkModelTest {

    private val linkWalletToDeviceUseCase: LinkWalletToDeviceUseCase = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxUnitFun = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    private val userWallet: UserWallet.Hot = mockk()
    private val actionScope = TangemModalActionScope {}

    private lateinit var model: WalletLinkModel

    @BeforeEach
    fun setup() {
        clearMocks(linkWalletToDeviceUseCase, uiMessageSender, analyticsEventHandler)
        model = WalletLinkModel(
            dispatchers = TestingCoroutineDispatcherProvider(),
            linkWalletToDeviceUseCase = linkWalletToDeviceUseCase,
            uiMessageSender = uiMessageSender,
            analyticsEventHandler = analyticsEventHandler,
        )
    }

    @AfterEach
    fun teardown() = model.onDestroy()

    @Test
    fun `GIVEN hidden sheet WHEN request THEN sheet is shown without progress`() = runTest {
        // Act
        requestLink(userWallet)

        // Assert
        assertThat(isLinkLoading()).isFalse()
    }

    @Test
    fun `GIVEN sheet shown WHEN link succeeds THEN sheet closes with Linked`() = runTest {
        // Arrange
        coEvery { linkWalletToDeviceUseCase(userWallet) } returns Unit.right()
        val result = requestLink(userWallet)

        // Act
        clickLink()

        // Assert
        assertThat(result.await()).isEqualTo(WalletLinkRequester.Result.Linked)
        assertThat(model.uiState.value).isNull()
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN rendered button WHEN it is clicked twice before recomposition THEN link is started once`() = runTest {
        // Arrange
        val pendingLink = CompletableDeferred<Unit>()
        coEvery { linkWalletToDeviceUseCase(userWallet) } coAnswers { pendingLink.await().right() }
        requestLink(userWallet)
        val renderedButton = requireNotNull(model.uiState.value).modal.buttons.single()

        // Act
        renderedButton.onClick(actionScope)
        renderedButton.onClick(actionScope)

        // Assert
        assertThat(isLinkLoading()).isTrue()
        coVerify(exactly = 1) { linkWalletToDeviceUseCase(userWallet) }
        pendingLink.complete(Unit)
    }

    @ParameterizedTest
    @MethodSource("provideSilentErrors")
    fun `GIVEN user cancels or scans another card WHEN link fails THEN sheet stays open without progress and error`(
        error: WalletLinkError,
    ) = runTest {
        // Arrange
        coEvery { linkWalletToDeviceUseCase(userWallet) } returns error.left()
        requestLink(userWallet)

        // Act
        clickLink()

        // Assert
        assertThat(isLinkLoading()).isFalse()
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN link fails WHEN link clicked THEN sheet closes with Dismissed and error is shown`(
        error: WalletLinkError,
    ) = runTest {
        // Arrange
        coEvery { linkWalletToDeviceUseCase(userWallet) } returns error.left()
        val result = requestLink(userWallet)

        // Act
        clickLink()

        // Assert
        assertThat(result.await()).isEqualTo(WalletLinkRequester.Result.Dismissed)
        assertThat(model.uiState.value).isNull()
        verify(exactly = 1) { uiMessageSender.send(any<DialogMessage>()) }
    }

    private fun provideTestModels() = listOf(WalletLinkError.Failed(reason = "api"), WalletLinkError.NotSupported)

    private fun provideSilentErrors() = listOf(WalletLinkError.Cancelled, WalletLinkError.WrongCard)

    @Test
    fun `GIVEN link in progress WHEN sheet dismissed THEN sheet closes with Dismissed`() = runTest {
        // Arrange
        coEvery { linkWalletToDeviceUseCase(userWallet) } coAnswers { CompletableDeferred<Unit>().await().right() }
        val result = requestLink(userWallet)
        clickLink()

        // Act
        model.onDismiss()

        // Assert
        assertThat(result.await()).isEqualTo(WalletLinkRequester.Result.Dismissed)
        assertThat(model.uiState.value).isNull()
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN request pending WHEN another wallet requested THEN each caller gets its own result`() = runTest {
        // Arrange
        val otherWallet: UserWallet.Hot = mockk()
        coEvery { linkWalletToDeviceUseCase(userWallet) } returns Unit.right()
        val firstResult = requestLink(userWallet)
        val secondResult = requestLink(otherWallet)

        // Act
        clickLink()
        runCurrent()
        model.onDismiss()

        // Assert
        assertThat(firstResult.await()).isEqualTo(WalletLinkRequester.Result.Linked)
        assertThat(secondResult.await()).isEqualTo(WalletLinkRequester.Result.Dismissed)
        coVerify(exactly = 0) { linkWalletToDeviceUseCase(otherWallet) }
    }

    @Test
    fun `GIVEN sheet shown WHEN link progress updates and next request comes THEN only the request changes the id`() =
        runTest {
            // Arrange
            val otherWallet: UserWallet.Hot = mockk()
            coEvery { linkWalletToDeviceUseCase(userWallet) } returns WalletLinkError.Cancelled.left()
            requestLink(userWallet)
            val firstRequestId = model.uiState.value?.requestId
            clickLink()
            val idAfterProgress = model.uiState.value?.requestId
            requestLink(otherWallet)

            // Act
            model.onDismiss()
            runCurrent()

            // Assert
            assertThat(idAfterProgress).isEqualTo(firstRequestId)
            assertThat(model.uiState.value?.requestId).isNotEqualTo(firstRequestId)
        }

    @Test
    fun `GIVEN link in progress WHEN caller cancelled THEN sheet closes and link is cancelled`() = runTest {
        // Arrange
        var isLinkCancelled = false
        coEvery { linkWalletToDeviceUseCase(userWallet) } coAnswers {
            try {
                awaitCancellation()
            } finally {
                isLinkCancelled = true
            }
        }
        val result = requestLink(userWallet)
        clickLink()

        // Act
        result.cancel()
        runCurrent()

        // Assert
        assertThat(model.uiState.value).isNull()
        assertThat(isLinkCancelled).isTrue()
    }

    @Test
    fun `GIVEN hidden sheet WHEN request THEN sheet shown is reported once`() = runTest {
        // Arrange
        coEvery { linkWalletToDeviceUseCase(userWallet) } returns WalletLinkError.WrongCard.left()

        // Act
        requestLink(userWallet)
        clickLink()

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(WalletLinkAnalyticsEvent.SheetShown()) }
    }

    @Test
    fun `GIVEN sheet shown WHEN link clicked THEN link button is reported`() = runTest {
        // Arrange
        coEvery { linkWalletToDeviceUseCase(userWallet) } returns Unit.right()
        requestLink(userWallet)

        // Act
        clickLink()

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(WalletLinkAnalyticsEvent.ButtonLinkWallet()) }
        verify(exactly = 0) { analyticsEventHandler.send(WalletLinkAnalyticsEvent.Canceled()) }
    }

    @ParameterizedTest
    @MethodSource("provideCancelCases")
    fun `GIVEN link fails WHEN link clicked THEN only a user cancel is reported as canceled`(
        error: WalletLinkError,
        isCanceledReported: Boolean,
    ) = runTest {
        // Arrange
        coEvery { linkWalletToDeviceUseCase(userWallet) } returns error.left()
        requestLink(userWallet)

        // Act
        clickLink()

        // Assert
        verify(exactly = if (isCanceledReported) 1 else 0) {
            analyticsEventHandler.send(WalletLinkAnalyticsEvent.Canceled())
        }
    }

    private fun provideCancelCases() = listOf(
        Arguments.of(WalletLinkError.Cancelled, true),
        Arguments.of(WalletLinkError.WrongCard, false),
        Arguments.of(WalletLinkError.NotSupported, false),
        Arguments.of(WalletLinkError.Failed(reason = "api"), false),
    )

    @Test
    fun `GIVEN sheet shown WHEN sheet dismissed THEN cancel is reported`() = runTest {
        // Arrange
        requestLink(userWallet)

        // Act
        model.onDismiss()

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(WalletLinkAnalyticsEvent.Canceled()) }
    }

    private fun TestScope.requestLink(wallet: UserWallet): Deferred<WalletLinkRequester.Result> {
        return backgroundScope.async(start = CoroutineStart.UNDISPATCHED) { model.request(wallet) }
    }

    private fun isLinkLoading(): Boolean = requireNotNull(model.uiState.value).modal.buttons.single().isLoading

    private fun clickLink() {
        val button = requireNotNull(model.uiState.value).modal.buttons.single()
        button.onClick(actionScope)
    }
}