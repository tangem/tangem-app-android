package com.tangem.features.yield.supply.impl.subcomponents.stopearning.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.yield.supply.YieldSupplyError
import com.tangem.domain.yield.supply.models.YieldSupplyClaimOrigin
import com.tangem.domain.yield.supply.usecase.YieldSupplyClaimTracker
import com.tangem.domain.yield.supply.usecase.YieldSupplyDeactivateUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyStopEarningUseCase
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import com.tangem.features.yield.supply.impl.subcomponents.YieldSupplyActionModelTestBase
import com.tangem.features.yield.supply.impl.subcomponents.stopearning.YieldSupplyStopEarningComponent
import com.tangem.test.mock.yield.MockYieldSupply
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class YieldSupplyStopEarningModelTest : YieldSupplyActionModelTestBase() {

    private val stopEarningUseCase: YieldSupplyStopEarningUseCase = mockk()
    private val deactivateUseCase: YieldSupplyDeactivateUseCase = mockk()
    private val withdrawalClaimPreparer: YieldSupplyWithdrawalClaimPreparer = mockk()
    private val claimTracker: YieldSupplyClaimTracker = mockk(relaxed = true)
    private val callback: YieldSupplyStopEarningComponent.ModelCallback = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        coEvery { stopEarningUseCase(any(), any(), any()) } returns uncompiledTx().right()
        coEvery { getFeeUseCase(any(), any(), any()) } returns transactionFee().right()
        coEvery { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) } returns "0xhash".right()
        coEvery { deactivateUseCase(any(), any()) } returns true.right()
        coEvery {
            withdrawalClaimPreparer.prepare(
                userWallet = any(),
                cryptoCurrencyStatus = any(),
                withdrawalFee = any(),
            )
        } returns YieldPreClaim.Skip
    }

    @Test
    fun `GIVEN successful fee load WHEN model created THEN fee content and button enabled`() = runTest {
        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.yieldSupplyFeeUM).isInstanceOf(YieldSupplyFeeUM.Content::class.java)
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isTrue()
        coVerify { notificationsUpdateTrigger.triggerUpdate(any()) }
    }

    @Test
    fun `GIVEN get fee fails WHEN model created THEN fee error state`() = runTest {
        // Arrange
        coEvery { getFeeUseCase(any(), any(), any()) } returns GetFeeError.UnknownError.left()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.yieldSupplyFeeUM).isEqualTo(YieldSupplyFeeUM.Error)
    }

    @Test
    fun `GIVEN non-token currency WHEN model created THEN fee not loaded`() = runTest {
        // Act
        val model = createModel(statusFlow = MutableStateFlow(statusOf(coin)))
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.yieldSupplyFeeUM).isEqualTo(YieldSupplyFeeUM.Loading)
        coVerify(exactly = 0) { getFeeUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN stop earning use case fails WHEN model created THEN fee not loaded`() = runTest {
        // Arrange
        coEvery { stopEarningUseCase(any(), any(), any()) } returns YieldSupplyError.DataError(Throwable()).left()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.yieldSupplyFeeUM).isEqualTo(YieldSupplyFeeUM.Loading)
        coVerify(exactly = 0) { getFeeUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN content loaded WHEN onClick THEN sends deactivates tracks pending and notifies sent`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        verify { callback.onTransactionProgress(true) }
        coVerify { yieldSupplyRepository.saveTokenProtocolPendingStatus(userWalletId, any(), any()) }
        coVerify { deactivateUseCase(any(), any()) }
        coVerify { pendingTracker.addPending(userWalletId, any(), any()) }
        verify { callback.onStopEarningTransactionSent() }

        // Token fee asset (default fee currency is the token itself)
        val events = mutableListOf<AnalyticsEvent>()
        verify { analytics.send(capture(events)) }
        val sent = events.filterIsInstance<Basic.TransactionSent>().single()
        assertThat(sent.params["Fee Token"]).isEqualTo("TTK")
        assertThat(sent.params["Fee Asset Type"]).isEqualTo(AnalyticsParam.FeeAssetType.Token.value)
    }

    @Test
    fun `GIVEN coin fee currency WHEN onClick succeeds THEN transaction sent analytics carries coin fee asset`() = runTest {
        // Arrange — network fee paid in the native coin, not the token
        coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase(any(), any()) } returns statusOf(coin).right()
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        val events = mutableListOf<AnalyticsEvent>()
        verify { analytics.send(capture(events)) }
        val sent = events.filterIsInstance<Basic.TransactionSent>().single()
        assertThat(sent.params["Fee Token"]).isEqualTo("ETH")
        assertThat(sent.params["Fee Asset Type"]).isEqualTo(AnalyticsParam.FeeAssetType.Coin.value)
    }

    @Test
    fun `GIVEN fee not loaded WHEN onClick THEN does not send transaction`() = runTest {
        // Arrange — fee load fails so the fee state is Error; onClick reports progress then early-returns
        coEvery { getFeeUseCase(any(), any(), any()) } returns GetFeeError.UnknownError.left()
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        verify { callback.onTransactionProgress(true) }
        coVerify(exactly = 0) { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) }
    }

    @Test
    fun `GIVEN notifications report an error WHEN flag emitted THEN primary button disabled`() = runTest {
        // Arrange
        val hasErrorFlow = MutableStateFlow(false)
        every { notificationsUpdateTrigger.hasErrorFlow } returns hasErrorFlow
        val model = createModel()
        advanceUntilIdle()
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isTrue()

        // Act
        hasErrorFlow.value = true
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN content loaded WHEN onClick and send fails THEN shows error and stops progress`() = runTest {
        // Arrange
        coEvery {
            sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
        } returns SendTransactionError.UnknownError().left()
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isTransactionSending).isFalse()
        verify { alertFactory.getSendTransactionErrorState(any(), any(), any()) }
        verify { callback.onTransactionProgress(false) }
        verify(exactly = 0) { callback.onStopEarningTransactionSent() }
    }

    @Test
    fun `WHEN onReadMoreClick THEN opens url`() = runTest {
        // Arrange
        mockkObject(TangemBlogUrlBuilder)
        try {
            coEvery { TangemBlogUrlBuilder.build(any()) } returns BLOG_URL
            val model = createModel()
            advanceUntilIdle()

            // Act
            model.onReadMoreClick()
            advanceUntilIdle()

            // Assert
            verify { urlOpener.openUrl(BLOG_URL) }
        } finally {
            unmockkObject(TangemBlogUrlBuilder)
        }
    }

    /** The principal withdrawal must leave regardless of what the rewards claim preceding it does. */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ClaimBeforeWithdrawal {

        private val claimTransaction = uncompiledTx().copy(destinationAddress = CLAIM_ADDRESS)

        @BeforeEach
        fun resetClaimMocks() {
            clearMocks(
                stopEarningUseCase,
                deactivateUseCase,
                withdrawalClaimPreparer,
                claimTracker,
                callback,
                pendingTracker,
                sendTransactionUseCase,
                getFeeUseCase,
            )
            coEvery { stopEarningUseCase(any(), any(), any()) } returns uncompiledTx().right()
            coEvery { getFeeUseCase(any(), any(), any()) } returns transactionFee().right()
            coEvery {
                sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
            } returns "0xhash".right()
            coEvery { deactivateUseCase(any(), any()) } returns true.right()
            coEvery {
                withdrawalClaimPreparer.prepare(
                    userWallet = any(),
                    cryptoCurrencyStatus = any(),
                    withdrawalFee = any(),
                )
            } returns readyPreClaim()
        }

        @Test
        fun `GIVEN a prepared claim WHEN withdrawing THEN it is signed before the withdrawal and followed`() = runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()

            // Act
            model.onClick()
            advanceUntilIdle()

            // Assert
            val sent = sentTransactions()
            assertThat(sent).hasSize(2)
            assertThat(sent.first()).isEqualTo(claimTransaction)
            verify {
                claimTracker.track(
                    userWalletId = userWalletId,
                    cryptoCurrency = token,
                    chainId = CHAIN_ID,
                    userAddress = SOURCE_ADDRESS,
                    transactionHash = "0xhash",
                    claimedRewards = listOf(MockYieldSupply.reward()),
                    origin = YieldSupplyClaimOrigin.Withdrawal,
                )

                model.onDestroy()
            }
            verify { callback.onStopEarningTransactionSent() }
        }

        @Test
        fun `GIVEN the claim sheet was offered WHEN withdrawing THEN nothing is claimed behind the owner`() = runTest {
            // Arrange
            val model = createModel(wasClaimOffered = true)
            advanceUntilIdle()

            // Act
            model.onClick()
            advanceUntilIdle()

            // Assert
            val sent = sentTransactions()
            assertThat(sent).hasSize(1)
            coVerify(exactly = 0) { withdrawalClaimPreparer.prepare(any(), any(), any()) }
            verify { callback.onStopEarningTransactionSent() }

            model.onDestroy()
        }

        @Test
        fun `GIVEN a withdrawal fee WHEN preparing the claim THEN the claim is weighed against that fee`() =
            runTest {
                // Arrange
                val withdrawalFee = slot<BigDecimal>()
                val model = createModel()
                advanceUntilIdle()

                // Act
                model.onClick()
                advanceUntilIdle()

                // Assert
                coVerify {
                    withdrawalClaimPreparer.prepare(
                        userWallet = any(),
                        cryptoCurrencyStatus = any(),
                        withdrawalFee = capture(withdrawalFee),
                    )
                }
                val withdrawal = sentTransactions().last() as TransactionData.Uncompiled
                assertThat(withdrawalFee.captured).isEqualTo(withdrawal.fee?.amount?.value)
                assertThat(withdrawalFee.captured).isGreaterThan(BigDecimal.ZERO)

                model.onDestroy()
            }

        @Test
        fun `GIVEN the claim preparation stalls WHEN withdrawing THEN the withdrawal still goes out`() = runTest {
            // Arrange
            coEvery {
                withdrawalClaimPreparer.prepare(
                    userWallet = any(),
                    cryptoCurrencyStatus = any(),
                    withdrawalFee = any(),
                )
            } coAnswers {
                delay(STALL_MILLIS)
                readyPreClaim()
            }
            val model = createModel()
            advanceUntilIdle()

            // Act
            model.onClick()
            advanceUntilIdle()

            // Assert
            assertThat(sentTransactions().single()).isNotEqualTo(claimTransaction)
            verify { callback.onStopEarningTransactionSent() }

            model.onDestroy()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the claim does not go through WHEN withdrawing THEN the withdrawal still goes out`(
            model: PreClaimFailureModel,
        ) = runTest {
            // Arrange
            model.arrange()
            val stopEarningModel = createModel()
            advanceUntilIdle()

            // Act
            stopEarningModel.onClick()
            advanceUntilIdle()

            // Assert
            val withdrawal = sentTransactions().last()
            assertThat(withdrawal).isNotEqualTo(claimTransaction)
            verify { callback.onStopEarningTransactionSent() }
            coVerify { pendingTracker.addPending(userWalletId, any(), any()) }
            verify(exactly = 0) { claimTracker.track(any(), any(), any(), any(), any(), any(), any()) }

            stopEarningModel.onDestroy()
        }

        private fun provideTestModels() = listOf(
            PreClaimFailureModel(reason = "the claim could not be prepared") {
                coEvery {
                    withdrawalClaimPreparer.prepare(
                        userWallet = any(),
                        cryptoCurrencyStatus = any(),
                        withdrawalFee = any(),
                    )
                } returns YieldPreClaim.Skip
            },
            PreClaimFailureModel(reason = "the user declined the claim signature") {
                coEvery {
                    sendTransactionUseCase(txData = claimTransaction, userWallet = any(), network = any())
                } returns SendTransactionError.UserCancelledError.left()
            },
            PreClaimFailureModel(reason = "the claim submission failed") {
                coEvery {
                    sendTransactionUseCase(txData = claimTransaction, userWallet = any(), network = any())
                } returns SendTransactionError.UnknownError().left()
            },
        )

        private fun sentTransactions(): List<TransactionData> {
            val captured = mutableListOf<TransactionData>()
            coVerify { sendTransactionUseCase(txData = capture(captured), userWallet = any(), network = any()) }

            return captured
        }

        private fun readyPreClaim() = YieldPreClaim.Ready(
            transactionData = claimTransaction,
            chainId = CHAIN_ID,
            userAddress = SOURCE_ADDRESS,
            token = token,
            claimedRewards = listOf(MockYieldSupply.reward()),
        )
    }

    internal data class PreClaimFailureModel(val reason: String, val arrange: () -> Unit) {
        override fun toString(): String = reason
    }

    private fun TestScope.createModel(
        statusFlow: StateFlow<CryptoCurrencyStatus> = cryptoCurrencyStatusFlow,
        wasClaimOffered: Boolean = false,
    ): YieldSupplyStopEarningModel = YieldSupplyStopEarningModel(
        dispatchers = createTestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(
            YieldSupplyStopEarningComponent.Params(
                userWallet = userWallet,
                cryptoCurrencyStatusFlow = statusFlow,
                callback = callback,
                wasClaimOffered = wasClaimOffered,
            ),
        ),
        analytics = analytics,
        getFeeUseCase = getFeeUseCase,
        getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
        getFeePaidCryptoCurrencyStatusSyncUseCase = getFeePaidCryptoCurrencyStatusSyncUseCase,
        sendTransactionUseCase = sendTransactionUseCase,
        yieldSupplyStopEarningUseCase = stopEarningUseCase,
        urlOpener = urlOpener,
        yieldSupplyNotificationsUpdateTrigger = notificationsUpdateTrigger,
        yieldSupplyAlertFactory = alertFactory,
        yieldSupplyDeactivateUseCase = deactivateUseCase,
        yieldSupplyRepository = yieldSupplyRepository,
        yieldSupplyPendingTracker = pendingTracker,
        appsFlyerRepository = appsFlyerRepository,
        yieldSupplyWithdrawalClaimPreparer = withdrawalClaimPreparer,
        yieldSupplyClaimTracker = claimTracker,
    )

    private companion object {
        const val BLOG_URL = "https://tangem.com/blog"
        const val CHAIN_ID = 1
        const val STALL_MILLIS = 60_000L
        const val CLAIM_ADDRESS = "0x3333333333333333333333333333333333333333"
    }
}