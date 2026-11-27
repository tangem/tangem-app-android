package com.tangem.features.onramp.hottokens.portfolio.model

import arrow.core.left
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.addtoken.AddTokenUM
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.wallets.usecase.ColdWalletAndHasMissedDerivationsUseCase
import com.tangem.features.onramp.OnrampModelTestBase
import com.tangem.features.onramp.hottokens.portfolio.OnrampAddTokenComponent
import com.tangem.features.onramp.hottokens.portfolio.entity.OnrampAddTokenUiBuilder
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class OnrampAddTokenModelTest : OnrampModelTestBase() {
    @Test
    fun `GIVEN add failure WHEN confirmed THEN toast is shown progress resets and success is not sent`() = runTest {
        // Arrange
        val currency = MockCryptoCurrencyFactory().ethereum
        val accountId = AccountId.forMainCryptoPortfolio(userWalletId)
        val data = OnrampAddTokenComponent.AddHotCryptoData(
            currency, mockk(), mockk { every { this@mockk.accountId } returns accountId }, false,
        )
        val builder = mockk<OnrampAddTokenUiBuilder>()
        coEvery { builder.updateContent(any(), any(), data) } answers {
            AddTokenUM(
                mockk(), mockk(), mockk(),
                AddTokenUM.Button(true, false, firstArg(), secondArg(), TextReference.EMPTY),
            )
        }
        val coldInteraction = mockk<ColdWalletAndHasMissedDerivationsUseCase>()
        coEvery { coldInteraction(any(), any()) } returns true
        val manage = mockk<ManageCryptoCurrenciesUseCase>()
        coEvery { manage(accountId, add = currency) } returns IllegalStateException("offline").left()
        val callbacks = mockk<OnrampAddTokenComponent.Callbacks>(relaxed = true)
        val messages = mockk<UiMessageSender>(relaxed = true)
        val model = OnrampAddTokenModel(
            MutableParamsContainer(OnrampAddTokenComponent.Params(flowOf(data), callbacks)),
            builder, messages, coldInteraction, testDispatchers(), manage, mockk(),
        )
        advanceUntilIdle()

        // Act
        model.uiState.value!!.button.onConfirmClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value!!.button.shouldShowProgress).isFalse()
        assertThat(model.uiState.value!!.button.isTangemIconVisible).isTrue()
        coVerify(exactly = 1) { manage(accountId, add = currency) }
        verify(exactly = 1) { messages.send(ofType<ToastMessage>()) }
        verify(exactly = 0) { callbacks.onTokenAdded(any()) }
        model.onDestroy()
    }
}