package com.tangem.tap.domain.yield

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import arrow.core.left
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.YieldSupplyClaimOutcome
import com.tangem.domain.yield.supply.models.YieldSupplyTransactionOutcome
import com.tangem.domain.yield.supply.usecase.YieldSupplyClaimTracker
import com.tangem.core.res.R
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
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
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase = mockk(relaxed = true)
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase = mockk()
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk()
    private val outcomes = MutableSharedFlow<YieldSupplyClaimOutcome>(extraBufferCapacity = 1)

    @BeforeEach
    fun setUp() {
        clearMocks(uiMessageSender, saveBlockchainErrorUseCase, getWalletMetaInfoUseCase)
        every { claimTracker.outcomes } returns outcomes
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

    @Test
    fun `GIVEN a reverted claim dialog WHEN support is clicked THEN the email reports that claim`() = runTest {
        // Arrange
        val message = slot<UiMessage>()
        coEvery { getWalletMetaInfoUseCase(USER_WALLET_ID) } returns IllegalStateException().left()
        startAnnouncer()
        outcomes.emit(outcome(YieldSupplyTransactionOutcome.Reverted))
        runCurrent()
        verify { uiMessageSender.send(capture(message)) }

        // Act
        (message.captured as DialogMessage).secondAction?.onClick?.invoke()
        runCurrent()

        // Assert
        verify(exactly = 1) {
            saveBlockchainErrorUseCase(
                error = BlockchainErrorInfo(
                    errorMessage = "Claim transaction $TX_HASH reverted",
                    networkId = NETWORK_ID,
                    tokenSymbol = TOKEN_SYMBOL,
                    destinationAddress = "",
                    amount = "",
                    fee = "",
                ),
            )
        }
    }

    private fun TestScope.startAnnouncer() {
        YieldSupplyClaimOutcomeAnnouncer(
            yieldSupplyClaimTracker = claimTracker,
            uiMessageSender = uiMessageSender,
            saveBlockchainErrorUseCase = saveBlockchainErrorUseCase,
            getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
            sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
            appScope = TestAppCoroutineScope(backgroundScope.coroutineContext),
        ).start()
        runCurrent()
    }

    private fun outcome(outcome: YieldSupplyTransactionOutcome) = YieldSupplyClaimOutcome(
        userWalletId = USER_WALLET_ID,
        cryptoCurrencyId = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId("ethereum"),
            suffix = CryptoCurrency.ID.Suffix.RawID("0xusdt"),
        ),
        networkId = NETWORK_ID,
        tokenSymbol = TOKEN_SYMBOL,
        chainId = 1,
        userAddress = "0xowner",
        transactionHash = TX_HASH,
        outcome = outcome,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("00")
        val NETWORK_ID = Network.ID(value = "ethereum", derivationPath = Network.DerivationPath.None)
        const val TOKEN_SYMBOL = "USDT"
        const val TX_HASH = "0xclaim"
    }
}