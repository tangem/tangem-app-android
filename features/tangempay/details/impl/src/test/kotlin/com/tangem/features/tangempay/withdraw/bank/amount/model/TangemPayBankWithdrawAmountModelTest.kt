package com.tangem.features.tangempay.withdraw.bank.amount.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.tangempay.TangemPayModelTestBase
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BASE_CHAIN_ID
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
import com.tangem.features.tangempay.withdraw.bank.USDC_BASE
import com.tangem.features.tangempay.withdraw.bank.amount.TangemPayBankWithdrawAmountComponent
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.SheetUM
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawOptions
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawSource
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawAmountResult
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawOptions
import com.tangem.features.tangempay.withdraw.bank.navigation.BankWithdrawEditTarget
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import com.tangem.features.tangempay.withdraw.bank.paymentToken
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.StringsSigns.THREE_STARS
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import io.mockk.verifySequence
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayBankWithdrawAmountModelTest : TangemPayModelTestBase() {

    private val onCloseFlow: () -> Unit = mockk(relaxed = true)
    private val networkFeeLoader: BankWithdrawNetworkFeeLoader = mockk()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val paymentAccount = BankWithdrawSourceAccount.Payment(UserWalletId("011"))
    private val moneyAccount = BankWithdrawSourceAccount.Money(UserWalletId("011"), productInstanceId = "instance-1")

    private val usdcPolygon = paymentToken()
    private val usdcBase = paymentToken(networkId = "base", networkName = "Base", contractAddress = USDC_BASE)
    private val baseSource = bankWithdrawSource(
        token = usdcBase,
        chainId = BASE_CHAIN_ID,
        fiatBalance = BigDecimal("12325.55"),
    )
    private val twoSourceOptions = bankWithdrawOptions(sources = listOf(bankWithdrawSource(), baseSource))

    private val confirmed = mutableListOf<BankWithdrawAmountResult>()
    private lateinit var draftFlow: MutableStateFlow<BankWithdrawDraft>
    private lateinit var optionsFlow: MutableStateFlow<BankWithdrawOptions>

    @BeforeEach
    fun resetMocks() {
        clearMocks(analytics, router, uiMessageSender, onCloseFlow, networkFeeLoader)
        confirmed.clear()
        coEvery { networkFeeLoader.load(any(), any()) } returns BigDecimal.ZERO.right()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InitialState {

        @Test
        fun `GIVEN prepared options WHEN model created THEN first source and first rail preselected`() {
            // Act
            val model = createModel()

            // Assert
            val state = model.content()
            assertThat(state.source.symbol).isEqualTo("USDC")
            assertThat(state.source.subtitle).isEqualTo(
                resourceReference(R.string.tangempay_withdraw_bank_source_subtitle, wrappedList("Polygon", "$2,325.55")),
            )
            assertThat(state.route.rail).isEqualTo(BankRail.ACH)
            assertThat(state.route.fee).isEqualTo("$1")
            assertThat(state.route.onClick).isNotNull()
            assertThat(state.amount.currencySymbol).isEqualTo("$")
            assertThat(state.amount.decimals).isEqualTo(2)
            assertThat(state.isContinueEnabled).isFalse()
            assertThat(state.header.subtitle)
                .isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_step_counter, wrappedList(1, 5)))
            model.onDestroy()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN no route to choose WHEN model created THEN route row not clickable`(testModel: InitialStateModel) {
            // Act
            val model = createModel(draft = testModel.draft, options = testModel.options)

            // Assert
            assertThat(model.content().route.onClick).isNull()
            model.onDestroy()
        }

        @Test
        fun `GIVEN saved recipient on wire WHEN model created THEN wire shown and route row not clickable`() {
            // Arrange
            val draft = BankWithdrawDraft(entry = BankWithdrawEntry.SeeAll, recipient = bankAccount(rail = BankRail.WIRE))

            // Act
            val model = createModel(draft = draft)

            // Assert
            val route = model.content().route
            assertThat(route.rail).isEqualTo(BankRail.WIRE)
            assertThat(route.onClick).isNull()
            model.onDestroy()
        }

        @Test
        fun `GIVEN balance hidden WHEN model created THEN source balance masked`() {
            // Act
            val model = createModel(isBalanceHidden = true)

            // Assert
            assertThat(model.content().source.subtitle).isEqualTo(
                resourceReference(R.string.tangempay_withdraw_bank_source_subtitle, wrappedList("Polygon", THREE_STARS)),
            )
            model.onDestroy()
        }

        @ParameterizedTest
        @MethodSource("unavailableSourceCases")
        fun `GIVEN preferred source not available WHEN model created THEN first available source preselected`(
            testModel: InitialStateModel,
        ) {
            // Act
            val model = createModel(draft = testModel.draft, options = testModel.options)

            // Assert
            assertThat(model.content().source.subtitle).isEqualTo(
                resourceReference(R.string.tangempay_withdraw_bank_source_subtitle, wrappedList("Polygon", "$2,325.55")),
            )
            model.onDestroy()
        }

        @Test
        fun `GIVEN draft with source and rail WHEN model created THEN preselected from draft`() {
            // Arrange
            val draft = BankWithdrawDraft(
                entry = BankWithdrawEntry.NewPersonal,
                source = baseSource,
                destination = offrampDestination(rail = BankRail.WIRE, fee = BigDecimal("11.00"), minDays = 0, maxDays = 0),
            )

            // Act
            val model = createModel(draft = draft)

            // Assert
            val state = model.content()
            assertThat(state.source.subtitle).isEqualTo(
                resourceReference(R.string.tangempay_withdraw_bank_source_subtitle, wrappedList("Base", "$12,325.55")),
            )
            assertThat(state.route.rail).isEqualTo(BankRail.WIRE)
            model.onDestroy()
        }

        @Test
        fun `GIVEN draft with amount WHEN reopened for editing THEN amount prefilled and continue enabled`() {
            // Arrange
            val draft = BankWithdrawDraft(
                entry = BankWithdrawEntry.NewPersonal,
                amount = BigDecimal("500"),
                source = bankWithdrawSource(),
                destination = offrampDestination(),
            )

            // Act
            val model = createModel(
                draft = draft,
                step = TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.Amount),
            )

            // Assert
            val state = model.content()
            assertThat(state.amount.value).isEqualTo("500")
            assertThat(state.isContinueEnabled).isTrue()
            model.onDestroy()
        }

        @Test
        fun `GIVEN edit step WHEN model created THEN header counts the edited step`() {
            // Act
            val model = createModel(step = TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.Amount))

            // Assert
            assertThat(model.content().header.subtitle)
                .isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_step_counter, wrappedList(1, 5)))
            model.onDestroy()
        }

        private fun provideTestModels() = listOf(
            InitialStateModel(
                name = "single destination",
                draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal),
                options = bankWithdrawOptions(destinations = listOf(offrampDestination())),
            ),
            InitialStateModel(
                name = "saved entry before its recipient loads",
                draft = BankWithdrawDraft(entry = BankWithdrawEntry.Saved("acc-1")),
                options = twoSourceOptions,
            ),
        )

        private fun unavailableSourceCases() = listOf(
            InitialStateModel(
                name = "unavailable token listed first",
                draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal),
                options = bankWithdrawOptions(sources = listOf(baseSource.copy(isAvailable = false), bankWithdrawSource())),
            ),
            InitialStateModel(
                name = "draft source became unavailable",
                draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal, source = baseSource),
                options = bankWithdrawOptions(sources = listOf(baseSource.copy(isAvailable = false), bankWithdrawSource())),
            ),
            InitialStateModel(
                name = "draft source no longer withdrawable",
                draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal, source = baseSource),
                options = bankWithdrawOptions(),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NetworkFee {

        @Test
        fun `GIVEN money account WHEN network fee loaded THEN max subtracts bank and network fees`() {
            // Arrange
            coEvery { networkFeeLoader.load(moneyAccount, any()) } returns NETWORK_FEE.right()
            val model = createModel(sourceAccount = moneyAccount, options = bankWithdrawOptions())

            // Act
            model.content().source.onMaxClick()

            // Assert
            assertThat(model.content().amount.value).isEqualTo("2324.51")
            model.onDestroy()
        }

        @Test
        fun `GIVEN money account WHEN continue clicked THEN result carries the network fee`() {
            // Arrange
            coEvery { networkFeeLoader.load(moneyAccount, any()) } returns NETWORK_FEE.right()
            val options = bankWithdrawOptions()
            val model = createModel(sourceAccount = moneyAccount, options = options)
            model.content().amount.onValueChange("500")

            // Act
            model.content().onContinueClick()

            // Assert
            assertThat(confirmed).containsExactly(
                BankWithdrawAmountResult(
                    amount = BigDecimal("500"),
                    source = options.sources.first(),
                    destination = options.destinations.first(),
                    networkFee = NETWORK_FEE,
                ),
            )
            model.onDestroy()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN money account network fee outcome WHEN amount not covered with the fees entered THEN not enough funds`(
            testModel: NotEnoughFundsModel,
        ) {
            // Arrange
            coEvery { networkFeeLoader.load(moneyAccount, any()) } returns testModel.networkFee
            val model = createModel(sourceAccount = moneyAccount, options = bankWithdrawOptions())

            // Act
            model.content().amount.onValueChange(testModel.amount)

            // Assert
            val state = model.content()
            assertThat(state.amount.error)
                .isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_amount_not_enough_funds))
            assertThat(state.isContinueEnabled).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN network fee in flight WHEN it arrives THEN loading state replaced by the form`() {
            // Arrange
            val fee = CompletableDeferred<Either<GetFeeError, BigDecimal>>()
            coEvery { networkFeeLoader.load(moneyAccount, any()) } coAnswers { fee.await() }
            val model = createModel(sourceAccount = moneyAccount)
            val beforeFee = model.uiState.value

            // Act
            fee.complete(NETWORK_FEE.right())

            // Assert
            assertThat(beforeFee).isInstanceOf(TangemPayBankWithdrawAmountUM.Loading::class.java)
            assertThat(model.uiState.value).isInstanceOf(TangemPayBankWithdrawAmountUM.Content::class.java)
            model.onDestroy()
        }

        @Test
        fun `GIVEN network fee failed WHEN retried THEN fee reloaded and the form shown`() {
            // Arrange
            coEvery { networkFeeLoader.load(moneyAccount, any()) } returnsMany listOf(
                GetFeeError.UnknownError.left(),
                NETWORK_FEE.right(),
            )
            val model = createModel(sourceAccount = moneyAccount)
            val unreachable = model.uiState.value as TangemPayBankWithdrawAmountUM.FeeUnreachable

            // Act
            unreachable.onRetryClick()

            // Assert
            assertThat(model.uiState.value).isInstanceOf(TangemPayBankWithdrawAmountUM.Content::class.java)
            coVerify(exactly = 2) { networkFeeLoader.load(moneyAccount, any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN balance not covering the network fee WHEN model created THEN form shown instead of fee unreachable`() {
            // Arrange
            coEvery { networkFeeLoader.load(moneyAccount, any()) } returns GetFeeError.GaslessError.NotEnoughFunds.left()

            // Act
            val model = createModel(sourceAccount = moneyAccount, options = bankWithdrawOptions())

            // Assert
            assertThat(model.uiState.value).isInstanceOf(TangemPayBankWithdrawAmountUM.Content::class.java)
            model.onDestroy()
        }

        private fun provideTestModels() = listOf(
            NotEnoughFundsModel(
                name = "network fee loaded, amount covers only the bank fee",
                networkFee = NETWORK_FEE.right(),
                amount = "2324.55",
            ),
            NotEnoughFundsModel(
                name = "balance not covering the network fee, amount within balance",
                networkFee = GetFeeError.GaslessError.NotEnoughFunds.left(),
                amount = "500",
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AmountInput {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN invalid amount WHEN value changed THEN matching error shown and continue disabled`(
            testModel: InvalidAmountModel,
        ) {
            // Arrange
            val model = createModel()

            // Act
            model.content().amount.onValueChange(testModel.amount)

            // Assert
            val state = model.content()
            assertThat(state.amount.error).isEqualTo(testModel.expectedError)
            assertThat(state.isContinueEnabled).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN non numeric input WHEN value changed THEN ignored`() {
            // Arrange
            val model = createModel()

            // Act
            model.content().amount.onValueChange("abc")

            // Assert
            assertThat(model.content().amount.value).isEmpty()
            model.onDestroy()
        }

        @Test
        fun `GIVEN comma decimal locale WHEN comma separated value entered THEN accepted and validated`() {
            val previousLocale = Locale.getDefault()
            try {
                // Arrange
                Locale.setDefault(Locale.GERMANY)
                val model = createModel()

                // Act
                model.content().amount.onValueChange("1,5")

                // Assert
                val state = model.content()
                assertThat(state.amount.value).isEqualTo("1,5")
                assertThat(state.amount.error).isNull()
                assertThat(state.isContinueEnabled).isTrue()
                model.onDestroy()
            } finally {
                Locale.setDefault(previousLocale)
            }
        }

        @ParameterizedTest
        @MethodSource("maxAmountCases")
        fun `GIVEN source balance WHEN max clicked THEN amount capped by balance minus fee and by the limit`(
            testModel: MaxAmountModel,
        ) {
            // Arrange
            val model = createModel(options = testModel.options)

            // Act
            model.content().source.onMaxClick()

            // Assert
            val state = model.content()
            assertThat(state.amount.value).isEqualTo(testModel.expectedAmount)
            assertThat(state.amount.error).isEqualTo(testModel.expectedError)
            assertThat(state.isContinueEnabled).isEqualTo(testModel.isContinueEnabled)
            model.onDestroy()
        }

        @Test
        fun `GIVEN prefilled amount WHEN cleared THEN stays empty instead of reverting to the draft`() {
            // Arrange
            val draft = BankWithdrawDraft(
                entry = BankWithdrawEntry.NewPersonal,
                amount = BigDecimal("500"),
                source = bankWithdrawSource(),
                destination = offrampDestination(),
            )
            val model = createModel(
                draft = draft,
                step = TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.Amount),
            )

            // Act
            model.content().amount.onValueChange("")

            // Assert
            assertThat(model.content().amount.value).isEmpty()
            model.onDestroy()
        }

        private fun provideTestModels() = listOf(
            InvalidAmountModel(name = "below minimum", amount = "0.5", expectedError = RANGE_ERROR),
            InvalidAmountModel(name = "above limit while balance is below it", amount = "10001", expectedError = RANGE_ERROR),
            InvalidAmountModel(
                name = "amount plus fee above balance",
                amount = "2325",
                expectedError = resourceReference(R.string.tangempay_withdraw_bank_amount_not_enough_funds),
            ),
        )

        private fun maxAmountCases() = listOf(
            MaxAmountModel(
                name = "prepared options, balance minus fee",
                options = twoSourceOptions,
                expectedAmount = "2324.55",
                expectedError = null,
                isContinueEnabled = true,
            ),
            MaxAmountModel(
                name = "balance above limit, limit",
                options = bankWithdrawOptions(sources = listOf(bankWithdrawSource(fiatBalance = BigDecimal("20000")))),
                expectedAmount = "10000",
                expectedError = null,
                isContinueEnabled = true,
            ),
            MaxAmountModel(
                name = "balance below fee, whole balance with range error",
                options = bankWithdrawOptions(sources = listOf(bankWithdrawSource(fiatBalance = BigDecimal("0.65")))),
                expectedAmount = "0.65",
                expectedError = RANGE_ERROR,
                isContinueEnabled = false,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SelectionSheets {

        @Test
        fun `GIVEN source row clicked WHEN token selected THEN source changes and sheet closes`() {
            // Arrange
            val model = createModel()
            model.content().source.onClick?.invoke()
            val sheet = model.content().sheet as SheetUM.Tokens
            assertThat(sheet.items.map { it.symbol to it.isSelected }).containsExactly(
                "USDC" to true,
                "USDC" to false,
            ).inOrder()

            // Act
            sheet.items.last().onClick()

            // Assert
            val state = model.content()
            assertThat(state.sheet).isEqualTo(SheetUM.Hidden)
            assertThat(state.source.subtitle).isEqualTo(
                resourceReference(R.string.tangempay_withdraw_bank_source_subtitle, wrappedList("Base", "$12,325.55")),
            )
            model.onDestroy()
        }

        @Test
        fun `GIVEN route row clicked WHEN wire selected THEN route row updates and sheet closes`() {
            // Arrange
            val model = createModel()
            model.content().route.onClick?.invoke()
            val sheet = model.content().sheet as SheetUM.Routes
            assertThat(sheet.items.map { it.rail to it.isSelected }).containsExactly(
                BankRail.ACH to true,
                BankRail.WIRE to false,
            ).inOrder()

            // Act
            sheet.items.last().onClick()

            // Assert
            val state = model.content()
            assertThat(state.sheet).isEqualTo(SheetUM.Hidden)
            assertThat(state.route.rail).isEqualTo(BankRail.WIRE)
            assertThat(state.route.fee).isEqualTo("$11")
            assertThat(state.route.eta).isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_eta_same_business_day))
            model.onDestroy()
        }

        @Test
        fun `GIVEN sheet opened WHEN dismissed THEN sheet closes`() {
            // Arrange
            val model = createModel()
            model.content().source.onClick?.invoke()

            // Act
            model.content().sheet.onDismiss()

            // Assert
            assertThat(model.content().sheet).isEqualTo(SheetUM.Hidden)
            model.onDestroy()
        }

        @Test
        fun `GIVEN token the off-ramp does not accept WHEN token sheet opened THEN listed unavailable`() {
            // Arrange
            val options = bankWithdrawOptions(sources = listOf(bankWithdrawSource(), baseSource.copy(isAvailable = false)))
            val model = createModel(options = options)

            // Act
            model.content().source.onClick?.invoke()

            // Assert
            val sheet = model.content().sheet as SheetUM.Tokens
            assertThat(sheet.items.map { Triple(it.subtitle, it.isAvailable, it.isSelected) }).containsExactly(
                Triple(
                    resourceReference(R.string.tangempay_withdraw_bank_source_subtitle, wrappedList("Polygon", "$2,325.55")),
                    true,
                    true,
                ),
                Triple(
                    resourceReference(
                        R.string.tangempay_withdraw_bank_source_subtitle,
                        wrappedList("Base", resourceReference(R.string.tangempay_withdraw_bank_offramp_unavailable)),
                    ),
                    false,
                    false,
                ),
            ).inOrder()
            model.onDestroy()
        }

        @Test
        fun `GIVEN balance hidden WHEN token sheet opened THEN balances masked and unavailable token still labelled`() {
            // Arrange
            val options = bankWithdrawOptions(sources = listOf(bankWithdrawSource(), baseSource.copy(isAvailable = false)))
            val model = createModel(options = options, isBalanceHidden = true)

            // Act
            model.content().source.onClick?.invoke()

            // Assert
            val sheet = model.content().sheet as SheetUM.Tokens
            assertThat(sheet.items.map { it.subtitle }).containsExactly(
                resourceReference(R.string.tangempay_withdraw_bank_source_subtitle, wrappedList("Polygon", THREE_STARS)),
                resourceReference(
                    R.string.tangempay_withdraw_bank_source_subtitle,
                    wrappedList("Base", resourceReference(R.string.tangempay_withdraw_bank_offramp_unavailable)),
                ),
            ).inOrder()
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class DraftAndOptionsUpdates {

        @Test
        fun `GIVEN wire picked WHEN saved recipient on ach selected THEN ach shown and route row not clickable`() {
            // Arrange
            val model = createModel()
            model.content().route.onClick?.invoke()
            (model.content().sheet as SheetUM.Routes).items.last().onClick()

            // Act
            draftFlow.value = draftFlow.value.copy(recipient = bankAccount(rail = BankRail.ACH))

            // Assert
            val route = model.content().route
            assertThat(route.rail).isEqualTo(BankRail.ACH)
            assertThat(route.onClick).isNull()
            model.onDestroy()
        }

        @Test
        fun `GIVEN continue committed WHEN draft amount changes afterwards THEN new amount shown and reported`() {
            // Arrange
            val model = createModel()
            model.content().amount.onValueChange("500")
            model.content().onContinueClick()

            // Act
            draftFlow.value = draftFlow.value.copy(amount = BigDecimal("700"))

            // Assert
            assertThat(model.content().amount.value).isEqualTo("700")
            model.content().onContinueClick()
            assertThat(confirmed.last().amount).isEqualTo(BigDecimal("700"))
            model.onDestroy()
        }

        @Test
        fun `GIVEN balance drops while the step is open WHEN options update THEN the amount is revalidated`() {
            // Arrange
            val model = createModel()
            model.content().amount.onValueChange("2000")
            assertThat(model.content().isContinueEnabled).isTrue()

            // Act
            optionsFlow.value = bankWithdrawOptions(
                sources = listOf(bankWithdrawSource(fiatBalance = BigDecimal("10")), baseSource),
            )

            // Assert
            val state = model.content()
            assertThat(state.amount.error)
                .isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_amount_not_enough_funds))
            assertThat(state.isContinueEnabled).isFalse()
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Continue {

        @Test
        fun `GIVEN valid amount WHEN continue clicked THEN result passed to container`() {
            // Arrange
            val model = createModel()
            model.content().amount.onValueChange("500")

            // Act
            model.content().onContinueClick()

            // Assert
            val result = confirmed.single()
            assertThat(result.amount).isEqualTo(BigDecimal("500"))
            assertThat(result.source.token).isEqualTo(usdcPolygon)
            assertThat(result.destination.rail).isEqualTo(BankRail.ACH)
            model.onDestroy()
        }

        @Test
        fun `GIVEN invalid amount WHEN continue clicked THEN nothing passed`() {
            // Arrange
            val model = createModel()

            // Act
            model.content().onContinueClick()

            // Assert
            assertThat(confirmed).isEmpty()
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Navigation {

        @Test
        fun `GIVEN the amount screen WHEN back clicked THEN router pops`() {
            // Arrange
            val model = createModel()

            // Act
            model.content().onBackClick()

            // Assert
            verify(exactly = 1) { router.pop() }
            model.onDestroy()
        }

        @Test
        fun `GIVEN model WHEN header close clicked THEN flow closed`() {
            // Arrange
            val model = createModel()

            // Act
            model.content().header.onCloseClick()

            // Assert
            verify(exactly = 1) { onCloseFlow() }
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Analytics {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN empty draft WHEN positive amount entered or changed again THEN AmountEntered is sent once`(
            testModel: TypedAmountsModel,
        ) {
            // Arrange
            val model = createModel()
            testModel.priorAmounts.forEach { model.content().amount.onValueChange(it) }

            // Act
            model.content().amount.onValueChange(testModel.amount)

            // Assert
            verify(exactly = testModel.expectedEvents) {
                analytics.send(TangemPayAnalyticsEvents.Offramp.AmountEntered())
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN empty draft WHEN zero entered THEN no offramp event is sent`() {
            // Arrange
            val model = createModel()

            // Act
            model.content().amount.onValueChange("0")

            // Assert
            verify(exactly = 0) { analytics.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN draft with amount WHEN amount changed THEN AmountEntered is not sent`() {
            // Arrange
            val draft = BankWithdrawDraft(
                entry = BankWithdrawEntry.NewPersonal,
                amount = BigDecimal("500"),
                source = bankWithdrawSource(),
                destination = offrampDestination(),
            )
            val model = createModel(draft = draft)

            // Act
            model.content().amount.onValueChange("20")

            // Assert
            verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.Offramp.AmountEntered>()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN empty draft WHEN max clicked THEN AmountEntered is sent`() {
            // Arrange
            val model = createModel()

            // Act
            model.content().source.onMaxClick()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.AmountEntered()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN model WHEN source row clicked THEN CoinSelectorClicked and CoinSelectionSheetOpened are sent`() {
            // Arrange
            val model = createModel()

            // Act
            model.content().source.onClick?.invoke()

            // Assert
            verifyOrder {
                analytics.send(TangemPayAnalyticsEvents.Offramp.CoinSelectorClicked())
                analytics.send(TangemPayAnalyticsEvents.Offramp.CoinSelectionSheetOpened())
            }
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.CoinSelectorClicked()) }
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.CoinSelectionSheetOpened()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN source sheet opened WHEN the selected source picked again THEN CoinInSelectionSheetClicked not sent`() {
            // Arrange
            val model = createModel()
            model.content().source.onClick?.invoke()
            val sheet = model.content().sheet as SheetUM.Tokens

            // Act
            sheet.items.first().onClick()

            // Assert
            verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.Offramp.CoinInSelectionSheetClicked>()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN source sheet opened WHEN another source picked THEN CoinInSelectionSheetClicked is sent`() {
            // Arrange
            val model = createModel()
            model.content().source.onClick?.invoke()
            val sheet = model.content().sheet as SheetUM.Tokens

            // Act
            sheet.items.last().onClick()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.CoinInSelectionSheetClicked()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN model WHEN route row clicked THEN RailSelectorClicked and RailSelectionSheetOpened are sent`() {
            // Arrange
            val model = createModel()

            // Act
            model.content().route.onClick?.invoke()

            // Assert
            verifyOrder {
                analytics.send(TangemPayAnalyticsEvents.Offramp.RailSelectorClicked())
                analytics.send(TangemPayAnalyticsEvents.Offramp.RailSelectionSheetOpened())
            }
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.RailSelectorClicked()) }
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.RailSelectionSheetOpened()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN route sheet opened WHEN the selected rail picked again THEN RailInSelectionSheetClicked is not sent`() {
            // Arrange
            val model = createModel()
            model.content().route.onClick?.invoke()
            val sheet = model.content().sheet as SheetUM.Routes

            // Act
            sheet.items.first().onClick()

            // Assert
            verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.Offramp.RailInSelectionSheetClicked>()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN route sheet opened WHEN another rail picked THEN RailInSelectionSheetClicked is sent`() {
            // Arrange
            val model = createModel()
            model.content().route.onClick?.invoke()
            val sheet = model.content().sheet as SheetUM.Routes

            // Act
            sheet.items.last().onClick()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.RailInSelectionSheetClicked()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN valid amount WHEN continue clicked THEN AmountContinueClicked is sent`() {
            // Arrange
            val model = createModel()
            model.content().amount.onValueChange("500")

            // Act
            model.content().onContinueClick()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.AmountContinueClicked()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN invalid amount WHEN continue clicked THEN AmountContinueClicked is not sent`() {
            // Arrange
            val model = createModel()

            // Act
            model.content().onContinueClick()

            // Assert
            verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.Offramp.AmountContinueClicked>()) }
            model.onDestroy()
        }

        @ParameterizedTest
        @MethodSource("insufficientFundsCases")
        fun `GIVEN typed amounts WHEN balance exceeded THEN InsufficientFundsStateShowed is sent once per shortage`(
            testModel: TypedAmountsModel,
        ) {
            // Arrange
            val model = createModel()
            testModel.priorAmounts.forEach { model.content().amount.onValueChange(it) }

            // Act
            model.content().amount.onValueChange(testModel.amount)

            // Assert
            verify(exactly = testModel.expectedEvents) {
                analytics.send(TangemPayAnalyticsEvents.Offramp.InsufficientFundsStateShowed())
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN draft amount above balance WHEN model created THEN InsufficientFundsStateShowed is sent`() {
            // Arrange
            val draft = BankWithdrawDraft(
                entry = BankWithdrawEntry.NewPersonal,
                amount = BigDecimal("2325"),
                source = bankWithdrawSource(),
                destination = offrampDestination(),
            )

            // Act
            val model = createModel(draft = draft)

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.InsufficientFundsStateShowed()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN amount below minimum WHEN value changed THEN InsufficientFundsStateShowed is not sent`() {
            // Arrange
            val model = createModel()

            // Act
            model.content().amount.onValueChange("0.5")

            // Assert
            verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.Offramp.InsufficientFundsStateShowed>()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN money account WHEN amount and rail picked and continued THEN only amount screen opened sent`() {
            // Arrange
            val model = createModel(sourceAccount = moneyAccount, options = bankWithdrawOptions())

            // Act
            model.content().amount.onValueChange("2325")
            model.content().amount.onValueChange("500")
            model.content().route.onClick?.invoke()
            (model.content().sheet as SheetUM.Routes).items.last().onClick()
            model.content().onContinueClick()

            // Assert
            verifySequence { analytics.send(MoneyAccountAnalyticsEvents.OfframpAmountScreenOpened()) }
            assertThat(confirmed).hasSize(1)
            model.onDestroy()
        }

        private fun provideTestModels() = listOf(
            TypedAmountsModel(priorAmounts = emptyList(), amount = "10", expectedEvents = 1),
            TypedAmountsModel(priorAmounts = listOf("10"), amount = "20", expectedEvents = 1),
        )

        private fun insufficientFundsCases() = listOf(
            TypedAmountsModel(priorAmounts = emptyList(), amount = "2325", expectedEvents = 1),
            TypedAmountsModel(priorAmounts = listOf("2325"), amount = "23255", expectedEvents = 1),
            TypedAmountsModel(priorAmounts = listOf("2325", "500"), amount = "2325", expectedEvents = 2),
        )
    }

    private fun createModel(
        draft: BankWithdrawDraft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal),
        step: TangemPayBankWithdrawInnerRoute = TangemPayBankWithdrawInnerRoute.Amount,
        options: BankWithdrawOptions = twoSourceOptions,
        isBalanceHidden: Boolean = false,
        sourceAccount: BankWithdrawSourceAccount = paymentAccount,
    ): TangemPayBankWithdrawAmountModel {
        draftFlow = MutableStateFlow(draft)
        optionsFlow = MutableStateFlow(options)
        return TangemPayBankWithdrawAmountModel(
            paramsContainer = MutableParamsContainer(
                TangemPayBankWithdrawAmountComponent.Params(
                    sourceAccount = sourceAccount,
                    draft = draftFlow,
                    options = optionsFlow,
                    step = step,
                    onContinue = confirmed::add,
                    onCloseFlow = onCloseFlow,
                ),
            ),
            dispatchers = dispatchers,
            router = router,
            analytics = analytics,
            getBalanceHidingSettingsUseCase = mockk { every { isBalanceHidden() } returns flowOf(isBalanceHidden) },
            networkFeeLoader = networkFeeLoader,
        ).destroyAfterTest()
    }

    private fun TangemPayBankWithdrawAmountModel.content(): TangemPayBankWithdrawAmountUM.Content =
        uiState.value as TangemPayBankWithdrawAmountUM.Content

    data class InitialStateModel(
        val name: String,
        val draft: BankWithdrawDraft,
        val options: BankWithdrawOptions,
    ) {
        override fun toString(): String = name
    }

    data class NotEnoughFundsModel(
        val name: String,
        val networkFee: Either<GetFeeError, BigDecimal>,
        val amount: String,
    ) {
        override fun toString(): String = name
    }

    data class InvalidAmountModel(
        val name: String,
        val amount: String,
        val expectedError: TextReference,
    ) {
        override fun toString(): String = name
    }

    data class MaxAmountModel(
        val name: String,
        val options: BankWithdrawOptions,
        val expectedAmount: String,
        val expectedError: TextReference?,
        val isContinueEnabled: Boolean,
    ) {
        override fun toString(): String = name
    }

    data class TypedAmountsModel(val priorAmounts: List<String>, val amount: String, val expectedEvents: Int)

    private companion object {
        val NETWORK_FEE = BigDecimal("0.04")
        val RANGE_ERROR = resourceReference(
            R.string.tangempay_withdraw_bank_amount_error_range,
            wrappedList("$1", "$10,000"),
        )
    }
}