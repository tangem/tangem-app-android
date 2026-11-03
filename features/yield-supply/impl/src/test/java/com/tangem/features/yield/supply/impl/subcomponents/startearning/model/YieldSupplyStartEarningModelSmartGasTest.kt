package com.tangem.features.yield.supply.impl.subcomponents.startearning.model

import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.YieldModuleSameTokenRule
import com.tangem.domain.transaction.models.GaslessFeePlan
import com.tangem.domain.transaction.models.GaslessProtocolData
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.yield.supply.models.YieldSupplyPendingStatus
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import com.tangem.features.yield.supply.impl.subcomponents.notifications.entity.YieldSupplyNotificationData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
internal class YieldSupplyStartEarningModelSmartGasTest : YieldSupplyStartEarningModelTestBase() {

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
    private val estimatedCalls = listOf(uncompiledTx(), uncompiledTx())
    private val rewrittenCalls = listOf(uncompiledTx(), uncompiledTx(fee = ethFee(BigDecimal("0.002"))))

    @BeforeEach
    fun setUp() {
        every { yieldSupplyFeatureToggles.isSmartGasEnabled } returns true
        coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase(any(), any()) } returns nativeFeeStatus.right()
        coEvery { estimateEnterFeeUseCase(any(), any(), any()) } returns estimatedCalls.right()
        coEvery {
            applyEnterAmountUseCase(
                userWalletId = any(),
                cryptoCurrencyStatus = any(),
                calls = any(),
                feeAmount = any(),
            )
        } returns rewrittenCalls.right()
        coEvery { createAndSendEvmGaslessTransactionUseCase(any(), any<List<TransactionData>>(), any()) } returns
            GASLESS_HASH.right()
        stubFeeTokenLookup(usdc.id, usdcStatus)
    }

    @Test
    fun `GIVEN toggle off WHEN fee loaded THEN Smart Gas fee is not estimated`() = runTest {
        // Arrange
        every { yieldSupplyFeatureToggles.isSmartGasEnabled } returns false

        // Act
        createModel()
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
    }

    @Test
    fun `GIVEN toggle on WHEN fee loaded THEN batch estimated with enter rule and estimated calls`() = runTest {
        // Arrange
        stubBatchEstimate(nativeFee().right())

        // Act
        createModel()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            estimateFeeForGaslessBatchUseCase(
                userWallet = userWallet,
                operationTokenStatus = cryptoCurrencyStatus,
                calls = estimatedCalls,
                sameTokenRule = YieldModuleSameTokenRule.Enter,
            )
        }
    }

    @Test
    fun `GIVEN same-token Smart Gas fee WHEN fee loaded THEN enter amount applied and fee shown in token`() = runTest {
        // Arrange
        stubBatchEstimate(gaslessFee(token.id).right())

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            applyEnterAmountUseCase(
                userWalletId = userWalletId,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                calls = estimatedCalls,
                feeAmount = TOKEN_FEE,
            )
        }
        val feeUM = model.uiState.value.yieldSupplyFeeUM as YieldSupplyFeeUM.Content
        assertThat(feeUM.transactionDataList).containsExactlyElementsIn(rewrittenCalls).inOrder()
        assertThat(feeUM.gaslessFee).isEqualTo(gaslessFee(token.id))
        assertThat(model.feeCryptoCurrencyStatusFlow.value).isEqualTo(cryptoCurrencyStatus)
        coVerify {
            notificationsUpdateTrigger.triggerUpdate(
                YieldSupplyNotificationData(feeValue = TOKEN_FEE, feeError = null),
            )
        }
    }

    @Test
    fun `GIVEN same-token Smart Gas fee WHEN onClick THEN sends gasless batch and saves one enter hash`() = runTest {
        // Arrange
        stubBatchEstimate(gaslessFee(token.id).right())
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            createAndSendEvmGaslessTransactionUseCase(userWallet, rewrittenCalls, gaslessFee(token.id))
        }
        coVerify(exactly = 0) {
            sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
        }
        coVerify {
            yieldSupplyRepository.saveTokenProtocolPendingStatus(
                userWalletId = userWalletId,
                cryptoCurrency = token,
                yieldSupplyPendingStatus = match<YieldSupplyPendingStatus.Enter> {
                    it.txIds == listOf(GASLESS_HASH)
                },
            )
        }
        coVerify {
            pendingTracker.addPending(
                userWalletId = userWalletId,
                cryptoCurrency = token,
                txIds = listOf(GASLESS_HASH),
                depositContext = any(),
            )
        }
        verify { callback.onTransactionSent() }
    }

    @Test
    fun `GIVEN other-token Smart Gas fee WHEN fee loaded THEN fee token status shown and calls kept`() = runTest {
        // Arrange
        stubBatchEstimate(gaslessFee(usdc.id).right())

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        coVerify(
            exactly = 0,
        ) {
            applyEnterAmountUseCase(
                userWalletId = any(),
                cryptoCurrencyStatus = any(),
                calls = any(),
                feeAmount = any(),
            )
        }
        val feeUM = model.uiState.value.yieldSupplyFeeUM as YieldSupplyFeeUM.Content
        assertThat(feeUM.transactionDataList).containsExactlyElementsIn(estimatedCalls).inOrder()
        assertThat(feeUM.gaslessFee).isEqualTo(gaslessFee(usdc.id))
        assertThat(model.feeCryptoCurrencyStatusFlow.value).isEqualTo(usdcStatus)
    }

    @Test
    fun `GIVEN other-token Smart Gas fee WHEN onClick THEN transaction sent once with token fee asset`() = runTest {
        // Arrange
        stubBatchEstimate(gaslessFee(usdc.id).right())
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        val expectedEvent = Basic.TransactionSent(
            sentFrom = AnalyticsParam.TxSentFrom.Earning(
                blockchain = "Ethereum",
                token = "TTK",
                feeType = AnalyticsParam.FeeType.Normal,
                feeToken = "USDC",
                feeAssetType = AnalyticsParam.FeeAssetType.Token,
            ),
            memoType = Basic.TransactionSent.MemoType.Null,
            isCustomToken = false,
            isCustomFeeToken = false,
        )
        verify(exactly = 1) { analytics.send(expectedEvent) }
        verify(exactly = 1) { analytics.send(ofType<Basic.TransactionSent>()) }
    }

    @Test
    fun `GIVEN native batch fee WHEN onClick THEN sent through send use case with coin fee asset`() = runTest {
        // Arrange
        stubBatchEstimate(nativeFee().right())
        coEvery {
            sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
        } returns listOf("0xfirst", "0xsecond").right()
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            sendTransactionUseCase(txsData = estimatedCalls, userWallet = any(), network = any(), sendMode = any())
        }
        coVerify(exactly = 0) {
            createAndSendEvmGaslessTransactionUseCase(any(), any<List<TransactionData>>(), any())
        }
        val expectedEvent = Basic.TransactionSent(
            sentFrom = AnalyticsParam.TxSentFrom.Earning(
                blockchain = "Ethereum",
                token = "TTK",
                feeType = AnalyticsParam.FeeType.Normal,
                feeToken = "ETH",
                feeAssetType = AnalyticsParam.FeeAssetType.Coin,
            ),
            memoType = Basic.TransactionSent.MemoType.Null,
            isCustomToken = false,
            isCustomFeeToken = false,
        )
        verify(exactly = 2) { analytics.send(expectedEvent) }
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
    }

    @Test
    fun `GIVEN enter amount cannot be applied WHEN fee loaded THEN native fee kept`() = runTest {
        // Arrange
        stubBatchEstimate(gaslessFee(token.id).right())
        coEvery {
            applyEnterAmountUseCase(
                userWalletId = any(),
                cryptoCurrencyStatus = any(),
                calls = any(),
                feeAmount = any(),
            )
        } returns IllegalStateException("old").left()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertNativeFeeKept(model)
    }

    @Test
    fun `GIVEN enter amount cannot be applied WHEN another token can pay THEN its fee shown and calls kept`() =
        runTest {
            // Arrange
            stubBatchEstimate(gaslessFee(token.id).right(), isOperationTokenExcluded = false)
            stubBatchEstimate(gaslessFee(usdc.id).right(), isOperationTokenExcluded = true)
            coEvery {
                applyEnterAmountUseCase(
                    userWalletId = any(),
                    cryptoCurrencyStatus = any(),
                    calls = any(),
                    feeAmount = any(),
                )
            } returns IllegalStateException("old").left()

            // Act
            val model = createModel()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                estimateFeeForGaslessBatchUseCase(
                    userWallet = userWallet,
                    operationTokenStatus = cryptoCurrencyStatus,
                    calls = estimatedCalls,
                    sameTokenRule = YieldModuleSameTokenRule.Enter,
                    isOperationTokenExcluded = true,
                )
            }
            val feeUM = model.uiState.value.yieldSupplyFeeUM as YieldSupplyFeeUM.Content
            assertThat(feeUM.transactionDataList).containsExactlyElementsIn(estimatedCalls).inOrder()
            assertThat(feeUM.gaslessFee).isEqualTo(gaslessFee(usdc.id))
            assertThat(model.feeCryptoCurrencyStatusFlow.value).isEqualTo(usdcStatus)
        }

    @Test
    fun `GIVEN fee token missing from portfolio WHEN fee loaded THEN native fee kept`() = runTest {
        // Arrange
        stubBatchEstimate(gaslessFee(usdc.id).right())
        every {
            with(CryptoCurrencyStatusOperations) {
                accountStatusList.getCryptoCurrencyStatus(usdc.id, any<Network>())
            }
        } returns none()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertNativeFeeKept(model)
    }

    @Test
    fun `GIVEN Smart Gas fee shown WHEN fee reloaded THEN enter fee estimated in native coin`() = runTest {
        // Arrange
        stubBatchEstimate(gaslessFee(usdc.id).right())
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onFeeReload()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { estimateEnterFeeUseCase(any(), usdc, any()) }
        coVerify(exactly = 2) { estimateEnterFeeUseCase(userWallet, coin, any()) }
    }

    @Test
    fun `GIVEN gasless fee load overtaken by a native one WHEN both finish THEN native fee and analytics kept`() =
        runTest {
            // Arrange
            val updatedStatus = cryptoCurrencyStatus.copy(
                value = (cryptoCurrencyStatus.value as CryptoCurrencyStatus.Custom).copy(amount = BigDecimal("11")),
            )
            val updatedAccountStatusList: AccountStatusList = mockk()
            every {
                with(CryptoCurrencyStatusOperations) {
                    updatedAccountStatusList.getCryptoCurrencyStatus(any<CryptoCurrency>())
                }
            } returns updatedStatus.some()
            val accountStatusLists = MutableStateFlow(accountStatusList)
            every { accountStatusListSupplier(userWalletId) } returns accountStatusLists
            coEvery {
                estimateFeeForGaslessBatchUseCase(
                    userWallet = any(),
                    operationTokenStatus = any(),
                    calls = any(),
                    sameTokenRule = any(),
                )
            } coAnswers {
                delay(1_000)
                gaslessFee(usdc.id).right()
            } andThen nativeFee().right()
            val model = createModel()
            runCurrent()
            accountStatusLists.value = updatedAccountStatusList
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
            val expectedEvent = Basic.TransactionSent(
                sentFrom = AnalyticsParam.TxSentFrom.Earning(
                    blockchain = "Ethereum",
                    token = "TTK",
                    feeType = AnalyticsParam.FeeType.Normal,
                    feeToken = "ETH",
                    feeAssetType = AnalyticsParam.FeeAssetType.Coin,
                ),
                memoType = Basic.TransactionSent.MemoType.Null,
                isCustomToken = false,
                isCustomFeeToken = false,
            )
            verify(exactly = 2) { analytics.send(expectedEvent) }
            verify(exactly = 2) { analytics.send(ofType<Basic.TransactionSent>()) }
        }

    private fun assertNativeFeeKept(model: YieldSupplyStartEarningModel) {
        val feeUM = model.uiState.value.yieldSupplyFeeUM as YieldSupplyFeeUM.Content
        assertThat(feeUM.transactionDataList).containsExactlyElementsIn(estimatedCalls).inOrder()
        assertThat(feeUM.gaslessFee).isNull()
        assertThat(model.feeCryptoCurrencyStatusFlow.value).isEqualTo(nativeFeeStatus)
        coVerify {
            notificationsUpdateTrigger.triggerUpdate(
                YieldSupplyNotificationData(feeValue = BigDecimal("0.002"), feeError = null),
            )
        }
    }

    private fun stubBatchEstimate(
        result: arrow.core.Either<GetFeeError, TransactionFeeExtended?>,
        isOperationTokenExcluded: Boolean? = null,
    ) {
        coEvery {
            estimateFeeForGaslessBatchUseCase(
                userWallet = any(),
                operationTokenStatus = any(),
                calls = any(),
                sameTokenRule = any(),
                isOperationTokenExcluded = isOperationTokenExcluded ?: any(),
            )
        } returns result
    }

    private fun stubFeeTokenLookup(id: CryptoCurrency.ID, status: CryptoCurrencyStatus) {
        every {
            with(CryptoCurrencyStatusOperations) {
                accountStatusList.getCryptoCurrencyStatus(id, any<Network>())
            }
        } returns status.some()
    }

    private fun nativeFee(): TransactionFeeExtended = TransactionFeeExtended(
        transactionFee = transactionFee(BigDecimal("0.002")),
        feeTokenId = coin.id,
        nativeFee = transactionFee(BigDecimal("0.002")),
    )

    private fun gaslessFee(feeTokenId: CryptoCurrency.ID): TransactionFeeExtended {
        val feeToken = if (feeTokenId == usdc.id) usdc else token
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
            feeTokenId = feeTokenId,
            gaslessFeePlan = GaslessFeePlan.TokenPay(feeToken = feeToken, fee = tokenFee),
            callGasLimits = listOf(BigInteger("29400"), BigInteger("29400")),
            gaslessProtocolData = GaslessProtocolData.Evm,
            nativeFee = transactionFee(BigDecimal("0.002")),
        )
    }

    private companion object {
        const val GASLESS_HASH = "0xgasless"
        val TOKEN_FEE = BigDecimal("0.5")
    }
}