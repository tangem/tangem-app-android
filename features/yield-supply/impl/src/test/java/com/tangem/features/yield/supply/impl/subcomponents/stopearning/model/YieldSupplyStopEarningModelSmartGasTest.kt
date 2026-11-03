package com.tangem.features.yield.supply.impl.subcomponents.stopearning.model

import arrow.core.Either
import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.MerklClaimCallData
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.YieldModuleSameTokenRule
import com.tangem.domain.transaction.models.GaslessFeePlan
import com.tangem.domain.transaction.models.GaslessProtocolData
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.yield.supply.increaseGasLimitBy
import com.tangem.domain.yield.supply.INCREASE_GAS_LIMIT_FOR_SUPPLY
import com.tangem.domain.yield.supply.YieldSupplyError
import com.tangem.domain.yield.supply.models.YieldSupplyClaimOrigin
import com.tangem.domain.yield.supply.models.YieldSupplyPendingStatus
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import com.tangem.features.yield.supply.impl.subcomponents.notifications.entity.YieldSupplyNotificationData
import com.tangem.test.mock.yield.MockYieldSupply
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.time.TestTimeSource

@OptIn(ExperimentalCoroutinesApi::class)
internal class YieldSupplyStopEarningModelSmartGasTest : YieldSupplyStopEarningModelTestBase() {

    private val usdc: CryptoCurrency.Token = token().copy(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId("ethereum"),
            suffix = CryptoCurrency.ID.Suffix.RawID("usd-coin"),
        ),
        name = "USD Coin",
        symbol = "USDC",
        contractAddress = "0xUSDC",
    )
    private val usdcStatus: CryptoCurrencyStatus = statusOf(usdc)
    private val nativeFeeStatus: CryptoCurrencyStatus = statusOf(coin)
    private val exitTransaction = uncompiledTx().copy(
        fee = transactionFee().normal.increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SUPPLY),
    )
    private val claimTransaction = uncompiledTx(fee = ethFee(CLAIM_FEE)).copy(
        destinationAddress = CLAIM_ADDRESS,
        extras = EthereumTransactionExtras(callData = MerklClaimCallData.fromHex(CLAIM_CALL_DATA)),
    )

    @BeforeEach
    fun setUp() {
        every { yieldSupplyFeatureToggles.isSmartGasEnabled } returns true
        coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase(any(), any()) } returns nativeFeeStatus.right()
        coEvery { createAndSendEvmGaslessTransactionUseCase(any(), any<List<TransactionData>>(), any()) } returns
            GASLESS_HASH.right()
        stubFeeTokenLookup(usdc.id, usdcStatus.some())
    }

    @Test
    fun `GIVEN toggle off WHEN fee loaded THEN Smart Gas fee is not estimated and native fee kept`() = runTest {
        // Arrange
        every { yieldSupplyFeatureToggles.isSmartGasEnabled } returns false

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) {
            estimateFeeForGaslessBatchUseCase(
                userWallet = any(),
                operationTokenStatus = any(),
                calls = any(),
                sameTokenRule = any(),
            )
        }
        assertNativeFeeKept(model)

        model.onDestroy()
    }

    @Test
    fun `GIVEN a prepared claim WHEN fee loaded THEN batch estimated with exit rule over claim and exit`() = runTest {
        // Arrange
        stubClaim()
        stubBatchEstimate(nativeFee().right())

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            estimateFeeForGaslessBatchUseCase(
                userWallet = userWallet,
                operationTokenStatus = cryptoCurrencyStatus,
                calls = listOf(claimTransaction, exitTransaction),
                sameTokenRule = YieldModuleSameTokenRule.Exit,
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN same-token Smart Gas fee WHEN fee loaded THEN fee shown in token and calls kept`() = runTest {
        // Arrange
        stubBatchEstimate(gaslessFee(token).right())

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        val feeUM = model.uiState.value.yieldSupplyFeeUM as YieldSupplyFeeUM.Content
        assertThat(feeUM.transactionDataList).containsExactly(exitTransaction)
        assertThat(feeUM.gaslessFee).isEqualTo(gaslessFee(token))
        assertThat(feeUM.feeCurrency).isEqualTo(token)
        assertThat(model.feeCryptoCurrencyStatusFlow.value).isEqualTo(cryptoCurrencyStatus)
        coVerify {
            notificationsUpdateTrigger.triggerUpdate(
                YieldSupplyNotificationData(feeValue = TOKEN_FEE, feeError = null),
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN other-token Smart Gas fee WHEN fee loaded THEN fee token status shown`() = runTest {
        // Arrange
        stubBatchEstimate(gaslessFee(usdc).right())

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        val feeUM = model.uiState.value.yieldSupplyFeeUM as YieldSupplyFeeUM.Content
        assertThat(feeUM.gaslessFee).isEqualTo(gaslessFee(usdc))
        assertThat(feeUM.feeCurrency).isEqualTo(usdc)
        assertThat(model.feeCryptoCurrencyStatusFlow.value).isEqualTo(usdcStatus)

        model.onDestroy()
    }

    @Test
    fun `GIVEN Smart Gas fee without claim WHEN onClick THEN exit sent gasless and its hash pending`() = runTest {
        // Arrange
        stubBatchEstimate(gaslessFee(usdc).right())
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            createAndSendEvmGaslessTransactionUseCase(userWallet, listOf(exitTransaction), gaslessFee(usdc))
        }
        coVerify(exactly = 0) {
            sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
        }
        coVerify {
            yieldSupplyRepository.saveTokenProtocolPendingStatus(
                userWalletId = userWalletId,
                cryptoCurrency = token,
                yieldSupplyPendingStatus = match<YieldSupplyPendingStatus.Exit> { it.txIds == listOf(GASLESS_HASH) },
            )
        }
        coVerify {
            pendingTracker.addPending(userWalletId = userWalletId, cryptoCurrency = token, txIds = listOf(GASLESS_HASH))
        }
        verify(exactly = 0) { claimTracker.track(any(), any(), any(), any(), any(), any(), any()) }
        verify { callback.onStopEarningTransactionSent() }

        model.onDestroy()
    }

    @Test
    fun `GIVEN Smart Gas fee with claim WHEN onClick THEN claim and exit sent gasless and claim tracked by its hash`() =
        runTest {
            // Arrange
            stubClaim()
            stubBatchEstimate(gaslessFee(usdc).right())
            val model = createModel()
            advanceUntilIdle()

            // Act
            model.onClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                createAndSendEvmGaslessTransactionUseCase(
                    userWallet,
                    listOf(claimTransaction, exitTransaction),
                    gaslessFee(usdc),
                )
            }
            verify(exactly = 1) {
                claimTracker.track(
                    userWalletId = userWalletId,
                    cryptoCurrency = token,
                    chainId = CHAIN_ID,
                    userAddress = SOURCE_ADDRESS,
                    transactionHash = GASLESS_HASH,
                    claimedRewards = listOf(MockYieldSupply.reward()),
                    origin = YieldSupplyClaimOrigin.Withdrawal,
                )
            }
            coVerify {
                yieldSupplyRepository.saveTokenProtocolPendingStatus(
                    userWalletId = userWalletId,
                    cryptoCurrency = token,
                    yieldSupplyPendingStatus = match<YieldSupplyPendingStatus.Exit> {
                        it.txIds == listOf(GASLESS_HASH)
                    },
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN Smart Gas fee WHEN onClick THEN transaction sent once with token fee asset`() = runTest {
        // Arrange
        stubClaim()
        stubBatchEstimate(gaslessFee(usdc).right())
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analytics.send(transactionSent(feeToken = "USDC", AnalyticsParam.FeeAssetType.Token)) }
        verify(exactly = 1) { analytics.send(ofType<Basic.TransactionSent>()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN native batch fee WHEN onClick THEN sent through send use case with coin fee asset`() = runTest {
        // Arrange
        stubBatchEstimate(nativeFee().right())
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            sendTransactionUseCase(
                txsData = listOf(exitTransaction),
                userWallet = any(),
                network = any(),
                sendMode = any(),
            )
        }
        coVerify(exactly = 0) {
            createAndSendEvmGaslessTransactionUseCase(any(), any<List<TransactionData>>(), any())
        }
        verify(exactly = 1) { analytics.send(transactionSent(feeToken = "ETH", AnalyticsParam.FeeAssetType.Coin)) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN gasless send fails with a claim WHEN onClick THEN error shown and claim held`() = runTest {
        // Arrange
        stubClaim()
        stubBatchEstimate(gaslessFee(usdc).right())
        val error = SendTransactionError.UnknownError()
        coEvery { createAndSendEvmGaslessTransactionUseCase(any(), any<List<TransactionData>>(), any()) } returns
            error.left()
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
        verify(exactly = 1) {
            analytics.send(
                YieldSupplyAnalytics.EarnErrors(
                    action = YieldSupplyAnalytics.Action.Stop,
                    errorDescription = error.getAnalyticsDescription(),
                ),
            )
        }
        verify { alertFactory.getSendTransactionErrorState(error, any(), any()) }
        verify { callback.onTransactionProgress(false) }
        verify(exactly = 0) { claimTracker.track(any(), any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { callback.onStopEarningTransactionSent() }

        model.onDestroy()
    }

    @Test
    fun `GIVEN gasless signing cancelled with a claim WHEN onClick THEN claim not held and handled as native cancel`() =
        runTest {
            // Arrange
            stubClaim()
            stubBatchEstimate(gaslessFee(usdc).right())
            coEvery {
                createAndSendEvmGaslessTransactionUseCase(any(), any<List<TransactionData>>(), any())
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
            verify(exactly = 1) {
                analytics.send(
                    YieldSupplyAnalytics.EarnErrors(
                        action = YieldSupplyAnalytics.Action.Stop,
                        errorDescription = SendTransactionError.UserCancelledError.getAnalyticsDescription(),
                    ),
                )
            }
            verify { callback.onTransactionProgress(false) }
            verify(exactly = 0) { claimTracker.track(any(), any(), any(), any(), any(), any(), any()) }
            verify(exactly = 0) { callback.onStopEarningTransactionSent() }
            coVerify(exactly = 1) { withdrawalClaimPreparer.prepare(userWallet = any(), cryptoCurrencyStatus = any()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN network not gasless WHEN fee loaded THEN native fee kept`() = runTest {
        // Arrange
        stubBatchEstimate(null.right())

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertNativeFeeKept(model)

        model.onDestroy()
    }

    @Test
    fun `GIVEN batch estimate fails WHEN fee loaded THEN native fee kept`() = runTest {
        // Arrange
        stubBatchEstimate(GetFeeError.UnknownError.left())

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertNativeFeeKept(model)

        model.onDestroy()
    }

    @Test
    fun `GIVEN fee token missing from portfolio WHEN fee loaded THEN native fee kept`() = runTest {
        // Arrange
        stubBatchEstimate(gaslessFee(usdc).right())
        stubFeeTokenLookup(usdc.id, none())

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertNativeFeeKept(model)

        model.onDestroy()
    }

    @Test
    fun `GIVEN gasless fee load overtaken by a native one WHEN both finish THEN native fee and analytics kept`() =
        runTest {
            // Arrange
            coEvery {
                estimateFeeForGaslessBatchUseCase(
                    userWallet = any(),
                    operationTokenStatus = any(),
                    calls = any(),
                    sameTokenRule = any(),
                )
            } coAnswers {
                delay(1_000)
                gaslessFee(usdc).right()
            } andThen nativeFee().right()
            val model = createModel()
            runCurrent()
            model.onFeeReload()
            advanceUntilIdle()

            // Act
            model.onClick()
            advanceUntilIdle()

            // Assert
            val feeUM = model.uiState.value.yieldSupplyFeeUM as YieldSupplyFeeUM.Content
            assertThat(feeUM.gaslessFee).isNull()
            assertThat(feeUM.feeCurrency).isEqualTo(coin)
            assertThat(model.feeCryptoCurrencyStatusFlow.value).isEqualTo(nativeFeeStatus)
            coVerify(exactly = 0) {
                createAndSendEvmGaslessTransactionUseCase(any(), any<List<TransactionData>>(), any())
            }
            verify(exactly = 1) { analytics.send(transactionSent(feeToken = "ETH", AnalyticsParam.FeeAssetType.Coin)) }
            verify(exactly = 1) { analytics.send(ofType<Basic.TransactionSent>()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN stale load returns early after a newer Smart Gas load WHEN both finish THEN fee token status kept`() =
        runTest {
            // Arrange
            stubBatchEstimate(gaslessFee(usdc).right())
            coEvery { stopEarningUseCase(any(), any(), any()) } coAnswers {
                delay(1_000)
                YieldSupplyError.DataError(Throwable()).left()
            } andThen uncompiledTx().right()
            val model = createModel()
            runCurrent()

            // Act
            model.onFeeReload()
            advanceUntilIdle()

            // Assert
            val feeUM = model.uiState.value.yieldSupplyFeeUM as YieldSupplyFeeUM.Content
            assertThat(feeUM.gaslessFee).isEqualTo(gaslessFee(usdc))
            assertThat(model.feeCryptoCurrencyStatusFlow.value).isEqualTo(usdcStatus)

            model.onDestroy()
        }

    @Test
    fun `GIVEN Smart Gas fee shown WHEN reload returns early THEN fee token status kept`() = runTest {
        // Arrange
        stubBatchEstimate(gaslessFee(usdc).right())
        val model = createModel()
        advanceUntilIdle()
        coEvery { stopEarningUseCase(any(), any(), any()) } returns YieldSupplyError.DataError(Throwable()).left()

        // Act
        model.onFeeReload()
        advanceUntilIdle()

        // Assert
        assertThat(model.feeCryptoCurrencyStatusFlow.value).isEqualTo(usdcStatus)

        model.onDestroy()
    }

    private fun assertNativeFeeKept(model: YieldSupplyStopEarningModel) {
        val feeUM = model.uiState.value.yieldSupplyFeeUM as YieldSupplyFeeUM.Content
        assertThat(feeUM.transactionDataList).containsExactly(exitTransaction)
        assertThat(feeUM.gaslessFee).isNull()
        assertThat(feeUM.feeCurrency).isEqualTo(coin)
        assertThat(model.feeCryptoCurrencyStatusFlow.value).isEqualTo(nativeFeeStatus)
        coVerify {
            notificationsUpdateTrigger.triggerUpdate(
                YieldSupplyNotificationData(feeValue = exitTransaction.fee?.amount?.value, feeError = null),
            )
        }
    }

    private fun transactionSent(feeToken: String, feeAssetType: AnalyticsParam.FeeAssetType) = Basic.TransactionSent(
        sentFrom = AnalyticsParam.TxSentFrom.Earning(
            blockchain = "Ethereum",
            token = "TTK",
            feeType = AnalyticsParam.FeeType.Normal,
            feeToken = feeToken,
            feeAssetType = feeAssetType,
        ),
        memoType = Basic.TransactionSent.MemoType.Null,
        isCustomToken = false,
        isCustomFeeToken = false,
    )

    private fun stubClaim() {
        coEvery {
            withdrawalClaimPreparer.prepare(userWallet = any(), cryptoCurrencyStatus = any())
        } returns YieldPreClaim.Ready(
            transactionData = claimTransaction,
            chainId = CHAIN_ID,
            userAddress = SOURCE_ADDRESS,
            token = token,
            claimedRewards = listOf(MockYieldSupply.reward()),
            preparedAt = TestTimeSource().markNow(),
        )
    }

    private fun stubBatchEstimate(result: Either<GetFeeError, TransactionFeeExtended?>) {
        coEvery {
            estimateFeeForGaslessBatchUseCase(
                userWallet = any(),
                operationTokenStatus = any(),
                calls = any(),
                sameTokenRule = any(),
            )
        } returns result
    }

    private fun stubFeeTokenLookup(id: CryptoCurrency.ID, result: arrow.core.Option<CryptoCurrencyStatus>) {
        every {
            with(CryptoCurrencyStatusOperations) {
                accountStatusList.getCryptoCurrencyStatus(id, any<Network>())
            }
        } returns result
    }

    private fun nativeFee(): TransactionFeeExtended = TransactionFeeExtended(
        transactionFee = transactionFee(),
        feeTokenId = coin.id,
        nativeFee = transactionFee(),
    )

    private fun gaslessFee(feeToken: CryptoCurrency.Token): TransactionFeeExtended {
        val tokenFee = Fee.Ethereum.TokenCurrency(
            amount = Amount(
                token = Token(symbol = feeToken.symbol, contractAddress = feeToken.contractAddress, decimals = 6),
                value = TOKEN_FEE,
            ),
            gasLimit = BigInteger("100000"),
            coinPriceInToken = BigInteger("2000000000"),
            feeTransferGasLimit = BigInteger("60000"),
            baseGas = BigInteger("21000"),
        )
        return TransactionFeeExtended(
            transactionFee = TransactionFee.Single(normal = tokenFee),
            feeTokenId = feeToken.id,
            gaslessFeePlan = GaslessFeePlan.TokenPay(feeToken = feeToken, fee = tokenFee),
            callGasLimits = listOf(BigInteger("29400")),
            gaslessProtocolData = GaslessProtocolData.Evm,
            nativeFee = transactionFee(),
        )
    }

    private companion object {
        const val GASLESS_HASH = "0xgasless"
        const val CHAIN_ID = 1
        const val CLAIM_CALL_DATA = "0xb4701e3e"
        const val CLAIM_ADDRESS = "0x3333333333333333333333333333333333333333"
        val CLAIM_FEE: BigDecimal = BigDecimal("0.0005")
        val TOKEN_FEE: BigDecimal = BigDecimal("0.5")
    }
}