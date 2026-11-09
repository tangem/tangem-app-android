package com.tangem.features.earn.impl.fake

import arrow.core.Either
import arrow.core.getOrElse
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.features.earn.product.api.analytics.EarnTxAnalytics
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.EarnActionInput
import com.tangem.features.earn.product.api.session.EarnAmount
import com.tangem.features.earn.product.api.session.EarnFeeSelection
import com.tangem.features.earn.product.api.session.EarnFlowAction
import com.tangem.features.earn.product.api.session.EarnFlowExit
import com.tangem.features.earn.product.api.session.EarnPlan
import com.tangem.features.earn.product.api.session.EarnResult
import com.tangem.features.earn.product.api.session.EarnResultButton
import com.tangem.features.earn.product.api.session.EarnSession
import com.tangem.features.earn.product.api.session.EarnSessionError
import com.tangem.features.earn.product.api.session.EarnSigningStep
import com.tangem.features.earn.product.api.session.EarnStatusDetail
import com.tangem.features.earn.product.api.session.EarnTxRef
import com.tangem.features.earn.product.api.session.EarnTxStatus
import com.tangem.features.earn.product.api.session.EarnVerification
import com.tangem.features.earn.product.api.session.VerifiedPlan
import com.tangem.features.earn.product.api.session.Withdraw
import com.tangem.features.earn.product.api.session.WithdrawMode
import com.tangem.test.core.ProvideTestModels
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FakeEarnSessionTest {

    private val fee = ethFee("0.00033")
    private val grownFee = ethFee("0.00066")
    private val feeSelection = EarnFeeSelection(fee = fee, extended = null)
    private val amount = EarnAmount.Exact(BigDecimal("0.5"))

    private val sessions = FakeEarnSessionPreset.entries.associateWith { createFakeEarnSession(it) }
    private val lending = sessions.getValue(FakeEarnSessionPreset.Lending)
    private val vault = sessions.getValue(FakeEarnSessionPreset.Vault)

    @Test
    fun `GIVEN lending session WHEN deposit flow runs from fee to execute THEN steps and result match the deposit`() =
        runTest {
            // Arrange
            val action = Deposit()

            // Act
            val loadedFee = lending.loadFee(action, amount)
            val maxAmount = lending.maxAmount(action)
            val plan = lending.prepareOrFail(action, amount)
            val steps = lending.execute(lending.verifyOrFail(plan)).toList()

            // Assert
            val done = steps.filterIsInstance<EarnSigningStep.Done>().single()
            val expectedDone = EarnSigningStep.Done(
                ref = EarnTxRef(
                    version = 1,
                    productId = EarnProductId("lending"),
                    id = "fake-deposit",
                    userWalletId = UserWalletId("0123456789abcdef"),
                    currencyId = CryptoCurrency.ID.fromValue("coin⟨ETH⟩ethereum"),
                    createdAt = Instant.parse("2030-01-01T00:00:00Z"),
                    analytics = EarnTxAnalytics(mapOf("action" to "deposit")),
                ),
                result = EarnResult(
                    title = stringReference("Deposited 0.5 ETH"),
                    subtitle = stringReference("Your ETH in Main account is now earning in Aave"),
                    chip = stringReference("Current 5.5% APY"),
                    rows = emptyList(),
                    primary = EarnResultButton(stringReference("Go to yield"), EarnFlowExit.ToYield),
                    secondary = EarnResultButton(stringReference("Go to token"), EarnFlowExit.ToToken),
                ),
            )
            assertThat(loadedFee).isEqualTo(Either.Right(TransactionFee.Single(normal = fee)))
            assertThat(maxAmount).isEqualTo(Either.Right(BigDecimal("2.01")))
            assertThat(steps).containsExactly(
                EarnSigningStep.AwaitingConfirmation(
                    stepLabel = null,
                    tx = EarnTxAnalytics(mapOf("action" to "deposit")),
                ),
                EarnSigningStep.Sent(done.ref),
                done,
            ).inOrder()
            assertThat(done).isEqualTo(expectedDone)
        }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MaxAmount {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN action WHEN maxAmount THEN deposit gives wallet balance and withdraw gives position`(
            model: MaxAmountModel,
        ) = runTest {
            // Arrange
            val session = sessions.getValue(model.preset)

            // Act
            val actual = session.maxAmount(model.action)

            // Assert
            assertThat(actual).isEqualTo(Either.Right(model.expected))
        }

        private fun provideTestModels() = FakeEarnSessionPreset.entries.flatMap { preset ->
            listOf(
                MaxAmountModel(preset = preset, action = Deposit(), expected = BigDecimal("2.01")),
                MaxAmountModel(preset = preset, action = Withdraw(), expected = BigDecimal("0.5")),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Prepare {

        private val selectedFee = EarnFeeSelection(fee = ethFee("0.0012"), extended = null)

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN action and amount WHEN prepare THEN plan holds the resolved amount and the selected fee`(
            model: PrepareModel,
        ) = runTest {
            // Arrange
            val session = sessions.getValue(model.preset)

            // Act
            val actual = session.prepare(model.action, model.amount, selectedFee)

            // Assert
            assertThat(actual.map { it.expectedAmount to it.fee })
                .isEqualTo(Either.Right(model.expected to selectedFee.fee))
        }

        @Test
        fun `GIVEN vault session WHEN prepare withdraw THEN plan is queued with quote ttl`() = runTest {
            // Act
            val plan = vault.prepareOrFail(Withdraw(), amount)

            // Assert
            val mode = plan.withdrawMode
            assertThat(mode).isInstanceOf(WithdrawMode.Queued::class.java)
            assertThat((mode as WithdrawMode.Queued).expectedBy).isNotNull()
            assertThat(plan.quoteTtl).isEqualTo(60.seconds)
        }

        @Test
        fun `GIVEN lending session WHEN prepare withdraw THEN plan is instant without quote ttl`() = runTest {
            // Act
            val actual = lending.prepare(Withdraw(), amount, feeSelection)

            // Assert
            val expected: Pair<WithdrawMode?, Duration?> = WithdrawMode.Instant to null
            assertThat(actual.map { it.withdrawMode to it.quoteTtl }).isEqualTo(Either.Right(expected))
        }

        @Test
        fun `GIVEN claim action WHEN prepare with exact amount THEN failed error is returned`() = runTest {
            // Act
            val actual = lending.prepare(fakeClaimAction(), amount, feeSelection)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(EarnSessionError.Failed()))
        }

        private fun provideTestModels() = listOf(
            PrepareModel(FakeEarnSessionPreset.Lending, Deposit(), amount, BigDecimal("0.5")),
            PrepareModel(
                preset = FakeEarnSessionPreset.Lending,
                action = Deposit(),
                amount = EarnAmount.Exact(BigDecimal("1.25")),
                expected = BigDecimal("1.25"),
            ),
            PrepareModel(FakeEarnSessionPreset.Lending, Deposit(), EarnAmount.Entire, BigDecimal("2.01")),
            PrepareModel(FakeEarnSessionPreset.Vault, Deposit(), amount, BigDecimal("0.5")),
            PrepareModel(FakeEarnSessionPreset.Vault, Deposit(), EarnAmount.Entire, BigDecimal("2.01")),
            PrepareModel(FakeEarnSessionPreset.Lending, Withdraw(), EarnAmount.Entire, BigDecimal("0.5")),
            PrepareModel(FakeEarnSessionPreset.Vault, Withdraw(), EarnAmount.Entire, BigDecimal("0.5")),
            PrepareModel(
                preset = FakeEarnSessionPreset.Lending,
                action = Withdraw(),
                amount = EarnAmount.Exact(BigDecimal("0.3")),
                expected = BigDecimal("0.3"),
            ),
            PrepareModel(
                preset = FakeEarnSessionPreset.Vault,
                action = Withdraw(),
                amount = EarnAmount.Exact(BigDecimal("0.3")),
                expected = BigDecimal("0.3"),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Verify {

        @Test
        fun `GIVEN fee growing on verify WHEN verify twice THEN first is Changed with grown fee and second is Verified`() =
            runTest {
                // Arrange
                val session = createFakeEarnSession(
                    preset = FakeEarnSessionPreset.Lending,
                    behavior = FakeEarnSessionBehavior(isFeeGrowingOnVerify = true),
                )
                val plan = session.prepareOrFail(Deposit(), amount)

                // Act
                val first = session.verify(plan)
                val changedPlan = (first as EarnVerification.Changed).newPlan
                val second = session.verify(changedPlan)

                // Assert
                assertThat(changedPlan.fee).isEqualTo(grownFee)
                assertThat(second).isInstanceOf(EarnVerification.Verified::class.java)
                assertThat((second as EarnVerification.Verified).plan.plan).isEqualTo(changedPlan)
            }

        @Test
        fun `GIVEN plan not made by the session WHEN verify THEN rejected with failed error`() = runTest {
            // Act
            val actual = lending.verify(ForeignPlan(fee))

            // Assert
            assertThat(actual).isEqualTo(EarnVerification.Rejected(EarnSessionError.Failed()))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Execute {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN execute failing WHEN execute THEN steps are AwaitingConfirmation then Failed network error`(
            model: ExecuteFailingModel,
        ) = runTest {
            // Arrange
            val session = createFakeEarnSession(
                preset = model.preset,
                behavior = FakeEarnSessionBehavior(isExecuteFailing = true),
            )
            val verified = session.verifyOrFail(session.prepareOrFail(model.action, model.amount))

            // Act
            val actual = session.execute(verified).toList()

            // Assert
            assertThat(actual).containsExactly(
                EarnSigningStep.AwaitingConfirmation(
                    stepLabel = null,
                    tx = EarnTxAnalytics(mapOf("action" to model.action.id)),
                ),
                EarnSigningStep.Failed(SendTransactionError.NetworkError(message = "fake", code = null)),
            ).inOrder()
        }

        @Test
        fun `GIVEN verified plan not made by the session WHEN execute THEN only Failed unknown error is emitted`() =
            runTest {
                // Arrange
                val foreign = ForeignVerifiedPlan(ForeignPlan(fee))

                // Act
                val actual = lending.execute(foreign).toList()

                // Assert
                assertThat(actual).containsExactly(EarnSigningStep.Failed(SendTransactionError.UnknownError()))
            }

        private fun provideTestModels() = listOf(
            ExecuteFailingModel(FakeEarnSessionPreset.Lending, Deposit(), amount),
            ExecuteFailingModel(FakeEarnSessionPreset.Vault, Withdraw(), amount),
            ExecuteFailingModel(FakeEarnSessionPreset.Lending, fakeClaimAction(), amount = null),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ExecuteResult {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN exact amount differing from position WHEN prepare and execute THEN plan and result follow amount`(
            model: ExecuteResultModel,
        ) = runTest {
            // Arrange
            val session = sessions.getValue(model.preset)
            val plan = session.prepareOrFail(model.action, EarnAmount.Exact(model.amount))

            // Act
            val steps = session.execute(session.verifyOrFail(plan)).toList()

            // Assert
            val done = steps.filterIsInstance<EarnSigningStep.Done>().single()
            assertThat(plan.expectedAmount to done.result.title)
                .isEqualTo(model.amount to stringReference(model.expectedTitle))
        }

        private fun provideTestModels() = listOf(
            ExecuteResultModel(FakeEarnSessionPreset.Lending, Deposit(), BigDecimal("0.7"), "Deposited 0.7 ETH"),
            ExecuteResultModel(FakeEarnSessionPreset.Vault, Deposit(), BigDecimal("0.7"), "Deposited 0.7 ETH"),
            ExecuteResultModel(FakeEarnSessionPreset.Lending, Withdraw(), BigDecimal("0.3"), "Withdrawn 0.3 ETH"),
            ExecuteResultModel(FakeEarnSessionPreset.Vault, Withdraw(), BigDecimal("0.3"), "Withdrawal requested"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Track {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN sent ref WHEN track THEN Unconfirmed then Confirmed`(model: TrackModel) = runTest {
            // Arrange
            val session = sessions.getValue(model.preset)
            val ref = session.executeToRef(session.prepareOrFail(model.action, amount))

            // Act
            val actual = session.track(ref).toList()

            // Assert
            assertThat(actual).containsExactly(
                EarnTxStatus(TxInfo.TransactionStatus.Unconfirmed),
                EarnTxStatus(TxInfo.TransactionStatus.Confirmed),
            ).inOrder()
        }

        @Test
        fun `GIVEN vault withdraw ref WHEN track THEN Unconfirmed then settlement awaited then Received and flow completes`() =
            runTest {
                // Arrange
                val plan = vault.prepareOrFail(Withdraw(), amount)
                val expectedBy = (plan.withdrawMode as WithdrawMode.Queued).expectedBy
                val ref = vault.executeToRef(plan)

                // Act
                val actual = vault.track(ref).toList()

                // Assert
                assertThat(expectedBy).isNotNull()
                assertThat(ref.id).isEqualTo("fake-withdraw")
                assertThat(actual).containsExactly(
                    EarnTxStatus(TxInfo.TransactionStatus.Unconfirmed),
                    EarnTxStatus(TxInfo.TransactionStatus.Confirmed, EarnStatusDetail.AwaitingSettlement(expectedBy)),
                    EarnTxStatus(TxInfo.TransactionStatus.Confirmed, EarnStatusDetail.Received(BigDecimal("0.5"))),
                ).inOrder()
            }

        private fun provideTestModels() = listOf(
            TrackModel(FakeEarnSessionPreset.Lending, Deposit()),
            TrackModel(FakeEarnSessionPreset.Lending, Withdraw()),
            TrackModel(FakeEarnSessionPreset.Vault, Deposit()),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Claim {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN claim action without amount WHEN fee prepare verify execute THEN plan has no amount and Claimed`(
            preset: FakeEarnSessionPreset,
        ) = runTest {
            // Arrange
            val session = sessions.getValue(preset)
            val action = fakeClaimAction()

            // Act
            val loadedFee = session.loadFee(action, amount = null)
            val plan = session.prepareOrFail(action, amount = null)
            val steps = session.execute(session.verifyOrFail(plan)).toList()

            // Assert
            val done = steps.filterIsInstance<EarnSigningStep.Done>().single()
            assertThat(loadedFee).isEqualTo(Either.Right(TransactionFee.Single(normal = fee)))
            assertThat(plan.expectedAmount).isNull()
            assertThat(steps).containsExactly(
                EarnSigningStep.AwaitingConfirmation(
                    stepLabel = null,
                    tx = EarnTxAnalytics(mapOf("action" to "claim")),
                ),
                EarnSigningStep.Sent(done.ref),
                done,
            ).inOrder()
            assertThat(done.result.title).isEqualTo(stringReference("Claimed"))
        }

        private fun provideTestModels() = FakeEarnSessionPreset.entries.toList()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class LoadFeeExtended {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN any action WHEN loadFeeExtended THEN unknown fee error because gasless is never offered`(
            model: LoadFeeExtendedModel,
        ) = runTest {
            // Arrange
            val session = sessions.getValue(model.preset)

            // Act
            val actual = session.loadFeeExtended(model.action, amount, feeCurrency = null)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(GetFeeError.UnknownError))
        }

        private fun provideTestModels() = FakeEarnSessionPreset.entries.flatMap { preset ->
            listOf(
                LoadFeeExtendedModel(preset, Deposit()),
                LoadFeeExtendedModel(preset, Withdraw()),
                LoadFeeExtendedModel(preset, fakeClaimAction()),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UnsupportedAction {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN action the session cannot build WHEN operation called THEN left error is returned`(
            model: UnsupportedModel,
        ) = runTest {
            // Arrange
            val session = sessions.getValue(model.preset)

            // Act
            val actual = model.call(session)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = FakeEarnSessionPreset.entries.flatMap { preset ->
            listOf(
                UnsupportedModel(
                    preset = preset,
                    operation = "prepare",
                    call = { it.prepare(UnknownAction, amount, feeSelection) },
                    expected = Either.Left(EarnSessionError.Failed()),
                ),
                UnsupportedModel(
                    preset = preset,
                    operation = "maxAmount",
                    call = { it.maxAmount(UnknownAction) },
                    expected = Either.Left(EarnSessionError.Failed()),
                ),
                UnsupportedModel(
                    preset = preset,
                    operation = "loadFee",
                    call = { it.loadFee(UnknownAction, amount) },
                    expected = Either.Left(GetFeeError.UnknownError),
                ),
                UnsupportedModel(
                    preset = preset,
                    operation = "loadFeeExtended",
                    call = { it.loadFeeExtended(UnknownAction, amount, feeCurrency = null) },
                    expected = Either.Left(GetFeeError.UnknownError),
                ),
            )
        }
    }

    private suspend fun EarnSession.prepareOrFail(action: EarnFlowAction, amount: EarnAmount?): EarnPlan =
        prepare(action, amount, feeSelection).getOrElse { error("Expected a plan but prepare failed with $it") }

    private suspend fun EarnSession.verifyOrFail(plan: EarnPlan): VerifiedPlan {
        val verification = verify(plan)
        return (verification as? EarnVerification.Verified)?.plan ?: error("Expected Verified but was $verification")
    }

    private suspend fun EarnSession.executeToRef(plan: EarnPlan): EarnTxRef =
        execute(verifyOrFail(plan)).toList().filterIsInstance<EarnSigningStep.Done>().single().ref

    internal data class MaxAmountModel(
        val preset: FakeEarnSessionPreset,
        val action: EarnFlowAction,
        val expected: BigDecimal,
    ) {
        override fun toString(): String = "$preset ${action.id}"
    }

    internal data class PrepareModel(
        val preset: FakeEarnSessionPreset,
        val action: EarnFlowAction,
        val amount: EarnAmount,
        val expected: BigDecimal,
    ) {
        override fun toString(): String = "$preset ${action.id} $amount"
    }

    internal data class ExecuteFailingModel(
        val preset: FakeEarnSessionPreset,
        val action: EarnFlowAction,
        val amount: EarnAmount?,
    ) {
        override fun toString(): String = "$preset ${action.id}"
    }

    internal data class ExecuteResultModel(
        val preset: FakeEarnSessionPreset,
        val action: EarnFlowAction,
        val amount: BigDecimal,
        val expectedTitle: String,
    ) {
        override fun toString(): String = "$preset ${action.id} $amount"
    }

    internal data class LoadFeeExtendedModel(val preset: FakeEarnSessionPreset, val action: EarnFlowAction) {
        override fun toString(): String = "$preset ${action.id}"
    }

    internal data class TrackModel(val preset: FakeEarnSessionPreset, val action: EarnFlowAction) {
        override fun toString(): String = "$preset ${action.id}"
    }

    internal data class UnsupportedModel(
        val preset: FakeEarnSessionPreset,
        val operation: String,
        val call: suspend (EarnSession) -> Either<Any, Any>,
        val expected: Either<Any, Any>,
    ) {
        override fun toString(): String = "$preset $operation"
    }
}

private fun ethFee(value: String): Fee =
    Fee.Common(Amount(currencySymbol = "ETH", value = BigDecimal(value), decimals = 18))

private object UnknownAction : EarnFlowAction {

    override val id: String = "unknown"

    override val label: TextReference = stringReference("Unknown")

    override val input: EarnActionInput = EarnActionInput.None

    override val isGaslessAllowed: Boolean = false

    override fun title(productTitle: TextReference): TextReference = label
}

private class ForeignPlan(override val fee: Fee) : EarnPlan {

    override val expectedAmount: BigDecimal? = null

    override val rows: List<EarnRow> = emptyList()

    override val withdrawMode: WithdrawMode? = null

    override val quoteTtl: Duration? = null

    override val notice: TextReference? = null

    override val isEstimate: Boolean = false
}

private class ForeignVerifiedPlan(override val plan: EarnPlan) : VerifiedPlan