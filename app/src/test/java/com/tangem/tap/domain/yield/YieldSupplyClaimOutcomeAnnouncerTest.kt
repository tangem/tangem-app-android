package com.tangem.tap.domain.yield

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.YieldSupplyClaimOutcome
import com.tangem.domain.yield.supply.models.YieldSupplyTransactionOutcome
import com.tangem.domain.yield.supply.usecase.YieldSupplyClaimTracker
import com.tangem.core.res.R
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class YieldSupplyClaimOutcomeAnnouncerTest {

    private val claimTracker: YieldSupplyClaimTracker = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase = mockk()
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk()
    private val outcomes = MutableSharedFlow<YieldSupplyClaimOutcome>(extraBufferCapacity = 1)

    @BeforeEach
    fun setUp() {
        clearMocks(uiMessageSender)
        every { claimTracker.outcomes } returns outcomes
    }

    @Test
    fun `GIVEN a claim that reverted WHEN it is reported THEN the failure is shown wherever the user is`() = runTest {
        // Arrange
        val message = slot<UiMessage>()
        startAnnouncer()

        // Act
        outcomes.emit(outcome(YieldSupplyTransactionOutcome.Reverted))
        runCurrent()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(capture(message)) }
        assertThat(message.captured).isInstanceOf(DialogMessage::class.java)
    }

    @Test
    fun `GIVEN a claim that reverted WHEN it is reported THEN the dialog names it and offers support`() = runTest {
        // Arrange
        val message = slot<UiMessage>()
        startAnnouncer()

        // Act
        outcomes.emit(outcome(YieldSupplyTransactionOutcome.Reverted))
        runCurrent()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(capture(message)) }
        val dialog = message.captured as DialogMessage
        assertThat(dialog.title).isEqualTo(resourceReference(R.string.common_something_went_wrong))
        assertThat(dialog.secondAction).isNotNull()
        assertThat(dialog.secondAction?.title).isEqualTo(resourceReference(R.string.common_support))
    }

    @Test
    fun `GIVEN a confirmed claim WHEN it is reported THEN nothing is announced`() = runTest {
        // Arrange
        startAnnouncer()

        // Act
        outcomes.emit(outcome(YieldSupplyTransactionOutcome.Confirmed))
        runCurrent()

        // Assert
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    /** The announcer never stops collecting, so it runs on the background scope and subscribes before the Act. */
    private fun TestScope.startAnnouncer() {
        YieldSupplyClaimOutcomeAnnouncer(
            yieldSupplyClaimTracker = claimTracker,
            uiMessageSender = uiMessageSender,
            getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
            sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
            appScope = TestAppCoroutineScope(backgroundScope.coroutineContext),
        ).start()
        runCurrent()
    }

    private fun outcome(outcome: YieldSupplyTransactionOutcome) = YieldSupplyClaimOutcome(
        userWalletId = UserWalletId("00"),
        cryptoCurrencyId = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId("ethereum"),
            suffix = CryptoCurrency.ID.Suffix.RawID("0xusdt"),
        ),
        chainId = 1,
        userAddress = "0xowner",
        outcome = outcome,
    )
}