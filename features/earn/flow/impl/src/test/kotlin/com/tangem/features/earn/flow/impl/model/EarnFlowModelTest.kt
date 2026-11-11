package com.tangem.features.earn.flow.impl.model

import arrow.core.left
import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.alerts.TransactionErrorDialogFactory
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.tokens.IsAmountSubtractAvailableUseCase
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.features.earn.flow.api.EarnFlowComponent
import com.tangem.features.earn.flow.impl.EarnSessionFactories
import com.tangem.features.earn.product.api.analytics.EarnTxAnalytics
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.EarnAmount
import com.tangem.features.earn.product.api.session.EarnFeeSelection
import com.tangem.features.earn.product.api.session.EarnFlowAction
import com.tangem.features.earn.product.api.session.EarnFlowExit
import com.tangem.features.earn.product.api.session.EarnSession
import com.tangem.features.earn.product.api.session.EarnSessionError
import com.tangem.features.earn.product.api.session.EarnSigningStep
import com.tangem.features.earn.product.api.session.EarnTxRef
import com.tangem.features.earn.product.api.session.EarnTxStatus
import com.tangem.features.earn.product.api.session.EarnVerification
import com.tangem.features.earn.product.api.session.Withdraw
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

internal class EarnFlowModelTest {

    private val originalLocale = Locale.getDefault()

    private val session: EarnSession = mockk()
    private val accountStatusList: AccountStatusList = mockk()
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase = mockk()
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase = mockk()
    private val feeSelectorReloadTrigger: FeeSelectorReloadTrigger = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val callback: EarnFlowComponent.ModelCallback = mockk(relaxed = true)
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase = mockk(relaxed = true)
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase = mockk()
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk(relaxed = true)

    private val txRef = EarnTxRef(
        version = 1,
        productId = testFlowProduct.productId,
        id = "tx",
        userWalletId = testUserWalletId,
        currencyId = testEthereum.id,
        createdAt = Instant.parse("2030-01-01T00:00:00Z"),
        analytics = EarnTxAnalytics(),
    )

    @BeforeEach
    fun setUp() {
        // The amount field reads the typed "0.5" with the default locale's decimal separator
        Locale.setDefault(Locale.US)
        clearMocks(
            session,
            accountStatusList,
            singleAccountStatusListSupplier,
            getSelectedAppCurrencyUseCase,
            isAmountSubtractAvailableUseCase,
            getFeePaidCryptoCurrencyStatusSyncUseCase,
            feeSelectorReloadTrigger,
            messageSender,
            callback,
            saveBlockchainErrorUseCase,
            getWalletMetaInfoUseCase,
            sendFeedbackEmailUseCase,
        )
        mockkObject(CryptoCurrencyStatusOperations)
        with(CryptoCurrencyStatusOperations) {
            every { accountStatusList.getCryptoCurrencyStatus(currency = testEthereum) } returns testStatus().some()
        }
        every { singleAccountStatusListSupplier.invoke(testUserWalletId) } returns flowOf(accountStatusList)
        every { getSelectedAppCurrencyUseCase.invoke() } returns flowOf(AppCurrency.Default.right())
        coEvery {
            isAmountSubtractAvailableUseCase.invoke(userWalletId = any(), currency = any(), maybeGaslessFee = any())
        } returns true.right()
        coEvery {
            getFeePaidCryptoCurrencyStatusSyncUseCase.invoke(userWalletId = any(), cryptoCurrencyStatus = any())
        } returns testStatus().right()

        coEvery { session.maxAmount(action = any()) } returns TEST_BALANCE.right()
        coEvery { session.prepare(action = any(), amount = any(), fee = any()) } returns TestPlan().right()
        coEvery { session.verify(plan = any()) } returns EarnVerification.Verified(TestVerifiedPlan(TestPlan()))
        every { session.execute(plan = any()) } returns flowOf(
            EarnSigningStep.AwaitingConfirmation(stepLabel = null, tx = EarnTxAnalytics()),
            EarnSigningStep.Sent(txRef),
            EarnSigningStep.Done(txRef, testResult()),
        )
        every { session.track(ref = any()) } returns flowOf(EarnTxStatus(TxInfo.TransactionStatus.Confirmed))
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(originalLocale)
        unmockkObject(CryptoCurrencyStatusOperations)
    }

    @Nested
    inner class Creation {

        @Test
        fun `GIVEN product factory in map WHEN flow created THEN session is created by it for the product params`() =
            runTest {
                // Arrange
                val createdFor = slot<EarnProduct.Params>()
                val factory = EarnSession.Factory { params -> session.also { createdFor.captured = params } }

                // Act
                val model = createModel(factories = mapOf(testFlowProduct.productId.value to factory))
                advanceUntilIdle()

                // Assert
                assertThat(createdFor.captured).isEqualTo(EarnProduct.Params(testUserWalletId, testEthereum))
                assertThat(model.action).isEqualTo(Deposit())
                model.onDestroy()
            }

        @Test
        fun `GIVEN product missing in map WHEN flow created THEN flow closes with Done without loading`() = runTest {
            // Act
            val model = createModel(factories = emptyMap())
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { callback.onFinished(EarnFlowExit.Done) }
            verify(exactly = 0) { singleAccountStatusListSupplier.invoke(testUserWalletId) }
            coVerify(exactly = 0) { session.maxAmount(action = any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN action with amount WHEN flow created THEN amount step and max amount loaded`() = runTest {
            // Act
            val model = createModel(action = Deposit())
            advanceUntilIdle()

            // Assert
            assertThat(model.initialStep).isEqualTo(EarnFlowStep.Amount)
            assertThat(model.state.value.maxAmount).isEqualTo(TEST_BALANCE)
            assertThat(model.amount.value?.amountState).isInstanceOf(AmountState.Data::class.java)
            model.onDestroy()
        }

        @Test
        fun `GIVEN max amount not loaded WHEN amount screen THEN card is empty and Max does nothing`() = runTest {
            // Arrange
            coEvery { session.maxAmount(action = any()) } returns EarnSessionError.Failed().left()

            // Act
            val model = createModel(action = Deposit())
            advanceUntilIdle()
            model.onMaxValueClick()

            // Assert
            assertThat(model.state.value.amountState).isEqualTo(AmountState.Empty)
            verify(exactly = 1) { messageSender.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN max amount failed WHEN retry in dialog THEN max amount requested again`() = runTest {
            // Arrange
            coEvery { session.maxAmount(action = any()) } returns EarnSessionError.Failed().left()
            val dialog = slot<UiMessage>()
            every { messageSender.send(capture(dialog)) } returns Unit
            val model = createModel(action = Deposit())
            advanceUntilIdle()

            // Act
            (dialog.captured as DialogMessage).firstAction.onClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 2) { session.maxAmount(action = Deposit()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN session minimum above entered amount WHEN submit THEN field shows error and no dialog`() = runTest {
            // Arrange
            coEvery {
                session.prepare(action = any(), amount = any(), fee = any())
            } returns EarnSessionError.BelowMinimum(BigDecimal("1")).left()
            val model = createModel(action = Deposit())
            advanceUntilIdle()
            model.onAmountValueChange("0.5")
            model.onFeeResult(testFeeContent("0.00033"))
            advanceUntilIdle()

            // Act
            model.onSubmit()
            advanceUntilIdle()

            // Assert
            val field = (model.state.value.amountState as AmountState.Data).amountTextField
            assertThat(field.isError).isTrue()
            assertThat(model.amount.value?.primary?.isEnabled).isFalse()
            assertThat(model.state.value.step).isEqualTo(EarnFlowStep.Amount)
            verify(exactly = 0) { messageSender.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN fiat input WHEN session reports minimum THEN entered fiat value is kept`() = runTest {
            // Arrange
            coEvery {
                session.prepare(action = any(), amount = any(), fee = any())
            } returns EarnSessionError.BelowMinimum(BigDecimal("1")).left()
            val model = createModel(action = Deposit())
            advanceUntilIdle()
            model.onCurrencyChangeClick(isFiat = true)
            model.onAmountValueChange("2000")
            model.onFeeResult(testFeeContent("0.00033"))
            advanceUntilIdle()

            // Act
            model.onSubmit()
            advanceUntilIdle()

            // Assert
            val field = (model.state.value.amountState as AmountState.Data).amountTextField
            assertThat(field.fiatValue).isEqualTo("2000")
            assertThat(field.cryptoAmount.value).isEqualTo(BigDecimal("0.5"))
            assertThat(field.isError).isTrue()
            model.onDestroy()
        }

        @Test
        fun `GIVEN entered amount passes minimum but deducted does not WHEN submit THEN dialog shown`() = runTest {
            // Arrange
            coEvery {
                session.prepare(action = any(), amount = any(), fee = any())
            } returns EarnSessionError.BelowMinimum(BigDecimal("0.5")).left()
            val model = createModel(action = Deposit())
            advanceUntilIdle()
            model.onAmountValueChange("0.5")
            model.onFeeResult(testFeeContent("0.00033"))
            advanceUntilIdle()

            // Act
            model.onSubmit()
            advanceUntilIdle()

            // Assert
            val field = (model.state.value.amountState as AmountState.Data).amountTextField
            assertThat(field.isError).isFalse()
            verify(exactly = 1) { messageSender.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN max with more decimals than the currency WHEN 25 percent THEN share is cut to currency decimals`() =
            runTest {
                // Arrange
                coEvery { session.maxAmount(action = any()) } returns BigDecimal("1.000000000000000001").right()
                val model = createModel(action = Deposit())
                advanceUntilIdle()

                // Act
                model.onPercentClick(25)

                // Assert
                assertThat(model.state.value.enteredAmount).isEqualTo(BigDecimal("0.250000000000000000"))
                model.onDestroy()
            }
    }

    @Nested
    inner class ActionWithoutAmount {

        @Test
        fun `GIVEN action without amount WHEN first fee arrives THEN confirm step with prepare without amount`() =
            runTest {
                // Arrange
                val model = createModel(action = TestClaim)

                // Act
                model.onFeeResult(testFeeContent("0.00033"))
                advanceUntilIdle()

                // Assert
                assertThat(model.initialStep).isEqualTo(EarnFlowStep.Confirm)
                coVerify(exactly = 1) {
                    session.prepare(action = TestClaim, amount = null, fee = EarnFeeSelection(testFee("0.00033"), null))
                }
                coVerify(exactly = 0) { session.maxAmount(action = any()) }
                assertThat(model.confirm.value).isNotNull()
                model.onDestroy()
            }

        @Test
        fun `GIVEN action without amount and fee error WHEN confirm screen THEN it shows the refresh card`() = runTest {
            // Arrange
            val model = createModel(action = TestClaim)
            advanceUntilIdle()

            // Act
            model.onFeeResult(FeeSelectorUM.Error(GetFeeError.UnknownError))
            advanceUntilIdle()

            // Assert
            assertThat(model.confirm.value?.notifications?.single())
                .isInstanceOf(NotificationUM.Warning.NetworkFeeUnreachable::class.java)
            model.onDestroy()
        }

        @Test
        fun `GIVEN prepare failed for action without amount WHEN confirm pressed THEN prepare retried`() = runTest {
            // Arrange
            coEvery {
                session.prepare(action = any(), amount = any(), fee = any())
            } returns EarnSessionError.Failed().left() andThen TestPlan().right()
            val model = createModel(action = TestClaim)
            model.onFeeResult(testFeeContent("0.00033"))
            advanceUntilIdle()

            // Act
            model.onConfirmClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 2) { session.prepare(action = TestClaim, amount = null, fee = any()) }
            coVerify(exactly = 0) { session.verify(plan = any()) }
            assertThat(model.state.value.plan).isEqualTo(TestPlan())
            model.onDestroy()
        }

        @Test
        fun `GIVEN action without amount WHEN back on confirm THEN flow closes with Done`() = runTest {
            // Arrange
            val model = createModel(action = TestClaim)

            // Act
            model.onBack()

            // Assert
            verify(exactly = 1) { callback.onFinished(EarnFlowExit.Done) }
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FeeDeduction {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN entered amount and fee WHEN submit THEN prepare gets the amount after deduction`(
            model: DeductionModel,
        ) = runTest {
            // Arrange
            val flow = createModel(action = Deposit())
            advanceUntilIdle()

            // Act
            if (model.entered == null) flow.onMaxValueClick() else flow.onAmountValueChange(model.entered)
            flow.onFeeResult(testFeeContent(model.fee))
            advanceUntilIdle()
            flow.onSubmit()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                session.prepare(
                    action = Deposit(),
                    amount = EarnAmount.Exact(BigDecimal(model.expected)),
                    fee = EarnFeeSelection(testFee(model.fee), null),
                )
            }
            assertThat(flow.state.value.enteredAmount).isEqualTo(BigDecimal(model.entered ?: "2.01"))
            flow.onDestroy()
        }

        private fun provideTestModels() = listOf(
            DeductionModel(entered = null, fee = "0.00033", expected = "2.00967"),
            DeductionModel(entered = "0.5", fee = "0.00033", expected = "0.5"),
            DeductionModel(entered = "2.00999", fee = "0.00033", expected = "2.00967"),
            DeductionModel(entered = null, fee = "0", expected = "2.01"),
        )

        @Test
        fun `GIVEN Max and fee not below balance WHEN amount screen THEN total exceeds balance and no prepare`() =
            runTest {
                // Arrange
                val model = createModel(action = Deposit())
                advanceUntilIdle()

                // Act
                model.onMaxValueClick()
                model.onFeeResult(testFeeContent("2.01"))
                advanceUntilIdle()
                model.onSubmit()
                advanceUntilIdle()

                // Assert
                val amountUM = model.amount.value
                assertThat(amountUM?.notifications).contains(NotificationUM.Error.TotalExceedsBalance)
                assertThat(amountUM?.primary?.isEnabled).isFalse()
                coVerify(exactly = 0) { session.prepare(action = any(), amount = any(), fee = any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN Max deposited with fee A WHEN fee B on confirm THEN deduction is recalculated from entered amount`() =
            runTest {
                // Arrange
                val model = createModel(action = Deposit())
                advanceUntilIdle()
                model.onMaxValueClick()
                model.onFeeResult(testFeeContent("0.00033"))
                advanceUntilIdle()
                model.onSubmit()
                advanceUntilIdle()

                // Act
                model.onFeeResult(testFeeContent("0.001"))
                advanceUntilIdle()

                // Assert
                coVerify(exactly = 1) {
                    session.prepare(
                        action = Deposit(),
                        amount = EarnAmount.Exact(BigDecimal("2.009")),
                        fee = EarnFeeSelection(testFee("0.001"), null),
                    )
                }
                assertThat(model.state.value.step).isEqualTo(EarnFlowStep.Confirm)
                model.onDestroy()
            }

        @Test
        fun `GIVEN amount subtraction unavailable WHEN submit THEN amount is not reduced`() = runTest {
            // Arrange
            coEvery {
                isAmountSubtractAvailableUseCase.invoke(userWalletId = any(), currency = any(), maybeGaslessFee = any())
            } returns false.right()
            val model = createModel(action = Deposit())
            advanceUntilIdle()

            // Act
            model.onAmountValueChange("2.00999")
            model.onFeeResult(testFeeContent("0.00033"))
            advanceUntilIdle()
            model.onSubmit()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                session.prepare(action = any(), amount = EarnAmount.Exact(BigDecimal("2.00999")), fee = any())
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN Max WHEN fee on confirm exceeds balance THEN confirm is blocked`() = runTest {
            // Arrange
            val model = createModel(action = Deposit())
            advanceUntilIdle()
            model.onAmountValueChange(TEST_BALANCE.toPlainString())
            model.onFeeResult(testFeeContent("0.00033"))
            advanceUntilIdle()
            model.onSubmit()
            advanceUntilIdle()

            // Act
            model.onFeeResult(testFeeContent("2.02"))
            advanceUntilIdle()

            // Assert
            val confirm = model.confirm.value
            assertThat(confirm?.notifications?.filterIsInstance<NotificationUM.Error.TotalExceedsBalance>()).hasSize(1)
            assertThat(confirm?.primary?.isEnabled).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN amount from position WHEN fee arrives THEN subtraction use case is not asked`() = runTest {
            // Arrange
            val model = createModel(action = Withdraw())
            advanceUntilIdle()

            // Act
            model.onAmountValueChange("0.5")
            model.onFeeResult(testFeeContent("0.00033"))
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 0) {
                isAmountSubtractAvailableUseCase.invoke(userWalletId = any(), currency = any(), maybeGaslessFee = any())
            }
            model.onDestroy()
        }
    }

    @Nested
    inner class Fee {

        @Test
        fun `GIVEN prepare in progress WHEN amount changed THEN old plan does not open confirm`() = runTest {
            // Arrange
            val prepareGate = CompletableDeferred<Unit>()
            coEvery { session.prepare(action = any(), amount = any(), fee = any()) } coAnswers {
                prepareGate.await()
                TestPlan().right()
            }
            val model = createModel(action = Deposit())
            advanceUntilIdle()
            model.onAmountValueChange("0.5")
            model.onFeeResult(testFeeContent("0.00033"))
            advanceUntilIdle()
            model.onSubmit()
            runCurrent()

            // Act
            model.onAmountValueChange("0.4")
            prepareGate.complete(Unit)
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.step).isEqualTo(EarnFlowStep.Amount)
            assertThat(model.state.value.isPreparing).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN entered amount WHEN selector loads fee THEN session gets the exact amount`() = runTest {
            // Arrange
            coEvery { session.loadFee(action = any(), amount = any()) } returns GetFeeError.UnknownError.left()
            val model = createModel(action = Deposit())
            advanceUntilIdle()
            model.onAmountValueChange("0.5")

            // Act
            model.loadFee()

            // Assert
            coVerify(exactly = 1) { session.loadFee(action = Deposit(), amount = EarnAmount.Exact(BigDecimal("0.5"))) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN new amount WHEN entered THEN fee block shows loading and reloads`() = runTest {
            // Arrange
            val model = createModel(action = Deposit())
            advanceUntilIdle()

            // Act
            model.onAmountValueChange("0.5")
            advanceUntilIdle()

            // Assert
            coVerifyOrder {
                feeSelectorReloadTrigger.triggerLoadingState()
                feeSelectorReloadTrigger.triggerUpdate(any())
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN fee loading or error WHEN submit THEN no prepare and primary disabled`() = runTest {
            // Arrange
            val model = createModel(action = Deposit())
            advanceUntilIdle()
            model.onAmountValueChange("0.5")

            // Act
            model.onFeeResult(FeeSelectorUM.Error(GetFeeError.UnknownError))
            advanceUntilIdle()
            model.onSubmit()
            advanceUntilIdle()

            // Assert
            assertThat(model.amount.value?.primary?.isEnabled).isFalse()
            assertThat(model.amount.value?.notifications?.single())
                .isInstanceOf(NotificationUM.Warning.NetworkFeeUnreachable::class.java)
            coVerify(exactly = 0) { session.prepare(action = any(), amount = any(), fee = any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN content with disabled primary WHEN fee result THEN no fee selection`() = runTest {
            // Arrange
            val model = createModel(action = Deposit())

            // Act
            model.onFeeResult(testFeeContent("0.00033", isPrimaryButtonEnabled = false))

            // Assert
            assertThat(model.state.value.feeSelection).isNull()
            model.onDestroy()
        }

        @Test
        fun `GIVEN fee error card WHEN refresh THEN fee block reloads`() = runTest {
            // Arrange
            val model = createModel(action = Deposit())
            advanceUntilIdle()
            model.onFeeResult(FeeSelectorUM.Error(GetFeeError.UnknownError))
            advanceUntilIdle()
            val refresh = model.amount.value?.notifications?.single() as NotificationUM.Warning.NetworkFeeUnreachable

            // Act
            refresh.onRefresh()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { feeSelectorReloadTrigger.triggerUpdate(any()) }
            model.onDestroy()
        }
    }

    @Nested
    inner class Confirmation {

        @Test
        fun `GIVEN verify returns Verified WHEN confirm THEN execute runs and result shown`() = runTest {
            // Arrange
            val model = createConfirmedModel()

            // Act
            model.onConfirmClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { session.execute(plan = TestVerifiedPlan(TestPlan())) }
            verify(exactly = 1) { session.track(ref = txRef) }
            assertThat(model.state.value.step).isEqualTo(EarnFlowStep.Result)
            assertThat(model.state.value.txStatus).isEqualTo(EarnTxStatus(TxInfo.TransactionStatus.Confirmed))
            model.onDestroy()
        }

        @Test
        fun `GIVEN verify returns Changed WHEN confirm THEN new plan shown and confirmation asked again`() = runTest {
            // Arrange
            val newPlan = TestPlan(expectedAmount = BigDecimal("0.49"))
            coEvery { session.verify(plan = any()) } returns EarnVerification.Changed(newPlan)
            val model = createConfirmedModel()

            // Act
            model.onConfirmClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.plan).isEqualTo(newPlan)
            assertThat(model.state.value.signing).isEqualTo(EarnSigningState.Idle)
            verify(exactly = 0) { session.execute(plan = any()) }
            coVerifyOrder {
                feeSelectorReloadTrigger.triggerLoadingState()
                feeSelectorReloadTrigger.triggerUpdate(any())
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN verify returns Rejected WHEN confirm THEN error shown and nothing executed`() = runTest {
            // Arrange
            coEvery { session.verify(plan = any()) } returns EarnVerification.Rejected(EarnSessionError.Failed())
            val model = createConfirmedModel()

            // Act
            model.onConfirmClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { session.execute(plan = any()) }
            verify(exactly = 1) { messageSender.send(any()) }
            assertThat(model.state.value.step).isEqualTo(EarnFlowStep.Confirm)
            model.onDestroy()
        }

        @Test
        fun `GIVEN prepare failed after fee change WHEN confirm THEN old plan is not verified`() = runTest {
            // Arrange
            val model = createConfirmedModel()
            coEvery {
                session.prepare(action = any(), amount = any(), fee = any())
            } returns EarnSessionError.Failed().left()
            model.onFeeResult(testFeeContent("0.001"))
            advanceUntilIdle()

            // Act
            model.onConfirmClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.plan).isNull()
            coVerify(exactly = 0) { session.verify(plan = any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN new fee on confirm WHEN re-prepare not finished THEN confirm does nothing`() = runTest {
            // Arrange
            val model = createConfirmedModel()

            // Act
            model.onFeeResult(testFeeContent("0.001"))
            model.onConfirmClick()

            // Assert
            assertThat(model.state.value.isPreparing).isTrue()
            coVerify(exactly = 0) { session.verify(plan = any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN execute ends without terminal step WHEN confirm THEN signing unlocks`() = runTest {
            // Arrange
            every { session.execute(plan = any()) } returns flowOf(
                EarnSigningStep.AwaitingConfirmation(stepLabel = null, tx = EarnTxAnalytics()),
            )
            val model = createConfirmedModel()

            // Act
            model.onConfirmClick()
            advanceUntilIdle()
            model.onBack()

            // Assert
            assertThat(model.state.value.signing).isEqualTo(EarnSigningState.Idle)
            assertThat(model.state.value.step).isEqualTo(EarnFlowStep.Amount)
            model.onDestroy()
        }

        @Test
        fun `GIVEN execute throws WHEN confirm THEN error dialog and signing unlocks`() = runTest {
            // Arrange
            every { session.execute(plan = any()) } returns flow { error("broken") }
            val model = createConfirmedModel()

            // Act
            model.onConfirmClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.signing).isEqualTo(EarnSigningState.Idle)
            verify(exactly = 1) { messageSender.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN execute throws after Sent WHEN confirm THEN flow closes without failure dialog`() = runTest {
            // Arrange
            every { session.execute(plan = any()) } returns flow {
                emit(EarnSigningStep.Sent(txRef))
                error("broken")
            }
            val model = createConfirmedModel()

            // Act
            model.onConfirmClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { callback.onFinished(EarnFlowExit.Done) }
            verify(exactly = 0) { messageSender.send(any()) }
            assertThat(model.state.value.plan).isNull()
            model.onDestroy()
        }

        @Test
        fun `GIVEN execute ends after Sent without Done WHEN confirm THEN flow closes`() = runTest {
            // Arrange
            every { session.execute(plan = any()) } returns flowOf(EarnSigningStep.Sent(txRef))
            val model = createConfirmedModel()

            // Act
            model.onConfirmClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { callback.onFinished(EarnFlowExit.Done) }
            assertThat(model.state.value.signing).isEqualTo(EarnSigningState.Idle)
            model.onDestroy()
        }

        @Test
        fun `GIVEN Failed after Sent WHEN confirm again THEN sent plan is not verified twice`() = runTest {
            // Arrange
            every { session.execute(plan = any()) } returns flowOf(
                EarnSigningStep.Sent(txRef),
                EarnSigningStep.Failed(SendTransactionError.BlockchainSdkError(code = 1, message = "reverted")),
            )
            val model = createConfirmedModel()
            model.onConfirmClick()
            advanceUntilIdle()

            // Act
            model.onConfirmClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { session.verify(plan = any()) }
            coVerify(exactly = 2) { session.prepare(action = any(), amount = any(), fee = any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN fee changed during verify WHEN verified THEN old plan is not executed and plan prepared again`() =
            runTest {
                // Arrange
                val verifyGate = CompletableDeferred<Unit>()
                coEvery { session.verify(plan = any()) } coAnswers {
                    verifyGate.await()
                    EarnVerification.Verified(TestVerifiedPlan(TestPlan()))
                }
                val model = createConfirmedModel()
                model.onConfirmClick()
                runCurrent()
                model.onFeeResult(testFeeContent("0.001"))
                runCurrent()

                // Act
                verifyGate.complete(Unit)
                advanceUntilIdle()

                // Assert
                verify(exactly = 0) { session.execute(plan = any()) }
                coVerify(exactly = 2) { session.prepare(action = any(), amount = any(), fee = any()) }
                assertThat(model.state.value.isPlanCurrent).isTrue()
                model.onDestroy()
            }

        @Test
        fun `GIVEN signing cancelled on card WHEN execute THEN back on confirm without dialog`() = runTest {
            // Arrange
            every { session.execute(plan = any()) } returns flowOf(
                EarnSigningStep.Failed(SendTransactionError.UserCancelledError),
            )
            val model = createConfirmedModel()

            // Act
            model.onConfirmClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.step).isEqualTo(EarnFlowStep.Confirm)
            assertThat(model.state.value.signing).isEqualTo(EarnSigningState.Idle)
            verify(exactly = 0) { messageSender.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN network rejects transaction WHEN execute THEN error dialog and back on confirm`() = runTest {
            // Arrange
            every { session.execute(plan = any()) } returns flowOf(
                EarnSigningStep.Failed(SendTransactionError.BlockchainSdkError(code = 1, message = "rejected")),
            )
            val model = createConfirmedModel()

            // Act
            model.onConfirmClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.step).isEqualTo(EarnFlowStep.Confirm)
            verify(exactly = 1) { messageSender.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN result with exit button WHEN pressed THEN flow finishes with that exit`() = runTest {
            // Arrange
            val model = createConfirmedModel()
            model.onConfirmClick()
            advanceUntilIdle()

            // Act
            model.result.value?.primary?.onClick?.invoke()

            // Assert
            verify(exactly = 1) { callback.onFinished(EarnFlowExit.ToYield) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN result shown WHEN stack recreated THEN it holds only the result`() = runTest {
            // Arrange
            val model = createConfirmedModel()
            model.onConfirmClick()
            advanceUntilIdle()

            // Act
            val stack = model.restoredStack()

            // Assert
            assertThat(stack).containsExactly(EarnFlowStep.Result)
            model.onDestroy()
        }

        @Test
        fun `GIVEN confirm of amount action WHEN stack recreated THEN amount stays beneath confirm`() = runTest {
            // Arrange
            val model = createConfirmedModel()

            // Act
            val stack = model.restoredStack()

            // Assert
            assertThat(stack).containsExactly(EarnFlowStep.Amount, EarnFlowStep.Confirm).inOrder()
            model.onDestroy()
        }

        @Test
        fun `GIVEN transaction error dialog WHEN support pressed THEN email sent with the entered amount`() = runTest {
            // Arrange
            every { session.execute(plan = any()) } returns flowOf(
                EarnSigningStep.Failed(SendTransactionError.BlockchainSdkError(code = 1, message = "rejected")),
            )
            val metaInfo: WalletMetaInfo = mockk()
            coEvery { getWalletMetaInfoUseCase.invoke(userWalletId = testUserWalletId) } returns metaInfo.right()
            val dialog = slot<UiMessage>()
            every { messageSender.send(capture(dialog)) } returns Unit
            val model = createConfirmedModel()
            model.onConfirmClick()
            advanceUntilIdle()

            // Act
            (dialog.captured as DialogMessage).firstAction.onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                saveBlockchainErrorUseCase.invoke(
                    error = BlockchainErrorInfo(
                        errorMessage = "1: rejected",
                        networkId = testEthereum.network.id,
                        destinationAddress = "",
                        tokenSymbol = null,
                        amount = "0.5 ETH",
                        fee = "0.00033 ETH",
                    ),
                )
            }
            coVerify(exactly = 1) {
                sendFeedbackEmailUseCase.invoke(type = FeedbackEmailType.TransactionSendingProblem(metaInfo))
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN no wallet meta info WHEN support pressed THEN no email sent`() = runTest {
            // Arrange
            every { session.execute(plan = any()) } returns flowOf(
                EarnSigningStep.Failed(SendTransactionError.BlockchainSdkError(code = 1, message = "rejected")),
            )
            coEvery {
                getWalletMetaInfoUseCase.invoke(userWalletId = testUserWalletId)
            } returns IllegalStateException().left()
            val dialog = slot<UiMessage>()
            every { messageSender.send(capture(dialog)) } returns Unit
            val model = createConfirmedModel()
            model.onConfirmClick()
            advanceUntilIdle()

            // Act
            (dialog.captured as DialogMessage).firstAction.onClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 0) { sendFeedbackEmailUseCase.invoke(type = any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN confirm step of amount action WHEN back THEN amount step again`() = runTest {
            // Arrange
            val model = createConfirmedModel()

            // Act
            model.onBack()

            // Assert
            assertThat(model.state.value.step).isEqualTo(EarnFlowStep.Amount)
            verify(exactly = 0) { callback.onFinished(any()) }
            model.onDestroy()
        }
    }

    private suspend fun TestScope.createConfirmedModel(): EarnFlowModel {
        val model = createModel(action = Deposit())
        advanceUntilIdle()
        model.onAmountValueChange("0.5")
        model.onFeeResult(testFeeContent("0.00033"))
        advanceUntilIdle()
        model.onSubmit()
        advanceUntilIdle()
        assertThat(model.state.value.step).isEqualTo(EarnFlowStep.Confirm)
        return model
    }

    private fun TestScope.createModel(
        action: EarnFlowAction = Deposit(),
        factories: Map<String, EarnSession.Factory> = mapOf(
            testFlowProduct.productId.value to EarnSession.Factory { session },
        ),
    ): EarnFlowModel = EarnFlowModel(
        dispatchers = createTestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(testParams(action, callback)),
        sessionFactories = EarnSessionFactories(factories),
        statuses = EarnFlowStatuses(
            singleAccountStatusListSupplier = singleAccountStatusListSupplier,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            getFeePaidCryptoCurrencyStatusSyncUseCase = getFeePaidCryptoCurrencyStatusSyncUseCase,
            isAmountSubtractAvailableUseCase = isAmountSubtractAvailableUseCase,
        ),
        feeSelectorReloadTrigger = feeSelectorReloadTrigger,
        messageSender = messageSender,
        converter = EarnFlowConverter(),
        support = EarnFlowSupport(
            transactionErrorDialogFactory = TransactionErrorDialogFactory(),
            saveBlockchainErrorUseCase = saveBlockchainErrorUseCase,
            getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
            sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
        ),
    )

    data class DeductionModel(val entered: String?, val fee: String, val expected: String)
}