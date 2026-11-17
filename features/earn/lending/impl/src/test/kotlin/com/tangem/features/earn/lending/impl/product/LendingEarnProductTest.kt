package com.tangem.features.earn.lending.impl.product

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.api.EarnFeatureToggles
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.lending.impl.provider.FakeLendingProvider
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import com.tangem.features.earn.lending.impl.provider.LendingDepositGate
import com.tangem.features.earn.lending.impl.provider.LendingMarket
import com.tangem.features.earn.lending.impl.provider.LendingPosition
import com.tangem.features.earn.lending.impl.supplier.LendingAssetState
import com.tangem.features.earn.lending.impl.supplier.LendingSnapshot
import com.tangem.features.earn.lending.impl.supplier.LendingSupplier
import com.tangem.features.earn.product.api.analytics.EarnProductAnalytics
import com.tangem.features.earn.product.api.navigation.EarnFlowRoute
import com.tangem.features.earn.product.api.navigation.EarnInfoRoute
import com.tangem.features.earn.product.api.navigation.EarnNavigator
import com.tangem.features.earn.product.api.navigation.EarnPickerRoute
import com.tangem.features.earn.product.api.product.EarnActionItem
import com.tangem.features.earn.product.api.product.EarnApyHistory
import com.tangem.features.earn.product.api.product.EarnBlock
import com.tangem.features.earn.product.api.product.EarnHowItWorks
import com.tangem.features.earn.product.api.product.EarnLegal
import com.tangem.features.earn.product.api.product.EarnPosition
import com.tangem.features.earn.product.api.product.EarnPositionState
import com.tangem.features.earn.product.api.product.EarnPositionSummary
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnRisk
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.product.EarnTextBlock
import com.tangem.features.earn.product.api.product.EarnValue
import com.tangem.features.earn.product.api.product.ProductAvailability
import com.tangem.features.earn.product.api.rate.EarnRateBadgeConverter
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.EarnAction
import com.tangem.features.earn.product.api.session.Withdraw
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.assertEither
import com.tangem.test.core.assertEitherLeft
import com.tangem.test.core.assertEitherRight
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LendingEarnProductTest {

    private val userWallet = MockUserWalletFactory.create()
    private val userWalletId = userWallet.walletId
    private val currencies = MockCryptoCurrencyFactory()
    private val eth = currencies.createCoin(Blockchain.Ethereum)
    private val asset = LendingAsset(currency = eth, wrappedToken = null)
    private val params = EarnProduct.Params(userWalletId = userWalletId, currency = eth)

    private val lendingSupplier: LendingSupplier = mockk()
    private val toggles: EarnFeatureToggles = mockk()
    private val navigator: EarnNavigator = mockk(relaxed = true)
    private val provider = FakeLendingProvider(assets = mapOf(eth.id to asset))

    private val rateBadgeConverter = EarnRateBadgeConverter()

    private val product = createProduct(provider)

    private val updateRequiredText = resourceReference(R.string.lending_update_app_message)

    @BeforeEach
    fun resetMocks() {
        clearMocks(lendingSupplier, toggles, navigator)
        provider.refreshCalls.clear()
        every { toggles.isEarnEnabled } returns true
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Identity {

        @Test
        fun `GIVEN product WHEN analytics requested THEN category and product follow the spec`() {
            // Act
            val actual = product.analytics

            // Assert
            assertThat(actual).isEqualTo(EarnProductAnalytics(category = "Earn", product = "Lending"))
        }

        @Test
        fun `GIVEN product WHEN how it works requested THEN four steps, the auditor and the blog post`() {
            // Act
            val actual = product.howItWorks

            // Assert
            assertThat(actual).isEqualTo(
                EarnHowItWorks(
                    title = resourceReference(R.string.earn_how_it_works_title),
                    entries = listOf(
                        EarnTextBlock(
                            title = resourceReference(R.string.lending_how_it_works_choose_amount_title),
                            description = resourceReference(R.string.lending_how_it_works_choose_amount_text),
                        ),
                        EarnTextBlock(
                            title = resourceReference(R.string.lending_how_it_works_money_works_title),
                            description = resourceReference(R.string.lending_how_it_works_money_works_text),
                        ),
                        EarnTextBlock(
                            title = resourceReference(R.string.earn_how_it_works_watch_it_grow_title),
                            description = resourceReference(
                                R.string.lending_how_it_works_watch_it_grow_text,
                                wrappedList(FEE_TEXT),
                            ),
                        ),
                        EarnTextBlock(
                            title = resourceReference(R.string.earn_how_it_works_take_it_back_title),
                            description = resourceReference(R.string.lending_how_it_works_take_it_back_text),
                        ),
                    ),
                    footnote = resourceReference(R.string.earn_how_it_works_audited_by, wrappedList("Pessimistic")),
                    learnMore = TangemBlogUrlBuilder.Post.HowYieldModeWorks,
                ),
            )
        }

        @Test
        fun `GIVEN state never collected WHEN how it works requested THEN learn more is already there`() {
            // Arrange
            val product = createProduct(provider)

            // Act
            val actual = product.howItWorks.learnMore

            // Assert
            assertThat(actual).isEqualTo(TangemBlogUrlBuilder.Post.HowYieldModeWorks)
            verify(exactly = 0) { lendingSupplier(any<UserWalletId>()) }
        }

        @Test
        fun `GIVEN provider WHEN legal requested THEN the provider's documents`() {
            // Act
            val actual = product.legal

            // Assert
            assertThat(actual).isEqualTo(
                EarnLegal(
                    termsOfUseUrl = "https://fake.example/terms",
                    privacyPolicyUrl = "https://fake.example/privacy",
                ),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class DepositBlock {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN deposit closed WHEN state THEN deposit block names the reason`(model: DepositBlockModel) = runTest {
            // Arrange
            every { lendingSupplier(userWalletId) } returns flowOf(
                snapshotOf(assetState(isOpen = model.isOpen, gate = model.gate)),
            )

            // Act
            val actual = product.state(params).first() as EarnProductState.Ready

            // Assert
            assertThat(actual.primary.block).isEqualTo(EarnBlock(reason = model.expectedReason))
        }

        private fun provideTestModels() = listOf(
            DepositBlockModel(
                isOpen = true,
                gate = LendingDepositGate.Unavailable,
                expectedReason = updateRequiredText,
            ),
            DepositBlockModel(
                isOpen = false,
                gate = LendingDepositGate.Open,
                expectedReason = DEPOSITS_PAUSED,
            ),
        )
    }

    @Nested
    inner class WithdrawBlock {

        @Test
        fun `GIVEN provider needs a newer app WHEN state THEN withdraw is blocked with the update text`() = runTest {
            // Arrange
            every { lendingSupplier(userWalletId) } returns flowOf(
                snapshotOf(assetState(position = BigDecimal.ONE, gate = LendingDepositGate.Unavailable)),
            )

            // Act
            val actual = product.state(params).first() as EarnProductState.Ready

            // Assert
            assertThat(actual.secondary).containsExactly(
                EarnActionItem(action = WITHDRAW, block = EarnBlock(reason = updateRequiredText)),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Summaries {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN snapshot WHEN summaries THEN maps position and click action`(model: SummaryModel) = runTest {
            // Arrange
            every { lendingSupplier(userWalletId) } returns flowOf(snapshotOf(assetState(position = model.position)))

            // Act
            val actual = product.summaries(userWalletId).first()

            // Assert
            assertThat(actual).containsExactly(
                eth.id,
                summaryOf(position = model.expectedPosition, onClickAction = model.expectedAction),
            )
        }

        @Test
        fun `GIVEN rate WHEN summaries THEN full summary is built`() = runTest {
            // Arrange
            every { lendingSupplier(userWalletId) } returns flowOf(snapshotOf(assetState(position = null)))

            // Act
            val actual = product.summaries(userWalletId).first()

            // Assert
            assertThat(actual).containsExactly(
                eth.id,
                summaryOf(position = null, onClickAction = LendingNavigationAction.OpenPicker),
            )
        }

        @Test
        fun `GIVEN no rate WHEN summaries THEN no summary is emitted`() = runTest {
            // Arrange
            every { lendingSupplier(userWalletId) } returns flowOf(snapshotOf(assetState(rate = null)))

            // Act
            val actual = product.summaries(userWalletId).first()

            // Assert
            assertThat(actual).isEmpty()
        }

        private fun provideTestModels() = listOf(
            SummaryModel(
                position = null,
                expectedPosition = null,
                expectedAction = LendingNavigationAction.OpenPicker,
            ),
            SummaryModel(
                position = BigDecimal("1.5"),
                expectedPosition = EarnPositionSummary(BigDecimal("1.5")),
                expectedAction = LendingNavigationAction.OpenPosition,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class State {

        @Test
        fun `GIVEN no asset WHEN state THEN unavailable`() = runTest {
            // Arrange
            every { lendingSupplier(userWalletId) } returns flowOf(LendingSnapshot.NoAssets)

            // Act
            val actual = product.state(params).first()

            // Assert
            assertThat(actual).isEqualTo(EarnProductState.Unavailable)
        }

        @Test
        fun `GIVEN market still loading WHEN state THEN loading`() = runTest {
            // Arrange
            every { lendingSupplier(userWalletId) } returns flowOf(
                snapshotOf(assetState(rate = null, gate = LendingDepositGate.Loading, isOpen = false)),
            )

            // Act
            val actual = product.state(params).first()

            // Assert
            assertThat(actual).isEqualTo(EarnProductState.Loading)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN market WHEN state THEN actions and blocks follow the rules`(model: ActionsModel) = runTest {
            // Arrange
            every { lendingSupplier(userWalletId) } returns flowOf(
                snapshotOf(
                    assetState(
                        position = model.position,
                        hasPendingTx = model.hasPendingTx,
                        isOpen = model.isOpen,
                    ),
                ),
            )

            // Act
            val actual = product.state(params).first() as EarnProductState.Ready

            // Assert
            assertThat(actual.primary).isEqualTo(model.expectedPrimary)
            assertThat(actual.secondary).isEqualTo(model.expectedSecondary)
        }

        @Test
        fun `GIVEN position WHEN state THEN position is known with total and available balance`() = runTest {
            // Arrange
            every { lendingSupplier(userWalletId) } returns flowOf(
                snapshotOf(assetState(position = BigDecimal("2"))),
            )

            // Act
            val actual = product.state(params).first() as EarnProductState.Ready

            // Assert
            val expected = EarnPositionState.Known(
                EarnPosition(
                    cryptoAmount = BigDecimal("2"),
                    earned = null,
                    description = resourceReference(R.string.lending_position_about),
                    rows = listOf(
                        EarnRow(
                            label = resourceReference(R.string.token_details_balance_total),
                            value = EarnValue.Amount(BigDecimal("2")),
                            info = EarnTextBlock(
                                title = resourceReference(R.string.token_details_balance_total),
                                description = resourceReference(
                                    R.string.lending_position_total_balance_info,
                                    wrappedList(FEE_TEXT),
                                ),
                            ),
                        ),
                        EarnRow(
                            label = resourceReference(R.string.token_details_balance_available),
                            value = EarnValue.Amount(BigDecimal("2")),
                            info = EarnTextBlock(
                                title = resourceReference(R.string.token_details_balance_available),
                                description = resourceReference(R.string.lending_position_available_balance_info),
                            ),
                        ),
                    ),
                    rewards = null,
                    freshness = null,
                ),
            )
            assertThat(actual.position).isEqualTo(expected)
            assertThat(actual.rate).isEqualTo(RATE)
        }

        @Test
        fun `GIVEN provider WHEN state THEN texts come from the keys and the rows carry the provider`() = runTest {
            // Arrange
            every { lendingSupplier(userWalletId) } returns flowOf(snapshotOf(assetState()))

            // Act
            val actual = product.state(params).first() as EarnProductState.Ready

            // Assert
            assertThat(product.title).isEqualTo(resourceReference(R.string.earn_product_lending_on_aave))
            assertThat(actual.description)
                .isEqualTo(resourceReference(R.string.earn_yield_picker_lending_description))
            assertThat(actual.details.map { it.value }).containsExactly(
                EarnValue.Text(resourceReference(R.string.earn_risk_level_low)),
                EarnValue.Link(text = stringReference("Fake"), url = "https://fake.example"),
                EarnValue.Text(resourceReference(R.string.earn_product_details_usually_instant)),
                EarnValue.Text(
                    resourceReference(R.string.earn_product_details_performance_fee_value, wrappedList(FEE_TEXT)),
                ),
            ).inOrder()
        }

        @Test
        fun `GIVEN no position WHEN state THEN position is none`() = runTest {
            // Arrange
            every { lendingSupplier(userWalletId) } returns flowOf(snapshotOf(assetState(position = null)))

            // Act
            val actual = product.state(params).first() as EarnProductState.Ready

            // Assert
            assertThat(actual.position).isEqualTo(EarnPositionState.None)
        }

        private fun provideTestModels() = listOf(
            ActionsModel(
                position = null,
                hasPendingTx = false,
                isOpen = true,
                expectedPrimary = EarnActionItem(action = Deposit(), label = START_EARNING),
                expectedSecondary = emptyList(),
            ),
            ActionsModel(
                position = BigDecimal.ONE,
                hasPendingTx = false,
                isOpen = true,
                expectedPrimary = EarnActionItem(action = Deposit(), label = ADD_FUNDS),
                expectedSecondary = listOf(EarnActionItem(action = WITHDRAW)),
            ),
            ActionsModel(
                position = BigDecimal.ONE,
                hasPendingTx = true,
                isOpen = true,
                expectedPrimary = EarnActionItem(action = Deposit(), block = PENDING_BLOCK, label = ADD_FUNDS),
                expectedSecondary = listOf(EarnActionItem(action = WITHDRAW, block = PENDING_BLOCK)),
            ),
            ActionsModel(
                position = BigDecimal.ONE,
                hasPendingTx = false,
                isOpen = false,
                expectedPrimary = EarnActionItem(action = Deposit(), block = CLOSED_BLOCK, label = ADD_FUNDS),
                expectedSecondary = listOf(EarnActionItem(action = WITHDRAW)),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ApyHistoryAndRefresh {

        @Test
        fun `GIVEN unknown currency WHEN apyHistory THEN left`() = runTest {
            // Arrange
            val other = currencies.createCoin(Blockchain.Bitcoin)

            // Act
            val actual = product.apyHistory(params.copy(currency = other), period = mockk())

            // Assert
            assertEitherLeft(actual, IllegalStateException("Lending is not offered for this currency"))
        }

        @Test
        fun `GIVEN asset WHEN apyHistory THEN delegates to provider`() = runTest {
            // Act
            val actual = product.apyHistory(params, period = mockk())

            // Assert
            assertEither(actual, EarnApyHistory(points = emptyList(), average = null).right())
        }

        @Test
        fun `GIVEN toggle on WHEN refresh THEN provider refreshes the asset`() = runTest {
            // Act
            val actual = product.refresh(params)

            // Assert
            assertEitherRight(actual)
            assertThat(provider.refreshCalls).containsExactly(userWalletId to asset)
        }

        @Test
        fun `GIVEN unknown currency WHEN refresh THEN left`() = runTest {
            // Arrange
            val other = currencies.createCoin(Blockchain.Bitcoin)

            // Act
            val actual = product.refresh(params.copy(currency = other))

            // Assert
            assertEitherLeft(actual, IllegalStateException("Lending is not offered for this currency"))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Gating {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN product not offered WHEN read THEN empty values and nothing is loaded`(model: GatingModel) = runTest {
            // Arrange
            every { toggles.isEarnEnabled } returns model.isEarnEnabled
            val provider = FakeLendingProvider(assets = mapOf(eth.id to asset), isAvailable = model.isAvailable)
            val product = createProduct(provider)

            // Act
            val summaries = product.summaries(userWalletId).first()
            val state = product.state(params).first()
            val apyHistory = product.apyHistory(params, period = mockk())
            val refresh = product.refresh(params)

            // Assert
            assertThat(summaries).isEmpty()
            assertThat(state).isEqualTo(EarnProductState.Unavailable)
            assertEither(apyHistory, EarnApyHistory(points = emptyList(), average = null).right())
            assertEitherRight(refresh)
            assertThat(provider.refreshCalls).isEmpty()
            verify(exactly = 0) { lendingSupplier(any<UserWalletId>()) }
        }

        private fun provideTestModels() = listOf(
            GatingModel(isEarnEnabled = false, isAvailable = true),
            GatingModel(isEarnEnabled = true, isAvailable = false),
            GatingModel(isEarnEnabled = false, isAvailable = false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ActionHandling {

        @Test
        fun `GIVEN flow action WHEN clicked THEN flow route`() {
            // Act
            product.actionHandler(params, navigator).onActionClick(Withdraw())

            // Assert
            verify(exactly = 1) { navigator.navigate(EarnFlowRoute(LendingEarnProduct.ID, Withdraw())) }
        }

        @Test
        fun `GIVEN open position WHEN clicked THEN info route`() {
            // Act
            product.actionHandler(params, navigator).onActionClick(LendingNavigationAction.OpenPosition)

            // Assert
            verify(exactly = 1) { navigator.navigate(EarnInfoRoute(LendingEarnProduct.ID)) }
        }

        @Test
        fun `GIVEN unknown action WHEN clicked THEN nothing is opened`() {
            // Arrange
            val unknown = object : EarnAction {
                override val id: String = "unknown"
                override val label: TextReference = stringReference("Unknown")
            }

            // Act
            product.actionHandler(params, navigator).onActionClick(unknown)

            // Assert
            verify(exactly = 0) { navigator.navigate(any()) }
        }

        @Test
        fun `GIVEN open picker WHEN clicked THEN picker route with preselection`() {
            // Act
            product.actionHandler(params, navigator).onActionClick(LendingNavigationAction.OpenPicker)

            // Assert
            verify(exactly = 1) { navigator.navigate(EarnPickerRoute(preselected = LendingEarnProduct.ID)) }
        }
    }

    private fun createProduct(lendingProvider: FakeLendingProvider) = LendingEarnProduct(
        lendingSupplier = lendingSupplier,
        lendingProvider = lendingProvider,
        toggles = toggles,
        rateBadgeConverter = rateBadgeConverter,
    )

    private fun summaryOf(position: EarnPositionSummary?, onClickAction: EarnAction) = EarnProductSummary(
        productId = EarnProductId("lending"),
        rateText = rateBadgeConverter.convert(RATE),
        risk = EarnRisk.Low,
        availability = ProductAvailability.Available,
        rate = RATE,
        position = position,
        source = StatusSource.ACTUAL,
        onClickAction = onClickAction,
    )

    private fun snapshotOf(state: LendingAssetState) =
        LendingSnapshot(assets = mapOf(eth.id to state), source = StatusSource.ACTUAL)

    private fun assetState(
        position: BigDecimal? = null,
        hasPendingTx: Boolean = false,
        isOpen: Boolean = true,
        rate: EarnProductRate? = RATE,
        gate: LendingDepositGate = LendingDepositGate.Open,
    ) = LendingAssetState(
        asset = asset,
        address = "0x1",
        balance = BigDecimal.TEN,
        fiatRate = null,
        market = LendingMarket(
            rate = rate,
            isOpen = isOpen,
            deposit = gate,
            position = position?.let(::LendingPosition),
            maxWithdraw = position,
            providerState = null,
            source = StatusSource.ACTUAL,
        ),
        hasPendingTx = hasPendingTx,
        isWrappedTokenInWallet = false,
        source = StatusSource.ACTUAL,
    )

    data class SummaryModel(
        val position: BigDecimal?,
        val expectedPosition: EarnPositionSummary?,
        val expectedAction: EarnAction,
    )

    data class GatingModel(val isEarnEnabled: Boolean, val isAvailable: Boolean)

    data class DepositBlockModel(val isOpen: Boolean, val gate: LendingDepositGate, val expectedReason: TextReference)

    data class ActionsModel(
        val position: BigDecimal?,
        val hasPendingTx: Boolean,
        val isOpen: Boolean,
        val expectedPrimary: EarnActionItem,
        val expectedSecondary: List<EarnActionItem>,
    )

    private companion object {
        val WITHDRAW = Withdraw(notice = resourceReference(R.string.lending_withdraw_instant_notice))
        val FEE_TEXT = BigDecimal.ZERO.format { percent(minFractionDigits = 0) }
        val RATE = EarnProductRate(fraction = BigDecimal("0.055"), type = EarnRateType.APY)
        val ADD_FUNDS = resourceReference(R.string.common_add_funds)
        val START_EARNING = resourceReference(R.string.eth_yield_start_earning)
        val PENDING_BLOCK = EarnBlock(
            reason = resourceReference(R.string.lending_error_previous_transaction_processing),
        )
        val DEPOSITS_PAUSED = resourceReference(R.string.lending_error_deposits_paused)
        val CLOSED_BLOCK = EarnBlock(reason = DEPOSITS_PAUSED)
    }
}