package com.tangem.features.earn.impl.info.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_lightning_20
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.impl.R
import com.tangem.features.earn.impl.fake.FakeEarnPosition
import com.tangem.features.earn.impl.fake.FakeEarnProductPreset
import com.tangem.features.earn.impl.fake.createFakeEarnProduct
import com.tangem.features.earn.impl.fake.fakeReadyState
import com.tangem.features.earn.impl.info.EarnInfoActions
import com.tangem.features.earn.impl.info.EarnInfoTexts
import com.tangem.features.earn.impl.info.label
import com.tangem.features.earn.impl.info.ui.state.EarnInfoTab
import com.tangem.features.earn.impl.info.ui.state.EarnInfoUM
import com.tangem.features.earn.impl.picker.ui.state.EarnBenefitUM
import com.tangem.features.earn.product.api.product.DefaultEarnProjection
import com.tangem.features.earn.product.api.product.EarnActionItem
import com.tangem.features.earn.product.api.product.EarnBlock
import com.tangem.features.earn.product.api.product.EarnFreshness
import com.tangem.features.earn.product.api.product.EarnHowItWorks
import com.tangem.features.earn.product.api.product.EarnLimits
import com.tangem.features.earn.product.api.product.EarnPosition
import com.tangem.features.earn.product.api.product.EarnPositionState
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.product.EarnTextBlock
import com.tangem.features.earn.product.api.product.EarnValue
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.EarnAction
import com.tangem.features.earn.product.api.session.Withdraw
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

internal class EarnInfoConverterTest {

    private val product = createFakeEarnProduct(FakeEarnProductPreset.Lending)
    private val ethereum = MockCryptoCurrencyFactory().ethereum
    private val converter = EarnInfoConverter(actions = EarnInfoActions())

    private val ready = fakeReadyState(FakeEarnProductPreset.Lending, FakeEarnPosition.Known)
    private val position = (ready.position as EarnPositionState.Known).position

    private val calls = mutableListOf<Call>()
    private val callbacks = EarnInfoConverter.Callbacks(
        onActionClick = { calls += Call.Action(it) },
        onBlocked = { calls += Call.Blocked(it) },
        onInfoClick = { calls += Call.Info(it) },
        onLinkClick = { calls += Call.Link(it) },
        onHowItWorksClick = { calls += Call.HowItWorks(it) },
        onClose = { calls += Call.Close },
    )

    @BeforeEach
    fun resetCalls() {
        calls.clear()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Position {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN position None or Unknown WHEN convert THEN null`(model: EarnPositionState) {
            // Arrange
            val state = ready.copy(position = model)

            // Act
            val actual = convert(ready = state)

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN position Known WHEN convert THEN title is product title, subtitle is the network name, my funds come from the provider and position`() {
            // Act
            val actual = requireNotNull(convert())

            // Assert
            assertThat(actual.title).isEqualTo(product.title)
            assertThat(actual.subtitle).isEqualTo(stringReference(ethereum.network.name))
            assertThat(actual.myFunds.copy(rows = persistentListOf(), onHowItWorksClick = null)).isEqualTo(
                EarnInfoUM.MyFundsUM(
                    provider = product.provider.title,
                    providerIcon = product.provider.icon,
                    description = position.description,
                    rows = persistentListOf(),
                    onHowItWorksClick = null,
                ),
            )
            assertThat(actual.myFunds.rows.map { it.label to it.value })
                .containsExactlyElementsIn(position.rows.map { it.label to it.textOf() })
                .inOrder()
        }

        @Test
        fun `GIVEN details with info and link WHEN row clicks THEN callbacks receive the block and the url`() {
            // Arrange
            val info = EarnTextBlock(stringReference("Risk"), stringReference("How risky"))
            val rows = listOf(
                EarnRow(stringReference("Risk"), EarnValue.Text(stringReference("Low")), info),
                EarnRow(stringReference("Protocol"), EarnValue.Link(stringReference("Aave"), "https://aave.com")),
            )
            val actual = requireNotNull(convert(ready = ready.copy(details = rows)))

            // Act
            actual.details[0].onInfoClick?.invoke()
            actual.details[1].onValueClick?.invoke()

            // Assert
            assertThat(calls).containsExactly(Call.Info(info), Call.Link("https://aave.com")).inOrder()
        }

        @Test
        fun `GIVEN ready WHEN onClose THEN callbacks receive close`() {
            // Arrange
            val actual = requireNotNull(convert())

            // Act
            actual.onClose()

            // Assert
            assertThat(calls).containsExactly(Call.Close)
        }

        private fun provideTestModels() = listOf(EarnPositionState.None, EarnPositionState.Unknown)
    }

    @Nested
    inner class Buttons {

        @Test
        fun `GIVEN ready WHEN convert THEN primary has Primary type and the item label`() {
            // Act
            val actual = requireNotNull(convert())

            // Assert
            assertThat(actual.primary.noOpClick()).isEqualTo(
                TangemButtonUM(text = ready.primary.label, type = TangemButtonType.Primary, onClick = NoOp),
            )
        }

        @Test
        fun `GIVEN primary with block WHEN convert THEN primary is dimmed and its click reports the block`() {
            // Arrange
            val state = ready.copy(primary = ready.primary.copy(block = PRODUCT_BLOCK))
            val actual = requireNotNull(convert(ready = state))

            // Act
            actual.primary.onClick()

            // Assert
            assertThat(actual.primary.isDimmed).isTrue()
            assertThat(calls).containsExactly(Call.Blocked(PRODUCT_BLOCK))
        }

        @Test
        fun `GIVEN primary without block WHEN primary click THEN callbacks receive the primary action`() {
            // Arrange
            val actual = requireNotNull(convert())

            // Act
            actual.primary.onClick()

            // Assert
            assertThat(calls).containsExactly(Call.Action(ready.primary.action))
        }

        @Test
        fun `GIVEN empty secondary WHEN convert THEN secondary is empty and primary is present`() {
            // Arrange
            val state = ready.copy(secondary = emptyList())

            // Act
            val actual = requireNotNull(convert(ready = state))

            // Assert
            assertThat(actual.secondary).isEmpty()
            assertThat(actual.primary.text).isEqualTo(ready.primary.label)
        }

        @Test
        fun `GIVEN two secondary actions WHEN convert THEN two buttons in product order with Secondary type`() {
            // Arrange
            val withdraw = EarnActionItem(action = Withdraw())
            val deposit = EarnActionItem(action = Deposit(), label = stringReference("Top up"))
            val state = ready.copy(secondary = listOf(withdraw, deposit))

            // Act
            val actual = requireNotNull(convert(ready = state))

            // Assert
            val expected = listOf(
                TangemButtonUM(text = withdraw.label, type = TangemButtonType.Secondary, onClick = NoOp),
                TangemButtonUM(text = deposit.label, type = TangemButtonType.Secondary, onClick = NoOp),
            )
            assertThat(actual.secondary.map { it.noOpClick() }).containsExactlyElementsIn(expected).inOrder()
        }
    }

    @Nested
    inner class Rewards {

        @Test
        fun `GIVEN rewards WHEN convert THEN claim label equals claimAction label and claim click calls onActionClick with claimAction`() {
            // Arrange
            val rewards = requireNotNull(position.rewards)

            // Act
            val actual = requireNotNull(convert()?.rewards)
            actual.onClaimClick()

            // Assert
            val expected = EarnInfoUM.RewardsUM(
                title = EarnInfoTexts.rewardsTitle(fiatText(rewards.amount.multiply(RATE))),
                description = EarnInfoTexts.rewardsDescription,
                claimLabel = rewards.claimAction.label,
                onClaimClick = NoOp,
            )
            assertThat(actual.copy(onClaimClick = NoOp)).isEqualTo(expected)
            assertThat(calls).containsExactly(Call.Action(rewards.claimAction))
        }

        @Test
        fun `GIVEN no rewards WHEN convert THEN rewards is null`() {
            // Arrange
            val state = readyWith(position.copy(rewards = null))

            // Act
            val actual = requireNotNull(convert(ready = state))

            // Assert
            assertThat(actual.rewards).isNull()
        }
    }

    @Nested
    inner class Freshness {

        @Test
        fun `GIVEN stale freshness WHEN convert THEN staleNotice contains the formatted time and both buttons are dimmed with the out-of-date reason`() {
            // Arrange
            val state = ready.copy(
                freshness = EarnFreshness(asOf = Instant.parse("2026-11-09T12:00:00Z"), isStale = true),
                primary = ready.primary.copy(block = PRODUCT_BLOCK),
            )

            // Act
            val actual = requireNotNull(convert(ready = state, locale = Locale.US, zoneId = ZoneOffset.UTC))
            actual.primary.onClick()
            actual.secondary.single().onClick()

            // Assert
            val notice = (actual.staleNotice as TextReference.Str).value
            assertThat(notice).contains("2026")
            assertThat(notice).contains("12:00")
            assertThat(actual.primary.isDimmed).isTrue()
            assertThat(actual.secondary.single().isDimmed).isTrue()
            assertThat(calls).containsExactly(Call.Blocked(OUT_OF_DATE_BLOCK), Call.Blocked(OUT_OF_DATE_BLOCK))
        }

        @Test
        fun `GIVEN fresh data WHEN convert THEN staleNotice is null and the buttons are not dimmed`() {
            // Arrange
            val state = ready.copy(freshness = EarnFreshness(asOf = Instant.parse("2026-11-09T12:00:00Z"), isStale = false))

            // Act
            val actual = requireNotNull(convert(ready = state))

            // Assert
            assertThat(actual.staleNotice).isNull()
            assertThat(actual.primary.isDimmed).isFalse()
            assertThat(actual.secondary.single().isDimmed).isFalse()
        }
    }

    @Nested
    inner class Balance {

        @Test
        fun `GIVEN fiat rate null WHEN convert THEN balanceFiat is the dash and earned is the dash`() {
            // Act
            val actual = requireNotNull(convert(status = status(fiatRate = null)))

            // Assert
            assertThat(actual.balanceFiat).isEqualTo(stringReference(StringsSigns.DASH_SIGN))
            assertThat(actual.earned).isEqualTo(EarnInfoTexts.earnedChip(StringsSigns.DASH_SIGN))
        }

        @Test
        fun `GIVEN zero balance WHEN convert THEN crypto shows zero and fiat is the formatted zero fiat`() {
            // Arrange
            val state = readyWith(position.copy(cryptoAmount = BigDecimal.ZERO))

            // Act
            val actual = requireNotNull(convert(ready = state))

            // Assert
            assertThat(actual.balanceCrypto).isEqualTo(stringReference(cryptoText(BigDecimal.ZERO)))
            assertThat(actual.balanceFiat).isEqualTo(stringReference(fiatText(BigDecimal.ZERO)))
        }

        @Test
        fun `GIVEN balance and rate WHEN convert THEN crypto is the position amount and fiat is converted by the rate`() {
            // Act
            val actual = requireNotNull(convert())

            // Assert
            assertThat(actual.balanceCrypto).isEqualTo(stringReference(cryptoText(position.cryptoAmount)))
            assertThat(actual.balanceFiat).isEqualTo(stringReference(fiatText(position.cryptoAmount.multiply(RATE))))
        }

        @Test
        fun `GIVEN earned null WHEN convert THEN earned is null`() {
            // Arrange
            val state = readyWith(position.copy(earned = null))

            // Act
            val actual = requireNotNull(convert(ready = state))

            // Assert
            assertThat(actual.earned).isNull()
        }

        @Test
        fun `GIVEN earned and rate WHEN convert THEN earned is fiat-converted`() {
            // Arrange
            val earned = requireNotNull(position.earned)

            // Act
            val actual = requireNotNull(convert())

            // Assert
            assertThat(actual.earned).isEqualTo(EarnInfoTexts.earnedChip(fiatText(earned.multiply(RATE))))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Tabs {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN benefits and details presence WHEN convert THEN tabs and benefits follow it`(model: TabsModel) {
            // Arrange
            val state = ready.copy(
                benefits = ready.benefits.takeIf { model.hasBenefits }.orEmpty(),
                details = ready.details.takeIf { model.hasDetails }.orEmpty(),
            )

            // Act
            val actual = requireNotNull(convert(ready = state))

            // Assert
            assertThat(actual.tabs).containsExactlyElementsIn(model.expectedTabs).inOrder()
            assertThat(actual.benefits.isEmpty()).isEqualTo(!model.hasBenefits)
            assertThat(actual.details.isEmpty()).isEqualTo(!model.hasDetails)
        }

        @Test
        fun `GIVEN benefits WHEN convert THEN benefits are benefit UMs of the text blocks with their icons`() {
            // Act
            val actual = requireNotNull(convert())

            // Assert
            val expected = ready.benefits.map { block ->
                EarnBenefitUM(icon = block.icon, title = block.title, description = block.description)
            }
            assertThat(actual.benefits).containsExactlyElementsIn(expected).inOrder()
        }

        @Test
        fun `GIVEN benefits with and without an icon WHEN convert THEN each benefit keeps its icon (null stays null)`() {
            // Arrange
            val icon = TangemIconUM.Icon(Icons.ic_lightning_20)
            val withIcon = EarnTextBlock(stringReference("No lock-ups."), stringReference("Anytime"), icon)
            val withoutIcon = EarnTextBlock(stringReference("Withdraw anytime."), stringReference("On demand"))
            val state = ready.copy(benefits = listOf(withIcon, withoutIcon))

            // Act
            val actual = requireNotNull(convert(ready = state))

            // Assert
            assertThat(actual.benefits).containsExactly(
                EarnBenefitUM(icon = icon, title = withIcon.title, description = withIcon.description),
                EarnBenefitUM(icon = null, title = withoutIcon.title, description = withoutIcon.description),
            ).inOrder()
        }

        @Test
        fun `GIVEN tabs WHEN label THEN every tab has a caption`() {
            // Act
            val actual = EarnInfoTab.entries.map { it.label() }

            // Assert
            assertThat(actual).containsNoDuplicates()
        }

        private fun provideTestModels() = listOf(
            TabsModel(
                hasBenefits = true,
                hasDetails = true,
                expectedTabs = listOf(EarnInfoTab.MyFunds, EarnInfoTab.Benefits, EarnInfoTab.Details),
            ),
            TabsModel(
                hasBenefits = false,
                hasDetails = true,
                expectedTabs = listOf(EarnInfoTab.MyFunds, EarnInfoTab.Details),
            ),
            TabsModel(
                hasBenefits = true,
                hasDetails = false,
                expectedTabs = listOf(EarnInfoTab.MyFunds, EarnInfoTab.Benefits),
            ),
            TabsModel(hasBenefits = false, hasDetails = false, expectedTabs = listOf(EarnInfoTab.MyFunds)),
        )
    }

    @Nested
    inner class HowItWorks {

        @Test
        fun `GIVEN empty apyPeriods, no howItWorks, no rewards, null freshness WHEN convert THEN UM is built and onHowItWorksClick is null`() {
            // Arrange
            val state = readyWith(position.copy(rewards = null)).copy(
                apyPeriods = emptyList(),
                howItWorks = null,
                freshness = null,
            )

            // Act
            val actual = convert(ready = state)

            // Assert
            assertThat(actual).isNotNull()
            assertThat(actual?.myFunds?.onHowItWorksClick).isNull()
            assertThat(actual?.rewards).isNull()
            assertThat(actual?.staleNotice).isNull()
        }

        @Test
        fun `GIVEN howItWorks WHEN onHowItWorksClick THEN callbacks onHowItWorksClick receives it`() {
            // Arrange
            val howItWorks = EarnHowItWorks(
                title = stringReference("How it works"),
                entries = listOf(EarnTextBlock(stringReference("Supply"), stringReference("Your ETH is supplied"))),
            )
            val actual = requireNotNull(convert(ready = ready.copy(howItWorks = howItWorks)))

            // Act
            actual.myFunds.onHowItWorksClick?.invoke()

            // Assert
            assertThat(calls).containsExactly(Call.HowItWorks(howItWorks))
        }

        @Test
        fun `GIVEN howItWorks with empty entries WHEN convert THEN myFunds onHowItWorksClick is null`() {
            // Arrange
            val howItWorks = EarnHowItWorks(title = stringReference("How it works"), entries = emptyList())

            // Act
            val actual = requireNotNull(convert(ready = ready.copy(howItWorks = howItWorks)))

            // Assert
            assertThat(actual.myFunds.onHowItWorksClick).isNull()
        }
    }

    @Nested
    inner class PooledStaking {

        @Test
        fun `GIVEN the real pooled staking state shape WHEN convert THEN my funds has no rows, no secondary, all three tabs`() {
            // Arrange
            val stakingAction = StakingAction(label = stringReference("Manage"))
            val benefit = EarnTextBlock(
                title = stringReference("Yield from Ethereum itself."),
                description = stringReference("You earn from securing the network, not from lending."),
            )
            val withdrawalsInfo = EarnTextBlock(
                title = stringReference("Withdrawals"),
                description = stringReference(
                    "Withdrawals take up to 5 days, because Ethereum releases staked ETH through an exit queue.",
                ),
            )
            val feeInfo = EarnTextBlock(
                title = stringReference("Performance fee"),
                description = stringReference(
                    "10% of the yield you earn. It is not charged on the amount you deposit.",
                ),
            )
            val details = listOf(
                EarnRow(stringReference("Risk"), EarnValue.Text(stringReference("Low"))),
                EarnRow(stringReference("Validator"), EarnValue.Text(stringReference("P2P"))),
                EarnRow(stringReference("Withdrawals"), EarnValue.Text(stringReference("Up to 5 days")), withdrawalsInfo),
                EarnRow(stringReference("Performance fee"), EarnValue.Text(stringReference("10% of yield")), feeInfo),
            )
            val pooledStaking = EarnProductState.Ready(
                rate = EarnProductRate(fraction = BigDecimal("0.03"), type = EarnRateType.APY),
                description = stringReference("Earn variable yield on ETH by staking with P2P."),
                projection = DefaultEarnProjection(netRate = BigDecimal("0.027")),
                benefits = listOf(benefit),
                howItWorks = null,
                details = details,
                limits = EarnLimits(minDeposit = BigDecimal("0.01"), remainingCap = null),
                apyPeriods = emptyList(),
                primary = EarnActionItem(stakingAction),
                freshness = null,
                position = EarnPositionState.Known(
                    EarnPosition(
                        cryptoAmount = BigDecimal("0.5"),
                        earned = BigDecimal("0.01"),
                        description = null,
                        rows = emptyList(),
                        rewards = null,
                        freshness = null,
                    ),
                ),
            )

            // Act
            val actual = requireNotNull(convert(ready = pooledStaking))

            // Assert
            assertThat(actual.myFunds).isEqualTo(
                EarnInfoUM.MyFundsUM(
                    provider = product.provider.title,
                    providerIcon = product.provider.icon,
                    description = null,
                    rows = persistentListOf(),
                    onHowItWorksClick = null,
                ),
            )
            assertThat(actual.balanceCrypto).isEqualTo(stringReference(cryptoText(BigDecimal("0.5"))))
            assertThat(actual.earned).isEqualTo(EarnInfoTexts.earnedChip(fiatText(BigDecimal("0.01").multiply(RATE))))
            assertThat(actual.rewards).isNull()
            assertThat(actual.staleNotice).isNull()
            assertThat(actual.secondary).isEmpty()
            assertThat(actual.tabs)
                .containsExactly(EarnInfoTab.MyFunds, EarnInfoTab.Benefits, EarnInfoTab.Details)
                .inOrder()
            assertThat(actual.benefits)
                .containsExactly(
                    EarnBenefitUM(icon = benefit.icon, title = benefit.title, description = benefit.description),
                )
            assertThat(actual.details.map { Triple(it.label, it.value, it.onInfoClick != null) })
                .containsExactly(
                    Triple(stringReference("Risk"), stringReference("Low"), false),
                    Triple(stringReference("Validator"), stringReference("P2P"), false),
                    Triple(stringReference("Withdrawals"), stringReference("Up to 5 days"), true),
                    Triple(stringReference("Performance fee"), stringReference("10% of yield"), true),
                )
                .inOrder()
            assertThat(actual.primary.noOpClick()).isEqualTo(
                TangemButtonUM(text = stakingAction.label, type = TangemButtonType.Primary, onClick = NoOp),
            )
        }
    }

    private fun convert(
        ready: EarnProductState.Ready = this.ready,
        status: CryptoCurrencyStatus = status(),
        locale: Locale = Locale.US,
        zoneId: ZoneId = ZoneOffset.UTC,
    ): EarnInfoUM? = converter.convert(
        product = product,
        status = status,
        appCurrency = AppCurrency.Default,
        ready = ready,
        callbacks = callbacks,
        locale = locale,
        zoneId = zoneId,
    )

    private fun readyWith(position: EarnPosition) = ready.copy(position = EarnPositionState.Known(position))

    private fun status(fiatRate: BigDecimal? = RATE) = CryptoCurrencyStatus(
        currency = ethereum,
        value = CryptoCurrencyStatus.Custom(
            amount = BigDecimal.ONE,
            fiatAmount = null,
            fiatRate = fiatRate,
            priceChange = null,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                NetworkAddress.Address(value = "0xUser", type = NetworkAddress.Address.Type.Primary),
            ),
            sources = CryptoCurrencyStatus.Sources(),
        ),
    )

    private fun cryptoText(amount: BigDecimal): String = amount.format { crypto(ethereum) }

    private fun fiatText(amount: BigDecimal): String =
        amount.format { fiat(fiatCurrencyCode = AppCurrency.Default.code, fiatCurrencySymbol = AppCurrency.Default.symbol) }

    private fun EarnRow.textOf(): TextReference = when (val rowValue = value) {
        is EarnValue.Text -> rowValue.text
        is EarnValue.Link -> rowValue.text
    }

    private fun TangemButtonUM.noOpClick() = copy(onClick = NoOp)

    internal data class TabsModel(
        val hasBenefits: Boolean,
        val hasDetails: Boolean,
        val expectedTabs: List<EarnInfoTab>,
    )

    internal data class StakingAction(override val label: TextReference) : EarnAction {
        override val id: String = "staking"
    }

    internal sealed interface Call {
        data class Action(val action: EarnAction) : Call
        data class Blocked(val block: EarnBlock) : Call
        data class Info(val block: EarnTextBlock) : Call
        data class Link(val url: String) : Call
        data class HowItWorks(val howItWorks: EarnHowItWorks) : Call
        data object Close : Call
    }

    private companion object {

        val NoOp: () -> Unit = {}
        val RATE = BigDecimal("2000")
        val PRODUCT_BLOCK = EarnBlock(reason = stringReference("blocked"))
        val OUT_OF_DATE_BLOCK = EarnBlock(
            reason = resourceReference(R.string.token_button_unavailability_reason_out_of_date_balance),
        )
    }
}