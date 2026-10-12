package com.tangem.features.yield.supply.impl.subcomponents.stopearning.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.TransactionData
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.yield.supply.models.YieldSupplyClaimOrigin
import com.tangem.domain.yield.supply.usecase.PrepareClaimBeforeWithdrawUseCase
import com.tangem.domain.yield.supply.usecase.YieldPreClaim
import com.tangem.domain.yield.supply.usecase.YieldSupplyClaimTracker
import com.tangem.domain.yield.supply.usecase.YieldSupplyDeactivateUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyStopEarningUseCase
import com.tangem.features.yield.supply.impl.subcomponents.YieldSupplyActionModelTestBase
import com.tangem.features.yield.supply.impl.subcomponents.stopearning.YieldSupplyStopEarningComponent
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * The principal withdrawal must leave regardless of what the rewards claim preceding it does (MTR-6 / MAC-8).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldSupplyStopEarningPreClaimTest : YieldSupplyActionModelTestBase() {

    private val stopEarningUseCase: YieldSupplyStopEarningUseCase = mockk()
    private val deactivateUseCase: YieldSupplyDeactivateUseCase = mockk()
    private val prepareClaimUseCase: PrepareClaimBeforeWithdrawUseCase = mockk()
    private val claimTracker: YieldSupplyClaimTracker = mockk(relaxed = true)
    private val callback: YieldSupplyStopEarningComponent.ModelCallback = mockk(relaxed = true)

    private val claimTransaction = uncompiledTx().copy(destinationAddress = CLAIM_ADDRESS)

    @BeforeEach
    fun setUp() {
        // One instance serves the whole class, so the shared mocks keep the previous test's calls without this.
        clearMocks(
            stopEarningUseCase,
            deactivateUseCase,
            prepareClaimUseCase,
            claimTracker,
            callback,
            pendingTracker,
            sendTransactionUseCase,
            getFeeUseCase,
        )
        coEvery { stopEarningUseCase(any(), any(), any()) } returns uncompiledTx().right()
        coEvery { getFeeUseCase(any(), any(), any()) } returns transactionFee().right()
        coEvery { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) } returns "0xhash".right()
        coEvery { deactivateUseCase(any(), any()) } returns true.right()
        coEvery {
            prepareClaimUseCase(
                userWallet = any(),
                cryptoCurrencyStatus = any(),
                feeCryptoCurrencyStatus = any(),
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
                origin = YieldSupplyClaimOrigin.Withdrawal,
            )
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
        coVerify(exactly = 0) { prepareClaimUseCase(any(), any(), any(), any()) }
        verify { callback.onStopEarningTransactionSent() }
    }

    @Test
    fun `GIVEN a withdrawal fee WHEN preparing the claim THEN that very fee is what the claim is weighed against`() =
        runTest {
            // Arrange
            val withdrawalFee = slot<BigDecimal>()
            val model = createModel()
            advanceUntilIdle()

            // Act
            model.onClick()
            advanceUntilIdle()

            // Assert — the claim may only go ahead if the owner can still pay for the withdrawal behind it, so
            // the fee handed over is the withdrawal's own, gas-limit adjustment included
            coVerify {
                prepareClaimUseCase(
                    userWallet = any(),
                    cryptoCurrencyStatus = any(),
                    feeCryptoCurrencyStatus = any(),
                    withdrawalFee = capture(withdrawalFee),
                )
            }
            val withdrawal = sentTransactions().last() as TransactionData.Uncompiled
            assertThat(withdrawalFee.captured).isEqualTo(withdrawal.fee?.amount?.value)
            assertThat(withdrawalFee.captured).isGreaterThan(BigDecimal.ZERO)
        }

    @Test
    fun `GIVEN no rewards to claim WHEN withdrawing THEN only the withdrawal is signed`() = runTest {
        // Arrange
        coEvery {
            prepareClaimUseCase(
                userWallet = any(),
                cryptoCurrencyStatus = any(),
                feeCryptoCurrencyStatus = any(),
                withdrawalFee = any(),
            )
        } returns YieldPreClaim.Skip
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(sentTransactions()).hasSize(1)
        assertThat(sentTransactions().single()).isNotEqualTo(claimTransaction)
        verify { callback.onStopEarningTransactionSent() }
        verify(exactly = 0) { claimTracker.track(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN the claim preparation stalls WHEN withdrawing THEN the withdrawal still goes out`() = runTest {
        // Arrange
        coEvery {
            prepareClaimUseCase(
                userWallet = any(),
                cryptoCurrencyStatus = any(),
                feeCryptoCurrencyStatus = any(),
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
    }

    @ParameterizedTest
    @ProvideTestModels
    fun withdrawalProceeds(model: PreClaimFailureModel) = runTest {
        // Arrange
        model.arrange()
        val stopEarningModel = createModel()
        advanceUntilIdle()

        // Act
        stopEarningModel.onClick()
        advanceUntilIdle()

        // Assert — the principal withdrawal always goes out, and it is never mistaken for the claim
        val withdrawal = sentTransactions().last()
        assertThat(withdrawal).isNotEqualTo(claimTransaction)
        verify { callback.onStopEarningTransactionSent() }
        coVerify { pendingTracker.addPending(userWalletId, any(), any()) }
        verify(exactly = 0) { claimTracker.track(any(), any(), any(), any(), any(), any()) }
    }

    internal data class PreClaimFailureModel(val reason: String, val arrange: () -> Unit) {
        override fun toString(): String = reason
    }

    private fun provideTestModels() = listOf(
        PreClaimFailureModel(reason = "the claim could not be prepared") {
            coEvery {
                prepareClaimUseCase(
                    userWallet = any(),
                    cryptoCurrencyStatus = any(),
                    feeCryptoCurrencyStatus = any(),
                    withdrawalFee = any(),
                )
            } returns YieldPreClaim.Skip
        },
        PreClaimFailureModel(reason = "the claim preparation throws") {
            coEvery {
                prepareClaimUseCase(
                    userWallet = any(),
                    cryptoCurrencyStatus = any(),
                    feeCryptoCurrencyStatus = any(),
                    withdrawalFee = any(),
                )
            } throws IllegalStateException("the incentives path blew up")
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
    )

    private fun TestScope.createModel(wasClaimOffered: Boolean = false): YieldSupplyStopEarningModel =
        YieldSupplyStopEarningModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            paramsContainer = MutableParamsContainer(
                YieldSupplyStopEarningComponent.Params(
                    userWallet = userWallet,
                    cryptoCurrencyStatusFlow = cryptoCurrencyStatusFlow,
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
            prepareClaimBeforeWithdrawUseCase = prepareClaimUseCase,
            yieldSupplyClaimTracker = claimTracker,
        )

    private companion object {
        const val CHAIN_ID = 1
        const val STALL_MILLIS = 60_000L
        const val CLAIM_ADDRESS = "0x3333333333333333333333333333333333333333"
    }
}