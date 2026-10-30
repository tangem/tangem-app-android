package com.tangem.feature.wallet.link

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
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
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletLinkModelTest {

    private val linkWalletToDeviceUseCase: LinkWalletToDeviceUseCase = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxUnitFun = true)
    private val userWallet: UserWallet.Hot = mockk()
    private val actionScope = TangemModalActionScope {}

    private lateinit var model: WalletLinkModel

    @BeforeEach
    fun setup() {
        clearMocks(linkWalletToDeviceUseCase, uiMessageSender)
        model = WalletLinkModel(
            dispatchers = TestingCoroutineDispatcherProvider(),
            linkWalletToDeviceUseCase = linkWalletToDeviceUseCase,
            uiMessageSender = uiMessageSender,
        )
    }

    @AfterEach
    fun teardown() = model.onDestroy()

    @Test
    fun `GIVEN hidden sheet WHEN show THEN sheet is shown without progress`() {
        // Act
        model.show(userWallet)

        // Assert
        assertThat(model.uiState.value?.buttons?.single()?.isLoading).isFalse()
    }

    @Test
    fun `GIVEN sheet shown WHEN link succeeds THEN sheet closes with Linked`() = runTest {
        // Arrange
        coEvery { linkWalletToDeviceUseCase(userWallet) } returns Unit.right()
        model.show(userWallet)
        val result = async { model.waitResult() }

        // Act
        clickLink()

        // Assert
        assertThat(result.await()).isEqualTo(WalletLinkRequester.Result.Linked)
        assertThat(model.uiState.value).isNull()
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN link in progress WHEN link clicked again THEN link is not started twice`() = runTest {
        // Arrange
        val pendingLink = CompletableDeferred<Unit>()
        coEvery { linkWalletToDeviceUseCase(userWallet) } coAnswers { pendingLink.await().right() }
        model.show(userWallet)
        clickLink()

        // Act
        clickLink()

        // Assert
        assertThat(model.uiState.value?.buttons?.single()?.isLoading).isTrue()
        coVerify(exactly = 1) { linkWalletToDeviceUseCase(userWallet) }
        pendingLink.complete(Unit)
    }

    @Test
    fun `GIVEN user cancels WHEN link fails THEN sheet stays open without progress and error`() = runTest {
        // Arrange
        coEvery { linkWalletToDeviceUseCase(userWallet) } returns WalletLinkError.Cancelled.left()
        model.show(userWallet)

        // Act
        clickLink()

        // Assert
        assertThat(model.uiState.value?.buttons?.single()?.isLoading).isFalse()
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN link fails WHEN link clicked THEN sheet closes with Dismissed and error is shown`(
        error: WalletLinkError,
    ) = runTest {
        // Arrange
        coEvery { linkWalletToDeviceUseCase(userWallet) } returns error.left()
        model.show(userWallet)
        val result = async { model.waitResult() }

        // Act
        clickLink()

        // Assert
        assertThat(result.await()).isEqualTo(WalletLinkRequester.Result.Dismissed)
        assertThat(model.uiState.value).isNull()
        verify(exactly = 1) { uiMessageSender.send(any<DialogMessage>()) }
    }

    private fun provideTestModels() = listOf(WalletLinkError.Failed(reason = "api"), WalletLinkError.NotSupported)

    @Test
    fun `GIVEN link in progress WHEN sheet dismissed THEN sheet closes with Dismissed`() = runTest {
        // Arrange
        coEvery { linkWalletToDeviceUseCase(userWallet) } coAnswers { CompletableDeferred<Unit>().await().right() }
        model.show(userWallet)
        val result = async { model.waitResult() }
        clickLink()

        // Act
        model.onDismiss()

        // Assert
        assertThat(result.await()).isEqualTo(WalletLinkRequester.Result.Dismissed)
        assertThat(model.uiState.value).isNull()
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    private fun clickLink() {
        val button = requireNotNull(model.uiState.value).buttons.single()
        button.onClick(actionScope)
    }
}