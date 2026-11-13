package com.tangem.features.earn.lending.impl.session

import app.cash.turbine.test
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.yieldsupply.YieldSupplyContractCallDataProviderFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import com.tangem.domain.yield.supply.fixFee
import com.tangem.domain.yield.supply.models.YieldSupplyTransactionOutcome
import com.tangem.domain.yield.supply.usecase.YieldSupplyPendingTracker
import com.tangem.features.earn.lending.impl.analytics.LendingAnalyticsSender
import com.tangem.features.earn.lending.impl.provider.FakeLendingProvider
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import com.tangem.features.earn.lending.impl.provider.LendingDepositGate
import com.tangem.features.earn.lending.impl.provider.LendingMarket
import com.tangem.features.earn.lending.impl.provider.LendingPosition
import com.tangem.features.earn.lending.impl.provider.LendingProvider
import com.tangem.features.earn.lending.impl.provider.LendingProviderState
import com.tangem.features.earn.lending.impl.provider.LendingTransactions
import com.tangem.features.earn.lending.impl.provider.LendingTxError
import com.tangem.features.earn.lending.impl.provider.LendingTxExpectation
import com.tangem.features.earn.lending.impl.supplier.LendingAssetState
import com.tangem.features.earn.lending.impl.supplier.LendingSnapshot
import com.tangem.features.earn.lending.impl.supplier.LendingSupplier
import com.tangem.features.earn.product.api.analytics.EarnTxAnalytics
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.product.EarnValue
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.EarnActionInput
import com.tangem.features.earn.product.api.session.EarnAmount
import com.tangem.features.earn.product.api.session.EarnFeeSelection
import com.tangem.features.earn.product.api.session.EarnFlowAction
import com.tangem.features.earn.product.api.session.EarnFlowExit
import com.tangem.features.earn.product.api.session.EarnPlan
import com.tangem.features.earn.product.api.session.EarnResult
import com.tangem.features.earn.product.api.session.EarnResultAsset
import com.tangem.features.earn.product.api.session.EarnResultButton
import com.tangem.features.earn.product.api.session.EarnSessionError
import com.tangem.features.earn.product.api.session.EarnSigningStep
import com.tangem.features.earn.product.api.session.EarnTxRef
import com.tangem.features.earn.product.api.session.EarnTxStatus
import com.tangem.features.earn.product.api.session.EarnVerification
import com.tangem.features.earn.product.api.session.VerifiedPlan
import com.tangem.features.earn.product.api.session.Withdraw
import com.tangem.features.earn.product.api.session.WithdrawMode
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.math.BigInteger
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fee
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.FACTORY
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.MODULE
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.OWNER
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.createTx
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.ethFee

internal class LendingSessionTest {

    private val userWallet = MockUserWalletFactory.create()
    private val userWalletId = userWallet.walletId
    private val currencies = MockCryptoCurrencyFactory()
    private val eth = currencies.createCoin(Blockchain.Ethereum)
    private val weth = currencies.createToken(
        blockchain = Blockchain.Ethereum,
        contractAddress = WETH,
        symbol = "WETH",
        decimals = 18,
    )
    private val asset = LendingAsset(currency = eth, wrappedToken = weth)
    private val params = EarnProduct.Params(userWalletId = userWalletId, currency = eth)

    private val lendingSupplier: LendingSupplier = mockk()
    private val transactions: LendingTransactions = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val sendTransactionUseCase: SendTransactionUseCase = mockk()
    private val transactionRepository: YieldSupplyTransactionRepository = mockk()
    private val analyticsSender: LendingAnalyticsSender = mockk(relaxUnitFun = true)
    private val pendingTracker: YieldSupplyPendingTracker = mockk(relaxUnitFun = true)
    private val provider = FakeLendingProvider(assets = mapOf(eth.id to asset))

    private val session = createSession(provider)

    private val rawPreparation = tx(destination = FACTORY, value = BigDecimal.ZERO, fee = null)
    private val rawDeposit = tx(destination = MODULE, value = DEPOSIT_AMOUNT, fee = null)
    private val rawWithdraw = tx(destination = MODULE, value = BigDecimal.ZERO, fee = null)
    private val estimatedPreparation = rawPreparation.copy(fee = ethFee(PREPARATION_GAS))
    private val estimatedDeposit = rawDeposit.copy(fee = ethFee(DEPOSIT_GAS))
    private val estimatedWithdraw = rawWithdraw.copy(fee = ethFee(WITHDRAW_GAS))

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            lendingSupplier,
            transactions,
            getUserWalletUseCase,
            sendTransactionUseCase,
            transactionRepository,
            analyticsSender,
            pendingTracker,
        )
        provider.refreshCalls.clear()
        provider.minDepositResult = BigDecimal.ZERO.right()
        every { getUserWalletUseCase(userWalletId) } returns userWallet.right()
        arrangeState(assetState())
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MaxAmount {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN snapshot WHEN maxAmount THEN returns the source of the action`(model: MaxAmountModel) = runTest {
            // Arrange
            arrangeSnapshot(model.assets)

            // Act
            val actual = session.maxAmount(model.action)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            MaxAmountModel(action = Deposit(), assets = stateMap(), expected = BALANCE.right()),
            MaxAmountModel(action = Withdraw(), assets = stateMap(), expected = MAX_WITHDRAW.right()),
            MaxAmountModel(
                action = Withdraw(),
                assets = stateMap(assetState(maxWithdraw = null)),
                expected = EarnSessionError.Failed().left(),
            ),
            MaxAmountModel(
                action = Deposit(),
                assets = stateMap(assetState(balance = null)),
                expected = EarnSessionError.Failed().left(),
            ),
            MaxAmountModel(action = UnknownAction, assets = stateMap(), expected = EarnSessionError.Failed().left()),
            MaxAmountModel(action = Deposit(), assets = emptyMap(), expected = EarnSessionError.Failed().left()),
        )
    }

    @Nested
    inner class LoadFee {

        @Test
        fun `GIVEN deposit with preparation WHEN loadFee THEN prices the built pair`() = runTest {
            // Arrange
            val levels = TransactionFee.Single(normal = selectedFee(TOTAL_GAS))
            arrangeDeposit(built = listOf(rawPreparation, rawDeposit))
            coEvery {
                transactions.feeLevels(
                    userWallet = userWallet,
                    asset = asset,
                    txs = listOf(rawPreparation, rawDeposit),
                )
            } returns levels.right()

            // Act
            val actual = session.loadFee(Deposit(), EarnAmount.Exact(DEPOSIT_AMOUNT))

            // Assert
            assertThat(actual).isEqualTo(levels.right())
        }

        @Test
        fun `GIVEN no amount WHEN loadFee THEN builds the transactions for the max amount`() = runTest {
            // Arrange
            val levels = TransactionFee.Single(normal = selectedFee(DEPOSIT_GAS))
            arrangeDeposit(built = listOf(rawDeposit), amount = BALANCE)
            coEvery { transactions.feeLevels(userWallet = any(), asset = any(), txs = any()) } returns
                levels.right()

            // Act
            val actual = session.loadFee(Deposit(), null)

            // Assert
            assertThat(actual).isEqualTo(levels.right())
        }

        @Test
        fun `GIVEN the build fails WHEN loadFee THEN returns unknown fee error`() = runTest {
            // Arrange
            coEvery {
                transactions.deposit(userWalletId = any(), asset = any(), amount = any(), state = any())
            } returns LendingTxError.ReadFailed.left()

            // Act
            val actual = session.loadFee(Deposit(), EarnAmount.Exact(DEPOSIT_AMOUNT))

            // Assert
            assertThat(actual).isEqualTo(GetFeeError.UnknownError.left())
        }

        @Test
        fun `GIVEN unknown action WHEN loadFee THEN returns unknown fee error`() = runTest {
            // Act
            val actual = session.loadFee(UnknownAction, EarnAmount.Exact(DEPOSIT_AMOUNT))

            // Assert
            assertThat(actual).isEqualTo(GetFeeError.UnknownError.left())
        }

        @Test
        fun `GIVEN fee levels WHEN loadFeeExtended THEN wraps them for the coin`() = runTest {
            // Arrange
            val levels = TransactionFee.Single(normal = selectedFee(DEPOSIT_GAS))
            arrangeDeposit(built = listOf(rawDeposit))
            coEvery { transactions.feeLevels(userWallet = any(), asset = any(), txs = any()) } returns
                levels.right()

            // Act
            val actual = session.loadFeeExtended(Deposit(), EarnAmount.Exact(DEPOSIT_AMOUNT), feeCurrency = null)

            // Assert
            assertThat(actual).isEqualTo(TransactionFeeExtended(transactionFee = levels, feeTokenId = eth.id).right())
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Prepare {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN deposit transactions WHEN prepare THEN plan carries the estimated ones`(model: PrepareModel) =
            runTest {
                // Arrange
                arrangeDeposit(built = model.built)

                // Act
                val actual = session.prepare(Deposit(), EarnAmount.Exact(DEPOSIT_AMOUNT), selection(TOTAL_GAS))

                // Assert
                assertThat(actual).isEqualTo(depositPlan(txs = model.estimated).right())
            }

        private fun provideTestModels() = listOf(
            PrepareModel(built = listOf(rawDeposit), estimated = listOf(estimatedDeposit)),
            PrepareModel(
                built = listOf(rawPreparation, rawDeposit),
                estimated = listOf(estimatedPreparation, estimatedDeposit),
            ),
        )

        @Test
        fun `GIVEN provider contract needs an upgrade WHEN prepare THEN plan is marked as with upgrade`() = runTest {
            // Arrange
            val outdated = TestProviderState(PLAN_KEY, isUpgradeRequired = true)
            arrangeState(assetState(providerState = outdated))
            arrangeDeposit(built = listOf(rawDeposit))

            // Act
            val actual = session.prepare(Deposit(), EarnAmount.Exact(DEPOSIT_AMOUNT), selection(TOTAL_GAS))

            // Assert
            assertThat(actual).isEqualTo(depositPlan(txs = listOf(estimatedDeposit), isWithUpgrade = true).right())
        }

        @Test
        fun `GIVEN withdraw WHEN prepare THEN builds an instant withdrawal for the position`() = runTest {
            // Arrange
            arrangeWithdraw(amount = WITHDRAW_AMOUNT)

            // Act
            val actual = session.prepare(Withdraw(), EarnAmount.Exact(WITHDRAW_AMOUNT), selection(WITHDRAW_GAS))

            // Assert
            assertThat(actual).isEqualTo(withdrawPlan().right())
        }

        @Test
        fun `GIVEN deposit below the min deposit WHEN prepare THEN returns below minimum without building`() =
            runTest {
                // Arrange
                provider.minDepositResult = (DEPOSIT_AMOUNT + BigDecimal.ONE).right()

                // Act
                val actual = session.prepare(Deposit(), EarnAmount.Exact(DEPOSIT_AMOUNT), selection(TOTAL_GAS))

                // Assert
                assertThat(actual).isEqualTo(EarnSessionError.BelowMinimum(DEPOSIT_AMOUNT + BigDecimal.ONE).left())
                coVerify(exactly = 0) {
                    transactions.deposit(userWalletId = any(), asset = any(), amount = any(), state = any())
                }
            }

        @Test
        fun `GIVEN min deposit unavailable WHEN prepare deposit THEN builds the plan`() = runTest {
            // Arrange
            provider.minDepositResult = IllegalStateException("no fee").left()
            arrangeDeposit(built = listOf(rawPreparation, rawDeposit))

            // Act
            val actual = session.prepare(Deposit(), EarnAmount.Exact(DEPOSIT_AMOUNT), selection(TOTAL_GAS))

            // Assert
            assertThat(actual).isEqualTo(depositPlan().right())
        }

        @Test
        fun `GIVEN withdraw below the min deposit WHEN prepare THEN builds the plan`() = runTest {
            // Arrange
            provider.minDepositResult = (WITHDRAW_AMOUNT + BigDecimal.ONE).right()
            arrangeWithdraw(amount = WITHDRAW_AMOUNT)

            // Act
            val actual = session.prepare(Withdraw(), EarnAmount.Exact(WITHDRAW_AMOUNT), selection(WITHDRAW_GAS))

            // Assert
            assertThat(actual).isEqualTo(withdrawPlan().right())
        }

        @Test
        fun `GIVEN entire withdraw WHEN prepare THEN uses the max withdraw`() = runTest {
            // Arrange
            arrangeWithdraw(amount = MAX_WITHDRAW)

            // Act
            val actual = session.prepare(Withdraw(), EarnAmount.Entire, selection(WITHDRAW_GAS))

            // Assert
            assertThat(actual).isEqualTo(withdrawPlan(amount = MAX_WITHDRAW).right())
        }

        @ParameterizedTest
        @MethodSource("buildErrors")
        fun `GIVEN the build fails WHEN prepare THEN maps the error`(model: BuildErrorModel) = runTest {
            // Arrange
            coEvery {
                transactions.deposit(userWalletId = any(), asset = any(), amount = any(), state = any())
            } returns model.error.left()

            // Act
            val actual = session.prepare(Deposit(), EarnAmount.Exact(DEPOSIT_AMOUNT), selection(TOTAL_GAS))

            // Assert
            assertThat(actual).isEqualTo(model.expected.left())
        }

        fun buildErrors() = listOf(
            BuildErrorModel(
                error = LendingTxError.UpdateRequired,
                expected = EarnSessionError.Rejected(updateRequiredText),
            ),
            BuildErrorModel(error = LendingTxError.ReadFailed, expected = EarnSessionError.Failed()),
            BuildErrorModel(error = LendingTxError.Unverified("call data"), expected = EarnSessionError.Failed()),
        )

        @Test
        fun `GIVEN module needs a newer app WHEN prepare withdraw THEN rejects with the update text`() = runTest {
            // Arrange
            coEvery {
                transactions.withdraw(
                    userWalletId = any(),
                    asset = any(),
                    amount = any(),
                    position = any(),
                    state = any(),
                )
            } returns LendingTxError.UpdateRequired.left()

            // Act
            val actual = session.prepare(Withdraw(), EarnAmount.Exact(WITHDRAW_AMOUNT), selection(WITHDRAW_GAS))

            // Assert
            assertThat(actual).isEqualTo(EarnSessionError.Rejected(updateRequiredText).left())
        }

        @Test
        fun `GIVEN estimation fails WHEN prepare THEN returns fee error`() = runTest {
            // Arrange
            coEvery {
                transactions.deposit(userWalletId = any(), asset = any(), amount = any(), state = any())
            } returns listOf(rawDeposit).right()
            coEvery { transactions.estimate(userWallet = any(), asset = any(), txs = any()) } returns
                GetFeeError.UnknownError.left()

            // Act
            val actual = session.prepare(Deposit(), EarnAmount.Exact(DEPOSIT_AMOUNT), selection(TOTAL_GAS))

            // Assert
            assertThat(actual).isEqualTo(EarnSessionError.Fee(GetFeeError.UnknownError).left())
        }

        @Test
        fun `GIVEN no provider state WHEN prepare THEN fails`() = runTest {
            // Arrange
            arrangeState(assetState(providerState = null))

            // Act
            val actual = session.prepare(Deposit(), EarnAmount.Exact(DEPOSIT_AMOUNT), selection(TOTAL_GAS))

            // Assert
            assertThat(actual).isEqualTo(EarnSessionError.Failed().left())
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Verify {

        @Test
        fun `GIVEN pending transaction WHEN verify THEN rejects with the wait text`() = runTest {
            // Arrange
            arrangeState(assetState(hasPendingTx = true))

            // Act
            val actual = session.verify(depositPlan())

            // Assert
            val reason = stringReference("Wait for the previous transaction to complete.")
            assertThat(actual).isEqualTo(EarnVerification.Rejected(EarnSessionError.Rejected(reason)))
        }

        @Test
        fun `GIVEN deposit with fee above the balance WHEN verify THEN rejects insufficient balance`() = runTest {
            // Arrange
            arrangeState(assetState(balance = DEPOSIT_AMOUNT))

            // Act
            val actual = session.verify(depositPlan())

            // Assert
            assertThat(actual).isEqualTo(EarnVerification.Rejected(EarnSessionError.InsufficientBalance))
        }

        @Test
        fun `GIVEN withdraw above max withdraw WHEN verify THEN rejects exceeding the limit`() = runTest {
            // Arrange
            val plan = withdrawPlan(amount = MAX_WITHDRAW + BigDecimal.ONE)

            // Act
            val actual = session.verify(plan)

            // Assert
            assertThat(actual).isEqualTo(EarnVerification.Rejected(EarnSessionError.ExceedsLimit(MAX_WITHDRAW)))
        }

        @Test
        fun `GIVEN closed market WHEN verify deposit THEN rejects with the paused text`() = runTest {
            // Arrange
            arrangeState(assetState(isOpen = false))

            // Act
            val actual = session.verify(depositPlan())

            // Assert
            val reason = stringReference("Deposits are paused by the provider.")
            assertThat(actual).isEqualTo(EarnVerification.Rejected(EarnSessionError.Rejected(reason)))
        }

        @Test
        fun `GIVEN closed market WHEN verify withdraw THEN verifies the plan`() = runTest {
            // Arrange
            arrangeState(assetState(isOpen = false))
            arrangeWithdraw(amount = WITHDRAW_AMOUNT)
            arrangeVerifier(Unit.right())

            // Act
            val actual = session.verify(withdrawPlan())

            // Assert
            assertThat(actual.verifiedPlan()).isEqualTo(withdrawPlan())
        }

        @Test
        fun `GIVEN withdraw fee above the balance WHEN verify THEN rejects insufficient balance`() = runTest {
            // Arrange
            arrangeState(assetState(balance = BigDecimal.ZERO))

            // Act
            val actual = session.verify(withdrawPlan())

            // Assert
            assertThat(actual).isEqualTo(EarnVerification.Rejected(EarnSessionError.InsufficientBalance))
        }

        @Test
        fun `GIVEN custom gas limit below the estimate WHEN verify THEN rejects the gas limit`() = runTest {
            // Arrange
            val plan = depositPlan(fee = selectedFee(TOTAL_GAS - BigInteger.ONE))
            arrangeDeposit(built = listOf(rawPreparation, rawDeposit))

            // Act
            val actual = session.verify(plan)

            // Assert
            val reason = stringReference("The gas limit is too low for this transaction.")
            assertThat(actual).isEqualTo(EarnVerification.Rejected(EarnSessionError.Rejected(reason)))
            coVerify(exactly = 0) {
                transactions.verify(userWalletId = any(), asset = any(), txs = any(), expected = any(), state = any())
            }
        }

        @Test
        fun `GIVEN fee short of a fresh estimate WHEN verify the changed plan THEN rejects, not loops`() =
            runTest {
                // Arrange
                val plan = depositPlan(fee = selectedFee(TOTAL_GAS))
                arrangeFreshEstimate(preparationGas = PREPARATION_GAS, depositGas = DEPOSIT_GAS + BigInteger.ONE)
                val changed = session.verify(plan) as EarnVerification.Changed

                // Act
                val actual = session.verify(changed.newPlan)

                // Assert
                val reason = stringReference("The gas limit is too low for this transaction.")
                assertThat(actual).isEqualTo(EarnVerification.Rejected(EarnSessionError.Rejected(reason)))
            }

        @ParameterizedTest
        @MethodSource("freshEstimates")
        fun `GIVEN fresh gas estimate WHEN verify THEN changed only when a transaction can run out of gas`(
            model: FreshEstimateModel,
        ) = runTest {
            // Arrange
            val plan = depositPlan(fee = selectedFee(model.selectedGas))
            arrangeFreshEstimate(preparationGas = model.preparationGas, depositGas = model.depositGas)
            arrangeVerifier(Unit.right())

            // Act
            val actual = session.verify(plan)

            // Assert
            val freshTxs = listOf(
                rawPreparation.copy(fee = ethFee(model.preparationGas)),
                rawDeposit.copy(fee = ethFee(model.depositGas)),
            )
            val expected = if (model.isChanged) {
                EarnVerification.Changed(depositPlan(txs = freshTxs, fee = plan.fee))
            } else {
                null
            }
            if (expected == null) {
                assertThat(actual.verifiedPlan()).isEqualTo(plan)
            } else {
                assertThat(actual).isEqualTo(expected)
            }
        }

        fun freshEstimates() = listOf(
            FreshEstimateModel(
                name = "same estimate",
                selectedGas = TOTAL_GAS,
                preparationGas = PREPARATION_GAS,
                depositGas = DEPOSIT_GAS,
                isChanged = false,
            ),
            FreshEstimateModel(
                name = "deposit grew within a raised limit",
                selectedGas = SELECTED_GAS,
                preparationGas = PREPARATION_GAS,
                depositGas = DEPOSIT_GAS + BigInteger.ONE,
                isChanged = false,
            ),
            FreshEstimateModel(
                name = "deposit grew beyond the limit",
                selectedGas = TOTAL_GAS,
                preparationGas = PREPARATION_GAS,
                depositGas = DEPOSIT_GAS + BigInteger.ONE,
                isChanged = true,
            ),
            FreshEstimateModel(
                name = "preparation grew beyond its own limit",
                selectedGas = SELECTED_GAS,
                preparationGas = PREPARATION_GAS + BigInteger.ONE,
                depositGas = DEPOSIT_GAS,
                isChanged = true,
            ),
            FreshEstimateModel(
                name = "estimate dropped",
                selectedGas = TOTAL_GAS,
                preparationGas = PREPARATION_GAS - BigInteger.ONE,
                depositGas = DEPOSIT_GAS - BigInteger.ONE,
                isChanged = false,
            ),
        )

        @ParameterizedTest
        @MethodSource("rebuiltDifferences")
        fun `GIVEN rebuilt transactions differ WHEN verify THEN returns the rebuilt plan`(model: RebuiltModel) =
            runTest {
                // Arrange
                arrangeDeposit(built = model.rebuilt)

                // Act
                val actual = session.verify(depositPlan())

                // Assert
                assertThat(actual).isEqualTo(EarnVerification.Changed(depositPlan(txs = model.rebuilt.map(::estimated))))
            }

        fun rebuiltDifferences() = listOf(
            RebuiltModel(name = "preparation dropped", rebuilt = listOf(rawDeposit)),
            RebuiltModel(name = "another destination", rebuilt = listOf(rawPreparation, rawDeposit.sentTo(OTHER))),
            RebuiltModel(
                name = "another value",
                rebuilt = listOf(rawPreparation, rawDeposit.withValue(DEPOSIT_AMOUNT + BigDecimal.ONE)),
            ),
            RebuiltModel(name = "another call", rebuilt = listOf(rawPreparation, rawDeposit.withCallData(OTHER_CALL))),
        )

        @Test
        fun `GIVEN non ethereum fee WHEN verify THEN fails`() = runTest {
            // Arrange
            val plan = depositPlan(fee = Fee.Common(Amount(BigDecimal.ONE, Blockchain.Ethereum)))

            // Act
            val actual = session.verify(plan)

            // Assert
            assertThat(actual).isEqualTo(EarnVerification.Rejected(EarnSessionError.Failed()))
        }

        @Test
        fun `GIVEN plan key changed WHEN verify THEN returns the plan rebuilt for the new state`() = runTest {
            // Arrange
            arrangeState(assetState(providerState = TestProviderState(NEW_PLAN_KEY)))
            arrangeDeposit(built = listOf(rawDeposit))

            // Act
            val actual = session.verify(depositPlan())

            // Assert
            val expected = depositPlan(txs = listOf(estimatedDeposit), planKey = NEW_PLAN_KEY)
            assertThat(actual).isEqualTo(EarnVerification.Changed(expected))
        }

        @Test
        fun `GIVEN verifier rejects WHEN verify THEN fails`() = runTest {
            // Arrange
            arrangeDeposit(built = listOf(rawPreparation, rawDeposit))
            arrangeVerifier(LendingTxError.Unverified("selector").left())

            // Act
            val actual = session.verify(depositPlan())

            // Assert
            assertThat(actual).isEqualTo(EarnVerification.Rejected(EarnSessionError.Failed()))
        }

        @Test
        fun `GIVEN unchanged deposit WHEN verify THEN verifies the plan against the wallet address`() = runTest {
            // Arrange
            val plan = depositPlan()
            arrangeDeposit(built = listOf(rawPreparation, rawDeposit))
            arrangeVerifier(Unit.right())

            // Act
            val actual = session.verify(plan)

            // Assert
            assertThat(actual.verifiedPlan()).isEqualTo(plan)
            coVerify(exactly = 1) {
                transactions.verify(
                    userWalletId = userWalletId,
                    asset = asset,
                    txs = plan.txs,
                    expected = LendingTxExpectation.Deposit(owner = OWNER, amount = DEPOSIT_AMOUNT),
                    state = TestProviderState(PLAN_KEY),
                )
            }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN withdraw WHEN verify THEN expects the entire flag of the plan`(model: WithdrawVerifyModel) =
            runTest {
                // Arrange
                val plan = withdrawPlan(amount = model.amount, isEntire = model.isEntire)
                arrangeState(assetState(position = model.position, maxWithdraw = model.position))
                arrangeWithdraw(amount = model.amount, position = model.position)
                arrangeVerifier(Unit.right())

                // Act
                val actual = session.verify(plan)

                // Assert
                assertThat(actual.verifiedPlan()).isEqualTo(plan)
                coVerify(exactly = 1) {
                    transactions.verify(
                        userWalletId = userWalletId,
                        asset = asset,
                        txs = plan.txs,
                        expected = LendingTxExpectation.Withdraw(
                            owner = OWNER,
                            amount = model.amount,
                            isEntire = model.isEntire,
                        ),
                        state = TestProviderState(PLAN_KEY),
                    )
                }
            }

        private fun provideTestModels() = listOf(
            WithdrawVerifyModel(amount = WITHDRAW_AMOUNT, position = POSITION, isEntire = false),
            WithdrawVerifyModel(amount = POSITION, position = POSITION, isEntire = true),
        )

        @Test
        fun `GIVEN entire withdraw and position grew since prepare WHEN verify THEN verified as entire`() = runTest {
            // Arrange
            val plan = withdrawPlan(amount = POSITION, isEntire = true)
            val grownPosition = POSITION + BigDecimal("0.001")
            arrangeState(assetState(position = grownPosition, maxWithdraw = grownPosition))
            arrangeWithdraw(amount = POSITION, position = grownPosition)
            arrangeVerifier(Unit.right())

            // Act
            val actual = session.verify(plan)

            // Assert
            assertThat(actual.verifiedPlan()).isEqualTo(plan)
            coVerify(exactly = 1) {
                transactions.verify(
                    userWalletId = userWalletId,
                    asset = asset,
                    txs = plan.txs,
                    expected = LendingTxExpectation.Withdraw(owner = OWNER, amount = POSITION, isEntire = true),
                    state = TestProviderState(PLAN_KEY),
                )
            }
        }

        @Test
        fun `GIVEN entire withdraw rebuilt as partial after position grew WHEN verify THEN returns the rebuilt plan`() =
            runTest {
                // Arrange
                val plan = withdrawPlan(amount = POSITION, isEntire = true)
                val grownPosition = POSITION + BigDecimal("0.001")
                val partialWithdraw = rawWithdraw.withCallData(OTHER_CALL)
                arrangeState(assetState(position = grownPosition, maxWithdraw = grownPosition))
                coEvery {
                    transactions.withdraw(
                        userWalletId = userWalletId,
                        asset = asset,
                        amount = POSITION,
                        position = grownPosition,
                        state = TestProviderState(PLAN_KEY),
                    )
                } returns partialWithdraw.right()
                coEvery {
                    transactions.estimate(userWallet = userWallet, asset = asset, txs = listOf(partialWithdraw))
                } returns listOf(partialWithdraw.copy(fee = ethFee(WITHDRAW_GAS))).right()

                // Act
                val actual = session.verify(plan)

                // Assert
                val expected = withdrawPlan(amount = POSITION, isEntire = false)
                    .copy(txs = listOf(partialWithdraw.copy(fee = ethFee(WITHDRAW_GAS))))
                assertThat(actual).isEqualTo(EarnVerification.Changed(expected))
            }

        @Test
        fun `GIVEN no wallet address WHEN verify THEN fails`() = runTest {
            // Arrange
            arrangeState(assetState(address = null))
            arrangeDeposit(built = listOf(rawPreparation, rawDeposit))

            // Act
            val actual = session.verify(depositPlan())

            // Assert
            assertThat(actual).isEqualTo(EarnVerification.Rejected(EarnSessionError.Failed()))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Execute {

        @Test
        fun `GIVEN two transactions WHEN execute THEN sends them at once with the selected fee split`() = runTest {
            // Arrange
            val selected = selectedFee(SELECTED_GAS)
            val expectedTxs = listOf(
                estimatedPreparation.copy(fee = selected.fixFee(eth, PREPARATION_GAS)),
                estimatedDeposit.copy(fee = selected.fixFee(eth, SELECTED_GAS - PREPARATION_GAS)),
            )
            arrangeDeposit(built = listOf(rawPreparation, rawDeposit))
            val plan = verified(depositPlan(fee = selected))
            arrangeSend(listOf(PREPARATION_HASH, DEPOSIT_HASH).right())

            // Act
            session.execute(plan).test {
                // Assert
                assertThat(awaitItem()).isEqualTo(
                    EarnSigningStep.AwaitingConfirmation(stepLabel = null, tx = analytics("deposit")),
                )
                val sent = awaitItem() as EarnSigningStep.Sent
                val expectedRef = txRef(id = DEPOSIT_HASH, actionId = "deposit", createdAt = sent.ref.createdAt)
                assertThat(sent).isEqualTo(EarnSigningStep.Sent(expectedRef))
                assertThat(awaitItem()).isEqualTo(EarnSigningStep.Done(expectedRef, depositResult()))
                awaitComplete()
            }
            coVerify(exactly = 1) {
                sendTransactionUseCase(
                    txsData = expectedTxs,
                    userWallet = userWallet,
                    network = eth.network,
                    sendMode = TransactionSender.MultipleTransactionSendMode.DEFAULT,
                )
            }
            val sentGas = expectedTxs.sumOf { (it.fee as Fee.Ethereum).gasLimit }
            assertThat(sentGas).isEqualTo(SELECTED_GAS)
            assertThat(provider.refreshCalls).containsExactly(userWalletId to asset)
        }

        @Test
        fun `GIVEN transactions sent WHEN execute THEN reports the sent plan`() = runTest {
            // Arrange
            arrangeDeposit(built = listOf(rawPreparation, rawDeposit))
            val plan = verified(depositPlan())
            arrangeSend(listOf(PREPARATION_HASH, DEPOSIT_HASH).right())

            // Act
            session.execute(plan).test { cancelAndConsumeRemainingEvents() }

            // Assert
            coVerify(exactly = 1) { analyticsSender.onSent(plan = plan.plan as LendingPlan, currency = eth) }
            coVerify(exactly = 0) { analyticsSender.onSendFailed(plan = any(), error = any()) }
        }

        @Test
        fun `GIVEN transactions sent WHEN execute THEN tracks their hashes until they confirm`() = runTest {
            // Arrange
            arrangeDeposit(built = listOf(rawPreparation, rawDeposit))
            val plan = verified(depositPlan())
            arrangeSend(listOf(PREPARATION_HASH, DEPOSIT_HASH).right())

            // Act
            session.execute(plan).test { cancelAndConsumeRemainingEvents() }

            // Assert
            coVerify(exactly = 1) {
                pendingTracker.addPending(
                    userWalletId = userWalletId,
                    cryptoCurrency = eth,
                    txIds = listOf(PREPARATION_HASH, DEPOSIT_HASH),
                    depositContext = null,
                )
            }
        }

        @Test
        fun `GIVEN withdraw WHEN execute THEN shows the withdrawal result`() = runTest {
            // Arrange
            arrangeWithdraw(amount = WITHDRAW_AMOUNT)
            val plan = verified(withdrawPlan())
            arrangeSend(listOf(WITHDRAW_HASH).right())

            // Act
            session.execute(plan).test {
                // Assert
                awaitItem()
                val sent = awaitItem() as EarnSigningStep.Sent
                val expectedRef = txRef(id = WITHDRAW_HASH, actionId = "withdraw", createdAt = sent.ref.createdAt)
                assertThat(awaitItem()).isEqualTo(EarnSigningStep.Done(expectedRef, withdrawResult(plan.plan.fee)))
                awaitComplete()
            }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN send fails WHEN execute THEN fails and refreshes`(error: SendTransactionError) = runTest {
            // Arrange
            arrangeDeposit(built = listOf(rawPreparation, rawDeposit))
            val plan = verified(depositPlan())
            arrangeSend(error.left())

            // Act
            session.execute(plan).test {
                // Assert
                assertThat(awaitItem()).isInstanceOf(EarnSigningStep.AwaitingConfirmation::class.java)
                assertThat(awaitItem()).isEqualTo(EarnSigningStep.Failed(error))
                awaitComplete()
            }
            assertThat(provider.refreshCalls).containsExactly(userWalletId to asset)
            coVerify(exactly = 1) { analyticsSender.onSendFailed(plan = plan.plan as LendingPlan, error = error) }
            coVerify(exactly = 0) { analyticsSender.onSent(plan = any(), currency = any()) }
            coVerify(exactly = 0) {
                pendingTracker.addPending(userWalletId = any(), cryptoCurrency = any(), txIds = any(), depositContext = any())
            }
        }

        @Test
        fun `GIVEN plan that skipped verify WHEN execute THEN fails without sending`() = runTest {
            // Arrange
            val unverified = object : VerifiedPlan {
                override val plan: EarnPlan = depositPlan()
            }

            // Act
            session.execute(unverified).test {
                // Assert
                assertThat(awaitItem()).isEqualTo(EarnSigningStep.Failed(SendTransactionError.UnknownError()))
                awaitComplete()
            }
            coVerify(exactly = 0) {
                sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
            }
        }

        private fun provideTestModels() = listOf(
            SendTransactionError.UserCancelledError,
            SendTransactionError.NetworkError(message = "deposit not sent", code = null),
        )
    }

    @Nested
    inner class Track {

        @Test
        fun `GIVEN not mined then confirmed WHEN track THEN polls every ten seconds until confirmed`() = runTest {
            // Arrange
            coEvery {
                transactionRepository.getTransactionOutcome(
                    userWalletId = userWalletId,
                    cryptoCurrency = weth,
                    transactionHash = DEPOSIT_HASH,
                )
            } returnsMany listOf(null, YieldSupplyTransactionOutcome.Confirmed)

            // Act
            session.track(txRef(id = DEPOSIT_HASH, actionId = "deposit")).test {
                // Assert
                assertThat(awaitItem()).isEqualTo(EarnTxStatus(TxInfo.TransactionStatus.Unconfirmed))
                assertThat(awaitItem()).isEqualTo(EarnTxStatus(TxInfo.TransactionStatus.Confirmed))
                assertThat(testScheduler.currentTime).isEqualTo(POLL_INTERVAL_MS)
                awaitComplete()
            }
        }

        @Test
        fun `GIVEN reverted WHEN track THEN ends failed`() = runTest {
            // Arrange
            coEvery {
                transactionRepository.getTransactionOutcome(
                    userWalletId = any(),
                    cryptoCurrency = any(),
                    transactionHash = any(),
                )
            } returns YieldSupplyTransactionOutcome.Reverted

            // Act
            session.track(txRef(id = DEPOSIT_HASH, actionId = "deposit")).test {
                // Assert
                assertThat(awaitItem()).isEqualTo(EarnTxStatus(TxInfo.TransactionStatus.Failed))
                awaitComplete()
            }
        }

        @Test
        fun `GIVEN never mined WHEN track THEN gives up unconfirmed after the last attempt`() = runTest {
            // Arrange
            coEvery {
                transactionRepository.getTransactionOutcome(
                    userWalletId = any(),
                    cryptoCurrency = any(),
                    transactionHash = any(),
                )
            } returns null

            // Act
            session.track(txRef(id = DEPOSIT_HASH, actionId = "deposit")).test {
                // Assert
                assertThat(awaitItem()).isEqualTo(EarnTxStatus(TxInfo.TransactionStatus.Unconfirmed))
                awaitComplete()
            }
            assertThat(testScheduler.currentTime).isEqualTo((MAX_TRACK_ATTEMPTS - 1) * POLL_INTERVAL_MS)
            coVerify(exactly = MAX_TRACK_ATTEMPTS) {
                transactionRepository.getTransactionOutcome(
                    userWalletId = userWalletId,
                    cryptoCurrency = weth,
                    transactionHash = DEPOSIT_HASH,
                )
            }
        }

        @Test
        fun `GIVEN token asset WHEN track THEN ends unconfirmed without polling`() = runTest {
            // Arrange
            val tokenSession = createSession(FakeLendingProvider(assets = mapOf(eth.id to asset.copy(currency = weth))))

            // Act
            tokenSession.track(txRef(id = DEPOSIT_HASH, actionId = "deposit")).test {
                // Assert
                assertThat(awaitItem()).isEqualTo(EarnTxStatus(TxInfo.TransactionStatus.Unconfirmed))
                awaitComplete()
            }
            coVerify(exactly = 0) {
                transactionRepository.getTransactionOutcome(
                    userWalletId = any(),
                    cryptoCurrency = any(),
                    transactionHash = any(),
                )
            }
        }

        @Test
        fun `GIVEN no wrapped token WHEN track THEN ends unconfirmed without polling`() = runTest {
            // Arrange
            val unwrapped = createSession(FakeLendingProvider(assets = mapOf(eth.id to asset.copy(wrappedToken = null))))

            // Act
            unwrapped.track(txRef(id = DEPOSIT_HASH, actionId = "deposit")).test {
                // Assert
                assertThat(awaitItem()).isEqualTo(EarnTxStatus(TxInfo.TransactionStatus.Unconfirmed))
                awaitComplete()
            }
            coVerify(exactly = 0) {
                transactionRepository.getTransactionOutcome(
                    userWalletId = any(),
                    cryptoCurrency = any(),
                    transactionHash = any(),
                )
            }
        }
    }

    private fun createSession(lendingProvider: LendingProvider) = LendingSession(
        params = params,
        lendingSupplier = lendingSupplier,
        lendingProvider = lendingProvider,
        planFactory = LendingPlanFactory(
            transactions = transactions,
            getUserWalletUseCase = getUserWalletUseCase,
            lendingProvider = lendingProvider,
        ),
        sender = LendingTxSender(
            getUserWalletUseCase = getUserWalletUseCase,
            sendTransactionUseCase = sendTransactionUseCase,
            pendingTracker = pendingTracker,
        ),
        transactionRepository = transactionRepository,
        analyticsSender = analyticsSender,
    )

    private fun arrangeState(state: LendingAssetState) = arrangeSnapshot(stateMap(state))

    private suspend fun verified(plan: LendingPlan): VerifiedPlan {
        arrangeVerifier(Unit.right())
        return (session.verify(plan) as EarnVerification.Verified).plan
    }

    private fun EarnVerification.verifiedPlan(): EarnPlan? = (this as? EarnVerification.Verified)?.plan?.plan

    private fun arrangeSnapshot(assets: Map<CryptoCurrency.ID, LendingAssetState>) {
        every { lendingSupplier(userWalletId) } returns
            flowOf(LendingSnapshot(assets = assets, source = StatusSource.ACTUAL))
    }

    private fun stateMap(state: LendingAssetState = assetState()) = mapOf(eth.id to state)

    private fun arrangeDeposit(built: List<TransactionData.Uncompiled>, amount: BigDecimal = DEPOSIT_AMOUNT) {
        coEvery {
            transactions.deposit(userWalletId = userWalletId, asset = asset, amount = amount, state = any())
        } returns built.right()
        coEvery { transactions.estimate(userWallet = userWallet, asset = asset, txs = built) } returns
            built.map(::estimated).right()
    }

    private fun arrangeFreshEstimate(preparationGas: BigInteger, depositGas: BigInteger) {
        val built = listOf(rawPreparation, rawDeposit)
        coEvery {
            transactions.deposit(userWalletId = userWalletId, asset = asset, amount = DEPOSIT_AMOUNT, state = any())
        } returns built.right()
        coEvery { transactions.estimate(userWallet = userWallet, asset = asset, txs = built) } returns listOf(
            rawPreparation.copy(fee = ethFee(preparationGas)),
            rawDeposit.copy(fee = ethFee(depositGas)),
        ).right()
    }

    private fun arrangeWithdraw(amount: BigDecimal, position: BigDecimal = POSITION) {
        coEvery {
            transactions.withdraw(
                userWalletId = userWalletId,
                asset = asset,
                amount = amount,
                position = position,
                state = TestProviderState(PLAN_KEY),
            )
        } returns rawWithdraw.right()
        coEvery { transactions.estimate(userWallet = userWallet, asset = asset, txs = listOf(rawWithdraw)) } returns
            listOf(estimatedWithdraw).right()
    }

    private fun arrangeVerifier(result: Either<LendingTxError, Unit>) {
        coEvery {
            transactions.verify(userWalletId = any(), asset = any(), txs = any(), expected = any(), state = any())
        } returns result
    }

    private fun arrangeSend(result: Either<SendTransactionError, List<String>>) {
        coEvery {
            sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
        } returns result
    }

    private fun estimated(tx: TransactionData.Uncompiled): TransactionData.Uncompiled = when (tx) {
        rawPreparation -> estimatedPreparation
        rawWithdraw -> estimatedWithdraw
        else -> tx.copy(fee = ethFee(DEPOSIT_GAS))
    }

    private fun assetState(
        balance: BigDecimal? = BALANCE,
        address: String? = OWNER,
        hasPendingTx: Boolean = false,
        isOpen: Boolean = true,
        position: BigDecimal? = POSITION,
        maxWithdraw: BigDecimal? = MAX_WITHDRAW,
        providerState: LendingProviderState? = TestProviderState(PLAN_KEY),
    ) = LendingAssetState(
        asset = asset,
        address = address,
        balance = balance,
        fiatRate = null,
        market = LendingMarket(
            rate = null,
            isOpen = isOpen,
            deposit = LendingDepositGate.Open,
            position = position?.let(::LendingPosition),
            maxWithdraw = maxWithdraw,
            providerState = providerState,
            source = StatusSource.ACTUAL,
        ),
        hasPendingTx = hasPendingTx,
        isWrappedTokenInWallet = false,
        source = StatusSource.ACTUAL,
    )

    private fun depositPlan(
        txs: List<TransactionData.Uncompiled> = listOf(estimatedPreparation, estimatedDeposit),
        fee: Fee = selectedFee(TOTAL_GAS),
        planKey: String = PLAN_KEY,
        isWithUpgrade: Boolean = false,
    ) = LendingPlan(
        fee = fee,
        expectedAmount = DEPOSIT_AMOUNT,
        rows = emptyList(),
        withdrawMode = null,
        action = Deposit(),
        operation = LendingPlannedOperation.Deposit,
        amount = DEPOSIT_AMOUNT,
        txs = txs,
        planKey = planKey,
        isWithUpgrade = isWithUpgrade,
    )

    private fun withdrawPlan(amount: BigDecimal = WITHDRAW_AMOUNT, isEntire: Boolean = false) = LendingPlan(
        fee = selectedFee(WITHDRAW_GAS),
        expectedAmount = amount,
        rows = emptyList(),
        withdrawMode = WithdrawMode.Instant,
        action = Withdraw(),
        operation = LendingPlannedOperation.Withdraw(isEntire = isEntire),
        amount = amount,
        txs = listOf(estimatedWithdraw),
        planKey = PLAN_KEY,
        isWithUpgrade = false,
    )

    private val updateRequiredText = combinedReference(
        stringReference("Update Tangem to the latest version to use Lending on "),
        stringReference("Fake"),
        stringReference("."),
    )

    private fun selection(gasLimit: BigInteger) = EarnFeeSelection(fee = selectedFee(gasLimit), extended = null)

    private fun selectedFee(gasLimit: BigInteger): Fee = ethFee(gasLimit, maxFeePerGas = SELECTED_GAS_PRICE)
        .fixFee(eth, gasLimit)

    private fun analytics(actionId: String) = EarnTxAnalytics(params = mapOf("action" to actionId))

    private fun txRef(id: String, actionId: String, createdAt: Instant = Instant.DISTANT_PAST) = EarnTxRef(
        version = 1,
        productId = EarnProductId("lending"),
        id = id,
        userWalletId = userWalletId,
        currencyId = eth.id,
        createdAt = createdAt,
        analytics = analytics(actionId),
    )

    private fun depositResult() = EarnResult(
        title = stringReference("Deposited ${DEPOSIT_AMOUNT.format { crypto(eth) }}"),
        subtitle = combinedReference(stringReference("Your ${eth.symbol} is now earning in "), provider.info.title),
        chip = null,
        rows = emptyList(),
        primary = EarnResultButton(stringReference("Go to yield"), EarnFlowExit.ToYield),
        secondary = EarnResultButton(resourceReference(R.string.common_go_to_token), EarnFlowExit.ToToken),
    )

    private fun withdrawResult(fee: Fee) = EarnResult(
        title = stringReference("Withdrawn ${WITHDRAW_AMOUNT.format { crypto(eth) }}"),
        subtitle = null,
        chip = null,
        rows = listOf(
            EarnRow(
                label = stringReference("Withdrawal period"),
                value = EarnValue.Text(stringReference("Usually instant")),
            ),
            EarnRow(
                label = resourceReference(R.string.common_network_fee_title),
                value = EarnValue.Text(stringReference(fee.amount.value.format { crypto(eth).fee() })),
            ),
        ),
        primary = EarnResultButton(resourceReference(R.string.common_done), EarnFlowExit.Done),
        secondary = null,
        asset = EarnResultAsset.Status(TangemStatusIcon.Kind.Checkmark),
        isExplorerShown = true,
    )

    data class MaxAmountModel(
        val action: EarnFlowAction,
        val assets: Map<CryptoCurrency.ID, LendingAssetState>,
        val expected: Either<EarnSessionError, BigDecimal>,
    )

    data class PrepareModel(
        val built: List<TransactionData.Uncompiled>,
        val estimated: List<TransactionData.Uncompiled>,
    )

    data class BuildErrorModel(val error: LendingTxError, val expected: EarnSessionError)

    data class RebuiltModel(val name: String, val rebuilt: List<TransactionData.Uncompiled>) {
        override fun toString(): String = name
    }

    data class FreshEstimateModel(
        val name: String,
        val selectedGas: BigInteger,
        val preparationGas: BigInteger,
        val depositGas: BigInteger,
        val isChanged: Boolean,
    ) {
        override fun toString(): String = name
    }

    data class WithdrawVerifyModel(val amount: BigDecimal, val position: BigDecimal, val isEntire: Boolean)

    private data class TestProviderState(
        override val planKey: String,
        override val isUpgradeRequired: Boolean = false,
    ) : LendingProviderState

    private object UnknownAction : EarnFlowAction {
        override val id: String = "unknown"
        override val label: TextReference = stringReference("Unknown")
        override val input: EarnActionInput = EarnActionInput.None
        override val isGaslessAllowed: Boolean = false
        override fun title(productTitle: TextReference): TextReference = productTitle
    }

    private companion object {
        const val WETH = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"
        const val OTHER = "0x0000000000000000000000000000000000000bad"
        const val PLAN_KEY = "plan-1"
        const val NEW_PLAN_KEY = "plan-2"
        const val PREPARATION_HASH = "0xpreparation"
        const val DEPOSIT_HASH = "0xdeposit"
        const val WITHDRAW_HASH = "0xwithdraw"
        const val POLL_INTERVAL_MS = 10_000L
        const val MAX_TRACK_ATTEMPTS = 15
        const val SELECTED_GAS_PRICE = 10L

        val BALANCE = BigDecimal("2")
        val DEPOSIT_AMOUNT = BigDecimal("1.5")
        val POSITION = BigDecimal("1.2")
        val MAX_WITHDRAW = BigDecimal("1")
        val WITHDRAW_AMOUNT = BigDecimal("0.5")
        val PREPARATION_GAS: BigInteger = BigInteger.valueOf(100_000)
        val DEPOSIT_GAS: BigInteger = BigInteger.valueOf(200_000)
        val WITHDRAW_GAS: BigInteger = BigInteger.valueOf(150_000)
        val TOTAL_GAS: BigInteger = PREPARATION_GAS + DEPOSIT_GAS
        val SELECTED_GAS: BigInteger = BigInteger.valueOf(330_000)
        val OTHER_CALL: SmartContractCallData = YieldSupplyContractCallDataProviderFactory.getExitNativeCallData()

        fun tx(destination: String, value: BigDecimal, fee: Fee?) =
            createTx(destination = destination, value = value, fee = fee)

        fun TransactionData.Uncompiled.sentTo(destination: String) = copy(destinationAddress = destination)

        fun TransactionData.Uncompiled.withValue(value: BigDecimal) =
            copy(amount = Amount(value = value, blockchain = Blockchain.Ethereum))

        fun TransactionData.Uncompiled.withCallData(callData: SmartContractCallData) =
            copy(extras = EthereumTransactionExtras(callData = callData))
    }
}