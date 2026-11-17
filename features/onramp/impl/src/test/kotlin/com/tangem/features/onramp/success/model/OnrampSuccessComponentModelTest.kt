package com.tangem.features.onramp.success.model

import arrow.core.left
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.onramp.GetOnrampTransactionUseCase
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.features.onramp.OnrampModelTestBase
import com.tangem.features.onramp.component.OnrampSuccessComponent
import com.tangem.features.onramp.success.entity.OnrampSuccessComponentUM
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class OnrampSuccessComponentModelTest : OnrampModelTestBase() {
    @Test
    fun `GIVEN missing transaction WHEN loaded THEN error is shown and provider and copy actions are forwarded`() = runTest {
        // Arrange
        val getTransaction = mockk<GetOnrampTransactionUseCase>()
        coEvery { getTransaction("tx") } returns OnrampError.AlreadyHandledTransaction.left()
        val messages = mockk<UiMessageSender>(relaxed = true)
        val opener = mockk<UrlOpener>(relaxed = true)
        val clipboard = mockk<ClipboardManager>(relaxed = true)
        val model = OnrampSuccessComponentModel(
            dispatchers = testDispatchers(),
            urlOpener = opener,
            getOnrampTransactionUseCase = getTransaction,
            getOnrampStatusUseCase = mockk(),
            singleAccountListSupplier = mockk(),
            onrampRemoveTransactionUseCase = mockk(),
            getUserWalletUseCase = mockk(),
            analyticsEventHandler = analyticsEventHandler,
            messageSender = messages,
            clipboardManager = clipboard,
            router = router,
            paramsContainer = MutableParamsContainer(OnrampSuccessComponent.Params("tx")),
        )

        // Act
        advanceUntilIdle()
        model.goToProviderClick("https://provider.example")
        model.onCopyClick("transaction")

        // Assert
        verify(exactly = 1) { messages.send(ofType<DialogMessage>()) }
        verify { opener.openUrl("https://provider.example") }
        verify { clipboard.setText("transaction", false) }
        assertThat(model.state.value).isEqualTo(OnrampSuccessComponentUM.Loading)
        model.onDestroy()
    }
}