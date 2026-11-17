package com.tangem.features.earn.flow.impl.model

import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.alerts.TransactionErrorDialogFactory
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.IsAmountSubtractAvailableUseCase
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.error.SolanaGaslessError
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.earn.api.EarnEntrySource
import com.tangem.features.earn.api.ui.EarnRowInfoUM
import com.tangem.features.earn.flow.api.EarnFlowComponent
import com.tangem.features.earn.flow.api.EarnFlowProduct
import com.tangem.features.earn.flow.impl.EarnSessionFactories
import com.tangem.features.earn.flow.impl.analytics.EarnDisplayCurrency
import com.tangem.features.earn.flow.impl.analytics.EarnErrorPopupAction
import com.tangem.features.earn.flow.impl.analytics.EarnFlowAnalyticsEvent
import com.tangem.features.earn.flow.impl.analytics.EarnFlowScreen
import com.tangem.features.earn.flow.impl.analytics.EarnResultDestination
import com.tangem.features.earn.flow.impl.analytics.EarnTxType
import com.tangem.features.earn.product.api.analytics.EarnScreenAnalytics
import com.tangem.features.earn.product.api.analytics.EarnTxAnalytics
import com.tangem.features.earn.product.api.analytics.EarnUsdValuation
import com.tangem.features.earn.product.api.product.EarnHowItWorks
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.product.EarnRowInfo
import com.tangem.features.earn.product.api.product.EarnTextBlock
import com.tangem.features.earn.product.api.product.EarnValue
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.EarnAmount
import com.tangem.features.earn.product.api.session.EarnFeeSelection
import com.tangem.features.earn.product.api.session.EarnFlowAction
import com.tangem.features.earn.product.api.session.EarnFlowExit
import com.tangem.features.earn.product.api.session.EarnResultButton
import com.tangem.features.earn.product.api.session.EarnSession
import com.tangem.features.earn.product.api.session.EarnSessionError
import com.tangem.features.earn.product.api.session.EarnSigningStep
import com.tangem.features.earn.product.api.session.EarnTxRef
import com.tangem.features.earn.product.api.session.EarnTxStatus
import com.tangem.features.earn.product.api.session.EarnVerification
import com.tangem.features.earn.product.api.session.Withdraw
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.yield.MockYieldSupply
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
import io.mockk.verifyOrder
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
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
import org.junit.jupiter.params.provider.MethodSource
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
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val shareManager: ShareManager = mockk(relaxed = true)
    private val blogUrlBuilder: TangemBlogUrlBuilder = mockk()
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase = mockk()
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val usdValuation: EarnUsdValuation = mockk()

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
            urlOpener,
            shareManager,
            blogUrlBuilder,
            getExplorerTransactionUrlUseCase,
            getUserWalletUseCase,
            getAccountCurrencyStatusUseCase,
            isAccountsModeEnabledUseCase,
            analyticsEventHandler,
            usdValuation,
        )
        coEvery { usdValuation.bucket(currency = any(), cryptoAmount = any()) } returns "1K-5K"
        coEvery { session.screenAnalytics(action = any()) } returns EarnScreenAnalytics()
        coEvery { getAccountCurrencyStatusUseCase.invokeSync(userWalletId = any(), currency = any()) } returns none()
        coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns false
        coEvery { blogUrlBuilder.build(TangemBlogUrlBuilder.Post.HowYieldModeWorks) } returns TEST_LEARN_MORE_URL
        every { getUserWalletUseCase.invoke(testUserWalletId) } returns mockk<UserWallet.Hot> { every { name } returns "Wallet" }.right()
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
        every {
            getExplorerTransactionUrlUseCase.invoke(txHash = any(), currency = any())
        } returns TEST_EXPLORER_URL.right()
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
                model.onFeeResult(testFeeContent("0"))
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
                    session.prepare(action = TestClaim, amount = null, fee = testFeeSelection("0.00033"))
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
        fun `GIVEN amount entered before fee WHEN fee arrives and submit THEN field and prepare stay within Max`(
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
                    fee = testFeeSelection(model.fee),
                )
            }
            assertThat(flow.state.value.enteredAmount).isEqualTo(BigDecimal(model.expected))
            flow.onDestroy()
        }

        private fun provideTestModels() = listOf(
            DeductionModel(entered = null, fee = "0.00033", expected = "2.00967"),
            DeductionModel(entered = "0.5", fee = "0.00033", expected = "0.5"),
            DeductionModel(entered = "2.00999", fee = "0.00033", expected = "2.00967"),
            DeductionModel(entered = null, fee = "0", expected = "2.01"),
        )

        @Test
        fun `GIVEN fee estimate WHEN Max THEN amount is balance less fee`() = runTest {
            // Arrange
            val model = createModel(action = Deposit())
            advanceUntilIdle()
            model.onFeeResult(testFeeContent("0.0004"))
            advanceUntilIdle()

            // Act
            model.onMaxValueClick()

            // Assert
            assertThat(model.state.value.enteredAmount).isEqualTo(BigDecimal("2.0096"))
            model.onDestroy()
        }

        @ParameterizedTest
        @MethodSource("providePercentModels")
        fun `GIVEN fee estimate WHEN percent THEN share of balance less fee`(model: PercentModel) = runTest {
            // Arrange
            val flow = createModel(action = Deposit())
            advanceUntilIdle()
            flow.onFeeResult(testFeeContent("0.01"))
            advanceUntilIdle()

            // Act
            flow.onPercentClick(model.percent)

            // Assert
            assertThat(flow.state.value.enteredAmount).isEqualToIgnoringScale(BigDecimal(model.expected))
            flow.onDestroy()
        }

        private fun providePercentModels() = listOf(
            PercentModel(percent = 25, expected = "0.5"),
            PercentModel(percent = 50, expected = "1"),
            PercentModel(percent = 75, expected = "1.5"),
            PercentModel(percent = 100, expected = "2"),
        )

        @Test
        fun `GIVEN no fee yet WHEN percent THEN share is set once the fee arrives`() = runTest {
            // Arrange
            val model = createModel(action = Deposit())
            advanceUntilIdle()

            // Act
            model.onPercentClick(25)
            val beforeFee = model.state.value.enteredAmount
            model.onFeeResult(testFeeContent("0.01"))
            advanceUntilIdle()

            // Assert
            assertThat(beforeFee).isNull()
            assertThat(model.state.value.enteredAmount).isEqualToIgnoringScale(BigDecimal("0.5"))
            model.onDestroy()
        }

        @Test
        fun `GIVEN percent pending WHEN amount typed before fee THEN typed amount is kept`() = runTest {
            // Arrange
            val model = createModel(action = Deposit())
            advanceUntilIdle()
            model.onPercentClick(25)

            // Act
            model.onAmountValueChange("0.3")
            model.onFeeResult(testFeeContent("0.01"))
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.enteredAmount).isEqualTo(BigDecimal("0.3"))
            model.onDestroy()
        }

        @Test
        fun `GIVEN fee estimate WHEN amount above Max typed THEN field shows error and is not reduced`() = runTest {
            // Arrange
            val model = createModel(action = Deposit())
            advanceUntilIdle()
            model.onFeeResult(testFeeContent("0.01"))
            advanceUntilIdle()

            // Act
            model.onAmountValueChange("2.005")
            advanceUntilIdle()

            // Assert
            val field = (model.state.value.amountState as AmountState.Data).amountTextField
            assertThat(field.isError).isTrue()
            assertThat(model.state.value.enteredAmount).isEqualTo(BigDecimal("2.005"))
            model.onDestroy()
        }

        @Test
        fun `GIVEN Max before fee WHEN fee arrives THEN fee is estimated again for the set amount`() = runTest {
            // Arrange
            val model = createModel(action = Deposit())
            advanceUntilIdle()
            model.onMaxValueClick()
            clearMocks(feeSelectorReloadTrigger, answers = false)

            // Act
            model.onFeeResult(testFeeContent("0.01"))
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { feeSelectorReloadTrigger.triggerUpdate(any()) }
            coVerify(exactly = 0) { feeSelectorReloadTrigger.triggerLoadingState() }
            model.onDestroy()
        }

        @Test
        fun `GIVEN amount WHEN fee leaves nothing to deposit THEN field shows error and amount is kept`() = runTest {
            // Arrange
            val model = createModel(action = Deposit())
            advanceUntilIdle()
            model.onFeeResult(testFeeContent("0.01"))
            advanceUntilIdle()
            model.onAmountValueChange("1")
            advanceUntilIdle()

            // Act
            model.onFeeResult(testFeeContent("2.02"))
            advanceUntilIdle()

            // Assert
            val field = (model.state.value.amountState as AmountState.Data).amountTextField
            assertThat(field.isError to model.state.value.enteredAmount).isEqualTo(true to BigDecimal("1"))
            model.onDestroy()
        }

        @Test
        fun `GIVEN fiat Max WHEN the same fee arrives again THEN Max stays valid and exact`() = runTest {
            // Arrange
            val model = createModel(action = Deposit())
            advanceUntilIdle()
            model.onFeeResult(testFeeContent("0.000333"))
            advanceUntilIdle()
            model.onCurrencyChangeClick(isFiat = true)
            model.onMaxValueClick()
            advanceUntilIdle()

            // Act
            model.onFeeResult(testFeeContent("0.000333"))
            advanceUntilIdle()

            // Assert
            val field = (model.state.value.amountState as AmountState.Data).amountTextField
            assertThat(field.isError to model.state.value.enteredAmount).isEqualTo(false to BigDecimal("2.009667"))
            model.onDestroy()
        }

        @Test
        fun `GIVEN Max tapped before Max loads WHEN fee then Max arrive THEN Max is set and fee estimated again`() =
            runTest {
                // Arrange
                coEvery { session.maxAmount(action = any()) } coAnswers {
                    delay(MAX_AMOUNT_DELAY_MS)
                    TEST_BALANCE.right()
                }
                val model = createModel(action = Deposit())
                runCurrent()
                model.onMaxValueClick()
                model.onFeeResult(testFeeContent("0.01"))
                runCurrent()
                clearMocks(feeSelectorReloadTrigger, answers = false)

                // Act
                advanceUntilIdle()

                // Assert
                assertThat(model.state.value.enteredAmount).isEqualTo(BigDecimal("2.00"))
                coVerify(exactly = 1) { feeSelectorReloadTrigger.triggerUpdate(any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN fee caps amount below minimum on confirm WHEN confirm pressed THEN nothing is prepared`() = runTest {
            // Arrange
            coEvery { session.prepare(action = any(), amount = any(), fee = any()) } returnsMany listOf(
                EarnSessionError.BelowMinimum(BigDecimal("1.95")).left(),
                TestPlan().right(),
            )
            val model = createModel(action = Deposit())
            advanceUntilIdle()
            model.onFeeResult(testFeeContent("0.001"))
            advanceUntilIdle()
            model.onAmountValueChange("1")
            model.onSubmit()
            advanceUntilIdle()
            model.onAmountValueChange("2")
            model.onSubmit()
            advanceUntilIdle()
            model.onFeeResult(testFeeContent("0.07"))
            advanceUntilIdle()
            model.onFeeResult(testFeeContent("0.07"))
            advanceUntilIdle()

            // Act
            model.onConfirmClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.confirm.value?.primary?.isEnabled).isFalse()
            coVerify(exactly = 2) { session.prepare(action = any(), amount = any(), fee = any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN amount fits both fees WHEN fee rises THEN amount is kept`() = runTest {
            // Arrange
            val model = createModel(action = Deposit())
            advanceUntilIdle()
            model.onFeeResult(testFeeContent("0.01"))
            advanceUntilIdle()
            model.onAmountValueChange("1")
            advanceUntilIdle()

            // Act
            model.onFeeResult(testFeeContent("0.02"))
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.enteredAmount).isEqualTo(BigDecimal("1"))
            model.onDestroy()
        }

        @Test
        fun `GIVEN Max WHEN fee rises on amount step THEN amount is capped to the new Max`() = runTest {
            // Arrange
            val model = createModel(action = Deposit())
            advanceUntilIdle()
            model.onFeeResult(testFeeContent("0.01"))
            advanceUntilIdle()
            model.onMaxValueClick()
            advanceUntilIdle()

            // Act
            model.onFeeResult(testFeeContent("0.02"))
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.enteredAmount).isEqualTo(BigDecimal("1.99"))
            model.onDestroy()
        }

        @Test
        fun `GIVEN fee not below balance WHEN Max THEN Max is zero with fee notice and no prepare`() =
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
                assertThat(model.state.value.enteredAmount).isNull()
                assertThat(amountUM?.notifications?.filterIsInstance<NotificationUM.Error.ExceedsBalance>()).hasSize(1)
                assertThat(amountUM?.primary?.isEnabled).isFalse()
                coVerify(exactly = 0) { session.prepare(action = any(), amount = any(), fee = any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN Max deposited with fee A WHEN fee B on confirm THEN capped amount is prepared with fee B`() =
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
                        fee = testFeeSelection("0.001"),
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
        fun `GIVEN Max WHEN fee on confirm exceeds balance THEN fee notice and confirm is blocked`() = runTest {
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
            assertThat(confirm?.notifications?.filterIsInstance<NotificationUM.Error.ExceedsBalance>()).hasSize(1)
            assertThat(confirm?.primary?.isEnabled).isFalse()
            assertThat(model.state.value.isPreparing).isFalse()
            coVerify(exactly = 1) { session.prepare(action = any(), amount = any(), fee = any()) }
            model.onDestroy()
        }

        @ParameterizedTest
        @MethodSource("providePositionAmountModels")
        fun `GIVEN amount from position WHEN submit THEN prepare and fee get the whole position only for the max`(
            model: PositionAmountModel,
        ) = runTest {
            // Arrange
            coEvery { session.loadFee(action = any(), amount = any()) } returns GetFeeError.UnknownError.left()
            val flow = createModel(action = Withdraw())
            advanceUntilIdle()

            // Act
            if (model.entered == null) flow.onMaxValueClick() else flow.onAmountValueChange(model.entered)
            flow.loadFee()
            flow.onFeeResult(testFeeContent("0.00033"))
            advanceUntilIdle()
            flow.onSubmit()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { session.loadFee(action = Withdraw(), amount = model.expected) }
            coVerify(exactly = 1) {
                session.prepare(action = Withdraw(), amount = model.expected, fee = testFeeSelection("0.00033"))
            }
            flow.onDestroy()
        }

        private fun providePositionAmountModels() = listOf(
            PositionAmountModel(entered = null, expected = EarnAmount.Entire),
            PositionAmountModel(entered = TEST_BALANCE.toPlainString(), expected = EarnAmount.Entire),
            PositionAmountModel(entered = "0.5", expected = EarnAmount.Exact(BigDecimal("0.5"))),
        )

        @ParameterizedTest
        @MethodSource("providePositionPercentModels")
        fun `GIVEN withdrawal without fee yet WHEN percent THEN share of the position is set right away`(
            model: PercentModel,
        ) = runTest {
            // Arrange
            val flow = createModel(action = Withdraw())
            advanceUntilIdle()

            // Act
            flow.onPercentClick(model.percent)

            // Assert
            assertThat(flow.state.value.enteredAmount).isEqualToIgnoringScale(BigDecimal(model.expected))
            flow.onDestroy()
        }

        private fun providePositionPercentModels() = listOf(
            PercentModel(percent = 25, expected = "0.5025"),
            PercentModel(percent = 50, expected = "1.005"),
            PercentModel(percent = 75, expected = "1.5075"),
            PercentModel(percent = 100, expected = "2.01"),
        )

        @Test
        fun `GIVEN position finer than the currency WHEN 100 percent and submit THEN entire position withdrawn`() =
            runTest {
                // Arrange
                coEvery { session.maxAmount(action = any()) } returns BigDecimal("1.0000000000000000001").right()
                coEvery { session.loadFee(action = any(), amount = any()) } returns GetFeeError.UnknownError.left()
                val model = createModel(action = Withdraw())
                advanceUntilIdle()

                // Act
                model.onPercentClick(100)
                model.loadFee()
                model.onFeeResult(testFeeContent("0.00033"))
                advanceUntilIdle()
                model.onSubmit()
                advanceUntilIdle()

                // Assert
                coVerify(exactly = 1) {
                    session.prepare(action = Withdraw(), amount = EarnAmount.Entire, fee = testFeeSelection("0.00033"))
                }
                model.onDestroy()
            }

        @Test
        fun `GIVEN share rounding to zero WHEN 25 percent after 100 percent THEN amount cleared and not submittable`() =
            runTest {
                // Arrange
                coEvery { session.maxAmount(action = any()) } returns BigDecimal("0.000000000000000003").right()
                val model = createModel(action = Withdraw())
                advanceUntilIdle()
                model.onFeeResult(testFeeContent("0.00033"))
                advanceUntilIdle()
                model.onPercentClick(100)

                // Act
                model.onPercentClick(25)

                // Assert
                assertThat(model.state.value.enteredAmount).isNull()
                assertThat(model.amount.value?.primary?.isEnabled).isFalse()
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

        @Test
        fun `GIVEN fee coin topped up WHEN account list updates THEN withdrawal fee warning disappears`() = runTest {
            // Arrange
            val accountStatusLists = MutableSharedFlow<AccountStatusList>(replay = 1)
            every { singleAccountStatusListSupplier.invoke(testUserWalletId) } returns accountStatusLists
            coEvery {
                getFeePaidCryptoCurrencyStatusSyncUseCase.invoke(userWalletId = any(), cryptoCurrencyStatus = any())
            } returnsMany listOf(testStatus(amount = BigDecimal("0.0001")).right(), testStatus().right())
            accountStatusLists.emit(accountStatusList)
            val model = createModel(action = Withdraw())
            advanceUntilIdle()
            model.onAmountValueChange("0.5")
            model.onFeeResult(testFeeContent("0.00033"))
            advanceUntilIdle()
            val warningsBeforeTopUp = model.amount.value?.notifications.orEmpty()
                .filterIsInstance<NotificationUM.Error.ExceedsBalance>()

            // Act
            accountStatusLists.emit(accountStatusList)
            advanceUntilIdle()

            // Assert
            assertThat(warningsBeforeTopUp).hasSize(1)
            assertThat(
                model.amount.value?.notifications.orEmpty().filterIsInstance<NotificationUM.Error.ExceedsBalance>(),
            )
                .isEmpty()
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
        fun `GIVEN fast fee picked from choosable levels WHEN fee result THEN fee selection reports Max`() = runTest {
            // Arrange
            val model = createModel(action = Deposit())
            val slow = testFee("0.0001")
            val market = testFee("0.0002")
            val fast = testFee("0.0003")
            val content = testFeeContent("0.0003").copy(
                fees = TransactionFee.Choosable(minimum = slow, normal = market, priority = fast),
                feeItems = persistentListOf(FeeItem.Slow(slow), FeeItem.Market(market), FeeItem.Fast(fast)),
                selectedFeeItem = FeeItem.Fast(fast),
            )

            // Act
            model.onFeeResult(content)

            // Assert
            assertThat(model.state.value.feeSelection)
                .isEqualTo(EarnFeeSelection(fee = fast, extended = null, feeType = AnalyticsParam.FeeType.Max))
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
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FeeTokenPick {

        @ParameterizedTest
        @MethodSource("providePicks")
        fun `GIVEN fee results WHEN selector asks for a quote THEN session gets the fee token the user picked`(
            model: FeeTokenPickModel,
        ) = runTest {
            // Arrange
            coEvery {
                session.loadFeeExtended(action = any(), amount = any(), feeCurrency = any())
            } returns GetFeeError.UnknownError.left()
            val flow = createModel(action = Withdraw(isGaslessAllowed = true))
            advanceUntilIdle()
            model.results.forEach(flow::onFeeResult)

            // Act
            flow.loadFeeExtended(feeCurrency = model.requested)

            // Assert
            coVerify(exactly = 1) {
                session.loadFeeExtended(action = any(), amount = any(), feeCurrency = model.expected)
            }
            flow.onDestroy()
        }

        private fun providePicks() = listOf(
            FeeTokenPickModel(
                name = "token picked, then the selector reset to loading",
                results = listOf(picked(usdcStatus), FeeSelectorUM.Loading),
                requested = null,
                expected = usdcStatus,
            ),
            FeeTokenPickModel(
                name = "token picked, then the coin picked",
                results = listOf(picked(usdcStatus), picked(testStatus()), FeeSelectorUM.Loading),
                requested = null,
                expected = null,
            ),
            FeeTokenPickModel(
                name = "token chosen automatically",
                results = listOf(testFeeContent("1.5", feeCryptoCurrencyStatus = usdcStatus)),
                requested = null,
                expected = null,
            ),
            FeeTokenPickModel(
                name = "token the selector asks for",
                results = listOf(picked(usdcStatus)),
                requested = usdtStatus,
                expected = usdtStatus,
            ),
        )

        private fun picked(feeCurrency: CryptoCurrencyStatus): FeeSelectorUM.Content {
            val content = testFeeContent("1.5", feeCryptoCurrencyStatus = feeCurrency)
            return content.copy(feeExtraInfo = content.feeExtraInfo.copy(isFeeTokenSelectedByUser = true))
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
        fun `GIVEN sent and result not ready WHEN confirm shown THEN it keeps the sent plan rows and amount`() =
            runTest {
            // Arrange
            val row = EarnRow(label = stringReference("Current APY"), value = EarnValue.Text(stringReference("5.5%")))
            val plan = TestPlan(rows = listOf(row))
            coEvery { session.prepare(action = any(), amount = any(), fee = any()) } returns plan.right()
            coEvery { session.verify(plan = any()) } returns EarnVerification.Verified(TestVerifiedPlan(plan))
            every { session.execute(plan = any()) } returns flow {
                emit(EarnSigningStep.Sent(txRef))
                awaitCancellation()
            }
            val model = createConfirmedModel()

            // Act
            model.onConfirmClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.plan).isNull()
            assertThat(model.confirm.value?.rows?.map { it.label }).containsExactly(row.label)
            assertThat(model.confirm.value?.amountCrypto).isNotEqualTo(TextReference.EMPTY)
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

    @Nested
    inner class WalletAndResultDetails {

        @Test
        fun `GIVEN deposit WHEN created THEN amount names the source wallet`() = runTest {
            // Act
            val model = createModel(action = Deposit())
            advanceUntilIdle()

            // Assert
            val amountState = model.amount.value?.amountState as AmountState.Data
            assertThat(amountState.accountTitleUM).isInstanceOf(AccountTitleUM.Text::class.java)
            model.onDestroy()
        }

        @Test
        fun `GIVEN withdrawal WHEN created THEN no source and the product heads the balance row`() = runTest {
            // Act
            val model = createModel(action = Withdraw())
            advanceUntilIdle()

            // Assert
            assertThat(model.amount.value?.isFromPosition).isTrue()
            assertThat((model.amount.value?.amountState as AmountState.Data).tokenName).isEqualTo(testFlowProduct.title)
            coVerify(exactly = 0) { getAccountCurrencyStatusUseCase.invokeSync(userWalletId = any(), currency = any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN card wallet WHEN confirm shown THEN primary carries the Tangem logo`() = runTest {
            // Arrange
            every { getUserWalletUseCase.invoke(testUserWalletId) } returns mockk<UserWallet.Cold> { every { name } returns "Wallet" }.right()

            // Act
            val model = createConfirmedModel()

            // Assert
            assertThat(model.confirm.value?.primary?.tangemIconUM).isNotNull()
            assertThat(model.confirm.value?.isHoldToConfirm).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN mobile wallet WHEN confirm shown THEN the action is held to confirm without the logo`() = runTest {
            // Act
            val model = createConfirmedModel()

            // Assert
            assertThat(model.confirm.value?.isHoldToConfirm).isTrue()
            assertThat(model.confirm.value?.primary?.tangemIconUM).isNull()
            model.onDestroy()
        }

        @Test
        fun `GIVEN result without subtitle WHEN result shown THEN flow adds amount and date`() = runTest {
            // Arrange
            val model = createConfirmedModel()

            // Act
            model.onConfirmClick()
            advanceUntilIdle()

            // Assert
            val subtitle = model.result.value?.subtitle
            assertThat(subtitle).isInstanceOf(TextReference.Combined::class.java)
            assertThat(model.state.value.sentAmount).isEqualTo(TestPlan().expectedAmount)
            model.onDestroy()
        }

        @Test
        fun `GIVEN two sent transactions WHEN result shown THEN the plan amount is kept`() = runTest {
            // Arrange
            every { session.execute(plan = any()) } returns flowOf(
                EarnSigningStep.Sent(txRef),
                EarnSigningStep.Sent(txRef),
                EarnSigningStep.Done(txRef, testResult()),
            )
            val model = createConfirmedModel()

            // Act
            model.onConfirmClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.sentAmount).isEqualTo(TestPlan().expectedAmount)
            model.onDestroy()
        }

        @Test
        fun `GIVEN partly sent attempt failed WHEN retried with new amount THEN result keeps the retry amount`() =
            runTest {
                // Arrange
                val retryPlan = TestPlan(expectedAmount = BigDecimal("0.8"))
                coEvery { session.verify(plan = any()) } answers {
                    EarnVerification.Verified(TestVerifiedPlan(firstArg()))
                }
                every { session.execute(plan = any()) } returnsMany listOf(
                    flowOf(
                        EarnSigningStep.Sent(txRef),
                        EarnSigningStep.Failed(SendTransactionError.UserCancelledError),
                    ),
                    flowOf(EarnSigningStep.Sent(txRef), EarnSigningStep.Done(txRef, testResult())),
                )
                val model = createConfirmedModel()
                model.onConfirmClick()
                advanceUntilIdle()
                coEvery { session.prepare(action = any(), amount = any(), fee = any()) } returns retryPlan.right()

                // Act
                model.onConfirmClick()
                advanceUntilIdle()
                model.onConfirmClick()
                advanceUntilIdle()

                // Assert
                assertThat(model.state.value.sentAmount).isEqualTo(retryPlan.expectedAmount)
                model.onDestroy()
            }

        @Test
        fun `GIVEN result with subtitle WHEN result shown THEN product subtitle kept`() = runTest {
            // Arrange
            val subtitle = stringReference("Now earning")
            every { session.execute(plan = any()) } returns flowOf(
                EarnSigningStep.Sent(txRef),
                EarnSigningStep.Done(txRef, testResult().copy(subtitle = subtitle)),
            )
            val model = createConfirmedModel()

            // Act
            model.onConfirmClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.result.value?.subtitle).isEqualTo(subtitle)
            model.onDestroy()
        }
    }

    @Nested
    inner class Explorer {

        @Test
        fun `GIVEN result with explorer WHEN result shown THEN explorer opens the tx link`() = runTest {
            // Arrange
            every { session.execute(plan = any()) } returns flowOf(
                EarnSigningStep.Sent(txRef),
                EarnSigningStep.Done(txRef, testResult().copy(isExplorerShown = true)),
            )
            val model = createConfirmedModel()

            // Act
            model.onConfirmClick()
            advanceUntilIdle()
            model.result.value?.explorer?.onExploreClick?.invoke()

            // Assert
            verify(exactly = 1) { getExplorerTransactionUrlUseCase.invoke(txHash = txRef.id, currency = testEthereum) }
            verify(exactly = 1) { urlOpener.openUrl(TEST_EXPLORER_URL) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN result without explorer WHEN result shown THEN no link is built`() = runTest {
            // Arrange
            val model = createConfirmedModel()

            // Act
            model.onConfirmClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.result.value?.explorer).isNull()
            verify(exactly = 0) { getExplorerTransactionUrlUseCase.invoke(txHash = any(), currency = any()) }
            model.onDestroy()
        }
    }

    @Nested
    inner class HowItWorks {

        @Test
        fun `GIVEN product with how it works WHEN info pressed THEN sheet shown`() = runTest {
            // Arrange
            val model = createModel(flowProduct = testFlowProduct.copy(howItWorks = testHowItWorks))
            advanceUntilIdle()

            // Act
            model.amount.value?.onHowItWorksClick?.invoke()

            // Assert
            assertThat(model.howItWorks.value?.title).isEqualTo(testHowItWorks.title)
            model.onDestroy()
        }

        @Test
        fun `GIVEN sheet shown WHEN dismissed THEN sheet hidden`() = runTest {
            // Arrange
            val model = createModel(flowProduct = testFlowProduct.copy(howItWorks = testHowItWorks))
            advanceUntilIdle()
            model.amount.value?.onHowItWorksClick?.invoke()

            // Act
            model.onHowItWorksDismiss()

            // Assert
            assertThat(model.howItWorks.value).isNull()
            model.onDestroy()
        }

        @Test
        fun `GIVEN sheet shown WHEN learn more pressed THEN the link of its post is built and opened`() = runTest {
            // Arrange
            val model = createModel(flowProduct = testFlowProduct.copy(howItWorks = testHowItWorks))
            advanceUntilIdle()
            model.amount.value?.onHowItWorksClick?.invoke()

            // Act
            model.howItWorks.value?.onLearnMoreClick?.invoke()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { urlOpener.openUrl(TEST_LEARN_MORE_URL) }
            model.onDestroy()
        }
    }

    @Nested
    inner class RowInfo {

        private val info = EarnRowInfo.Text(title = stringReference("Current APY"), text = stringReference("It varies"))
        private val row = EarnRow(
            label = stringReference("Current APY"),
            value = EarnValue.Text(stringReference("5.5%")),
            info = info,
        )

        @Test
        fun `GIVEN a confirm row with info WHEN its info pressed THEN rowInfo holds its sheet`() = runTest {
            // Arrange
            coEvery { session.prepare(action = any(), amount = any(), fee = any()) } returns
                TestPlan(rows = listOf(row)).right()
            val model = createConfirmedModel()

            // Act
            model.confirm.value?.rows?.single()?.onInfoClick?.invoke()

            // Assert
            val expected = EarnRowInfoUM.Text(
                title = stringReference("Current APY"),
                text = stringReference("It varies"),
            )
            assertThat(model.rowInfo.value).isEqualTo(expected)
            model.onDestroy()
        }

        @Test
        fun `GIVEN an open row info WHEN onRowInfoDismiss THEN rowInfo is cleared`() = runTest {
            // Arrange
            coEvery { session.prepare(action = any(), amount = any(), fee = any()) } returns
                TestPlan(rows = listOf(row)).right()
            val model = createConfirmedModel()
            model.confirm.value?.rows?.single()?.onInfoClick?.invoke()

            // Act
            model.onRowInfoDismiss()

            // Assert
            assertThat(model.rowInfo.value).isNull()
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Analytics {

        @Test
        fun `GIVEN deposit from the banner WHEN created THEN deposit screen opened is sent once with the rate`() =
            runTest {
                // Arrange
                coEvery { session.screenAnalytics(action = Deposit()) } returns
                    EarnScreenAnalytics(rate = createRate("0.055"))

                // Act
                val model = createModel(action = Deposit(), source = EarnEntrySource.EarnBanner)
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) {
                    analyticsEventHandler.send(
                        EarnFlowAnalyticsEvent.DepositScreenOpened(
                            category = "ETH Lending",
                            source = EarnEntrySource.EarnBanner,
                            currentApy = "5.50",
                        ),
                    )
                }
                verify(exactly = 1) { analyticsEventHandler.send(ofType<EarnFlowAnalyticsEvent>()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN deposit without a rate WHEN created THEN deposit screen opened omits the apy`() = runTest {
            // Act
            val model = createModel(action = Deposit(), source = EarnEntrySource.Markets)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    EarnFlowAnalyticsEvent.DepositScreenOpened(
                        category = "ETH Lending",
                        source = EarnEntrySource.Markets,
                        currentApy = null,
                    ),
                )
            }
            model.onDestroy()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN withdraw WHEN created THEN withdraw screen opened carries the position bucket and the cap`(
            model: LiquidityModel,
        ) = runTest {
            // Arrange
            coEvery { session.screenAnalytics(action = Withdraw()) } returns EarnScreenAnalytics(
                position = model.position,
                availableToWithdraw = model.availableToWithdraw,
                isLiquidityKnown = model.isLiquidityKnown,
            )

            // Act
            val flow = createModel(action = Withdraw())
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    EarnFlowAnalyticsEvent.WithdrawScreenOpened(
                        category = "ETH Lending",
                        positionBucket = "1K-5K",
                        isLiquidityCapped = model.expectedCapped,
                    ),
                )
            }
            coVerify(exactly = 1) { usdValuation.bucket(currency = testEthereum, cryptoAmount = model.position) }
            verify(exactly = 1) { analyticsEventHandler.send(ofType<EarnFlowAnalyticsEvent>()) }
            flow.onDestroy()
        }

        @Test
        fun `GIVEN amount entered WHEN submitted twice with a back in between THEN confirm screen opened is sent twice`() =
            runTest {
                // Arrange
                coEvery { session.screenAnalytics(action = Deposit()) } returns EarnScreenAnalytics(
                    rate = createRate("0.055"),
                    performanceFee = BigDecimal("0.15"),
                )
                val model = createConfirmedModel()

                // Act
                model.onBack()
                model.onSubmit()
                advanceUntilIdle()

                // Assert
                assertThat(model.state.value.step).isEqualTo(EarnFlowStep.Confirm)
                verify(exactly = 2) {
                    analyticsEventHandler.send(
                        EarnFlowAnalyticsEvent.ConfirmScreenOpened(
                            category = "ETH Lending",
                            type = EarnTxType.Deposit,
                            amountBucket = "1K-5K",
                            currentApy = "5.50",
                            performanceFee = "15.00",
                        ),
                    )
                }
                coVerify(exactly = 2) {
                    usdValuation.bucket(
                        currency = testEthereum,
                        cryptoAmount = match { amount -> amount?.compareTo(BigDecimal("0.5")) == 0 },
                    )
                }
                model.onDestroy()
            }

        @Test
        fun `GIVEN Done WHEN the result shows THEN success screen opened is sent with the rate`() = runTest {
            // Arrange
            coEvery { session.screenAnalytics(action = Deposit()) } returns
                EarnScreenAnalytics(rate = createRate("0.055"))
            val model = createConfirmedModel()

            // Act
            model.onConfirmClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.step).isEqualTo(EarnFlowStep.Result)
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    EarnFlowAnalyticsEvent.SuccessScreenOpened(
                        category = "ETH Lending",
                        type = EarnTxType.Deposit,
                        currentApy = "5.50",
                    ),
                )
            }
            model.onDestroy()
        }

        @ParameterizedTest
        @MethodSource("provideResultExitModels")
        fun `GIVEN the result WHEN its button exits THEN the result action is sent only for a destination`(
            model: ResultExitModel,
        ) = runTest {
            // Arrange
            every { session.execute(plan = any()) } returns flowOf(
                EarnSigningStep.Done(txRef, testResult(primary = EarnResultButton(stringReference("Exit"), model.exit))),
            )
            val flow = createConfirmedModel()
            flow.onConfirmClick()
            advanceUntilIdle()

            // Act
            flow.result.value?.primary?.onClick?.invoke()

            // Assert
            verify(exactly = 1) { callback.onFinished(model.exit) }
            if (model.expectedDestination == null) {
                verify(exactly = 0) { analyticsEventHandler.send(ofType<EarnFlowAnalyticsEvent.ResultAction>()) }
            } else {
                verify(exactly = 1) {
                    analyticsEventHandler.send(
                        EarnFlowAnalyticsEvent.ResultAction(
                            category = "ETH Lending",
                            destination = model.expectedDestination,
                        ),
                    )
                }
            }
            flow.onDestroy()
        }

        @Test
        fun `GIVEN the result WHEN closed THEN no result action is sent`() = runTest {
            // Arrange
            val model = createConfirmedModel()
            model.onConfirmClick()
            advanceUntilIdle()

            // Act
            model.result.value?.onClose?.invoke()

            // Assert
            verify(exactly = 1) { callback.onFinished(EarnFlowExit.Done) }
            verify(exactly = 0) { analyticsEventHandler.send(ofType<EarnFlowAnalyticsEvent.ResultAction>()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN How it works WHEN opened and Learn more clicked THEN both events carry the entry source`() =
            runTest {
                // Arrange
                val model = createModel(
                    flowProduct = testFlowProduct.copy(howItWorks = testHowItWorks),
                    source = EarnEntrySource.YieldSheet,
                )
                advanceUntilIdle()

                // Act
                model.amount.value?.onHowItWorksClick?.invoke()
                model.howItWorks.value?.onLearnMoreClick?.invoke()
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) {
                    analyticsEventHandler.send(
                        EarnFlowAnalyticsEvent.HowItWorksOpened(
                            category = "ETH Lending",
                            source = EarnEntrySource.YieldSheet,
                        ),
                    )
                }
                verify(exactly = 1) {
                    analyticsEventHandler.send(
                        EarnFlowAnalyticsEvent.LearnMore(
                            category = "ETH Lending",
                            source = EarnEntrySource.YieldSheet,
                        ),
                    )
                }
                model.onDestroy()
            }

        @Test
        fun `GIVEN product without How it works WHEN info pressed THEN no How it works event is sent`() = runTest {
            // Arrange
            val model = createModel(action = Deposit())
            advanceUntilIdle()

            // Act
            model.amount.value?.onHowItWorksClick?.invoke()

            // Assert
            verify(exactly = 0) { analyticsEventHandler.send(ofType<EarnFlowAnalyticsEvent.HowItWorksOpened>()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN an action that is neither deposit nor withdraw WHEN the flow runs to the result THEN no flow event is sent`() =
            runTest {
                // Arrange
                val model = createModel(action = TestClaim)
                model.onFeeResult(testFeeContent("0.00033"))
                advanceUntilIdle()

                // Act
                model.onConfirmClick()
                advanceUntilIdle()
                model.result.value?.primary?.onClick?.invoke()

                // Assert
                assertThat(model.state.value.step).isEqualTo(EarnFlowStep.Result)
                verify(exactly = 0) { analyticsEventHandler.send(ofType<EarnFlowAnalyticsEvent>()) }
                coVerify(exactly = 0) { session.screenAnalytics(action = any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN an action that is neither deposit nor withdraw WHEN fee speed changes and signing fails THEN no flow event is sent`() =
            runTest {
                // Arrange
                every { session.execute(plan = any()) } returns flowOf(
                    EarnSigningStep.Failed(SendTransactionError.BlockchainSdkError(code = 1, message = "rejected")),
                )
                coEvery { getWalletMetaInfoUseCase.invoke(userWalletId = testUserWalletId) } returns
                    IllegalStateException().left()
                val dialog = slot<UiMessage>()
                every { messageSender.send(capture(dialog)) } returns Unit
                val model = createModel(action = TestClaim)
                model.onFeeResult(choosableFeeContent(isFast = false))
                advanceUntilIdle()

                // Act
                model.onFeeResult(choosableFeeContent(isFast = true))
                advanceUntilIdle()
                model.onConfirmClick()
                advanceUntilIdle()
                (dialog.captured as DialogMessage).firstAction.onClick()
                (dialog.captured as DialogMessage).secondAction?.onClick?.invoke()
                advanceUntilIdle()

                // Assert
                verify(exactly = 0) { analyticsEventHandler.send(ofType<EarnFlowAnalyticsEvent>()) }
                model.onDestroy()
            }

        @ParameterizedTest
        @MethodSource("provideCurrencyModels")
        fun `GIVEN the amount step WHEN currency toggled THEN currency toggle carries the currency and the screen`(
            model: CurrencyToggleModel,
        ) = runTest {
            // Arrange
            val flow = createModel(action = model.action)
            advanceUntilIdle()

            // Act
            flow.onCurrencyChangeClick(model.isFiat)

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    EarnFlowAnalyticsEvent.CurrencyToggle(
                        category = "ETH Lending",
                        currency = model.expectedCurrency,
                        screen = model.expectedScreen,
                    ),
                )
            }
            flow.onDestroy()
        }

        @ParameterizedTest
        @MethodSource("provideAmountScreenModels")
        fun `GIVEN the amount step WHEN Max pressed THEN max is sent for the amount screen`(
            model: AmountScreenModel,
        ) = runTest {
            // Arrange
            val flow = createModel(action = model.action)
            advanceUntilIdle()

            // Act
            flow.onMaxValueClick()

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    EarnFlowAnalyticsEvent.Max(category = "ETH Lending", screen = model.expectedScreen),
                )
            }
            flow.onDestroy()
        }

        @ParameterizedTest
        @MethodSource("provideAmountScreenModels")
        fun `GIVEN the amount step WHEN 50 percent pressed THEN percent is sent for the amount screen`(
            model: AmountScreenModel,
        ) = runTest {
            // Arrange
            val flow = createModel(action = model.action)
            advanceUntilIdle()

            // Act
            flow.onPercentClick(percent = 50)

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    EarnFlowAnalyticsEvent.Percent(
                        category = "ETH Lending",
                        percent = 50,
                        screen = model.expectedScreen,
                    ),
                )
            }
            flow.onDestroy()
        }

        @Test
        fun `GIVEN the currency status is not loaded WHEN currency pressed THEN no toggle event is sent`() = runTest {
            // Arrange
            every { singleAccountStatusListSupplier.invoke(testUserWalletId) } returns MutableSharedFlow()
            val flow = createModel(action = Deposit())
            advanceUntilIdle()

            // Act
            flow.onCurrencyChangeClick(isFiat = true)

            // Assert
            verify(exactly = 0) { analyticsEventHandler.send(ofType<EarnFlowAnalyticsEvent.CurrencyToggle>()) }
            flow.onDestroy()
        }

        @Test
        fun `GIVEN no fee yet WHEN Max and percent pressed THEN both taps are sent while they wait for the fee`() =
            runTest {
                // Arrange
                val flow = createModel(action = Deposit())
                advanceUntilIdle()

                // Act
                flow.onMaxValueClick()
                flow.onPercentClick(percent = 50)

                // Assert
                assertThat(flow.state.value.pendingSharePercent).isEqualTo(50)
                val max = EarnFlowAnalyticsEvent.Max(category = "ETH Lending", screen = EarnFlowScreen.Deposit)
                val percent = EarnFlowAnalyticsEvent.Percent(
                    category = "ETH Lending",
                    percent = 50,
                    screen = EarnFlowScreen.Deposit,
                )
                verify(exactly = 1) { analyticsEventHandler.send(max) }
                verify(exactly = 1) { analyticsEventHandler.send(percent) }
                flow.onDestroy()
            }

        @Test
        fun `GIVEN Normal fee loaded WHEN Max selected THEN fee speed Max is sent once`() = runTest {
            // Arrange
            val flow = createModel(action = Deposit())
            advanceUntilIdle()
            flow.onFeeResult(choosableFeeContent(isFast = false))
            advanceUntilIdle()

            // Act
            flow.onFeeResult(choosableFeeContent(isFast = true))
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    EarnFlowAnalyticsEvent.FeeSpeed(
                        category = "ETH Lending",
                        feeType = AnalyticsParam.FeeType.Max,
                        screen = EarnFlowScreen.Deposit,
                    ),
                )
            }
            verify(exactly = 1) { analyticsEventHandler.send(ofType<EarnFlowAnalyticsEvent.FeeSpeed>()) }
            flow.onDestroy()
        }

        @Test
        fun `GIVEN the first fee load WHEN it arrives THEN no fee speed is sent`() = runTest {
            // Arrange
            val flow = createModel(action = Deposit())
            advanceUntilIdle()

            // Act
            flow.onFeeResult(choosableFeeContent(isFast = false))
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { analyticsEventHandler.send(ofType<EarnFlowAnalyticsEvent.FeeSpeed>()) }
            flow.onDestroy()
        }

        @Test
        fun `GIVEN Max selected WHEN the fee reloads through Loading back to Max THEN no second fee speed is sent`() =
            runTest {
                // Arrange
                val flow = createModel(action = Deposit())
                advanceUntilIdle()
                flow.onFeeResult(choosableFeeContent(isFast = false))
                flow.onFeeResult(choosableFeeContent(isFast = true))
                advanceUntilIdle()

                // Act
                flow.onFeeResult(FeeSelectorUM.Loading)
                flow.onFeeResult(choosableFeeContent(isFast = true))
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) { analyticsEventHandler.send(ofType<EarnFlowAnalyticsEvent.FeeSpeed>()) }
                flow.onDestroy()
            }

        @Test
        fun `GIVEN Max selected WHEN the fee reloads through Loading as Normal THEN fee speed Normal is sent`() =
            runTest {
                // Arrange
                val flow = createModel(action = Deposit())
                advanceUntilIdle()
                flow.onFeeResult(choosableFeeContent(isFast = true))
                advanceUntilIdle()

                // Act
                flow.onFeeResult(FeeSelectorUM.Loading)
                flow.onFeeResult(choosableFeeContent(isFast = false))
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) {
                    analyticsEventHandler.send(
                        EarnFlowAnalyticsEvent.FeeSpeed(
                            category = "ETH Lending",
                            feeType = AnalyticsParam.FeeType.Normal,
                            screen = EarnFlowScreen.Deposit,
                        ),
                    )
                }
                flow.onDestroy()
            }

        @Test
        fun `GIVEN a fee with a disabled primary button WHEN its type differs THEN no fee speed is sent`() = runTest {
            // Arrange
            val flow = createModel(action = Deposit())
            advanceUntilIdle()
            flow.onFeeResult(choosableFeeContent(isFast = false))
            advanceUntilIdle()

            // Act
            flow.onFeeResult(choosableFeeContent(isFast = true).copy(isPrimaryButtonEnabled = false))
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { analyticsEventHandler.send(ofType<EarnFlowAnalyticsEvent.FeeSpeed>()) }
            flow.onDestroy()
        }

        @Test
        fun `GIVEN the confirm step WHEN the fee speed changes THEN fee speed carries the Confirm screen`() = runTest {
            // Arrange
            val flow = createModel(action = Deposit())
            advanceUntilIdle()
            flow.onAmountValueChange("0.5")
            flow.onFeeResult(choosableFeeContent(isFast = false))
            advanceUntilIdle()
            flow.onSubmit()
            advanceUntilIdle()

            // Act
            flow.onFeeResult(choosableFeeContent(isFast = true))
            advanceUntilIdle()

            // Assert
            assertThat(flow.state.value.step).isEqualTo(EarnFlowStep.Confirm)
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    EarnFlowAnalyticsEvent.FeeSpeed(
                        category = "ETH Lending",
                        feeType = AnalyticsParam.FeeType.Max,
                        screen = EarnFlowScreen.Confirm,
                    ),
                )
            }
            flow.onDestroy()
        }

        @ParameterizedTest
        @MethodSource("provideSigningErrorModels")
        fun `GIVEN signing failed WHEN the dialog shows THEN error popup carries the error description`(
            model: SigningErrorModel,
        ) = runTest {
            // Arrange
            every { session.execute(plan = any()) } returns flowOf(EarnSigningStep.Failed(model.error))
            val flow = createConfirmedModel()

            // Act
            flow.onConfirmClick()
            advanceUntilIdle()

            // Assert
            verifyOrder {
                analyticsEventHandler.send(
                    EarnFlowAnalyticsEvent.ErrorPopup(
                        category = "ETH Lending",
                        type = EarnTxType.Deposit,
                        errorCode = model.expectedErrorCode,
                    ),
                )
                messageSender.send(any())
            }
            flow.onDestroy()
        }

        @Test
        fun `GIVEN withdraw signing failed WHEN the dialog shows THEN error popup carries the Withdraw type`() =
            runTest {
                // Arrange
                every { session.execute(plan = any()) } returns flowOf(
                    EarnSigningStep.Failed(
                        SendTransactionError.TangemSdkError(code = 50001, messageRes = 0, args = emptyList()),
                    ),
                )
                val flow = createModel(action = Withdraw())
                advanceUntilIdle()
                flow.onAmountValueChange("0.5")
                flow.onFeeResult(testFeeContent("0.00033"))
                advanceUntilIdle()
                flow.onSubmit()
                advanceUntilIdle()

                // Act
                flow.onConfirmClick()
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) {
                    analyticsEventHandler.send(
                        EarnFlowAnalyticsEvent.ErrorPopup(
                            category = "ETH Lending",
                            type = EarnTxType.Withdraw,
                            errorCode = "TangemSdkError: code 50001",
                        ),
                    )
                }
                flow.onDestroy()
            }

        @Test
        fun `GIVEN signing failed with an error without a dialog WHEN it fails THEN no error popup is sent`() = runTest {
            // Arrange
            every { session.execute(plan = any()) } returns flowOf(
                EarnSigningStep.Failed(SendTransactionError.SolanaGasless(SolanaGaslessError.Disabled)),
            )
            val flow = createConfirmedModel()

            // Act
            flow.onConfirmClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { messageSender.send(any()) }
            verify(exactly = 0) { analyticsEventHandler.send(ofType<EarnFlowAnalyticsEvent.ErrorPopup>()) }
            flow.onDestroy()
        }

        @Test
        fun `GIVEN the user cancelled on the card WHEN signing fails THEN no error popup is sent`() = runTest {
            // Arrange
            every { session.execute(plan = any()) } returns flowOf(
                EarnSigningStep.Failed(SendTransactionError.UserCancelledError),
            )
            val flow = createConfirmedModel()

            // Act
            flow.onConfirmClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { analyticsEventHandler.send(ofType<EarnFlowAnalyticsEvent.ErrorPopup>()) }
            verify(exactly = 0) { analyticsEventHandler.send(ofType<EarnFlowAnalyticsEvent.ErrorPopupButton>()) }
            flow.onDestroy()
        }

        @Test
        fun `GIVEN the error dialog WHEN Support pressed THEN the Support button event is sent`() = runTest {
            // Arrange
            every { session.execute(plan = any()) } returns flowOf(
                EarnSigningStep.Failed(SendTransactionError.BlockchainSdkError(code = 1, message = "rejected")),
            )
            coEvery { getWalletMetaInfoUseCase.invoke(userWalletId = testUserWalletId) } returns
                IllegalStateException().left()
            val dialog = slot<UiMessage>()
            every { messageSender.send(capture(dialog)) } returns Unit
            val flow = createConfirmedModel()
            flow.onConfirmClick()
            advanceUntilIdle()
            verify(exactly = 0) { analyticsEventHandler.send(ofType<EarnFlowAnalyticsEvent.ErrorPopupButton>()) }

            // Act
            (dialog.captured as DialogMessage).firstAction.onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    EarnFlowAnalyticsEvent.ErrorPopupButton(
                        category = "ETH Lending",
                        type = EarnTxType.Deposit,
                        action = EarnErrorPopupAction.Support,
                    ),
                )
            }
            verify(exactly = 1) { analyticsEventHandler.send(ofType<EarnFlowAnalyticsEvent.ErrorPopupButton>()) }
            flow.onDestroy()
        }

        @Test
        fun `GIVEN the error dialog WHEN Close pressed THEN the Close button event is sent`() = runTest {
            // Arrange
            every { session.execute(plan = any()) } returns flowOf(
                EarnSigningStep.Failed(SendTransactionError.BlockchainSdkError(code = 1, message = "rejected")),
            )
            val dialog = slot<UiMessage>()
            every { messageSender.send(capture(dialog)) } returns Unit
            val flow = createConfirmedModel()
            flow.onConfirmClick()
            advanceUntilIdle()

            // Act
            (dialog.captured as DialogMessage).secondAction?.onClick?.invoke()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    EarnFlowAnalyticsEvent.ErrorPopupButton(
                        category = "ETH Lending",
                        type = EarnTxType.Deposit,
                        action = EarnErrorPopupAction.Close,
                    ),
                )
            }
            verify(exactly = 1) { analyticsEventHandler.send(ofType<EarnFlowAnalyticsEvent.ErrorPopupButton>()) }
            flow.onDestroy()
        }

        private fun provideTestModels() = listOf(
            LiquidityModel(
                position = BigDecimal("2"),
                availableToWithdraw = BigDecimal("1"),
                isLiquidityKnown = true,
                expectedCapped = true,
            ),
            LiquidityModel(
                position = BigDecimal("2"),
                availableToWithdraw = BigDecimal("2"),
                isLiquidityKnown = true,
                expectedCapped = false,
            ),
            LiquidityModel(
                position = BigDecimal("2"),
                availableToWithdraw = BigDecimal("1"),
                isLiquidityKnown = false,
                expectedCapped = null,
            ),
            LiquidityModel(
                position = BigDecimal("2"),
                availableToWithdraw = null,
                isLiquidityKnown = true,
                expectedCapped = null,
            ),
        )

        private fun provideResultExitModels() = listOf(
            ResultExitModel(exit = EarnFlowExit.ToYield, expectedDestination = EarnResultDestination.Yield),
            ResultExitModel(exit = EarnFlowExit.ToToken, expectedDestination = EarnResultDestination.Token),
            ResultExitModel(exit = EarnFlowExit.Done, expectedDestination = null),
        )

        private fun provideCurrencyModels() = listOf(
            CurrencyToggleModel(
                action = Deposit(),
                isFiat = true,
                expectedCurrency = EarnDisplayCurrency.Fiat,
                expectedScreen = EarnFlowScreen.Deposit,
            ),
            CurrencyToggleModel(
                action = Deposit(),
                isFiat = false,
                expectedCurrency = EarnDisplayCurrency.Crypto,
                expectedScreen = EarnFlowScreen.Deposit,
            ),
            CurrencyToggleModel(
                action = Withdraw(),
                isFiat = true,
                expectedCurrency = EarnDisplayCurrency.Fiat,
                expectedScreen = EarnFlowScreen.Withdraw,
            ),
        )

        private fun provideAmountScreenModels() = listOf(
            AmountScreenModel(action = Deposit(), expectedScreen = EarnFlowScreen.Deposit),
            AmountScreenModel(action = Withdraw(), expectedScreen = EarnFlowScreen.Withdraw),
        )

        private fun provideSigningErrorModels() = listOf(
            SigningErrorModel(
                error = SendTransactionError.TangemSdkError(code = 50001, messageRes = 0, args = emptyList()),
                expectedErrorCode = "TangemSdkError: code 50001",
            ),
            SigningErrorModel(
                error = SendTransactionError.BlockchainSdkError(code = 1, message = "rejected"),
                expectedErrorCode = "BlockchainSdkError: 1",
            ),
            SigningErrorModel(
                error = SendTransactionError.NetworkError(message = "timeout", code = "504"),
                expectedErrorCode = "NetworkError: 504",
            ),
            SigningErrorModel(
                error = SendTransactionError.UnknownError(),
                expectedErrorCode = "UnknownError",
            ),
        )

        private fun choosableFeeContent(isFast: Boolean): FeeSelectorUM.Content {
            val slow = testFee("0.0001")
            val market = testFee("0.0002")
            val fast = testFee("0.0003")
            val selected = if (isFast) FeeItem.Fast(fast) else FeeItem.Market(market)
            return testFeeContent("0.0003").copy(
                fees = TransactionFee.Choosable(minimum = slow, normal = market, priority = fast),
                feeItems = persistentListOf(FeeItem.Slow(slow), FeeItem.Market(market), FeeItem.Fast(fast)),
                selectedFeeItem = selected,
            )
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

    private val usdcStatus = MockYieldSupply.loadedStatus(currency = MockYieldSupply.token(symbol = "USDC"))
    private val usdtStatus = MockYieldSupply.loadedStatus(
        currency = MockYieldSupply.token(contractAddress = "0xdac17f958d2ee523a2206206994597c13d831ec7", symbol = "USDT"),
    )

    data class FeeTokenPickModel(
        val name: String,
        val results: List<FeeSelectorUM>,
        val requested: CryptoCurrencyStatus?,
        val expected: CryptoCurrencyStatus?,
    ) {
        override fun toString(): String = name
    }

    private fun TestScope.createModel(
        action: EarnFlowAction = Deposit(),
        factories: Map<String, EarnSession.Factory> = mapOf(
            testFlowProduct.productId.value to EarnSession.Factory { session },
        ),
        flowProduct: EarnFlowProduct = testFlowProduct,
        source: EarnEntrySource = EarnEntrySource.TokenPage,
    ): EarnFlowModel = EarnFlowModel(
        dispatchers = createTestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(
            testParams(action, callback).copy(flowProduct = flowProduct, source = source),
        ),
        sessionFactories = EarnSessionFactories(factories),
        statuses = EarnFlowStatuses(
            singleAccountStatusListSupplier = singleAccountStatusListSupplier,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            getFeePaidCryptoCurrencyStatusSyncUseCase = getFeePaidCryptoCurrencyStatusSyncUseCase,
            isAmountSubtractAvailableUseCase = isAmountSubtractAvailableUseCase,
        ),
        wallet = EarnFlowWallet(
            getUserWalletUseCase = getUserWalletUseCase,
            getAccountCurrencyStatusUseCase = getAccountCurrencyStatusUseCase,
            isAccountsModeEnabledUseCase = isAccountsModeEnabledUseCase,
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
        links = EarnFlowLinks(
            urlOpener = urlOpener,
            shareManager = shareManager,
            getExplorerTransactionUrlUseCase = getExplorerTransactionUrlUseCase,
            blogUrlBuilder = blogUrlBuilder,
        ),
        analyticsEventHandler = analyticsEventHandler,
        usdValuation = usdValuation,
    )

    data class DeductionModel(val entered: String?, val fee: String, val expected: String)

    data class PercentModel(val percent: Int, val expected: String)

    data class PositionAmountModel(val entered: String?, val expected: EarnAmount)

    data class LiquidityModel(
        val position: BigDecimal,
        val availableToWithdraw: BigDecimal?,
        val isLiquidityKnown: Boolean,
        val expectedCapped: Boolean?,
    )

    data class ResultExitModel(val exit: EarnFlowExit, val expectedDestination: EarnResultDestination?)

    data class CurrencyToggleModel(
        val action: EarnFlowAction,
        val isFiat: Boolean,
        val expectedCurrency: EarnDisplayCurrency,
        val expectedScreen: EarnFlowScreen,
    )

    data class AmountScreenModel(val action: EarnFlowAction, val expectedScreen: EarnFlowScreen)

    data class SigningErrorModel(val error: SendTransactionError, val expectedErrorCode: String)
}

private const val MAX_AMOUNT_DELAY_MS = 1_000L
private const val TEST_EXPLORER_URL = "https://etherscan.io/tx/0xhash"
private const val TEST_LEARN_MORE_URL = "https://tangem.com/blog/yield-mode"

private val testHowItWorks = EarnHowItWorks(
    title = stringReference("How it works"),
    entries = listOf(EarnTextBlock(title = stringReference("Supply"), description = stringReference("Lend"))),
    learnMore = TangemBlogUrlBuilder.Post.HowYieldModeWorks,
)