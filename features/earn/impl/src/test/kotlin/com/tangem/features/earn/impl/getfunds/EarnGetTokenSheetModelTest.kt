package com.tangem.features.earn.impl.getfunds

import arrow.core.none
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.backup.BackupErrorWarningSender
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.ManageFundsAnalyticsEvent
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.tokens.model.TokenActionsState.ActionState
import com.tangem.domain.transaction.usecase.ReceiveAddressesFactory
import com.tangem.features.earn.impl.R
import com.tangem.features.earn.impl.analytics.EarnAddFundsSource
import com.tangem.features.earn.impl.analytics.EarnPickerAnalyticsEvent
import com.tangem.features.earn.impl.overview.USER_WALLET_ID
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnGetTokenSheetModelTest {

    private val accountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val accountStatusList: AccountStatusList = mockk()
    private val actionsUseCase: GetCryptoCurrencyActionsUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val receiveAddressesFactory: ReceiveAddressesFactory = mockk()
    private val backupErrorWarningSender: BackupErrorWarningSender = mockk()
    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val userWallet: UserWallet = mockk()
    private val status: CryptoCurrencyStatus = mockk()
    private val currency: CryptoCurrency = MockCryptoCurrencyFactory().ethereum
    private val request = EarnGetTokenSheetRequest(USER_WALLET_ID, currency, EarnAddFundsSource.YieldPicker)
    private val receiveConfig: TokenReceiveConfig = mockk()

    private var dismissals = 0

    @BeforeEach
    fun setUp() {
        clearMocks(
            accountStatusListSupplier,
            accountStatusList,
            actionsUseCase,
            userWalletsListRepository,
            receiveAddressesFactory,
            backupErrorWarningSender,
            router,
            uiMessageSender,
            analyticsEventHandler,
            userWallet,
            status,
            receiveConfig,
        )
        mockkObject(CryptoCurrencyStatusOperations)
        mockkObject(TangemLogger)
        dismissals = 0
        every { accountStatusListSupplier(USER_WALLET_ID) } returns flowOf(accountStatusList)
        stubStatus(found = true)
        every { userWallet.walletId } returns USER_WALLET_ID
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        every { status.currency } returns currency
        stubNetworkSource(StatusSource.ACTUAL)
        stubActions(allActions())
        stubBackupPasses(passes = true)
        coEvery { receiveAddressesFactory.create(any(), any(), any()) } returns receiveConfig
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(CryptoCurrencyStatusOperations)
        unmockkObject(TangemLogger)
    }

    @Nested
    inner class Start {

        @Test
        fun `GIVEN the wallet actions are known WHEN the model is created THEN the sheet has the Deposit, Swap, Buy rows`() =
            runTest {
                // Arrange
                val model = createModel()

                // Act
                advanceUntilIdle()

                // Assert
                val sheet = requireNotNull(model.uiState.value)
                assertThat(sheet.rows.map { it.title }).containsExactly(
                    resourceReference(R.string.earn_get_token_deposit_title, wrappedList(currency.symbol)),
                    resourceReference(R.string.earn_get_token_swap_title, wrappedList(currency.symbol)),
                    resourceReference(R.string.earn_get_token_buy_title, wrappedList(currency.symbol)),
                ).inOrder()
                assertThat(dismissals).isEqualTo(0)
                model.onDestroy()
            }

        @Test
        fun `GIVEN the wallet has no status for the currency WHEN the model is created THEN the sheet closes itself`() =
            runTest {
                // Arrange
                stubStatus(found = false)
                val model = createModel()

                // Act
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value).isNull()
                assertThat(dismissals).isEqualTo(1)
                model.onDestroy()
            }

        @Test
        fun `GIVEN the wallet is not in the wallets list WHEN the model is created THEN the sheet closes itself`() =
            runTest {
                // Arrange
                every { userWalletsListRepository.userWallets } returns MutableStateFlow(emptyList())
                val model = createModel()

                // Act
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value).isNull()
                assertThat(dismissals).isEqualTo(1)
                model.onDestroy()
            }

        @Test
        fun `GIVEN the actions change WHEN the use case emits again THEN the sheet is rebuilt`() = runTest {
            // Arrange
            val actions = MutableStateFlow(allActions())
            every { actionsUseCase(any(), any(), any()) } returns flow {
                actions.collect { emit(TokenActionsState(USER_WALLET_ID, status, it)) }
            }
            val model = createModel()
            advanceUntilIdle()

            // Act
            actions.value = allActions(swap = ScenarioUnavailabilityReason.NotExchangeable("Ethereum"))
            advanceUntilIdle()

            // Assert
            assertThat(requireNotNull(model.uiState.value).rows[1].isEnabled).isFalse()
            model.onDestroy()
        }
    }

    @Nested
    inner class Swap {

        @Test
        fun `GIVEN the Swap row WHEN clicked THEN the swap screen opens with the currency as the target and the sheet closes`() =
            runTest {
                // Arrange
                val model = createModel()
                advanceUntilIdle()

                // Act
                requireNotNull(model.uiState.value).rows[1].onClick()

                // Assert
                verify(exactly = 1) {
                    router.push(
                        AppRoute.Swap(
                            userWalletId = USER_WALLET_ID,
                            fromCryptoCurrency = currency,
                            screenSource = "Earn",
                            fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.TO,
                        ),
                        any(),
                    )
                }
                assertThat(dismissals).isEqualTo(1)
                model.onDestroy()
            }

        @Test
        fun `GIVEN a joint account WHEN the Swap row is clicked THEN the swap screen opens without a dialog`() =
            runTest {
                // Arrange
                stubActions(allActions(swap = ScenarioUnavailabilityReason.JointAccount))
                val model = createModel()
                advanceUntilIdle()

                // Act
                requireNotNull(model.uiState.value).rows[1].onClick()

                // Assert
                verify(exactly = 0) { uiMessageSender.send(any()) }
                verify(exactly = 1) { router.push(match { it is AppRoute.Swap }, any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN Swap is unavailable WHEN its row is clicked THEN the reason dialog is sent and nothing opens`() =
            runTest {
                // Arrange
                stubActions(allActions(swap = ScenarioUnavailabilityReason.NotExchangeable("Ethereum")))
                val model = createModel()
                advanceUntilIdle()

                // Act
                requireNotNull(model.uiState.value).rows[1].onClick()

                // Assert
                verify(exactly = 1) { uiMessageSender.send(match<DialogMessage> { true }) }
                verify(exactly = 0) { router.push(any(), any()) }
                verify(exactly = 0) { analyticsEventHandler.send(match { it is ManageFundsAnalyticsEvent.ButtonSwap }) }
                assertThat(dismissals).isEqualTo(0)
                model.onDestroy()
            }
    }

    @Nested
    inner class Buy {

        @Test
        fun `GIVEN the Buy row WHEN clicked THEN the onramp opens for the currency and the sheet closes`() = runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()

            // Act
            requireNotNull(model.uiState.value).rows[2].onClick()

            // Assert
            verify(exactly = 1) {
                router.push(
                    AppRoute.Onramp(source = OnrampSource.MARKETS, userWalletId = USER_WALLET_ID, currency = currency),
                    any(),
                )
            }
            assertThat(dismissals).isEqualTo(1)
            model.onDestroy()
        }

        @Test
        fun `GIVEN the wallet backup warning does not let the user proceed WHEN Buy is clicked THEN nothing opens`() =
            runTest {
                // Arrange
                stubBackupPasses(passes = false)
                val model = createModel()
                advanceUntilIdle()

                // Act
                requireNotNull(model.uiState.value).rows[2].onClick()

                // Assert
                verify(exactly = 0) { router.push(any(), any()) }
                verify(exactly = 0) { analyticsEventHandler.send(match { it is ManageFundsAnalyticsEvent.ButtonBuy }) }
                assertThat(dismissals).isEqualTo(0)
                model.onDestroy()
            }

        @Test
        fun `GIVEN a joint account WHEN the Buy row is clicked THEN the reason dialog is sent and nothing opens`() =
            runTest {
                // Arrange
                stubActions(allActions(buy = ScenarioUnavailabilityReason.JointAccount))
                val model = createModel()
                advanceUntilIdle()

                // Act
                requireNotNull(model.uiState.value).rows[2].onClick()

                // Assert
                verify(exactly = 1) { uiMessageSender.send(match<DialogMessage> { true }) }
                verify(exactly = 0) { router.push(any(), any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN Buy is unavailable WHEN its row is clicked THEN the reason dialog is sent and nothing opens`() =
            runTest {
                // Arrange
                stubActions(allActions(buy = ScenarioUnavailabilityReason.BuyUnavailable("Ethereum")))
                val model = createModel()
                advanceUntilIdle()

                // Act
                requireNotNull(model.uiState.value).rows[2].onClick()

                // Assert
                verify(exactly = 1) { uiMessageSender.send(match<DialogMessage> { true }) }
                verify(exactly = 0) { router.push(any(), any()) }
                model.onDestroy()
            }
    }

    @Nested
    inner class Deposit {

        @Test
        fun `GIVEN the Deposit row WHEN clicked THEN the receive config is emitted and the sheet stays open`() =
            runTest {
                // Arrange
                val configs = mutableListOf<TokenReceiveConfig>()
                val model = createModel()
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    model.receiveConfigs.collect { configs.add(it) }
                }
                advanceUntilIdle()

                // Act
                requireNotNull(model.uiState.value).rows[0].onClick()
                advanceUntilIdle()

                // Assert
                assertThat(configs).containsExactly(receiveConfig)
                assertThat(dismissals).isEqualTo(0)
                verify(exactly = 0) { router.push(any(), any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN no receive config for the status WHEN Deposit is clicked THEN nothing is emitted`() = runTest {
            // Arrange
            coEvery { receiveAddressesFactory.create(any(), any(), any()) } returns null
            val configs = mutableListOf<TokenReceiveConfig>()
            val model = createModel()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                model.receiveConfigs.collect { configs.add(it) }
            }
            advanceUntilIdle()

            // Act
            requireNotNull(model.uiState.value).rows[0].onClick()
            advanceUntilIdle()

            // Assert
            assertThat(configs).isEmpty()
            model.onDestroy()
        }

        @Test
        fun `GIVEN the wallet backup warning does not let the user proceed WHEN Deposit is clicked THEN no config is emitted`() =
            runTest {
                // Arrange
                stubBackupPasses(passes = false)
                val configs = mutableListOf<TokenReceiveConfig>()
                val model = createModel()
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    model.receiveConfigs.collect { configs.add(it) }
                }
                advanceUntilIdle()

                // Act
                requireNotNull(model.uiState.value).rows[0].onClick()
                advanceUntilIdle()

                // Assert
                assertThat(configs).isEmpty()
                model.onDestroy()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Analytics {

        @Test
        fun `GIVEN the actions emitted twice WHEN the sheet is shown THEN the method screen event is sent once`() =
            runTest {
                // Arrange
                val actions = MutableStateFlow(allActions())
                every { actionsUseCase(any(), any(), any()) } returns flow {
                    actions.collect { emit(TokenActionsState(USER_WALLET_ID, status, it)) }
                }
                val model = createModel()
                advanceUntilIdle()

                // Act
                actions.value = allActions(swap = ScenarioUnavailabilityReason.NotExchangeable("Ethereum"))
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) {
                    analyticsEventHandler.send(
                        ManageFundsAnalyticsEvent.MethodScreenOpened(AnalyticsParam.ScreensSources.Earn),
                    )
                }
                model.onDestroy()
            }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN an available row WHEN clicked THEN its button event is sent`(model: ButtonModel) = runTest {
            // Arrange
            val sheetModel = createModel()
            advanceUntilIdle()

            // Act
            requireNotNull(sheetModel.uiState.value).rows[model.rowIndex].onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { analyticsEventHandler.send(model.expected) }
            sheetModel.onDestroy()
        }

        private fun provideTestModels() = listOf(
            ButtonModel(rowIndex = 0, expected = ManageFundsAnalyticsEvent.ButtonReceive()),
            ButtonModel(rowIndex = 1, expected = ManageFundsAnalyticsEvent.ButtonSwap()),
            ButtonModel(rowIndex = 2, expected = ManageFundsAnalyticsEvent.ButtonBuy()),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetAssetEvents {

        @Test
        fun `GIVEN data arrives twice WHEN collected THEN GetAssetSheetOpened is sent once`() = runTest {
            // Arrange
            val actions = MutableStateFlow(allActions())
            every { actionsUseCase(any(), any(), any()) } returns flow {
                actions.collect { emit(TokenActionsState(USER_WALLET_ID, status, it)) }
            }
            val model = createModel()
            advanceUntilIdle()

            // Act
            actions.value = allActions(swap = ScenarioUnavailabilityReason.NotExchangeable("Ethereum"))
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    EarnPickerAnalyticsEvent.GetAssetSheetOpened(
                        token = currency.symbol,
                        source = EarnAddFundsSource.YieldPicker,
                    ),
                )
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN a request from the Info screen WHEN the sheet is shown THEN GetAssetSheetOpened carries its source`() =
            runTest {
                // Arrange
                val infoRequest = EarnGetTokenSheetRequest(USER_WALLET_ID, currency, EarnAddFundsSource.InfoScreen)
                val model = createModel(sheetRequest = infoRequest)

                // Act
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) {
                    analyticsEventHandler.send(
                        EarnPickerAnalyticsEvent.GetAssetSheetOpened(
                            token = currency.symbol,
                            source = EarnAddFundsSource.InfoScreen,
                        ),
                    )
                }
                model.onDestroy()
            }

        @Test
        fun `GIVEN no status for the currency WHEN the model is created THEN GetAssetSheetOpened is not sent`() =
            runTest {
                // Arrange
                stubStatus(found = false)
                val model = createModel()

                // Act
                advanceUntilIdle()

                // Assert
                verify(exactly = 0) {
                    analyticsEventHandler.send(match { it is EarnPickerAnalyticsEvent.GetAssetSheetOpened })
                }
                model.onDestroy()
            }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN an available row WHEN clicked THEN its GetAssetMethod is sent`(model: ButtonModel) = runTest {
            // Arrange
            val sheetModel = createModel()
            advanceUntilIdle()

            // Act
            requireNotNull(sheetModel.uiState.value).rows[model.rowIndex].onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { analyticsEventHandler.send(model.expected) }
            sheetModel.onDestroy()
        }

        @Test
        fun `GIVEN Buy is available WHEN clicked THEN GetAssetMethod Buy is sent before the navigation`() = runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()

            // Act
            requireNotNull(model.uiState.value).rows[2].onClick()

            // Assert
            verifyOrder {
                analyticsEventHandler.send(
                    EarnPickerAnalyticsEvent.GetAssetMethod(token = currency.symbol, method = EarnGetTokenAction.Buy),
                )
                router.push(any(), any())
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN a blocked action WHEN clicked THEN no GetAssetMethod is sent`() = runTest {
            // Arrange
            stubActions(allActions(swap = ScenarioUnavailabilityReason.NotExchangeable("Ethereum")))
            val model = createModel()
            advanceUntilIdle()

            // Act
            requireNotNull(model.uiState.value).rows[1].onClick()

            // Assert
            verify(exactly = 0) { analyticsEventHandler.send(match { it is EarnPickerAnalyticsEvent.GetAssetMethod }) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the backup warning does not let the user proceed WHEN Buy is clicked THEN no GetAssetMethod is sent`() =
            runTest {
                // Arrange
                stubBackupPasses(passes = false)
                val model = createModel()
                advanceUntilIdle()

                // Act
                requireNotNull(model.uiState.value).rows[2].onClick()

                // Assert
                verify(exactly = 0) {
                    analyticsEventHandler.send(match { it is EarnPickerAnalyticsEvent.GetAssetMethod })
                }
                model.onDestroy()
            }

        private fun provideTestModels() = listOf(
            ButtonModel(
                rowIndex = 0,
                expected = EarnPickerAnalyticsEvent.GetAssetMethod(
                    token = currency.symbol,
                    method = EarnGetTokenAction.Deposit,
                ),
            ),
            ButtonModel(
                rowIndex = 1,
                expected = EarnPickerAnalyticsEvent.GetAssetMethod(
                    token = currency.symbol,
                    method = EarnGetTokenAction.Swap,
                ),
            ),
            ButtonModel(
                rowIndex = 2,
                expected = EarnPickerAnalyticsEvent.GetAssetMethod(
                    token = currency.symbol,
                    method = EarnGetTokenAction.Buy,
                ),
            ),
        )
    }

    @Nested
    inner class Loading {

        @Test
        fun `GIVEN the network status is cached WHEN the sheet is shown THEN only the Deposit row is loading`() =
            runTest {
                // Arrange
                stubNetworkSource(StatusSource.CACHE)
                val model = createModel()

                // Act
                advanceUntilIdle()

                // Assert
                assertThat(requireNotNull(model.uiState.value).rows.map { it.isLoading })
                    .containsExactly(true, false, false).inOrder()
                model.onDestroy()
            }
    }

    data class ButtonModel(val rowIndex: Int, val expected: AnalyticsEvent)

    private fun TestScope.createModel(sheetRequest: EarnGetTokenSheetRequest = request) = EarnGetTokenSheetModel(
        paramsContainer = MutableParamsContainer(
            EarnGetTokenSheetComponent.Params(request = sheetRequest, onDismiss = { dismissals++ }),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        singleAccountStatusListSupplier = accountStatusListSupplier,
        getCryptoCurrencyActionsUseCase = actionsUseCase,
        userWalletsListRepository = userWalletsListRepository,
        receiveAddressesFactory = receiveAddressesFactory,
        backupErrorWarningSender = backupErrorWarningSender,
        router = router,
        uiMessageSender = uiMessageSender,
        analyticsEventHandler = analyticsEventHandler,
    )

    private fun stubNetworkSource(source: StatusSource) {
        every { status.value.sources.networkSource } returns source
    }

    private fun stubStatus(found: Boolean) {
        every {
            with(CryptoCurrencyStatusOperations) {
                accountStatusList.getCryptoCurrencyStatus(any<CryptoCurrency>())
            }
        } returns (if (found) status.some() else none())
    }

    private fun stubActions(actions: List<ActionState>) {
        every { actionsUseCase(any(), any(), any()) } returns flowOf(TokenActionsState(USER_WALLET_ID, status, actions))
    }

    private fun stubBackupPasses(passes: Boolean) {
        every { backupErrorWarningSender.forWallet(any(), any(), any(), any(), any()) } answers {
            if (passes) arg<() -> Unit>(4).invoke()
        }
    }

    private fun allActions(
        deposit: ScenarioUnavailabilityReason = ScenarioUnavailabilityReason.None,
        swap: ScenarioUnavailabilityReason = ScenarioUnavailabilityReason.None,
        buy: ScenarioUnavailabilityReason = ScenarioUnavailabilityReason.None,
    ) = listOf(
        ActionState.Receive(deposit),
        ActionState.Swap(unavailabilityReason = swap, shouldShowBadge = false),
        ActionState.Buy(buy),
    )
}