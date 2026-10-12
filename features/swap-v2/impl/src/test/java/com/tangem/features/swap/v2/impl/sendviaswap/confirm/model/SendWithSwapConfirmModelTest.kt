package com.tangem.features.swap.v2.impl.sendviaswap.confirm.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount as BlockchainAmount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.backup.BackupErrorWarningSender
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.account.status.usecase.GetAccountCurrencyByAddressUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.quotes.IsHighNetworkFeeUseCase
import com.tangem.domain.settings.IsSendTapHelpEnabledUseCase
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.domain.swap.models.SwapRateMode
import com.tangem.domain.tokens.IsAmountSubtractAvailableUseCase
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.EstimateFeeUseCase
import com.tangem.domain.transaction.usecase.gasless.EstimateFeeForGaslessTxUseCase
import com.tangem.domain.transaction.usecase.gasless.EstimateFeeForTokenUseCase
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents.SendScreenSource
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationTextFieldUM
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.api.subcomponents.notifications.SendNotificationsUpdateListener
import com.tangem.features.send.api.subcomponents.notifications.SendNotificationsUpdateTrigger
import com.tangem.features.swap.v2.api.subcomponents.SwapAmountUpdateTrigger
import com.tangem.features.swap.v2.impl.amount.SwapAmountReduceTrigger
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.SwapAlertFactory
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.notifications.SwapNotificationsUpdateListener
import com.tangem.features.swap.v2.impl.notifications.SwapNotificationsUpdateTrigger
import com.tangem.features.swap.v2.impl.sendviaswap.SendWithSwapRoute
import com.tangem.features.swap.v2.impl.sendviaswap.confirm.SendWithSwapConfirmComponent
import com.tangem.features.swap.v2.impl.sendviaswap.entity.SendWithSwapUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * The model requires a [SwapAmountUM.Content] with a secondary currency at construction
 * (`secondaryCurrency` is `requireNotNull`), so unlike the parent's `initialState()` the default
 * state carries a content amount rather than `SwapAmountUM.Empty`.
 *
 * [SwapTransactionSender] is replaced by a relaxed mock; it is covered by [SwapTransactionSenderTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SendWithSwapConfirmModelTest {

    private val router: Router = mockk(relaxed = true)
    private val isSendTapHelpEnabledUseCase: IsSendTapHelpEnabledUseCase = mockk(relaxed = true)
    private val estimateFeeUseCase: EstimateFeeUseCase = mockk(relaxed = true)
    private val estimateFeeForTokenUseCase: EstimateFeeForTokenUseCase = mockk(relaxed = true)
    private val estimateFeeForGaslessTxUseCase: EstimateFeeForGaslessTxUseCase = mockk(relaxed = true)
    private val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase = mockk(relaxed = true)
    private val isHighNetworkFeeUseCase: IsHighNetworkFeeUseCase = mockk(relaxed = true)
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase = mockk(relaxed = true)
    private val sendNotificationsUpdateTrigger: SendNotificationsUpdateTrigger = mockk(relaxed = true)
    private val swapNotificationsUpdateTrigger: SwapNotificationsUpdateTrigger = mockk(relaxed = true)
    private val sendNotificationsUpdateListener: SendNotificationsUpdateListener = mockk(relaxed = true)
    private val swapNotificationsUpdateListener: SwapNotificationsUpdateListener = mockk(relaxed = true)
    private val getAccountCurrencyByAddressUseCase: GetAccountCurrencyByAddressUseCase = mockk(relaxed = true)
    private val backupErrorWarningSender: BackupErrorWarningSender = mockk(relaxed = true)
    private val swapAmountReduceTrigger: SwapAmountReduceTrigger = mockk(relaxed = true)
    private val swapAmountUpdateTrigger: SwapAmountUpdateTrigger = mockk(relaxed = true)
    private val feeSelectorReloadTrigger: FeeSelectorReloadTrigger = mockk(relaxed = true)
    private val swapAlertFactory: SwapAlertFactory = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val swapTransactionSenderFactory: SwapTransactionSender.Factory = mockk {
        every { create(any()) } returns mockk(relaxed = true)
    }

    private val userWallet: UserWallet = mockk(relaxed = true) {
        every { walletId } returns UserWalletId(stringValue = "0123456789")
    }

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val primaryCurrencyStatus = CryptoCurrencyStatus(
        currency = currencyFactory.createCoin(Blockchain.Ethereum),
        value = CryptoCurrencyStatus.Loading,
    )
    private val secondaryCurrencyStatus = CryptoCurrencyStatus(
        currency = currencyFactory.createCoin(Blockchain.Bitcoin),
        value = CryptoCurrencyStatus.Loading,
    )
    private val feeTokenCurrencyStatus = CryptoCurrencyStatus(
        currency = currencyFactory.createToken(Blockchain.Ethereum, symbol = "USDT"),
        value = CryptoCurrencyStatus.Loading,
    )

    private val defaultSendWithSwapUM = SendWithSwapUM(
        amountUM = contentAmountUM(),
        destinationUM = DestinationUM.Empty(),
        feeSelectorUM = FeeSelectorUM.Loading,
        confirmUM = ConfirmUM.Empty,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            isSendTapHelpEnabledUseCase,
            estimateFeeUseCase,
            estimateFeeForTokenUseCase,
            estimateFeeForGaslessTxUseCase,
            isAmountSubtractAvailableUseCase,
            isHighNetworkFeeUseCase,
            getExplorerTransactionUrlUseCase,
            sendNotificationsUpdateTrigger,
            swapNotificationsUpdateTrigger,
            sendNotificationsUpdateListener,
            swapNotificationsUpdateListener,
            getAccountCurrencyByAddressUseCase,
            backupErrorWarningSender,
            swapAmountReduceTrigger,
            swapAmountUpdateTrigger,
            feeSelectorReloadTrigger,
            swapAlertFactory,
            analyticsEventHandler,
        )
        stubInitCollaborators()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UpdateEditedState {

        @Test
        fun `GIVEN confirm-local state WHEN updateEditedState THEN only amount and destination replaced`() =
            runTest {
                // Arrange
                val model = createModel(
                    testScope = this,
                    sendWithSwapUM = defaultSendWithSwapUM.copy(
                        destinationUM = contentDestinationUM(address = "bc1-original"),
                        feeSelectorUM = FeeSelectorUM.Error(error = GetFeeError.UnknownError),
                        confirmUM = contentConfirmUM(),
                    ),
                )
                advanceUntilIdle()
                val stateBeforeEdit = model.uiState.value
                val parentState = SendWithSwapUM(
                    amountUM = contentAmountUM(enteredFromAmount = BigDecimal("2.5")),
                    destinationUM = contentDestinationUM(address = "bc1-edited"),
                    feeSelectorUM = FeeSelectorUM.Loading,
                    confirmUM = ConfirmUM.Empty,
                )

                // Act
                model.updateEditedState(parentState)
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value).isEqualTo(
                    stateBeforeEdit.copy(
                        amountUM = parentState.amountUM,
                        destinationUM = parentState.destinationUM,
                    ),
                )
                model.onDestroy()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BlockResults {

        @Test
        fun `GIVEN model created WHEN onAmountResult THEN amount merged into state`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()
            val stateBefore = model.uiState.value
            val amountUM = contentAmountUM(enteredFromAmount = BigDecimal("3"))

            // Act
            model.onAmountResult(amountUM)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isEqualTo(stateBefore.copy(amountUM = amountUM))
            model.onDestroy()
        }

        @Test
        fun `GIVEN model created WHEN onDestinationResult THEN destination merged into state`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()
            val stateBefore = model.uiState.value
            val destinationUM = contentDestinationUM(address = "bc1-new")

            // Act
            model.onDestinationResult(destinationUM)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isEqualTo(stateBefore.copy(destinationUM = destinationUM))
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ShowEdit {

        @Test
        fun `GIVEN confirm screen WHEN showEditAmount THEN reopen sent and amount pushed in edit mode`() =
            runTest {
                // Arrange
                val model = createModel(testScope = this)

                // Act
                model.showEditAmount()

                // Assert
                verify(exactly = 1) {
                    analyticsEventHandler.send(
                        CommonSendAnalyticEvents.ScreenReopened(
                            categoryName = ANALYTICS_CATEGORY,
                            source = SendScreenSource.Amount,
                        ),
                    )
                }
                verify(exactly = 1) { router.push(SendWithSwapRoute.Amount(isEditMode = true), any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN confirm screen WHEN showEditDestination THEN reopen sent and destination pushed`() =
            runTest {
                // Arrange
                val model = createModel(testScope = this)

                // Act
                model.showEditDestination()

                // Assert
                verify(exactly = 1) {
                    analyticsEventHandler.send(
                        CommonSendAnalyticEvents.ScreenReopened(
                            categoryName = ANALYTICS_CATEGORY,
                            source = SendScreenSource.Address,
                        ),
                    )
                }
                verify(exactly = 1) { router.push(SendWithSwapRoute.Destination(isEditMode = true), any()) }
                model.onDestroy()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class LoadFee {

        @Test
        fun `GIVEN CEX quote WHEN loadFee THEN returns estimated fee for primary currency`() = runTest {
            // Arrange
            coEvery {
                estimateFeeUseCase(amount = any(), userWallet = any(), cryptoCurrencyStatus = any())
            } returns commonTransactionFee.right()
            val model = createModel(testScope = this)

            // Act
            val actual = model.loadFee()

            // Assert
            assertThat(actual).isEqualTo(commonTransactionFee.right())
            coVerify(exactly = 1) {
                estimateFeeUseCase(
                    amount = ENTERED_FROM_AMOUNT,
                    userWallet = userWallet,
                    cryptoCurrencyStatus = primaryCurrencyStatus,
                )
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN CEX quote and estimation fails WHEN loadFee THEN returns estimation error`() = runTest {
            // Arrange
            coEvery {
                estimateFeeUseCase(amount = any(), userWallet = any(), cryptoCurrencyStatus = any())
            } returns GetFeeError.BlockchainErrors.KaspaZeroUtxo.left()
            val model = createModel(testScope = this)

            // Act
            val actual = model.loadFee()

            // Assert
            assertThat(actual).isEqualTo(GetFeeError.BlockchainErrors.KaspaZeroUtxo.left())
            model.onDestroy()
        }

        @Test
        fun `GIVEN quote not loaded WHEN loadFee THEN returns unknown error without estimating`() = runTest {
            // Arrange
            val model = createModel(
                testScope = this,
                sendWithSwapUM = defaultSendWithSwapUM.copy(
                    amountUM = contentAmountUM(selectedQuote = SwapQuoteUM.Loading),
                ),
            )

            // Act
            val actual = model.loadFee()

            // Assert
            assertThat(actual).isEqualTo(GetFeeError.UnknownError.left())
            coVerify(exactly = 0) {
                estimateFeeUseCase(amount = any(), userWallet = any(), cryptoCurrencyStatus = any())
            }
            model.onDestroy()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN non-CEX provider WHEN loadFee THEN returns data error without estimating`(
            case: NonCexProviderTypeCase,
        ) = runTest {
            // Arrange
            val model = createModel(testScope = this, sendWithSwapUM = stateWithProvider(case.providerType))

            // Act
            val actual = model.loadFee()

            // Assert
            assertUnsupportedProviderError(actual = actual.leftOrNull(), providerType = case.providerType)
            coVerify(exactly = 0) {
                estimateFeeUseCase(amount = any(), userWallet = any(), cryptoCurrencyStatus = any())
            }
            model.onDestroy()
        }

        private fun provideTestModels() = nonCexProviderTypeCases
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class LoadFeeExtended {

        @Test
        fun `GIVEN CEX quote and fee token WHEN loadFeeExtended THEN estimates fee in that token`() = runTest {
            // Arrange
            coEvery {
                estimateFeeForTokenUseCase(
                    userWallet = any(),
                    feeTokenCurrencyStatus = any(),
                    sendingTokenCurrencyStatus = any(),
                    amount = any(),
                )
            } returns extendedFee.right()
            val model = createModel(testScope = this)

            // Act
            val actual = model.loadFeeExtended(maybeToken = feeTokenCurrencyStatus)

            // Assert
            assertThat(actual).isEqualTo(extendedFee.right())
            coVerify(exactly = 1) {
                estimateFeeForTokenUseCase(
                    userWallet = userWallet,
                    feeTokenCurrencyStatus = feeTokenCurrencyStatus,
                    sendingTokenCurrencyStatus = primaryCurrencyStatus,
                    amount = ENTERED_FROM_AMOUNT,
                )
            }
            coVerify(exactly = 0) {
                estimateFeeForGaslessTxUseCase(userWallet = any(), amount = any(), sendingTokenCurrencyStatus = any())
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN CEX quote and no fee token WHEN loadFeeExtended THEN estimates gasless fee`() = runTest {
            // Arrange
            coEvery {
                estimateFeeForGaslessTxUseCase(userWallet = any(), amount = any(), sendingTokenCurrencyStatus = any())
            } returns extendedFee.right()
            val model = createModel(testScope = this)

            // Act
            val actual = model.loadFeeExtended(maybeToken = null)

            // Assert
            assertThat(actual).isEqualTo(extendedFee.right())
            coVerify(exactly = 1) {
                estimateFeeForGaslessTxUseCase(
                    userWallet = userWallet,
                    amount = ENTERED_FROM_AMOUNT,
                    sendingTokenCurrencyStatus = primaryCurrencyStatus,
                )
            }
            coVerify(exactly = 0) {
                estimateFeeForTokenUseCase(
                    userWallet = any(),
                    feeTokenCurrencyStatus = any(),
                    sendingTokenCurrencyStatus = any(),
                    amount = any(),
                )
            }
            model.onDestroy()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN non-CEX provider WHEN loadFeeExtended THEN returns data error without estimating`(
            case: NonCexProviderTypeCase,
        ) = runTest {
            // Arrange
            val model = createModel(testScope = this, sendWithSwapUM = stateWithProvider(case.providerType))

            // Act
            val actual = model.loadFeeExtended(maybeToken = feeTokenCurrencyStatus)

            // Assert
            assertUnsupportedProviderError(actual = actual.leftOrNull(), providerType = case.providerType)
            coVerify(exactly = 0) {
                estimateFeeForTokenUseCase(
                    userWallet = any(),
                    feeTokenCurrencyStatus = any(),
                    sendingTokenCurrencyStatus = any(),
                    amount = any(),
                )
            }
            coVerify(exactly = 0) {
                estimateFeeForGaslessTxUseCase(userWallet = any(), amount = any(), sendingTokenCurrencyStatus = any())
            }
            model.onDestroy()
        }

        private fun provideTestModels() = nonCexProviderTypeCases
    }

    private val commonTransactionFee = TransactionFee.Single(
        normal = Fee.Common(
            amount = BlockchainAmount(currencySymbol = "ETH", value = BigDecimal("0.001"), decimals = 18),
        ),
    )

    private val extendedFee = TransactionFeeExtended(
        transactionFee = commonTransactionFee,
        feeTokenId = feeTokenCurrencyStatus.currency.id,
    )

    private fun stateWithProvider(providerType: ExpressProviderType): SendWithSwapUM = defaultSendWithSwapUM.copy(
        amountUM = contentAmountUM(
            selectedQuote = contentQuote(
                fromAmount = ENTERED_FROM_AMOUNT,
                toAmount = BigDecimal.ONE,
                provider = expressProvider(type = providerType),
            ),
        ),
    )

    /** The cause is a fresh `IllegalStateException`, so it can only be matched by type and message. */
    private fun assertUnsupportedProviderError(actual: GetFeeError?, providerType: ExpressProviderType) {
        assertThat(actual).isInstanceOf(GetFeeError.DataError::class.java)
        assertThat((actual as GetFeeError.DataError).cause)
            .hasMessageThat()
            .isEqualTo("Provider $providerType is not supported in Send With Swap")
    }

    /** `init {}` folds these `Either`/`Flow` results; relaxed mocks would hand back unusable `Either` stubs. */
    private fun stubInitCollaborators() {
        every { isSendTapHelpEnabledUseCase() } returns flowOf(true).right()
        coEvery { isSendTapHelpEnabledUseCase.invokeSync() } returns true.right()
        coEvery {
            isAmountSubtractAvailableUseCase(userWalletId = any(), currency = any(), maybeGaslessFee = any())
        } returns false.right()
        every { sendNotificationsUpdateListener.hasErrorFlow } returns flowOf(false)
        every { swapNotificationsUpdateListener.hasErrorFlow } returns flowOf(false)
    }

    private fun createModel(
        testScope: TestScope,
        sendWithSwapUM: SendWithSwapUM = defaultSendWithSwapUM,
        callback: SendWithSwapConfirmComponent.ModelCallback = mockk(relaxed = true),
    ): SendWithSwapConfirmModel {
        val params = SendWithSwapConfirmComponent.Params(
            sendWithSwapUM = sendWithSwapUM,
            analyticsCategoryName = ANALYTICS_CATEGORY,
            analyticsSendSource = CommonSendAnalyticEvents.CommonSendSource.SendWithSwap,
            userWallet = userWallet,
            appCurrency = AppCurrency.Default,
            swapDirection = SwapDirection.Direct,
            isBalanceHidingFlow = MutableStateFlow(false),
            primaryCryptoCurrencyStatusFlow = MutableStateFlow(primaryCurrencyStatus),
            primaryFeePaidCurrencyStatusFlow = MutableStateFlow(primaryCurrencyStatus),
            accountFlow = MutableStateFlow<Account?>(null),
            isAccountModeFlow = MutableStateFlow(false),
            callback = callback,
        )
        return SendWithSwapConfirmModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            router = router,
            isSendTapHelpEnabledUseCase = isSendTapHelpEnabledUseCase,
            estimateFeeUseCase = estimateFeeUseCase,
            estimateFeeForTokenUseCase = estimateFeeForTokenUseCase,
            estimateFeeForGaslessTxUseCase = estimateFeeForGaslessTxUseCase,
            isAmountSubtractAvailableUseCase = isAmountSubtractAvailableUseCase,
            isHighNetworkFeeUseCase = isHighNetworkFeeUseCase,
            getExplorerTransactionUrlUseCase = getExplorerTransactionUrlUseCase,
            sendNotificationsUpdateTrigger = sendNotificationsUpdateTrigger,
            swapNotificationsUpdateTrigger = swapNotificationsUpdateTrigger,
            sendNotificationsUpdateListener = sendNotificationsUpdateListener,
            swapNotificationsUpdateListener = swapNotificationsUpdateListener,
            getAccountCurrencyByAddressUseCase = getAccountCurrencyByAddressUseCase,
            backupErrorWarningSender = backupErrorWarningSender,
            swapAmountReduceTrigger = swapAmountReduceTrigger,
            swapAmountUpdateTrigger = swapAmountUpdateTrigger,
            feeSelectorReloadTrigger = feeSelectorReloadTrigger,
            swapAlertFactory = swapAlertFactory,
            analyticsEventHandler = analyticsEventHandler,
            appScope = TestAppCoroutineScope(testScope),
            swapTransactionSenderFactory = swapTransactionSenderFactory,
            paramsContainer = MutableParamsContainer(value = params),
        )
    }

    private fun contentAmountUM(
        enteredFromAmount: BigDecimal = ENTERED_FROM_AMOUNT,
        selectedQuote: SwapQuoteUM = contentQuote(fromAmount = ENTERED_FROM_AMOUNT, toAmount = BigDecimal.ONE),
    ): SwapAmountUM.Content = SwapAmountUM.Content(
        isPrimaryButtonEnabled = true,
        swapDirection = SwapDirection.Direct,
        selectedAmountType = SwapAmountType.From,
        primaryAmount = amountField(amountType = SwapAmountType.From, cryptoValue = enteredFromAmount),
        secondaryAmount = amountField(amountType = SwapAmountType.To, cryptoValue = BigDecimal.ONE),
        primaryCryptoCurrencyStatus = primaryCurrencyStatus,
        secondaryCryptoCurrencyStatus = secondaryCurrencyStatus,
        swapRateType = ExpressRateType.Float,
        swapRateMode = SwapRateMode.FLOAT_ONLY,
        priceImpact = null,
        swapCurrencies = SwapCurrencies.EMPTY,
        swapQuotes = persistentListOf(selectedQuote),
        selectedQuote = selectedQuote,
        isShowFCAWarning = false,
        appCurrency = AppCurrency.Default,
        isShowBestRateAnimation = false,
    )

    private fun contentDestinationUM(address: String): DestinationUM.Content = DestinationUM.Content(
        isPrimaryButtonEnabled = true,
        addressTextField = DestinationTextFieldUM.RecipientAddress(
            value = address,
            keyboardOptions = mockk(relaxed = true),
            placeholder = TextReference.EMPTY,
            label = TextReference.EMPTY,
            isValuePasted = false,
        ),
        memoTextField = null,
        recent = persistentListOf(),
        wallets = persistentListOf(),
        networkName = "Bitcoin",
        isRecentHidden = false,
    )

    private fun contentConfirmUM(): ConfirmUM.Content = ConfirmUM.Content(
        isPrimaryButtonEnabled = false,
        isTransactionInProcess = false,
        isShowTapHelp = true,
        sendingFooter = TextReference.EMPTY,
        notifications = persistentListOf(),
        tosUM = null,
    )

    private fun amountField(amountType: SwapAmountType, cryptoValue: BigDecimal): SwapAmountFieldUM.Content =
        SwapAmountFieldUM.Content(
            amountType = amountType,
            amountField = amountState(cryptoValue),
            title = TextReference.EMPTY,
            subtitleLeft = TextReference.EMPTY,
            subtitleRight = TextReference.EMPTY,
            subtitleEllipsisLeft = TextEllipsis.End,
            subtitleEllipsisRight = TextEllipsis.End,
            isClickEnabled = true,
            shouldShowApproximatePrefix = false,
        )

    private fun amountState(cryptoValue: BigDecimal): AmountState.Data = AmountState.Data(
        isPrimaryButtonEnabled = true,
        accountTitleUM = mockk(relaxed = true),
        availableBalanceCrypto = TextReference.EMPTY,
        availableBalanceFiat = TextReference.EMPTY,
        tokenName = TextReference.EMPTY,
        tokenIconState = mockk(relaxed = true),
        amountTextField = AmountFieldModel(
            value = cryptoValue.toPlainString(),
            onValueChange = {},
            keyboardOptions = mockk(relaxed = true),
            keyboardActions = mockk(relaxed = true),
            cryptoAmount = Amount(currencySymbol = "ETH", value = cryptoValue, decimals = 18),
            fiatAmount = Amount(currencySymbol = "USD", value = BigDecimal.ZERO, decimals = 2),
            isFiatValue = false,
            fiatValue = "0",
            isFiatUnavailable = false,
            isValuePasted = false,
            onValuePastedTriggerDismiss = {},
            isError = false,
            isWarning = false,
            error = TextReference.EMPTY,
        ),
        appCurrency = AppCurrency.Default,
    )

    private companion object {
        const val ANALYTICS_CATEGORY = "Token / Send"
        val ENTERED_FROM_AMOUNT = BigDecimal("1.5")
    }
}