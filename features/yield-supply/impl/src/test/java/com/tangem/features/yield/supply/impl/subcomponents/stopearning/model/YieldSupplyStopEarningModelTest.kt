package com.tangem.features.yield.supply.impl.subcomponents.stopearning.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.MerklClaimCallData
import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.yield.supply.YieldSupplyError
import com.tangem.domain.yield.supply.models.YieldSupplyClaimOrigin
import com.tangem.domain.yield.supply.usecase.YieldSupplyClaimTracker
import com.tangem.domain.yield.supply.usecase.YieldSupplyDeactivateUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyStopEarningUseCase
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import com.tangem.features.yield.supply.impl.subcomponents.YieldSupplyActionModelTestBase
import com.tangem.features.yield.supply.impl.subcomponents.notifications.entity.YieldSupplyNotificationData
import com.tangem.features.yield.supply.impl.subcomponents.stopearning.YieldSupplyStopEarningComponent
import com.tangem.test.mock.yield.MockYieldSupply
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TestTimeSource
import kotlin.time.TimeMark

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
        coEvery {
            sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
        } returns listOf("0xhash").right()
        coEvery { deactivateUseCase(any(), any()) } returns true.right()
        coEvery {
            withdrawalClaimPreparer.prepare(
                userWallet = any(),
                cryptoCurrencyStatus = any(),
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
    fun `GIVEN the fee balance changed WHEN the fee reloads THEN the fresh fee-paying status is used`() = runTest {
        // Arrange
        val freshFeeStatus = statusOf(coin)
        coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase(any(), any()) } returnsMany listOf(
            cryptoCurrencyStatus.right(),
            freshFeeStatus.right(),
        )
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onFeeReload()
        advanceUntilIdle()

        // Assert
        assertThat(model.feeCryptoCurrencyStatusFlow.value).isEqualTo(freshFeeStatus)
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
        coVerify(exactly = 0) {
            sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
        }
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
            sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ClaimInWithdrawalBatch {

        private val claimTransaction = uncompiledTx(fee = ethFee(CLAIM_FEE)).copy(
            destinationAddress = CLAIM_ADDRESS,
            extras = EthereumTransactionExtras(callData = MerklClaimCallData.fromHex(CLAIM_CALL_DATA)),
        )

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
                notificationsUpdateTrigger,
            )
            every { notificationsUpdateTrigger.hasErrorFlow } returns MutableStateFlow(false)
            coEvery { stopEarningUseCase(any(), any(), any()) } returns uncompiledTx().right()
            coEvery { getFeeUseCase(any(), any(), any()) } returns transactionFee().right()
            coEvery {
                sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
            } returns listOf(CLAIM_HASH, EXIT_HASH).right()
            coEvery { deactivateUseCase(any(), any()) } returns true.right()
            coEvery {
                withdrawalClaimPreparer.prepare(userWallet = any(), cryptoCurrencyStatus = any())
            } returns readyPreClaim()
        }

        @Test
        fun `GIVEN a prepared claim WHEN the fee loads THEN the sheet carries both transactions and both fees`() =
            runTest {
                // Act
                val model = createModel()
                advanceUntilIdle()

                // Assert
                val content = model.uiState.value.yieldSupplyFeeUM as YieldSupplyFeeUM.Content
                assertThat(content.transactionDataList).hasSize(2)
                assertThat(content.transactionDataList.first()).isEqualTo(claimTransaction)
                val exitFee = content.transactionDataList.last().fee?.amount?.value
                val feeData = slot<YieldSupplyNotificationData>()
                coVerify { notificationsUpdateTrigger.triggerUpdate(data = capture(feeData)) }
                assertThat(feeData.captured.feeValue).isEqualTo(CLAIM_FEE + exitFee!!)

                model.onDestroy()
            }

        @Test
        fun `GIVEN a prepared claim WHEN withdrawing THEN one batch is sent and the claim hash is followed`() =
            runTest {
                // Arrange
                val model = createModel()
                advanceUntilIdle()

                // Act
                model.onClick()
                advanceUntilIdle()

                // Assert
                assertThat(sentBatch().first()).isEqualTo(claimTransaction)
                verify {
                    claimTracker.track(
                        userWalletId = userWalletId,
                        cryptoCurrency = token,
                        chainId = CHAIN_ID,
                        userAddress = SOURCE_ADDRESS,
                        transactionHash = CLAIM_HASH,
                        claimedRewards = listOf(MockYieldSupply.reward()),
                        origin = YieldSupplyClaimOrigin.Withdrawal,
                    )
                }
                coVerify {
                    pendingTracker.addPending(userWalletId = userWalletId, cryptoCurrency = any(), txIds = listOf(EXIT_HASH))
                }
                verify { callback.onStopEarningTransactionSent() }

                model.onDestroy()
            }

        @Test
        fun `GIVEN no claim to make WHEN withdrawing THEN the withdrawal goes alone and nothing is followed`() =
            runTest {
                // Arrange
                coEvery {
                    withdrawalClaimPreparer.prepare(userWallet = any(), cryptoCurrencyStatus = any())
                } returns YieldPreClaim.Skip
                coEvery {
                    sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
                } returns listOf(EXIT_HASH).right()
                val model = createModel()
                advanceUntilIdle()

                // Act
                model.onClick()
                advanceUntilIdle()

                // Assert
                assertThat(sentBatch()).hasSize(1)
                verify(exactly = 0) { claimTracker.track(any(), any(), any(), any(), any(), any(), any()) }
                coVerify {
                    pendingTracker.addPending(userWalletId = userWalletId, cryptoCurrency = any(), txIds = listOf(EXIT_HASH))
                }

                model.onDestroy()
            }

        @Test
        fun `GIVEN an Avalanche token WHEN the fee loads THEN no claim is prepared`() = runTest {
            // Arrange
            val avalancheToken = token.copy(
                network = network().copy(
                    id = Network.ID(value = AVALANCHE_NETWORK_ID, derivationPath = Network.DerivationPath.None),
                ),
            )

            // Act
            val model = createModel(statusFlow = MutableStateFlow(statusOf(avalancheToken)))
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 0) { withdrawalClaimPreparer.prepare(userWallet = any(), cryptoCurrencyStatus = any()) }
            val content = model.uiState.value.yieldSupplyFeeUM as YieldSupplyFeeUM.Content
            assertThat(content.transactionDataList).hasSize(1)

            model.onDestroy()
        }

        @Test
        fun `GIVEN the batch is not sent WHEN withdrawing THEN the error stands and nothing is followed`() = runTest {
            // Arrange
            coEvery {
                sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
            } returns SendTransactionError.UserCancelledError.left()
            val model = createModel()
            advanceUntilIdle()

            // Act
            model.onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { claimTracker.track(any(), any(), any(), any(), any(), any(), any()) }
            verify(exactly = 0) { callback.onStopEarningTransactionSent() }
            verify { callback.onTransactionProgress(false) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN a batch with the claim fails WHEN withdrawing THEN the claim is held and the fee reloads`() = runTest {
            // Arrange
            coEvery {
                sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
            } returns SendTransactionError.UnknownError().left()
            val model = createModel()
            advanceUntilIdle()

            // Act
            model.onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                claimTracker.trackUnresolved(
                    chainId = CHAIN_ID,
                    userAddress = SOURCE_ADDRESS,
                    claimedRewards = listOf(MockYieldSupply.reward()),
                )
            }
            coVerify(exactly = 2) { withdrawalClaimPreparer.prepare(userWallet = any(), cryptoCurrencyStatus = any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN the user declines the batch signature WHEN withdrawing THEN the claim is not held`() = runTest {
            // Arrange
            coEvery {
                sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
            } returns SendTransactionError.UserCancelledError.left()
            val model = createModel()
            advanceUntilIdle()

            // Act
            model.onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) {
                claimTracker.trackUnresolved(chainId = any(), userAddress = any(), claimedRewards = any())
            }

            model.onDestroy()
        }

        @Test
        fun `GIVEN a claim prepared too long ago WHEN withdrawing THEN nothing is sent and the claim is prepared again`() =
            runTest {
                // Arrange
                val clock = TestTimeSource()
                val preparedAt = clock.markNow()
                clock += 3.minutes
                coEvery {
                    withdrawalClaimPreparer.prepare(userWallet = any(), cryptoCurrencyStatus = any())
                } returns readyPreClaim(preparedAt = preparedAt)
                val model = createModel()
                advanceUntilIdle()

                // Act
                model.onClick()
                advanceUntilIdle()

                // Assert
                coVerify(exactly = 0) {
                    sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
                }
                coVerify(exactly = 2) {
                    withdrawalClaimPreparer.prepare(userWallet = any(), cryptoCurrencyStatus = any())
                }
                verify { callback.onTransactionProgress(false) }

                model.onDestroy()
            }

        @Test
        fun `GIVEN the fee currency is not found WHEN the fee loads THEN the withdrawal goes without the claim`() =
            runTest {
                // Arrange
                coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase(any(), any()) } returns null.right()

                // Act
                val model = createModel()
                advanceUntilIdle()

                // Assert
                coVerify(exactly = 0) {
                    withdrawalClaimPreparer.prepare(userWallet = any(), cryptoCurrencyStatus = any())
                }
                val content = model.uiState.value.yieldSupplyFeeUM as YieldSupplyFeeUM.Content
                assertThat(content.transactionDataList).hasSize(1)

                model.onDestroy()
            }

        @Test
        fun `GIVEN the claim is gone on a fee reload WHEN withdrawing THEN only the withdrawal is sent`() = runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()
            coEvery {
                withdrawalClaimPreparer.prepare(userWallet = any(), cryptoCurrencyStatus = any())
            } returns YieldPreClaim.Skip
            coEvery {
                sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
            } returns listOf(EXIT_HASH).right()
            model.onFeeReload()
            advanceUntilIdle()

            // Act
            model.onClick()
            advanceUntilIdle()

            // Assert
            assertThat(sentBatch()).hasSize(1)
            verify(exactly = 0) { claimTracker.track(any(), any(), any(), any(), any(), any(), any()) }

            model.onDestroy()
        }

        private fun sentBatch(): List<TransactionData> {
            val sent = slot<List<TransactionData>>()
            coVerify(exactly = 1) {
                sendTransactionUseCase(txsData = capture(sent), userWallet = any(), network = any(), sendMode = any())
            }

            return sent.captured
        }

        private fun readyPreClaim(preparedAt: TimeMark = TestTimeSource().markNow()) = YieldPreClaim.Ready(
            transactionData = claimTransaction,
            chainId = CHAIN_ID,
            userAddress = SOURCE_ADDRESS,
            token = token,
            claimedRewards = listOf(MockYieldSupply.reward()),
            preparedAt = preparedAt,
        )
    }

    private fun TestScope.createModel(
        statusFlow: StateFlow<CryptoCurrencyStatus> = cryptoCurrencyStatusFlow,
    ): YieldSupplyStopEarningModel = YieldSupplyStopEarningModel(
        dispatchers = createTestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(
            YieldSupplyStopEarningComponent.Params(
                userWallet = userWallet,
                cryptoCurrencyStatusFlow = statusFlow,
                callback = callback,
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
        const val CLAIM_HASH = "0xclaim"
        const val CLAIM_CALL_DATA = "0xb4701e3e"
        const val EXIT_HASH = "0xexit"
        const val AVALANCHE_NETWORK_ID = "avalanche"
        val CLAIM_FEE: BigDecimal = BigDecimal("0.0005")
        const val CLAIM_ADDRESS = "0x3333333333333333333333333333333333333333"
    }
}