package com.tangem.features.staking.impl.presentation.state.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingBalanceEntry
import com.tangem.domain.models.staking.StakingEntryActions
import com.tangem.domain.models.staking.StakingEntryType
import com.tangem.domain.models.staking.ValidatorInfo
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.model.CooldownPeriod
import com.tangem.domain.staking.model.Period
import com.tangem.domain.staking.model.StakeKitIntegration
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.fixtures.coin
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.fixtures.p2pEntry
import com.tangem.features.staking.impl.fixtures.p2pIntegration
import com.tangem.features.staking.impl.fixtures.stakeKitEntry
import com.tangem.features.staking.impl.fixtures.stakeKitIntegration
import com.tangem.features.staking.impl.presentation.state.utils.toTextReference
import com.tangem.test.mock.staking.MockStaking
import com.tangem.utils.Provider
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.util.TimeZone

internal class StakingBalanceEntryConverterTest {

    private val jailedTarget = StakingTarget.Validator(
        Yield.Validator(
            address = JAILED_ADDRESS,
            status = Yield.Validator.ValidatorStatus.JAILED,
            name = "Jailed validator",
            isPreferred = false,
            isStrategicPartner = false,
        ),
    )

    private val integration: StakeKitIntegration = mockk {
        every { allTargets } returns listOf(jailedTarget)
    }

    private val converter = StakingBalanceEntryConverter(
        cryptoCurrencyStatus = CryptoCurrencyStatus(
            currency = mockk(relaxed = true),
            value = mockk(relaxed = true) {
                every { this@mockk.fiatRate } returns null
                every { this@mockk.contributions } returns emptyList()
            },
        ),
        appCurrencyProvider = Provider { AppCurrency.Default },
        integration = integration,
    )

    @Test
    fun `GIVEN stake at a jailed validator WHEN convert THEN row is kept with the validator name`() {
        // Arrange
        val entry = createStakedEntry(validatorAddress = JAILED_ADDRESS)

        // Act
        val actual = converter.convert(entry)

        // Assert
        assertThat(actual?.title).isEqualTo(stringReference("Jailed validator"))
        assertThat(actual?.target).isEqualTo(jailedTarget)
    }

    @Test
    fun `GIVEN stake at a validator StakeKit no longer lists WHEN convert THEN row keeps that validator as target`() {
        // Arrange
        val entry = createStakedEntry(validatorAddress = UNKNOWN_ADDRESS)

        // Act
        val actual = converter.convert(entry)

        // Assert
        assertThat(actual?.title).isEqualTo(stringReference("cosmos...wxyz"))
        assertThat(actual?.target?.address).isEqualTo(UNKNOWN_ADDRESS)
        assertThat(actual?.target?.isPreferred).isFalse()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Titles {

        @Test
        fun `GIVEN staked entry at listed validator WHEN convert THEN validator name title`() {
            // Act
            val actual = converter().convert(stakeKitEntry(type = StakingEntryType.STAKED))

            // Assert
            assertThat(actual?.title).isEqualTo(stringReference("Validator 1"))
            assertThat(actual?.type).isEqualTo(BalanceType.STAKED)
        }

        @Test
        fun `GIVEN staked entry at listed validator WHEN convert THEN validator address as target address`() {
            // Act
            val actual = converter().convert(stakeKitEntry(type = StakingEntryType.STAKED))

            // Assert
            assertThat(actual?.targetAddress).isEqualTo(MockStaking.VALIDATOR_ADDRESS)
        }

        @Test
        fun `GIVEN preparing entry WHEN convert THEN validator name title and preparing type`() {
            // Act
            val actual = converter().convert(stakeKitEntry(type = StakingEntryType.PREPARING))

            // Assert
            assertThat(actual?.title).isEqualTo(stringReference("Validator 1"))
            assertThat(actual?.type).isEqualTo(BalanceType.PREPARING)
        }

        @Test
        fun `GIVEN withdrawable entry WHEN convert THEN unstaked title and unstaked type`() {
            // Act
            val actual = converter().convert(stakeKitEntry(type = StakingEntryType.WITHDRAWABLE))

            // Assert
            assertThat(actual?.title).isEqualTo(resourceReference(R.string.staking_unstaked))
            assertThat(actual?.type).isEqualTo(BalanceType.UNSTAKED)
        }

        @Test
        fun `GIVEN unstaking entry WHEN convert THEN unstaking title`() {
            // Act
            val actual = converter().convert(stakeKitEntry(type = StakingEntryType.UNSTAKING))

            // Assert
            assertThat(actual?.title).isEqualTo(resourceReference(R.string.staking_unstaking))
            assertThat(actual?.type).isEqualTo(BalanceType.UNSTAKING)
        }

        @Test
        fun `GIVEN locked entry WHEN convert THEN locked title`() {
            // Act
            val actual = converter().convert(stakeKitEntry(type = StakingEntryType.LOCKED))

            // Assert
            assertThat(actual?.title).isEqualTo(resourceReference(R.string.staking_locked))
            assertThat(actual?.type).isEqualTo(BalanceType.LOCKED)
        }

        @ParameterizedTest
        @EnumSource(value = StakingEntryType::class, names = ["AVAILABLE", "REWARDS", "UNLOCKING", "UNKNOWN"])
        fun `GIVEN entry type without title WHEN convert THEN null`(type: StakingEntryType) {
            // Act & Assert
            assertThat(converter().convert(stakeKitEntry(type = type))).isNull()
        }

        @Test
        fun `GIVEN withdrawable entry without validator WHEN convert THEN no target address`() {
            // Act
            val actual = converter().convert(
                stakeKitEntry(type = StakingEntryType.WITHDRAWABLE, validatorAddress = null),
            )

            // Assert
            assertThat(actual?.targetAddress).isNull()
            assertThat(actual?.title).isEqualTo(resourceReference(R.string.staking_unstaked))
        }

        @Test
        fun `GIVEN staked entry without validator WHEN convert THEN null`() {
            // Act & Assert
            assertThat(converter().convert(stakeKitEntry(validatorAddress = null))).isNull()
        }

        @Test
        fun `GIVEN p2p staked entry at unknown vault WHEN convert THEN entry vault name used and target null`() {
            // Arrange
            val converter = converter(currency = coin(Blockchain.Ethereum), integration = p2pIntegration())

            // Act
            val actual = converter.convert(p2pEntry(vaultAddress = "0xother", vaultName = "Other"))

            // Assert
            assertThat(actual?.title).isEqualTo(stringReference("Other"))
            assertThat(actual?.target).isNull()
        }
    }

    @Nested
    inner class Amounts {

        private val cardano = coin(Blockchain.Cardano)
        private val cardanoIntegration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Cardano)

        @Test
        fun `GIVEN cardano stakeKit balance with rewards WHEN convert THEN amount minus total rewards`() {
            // Arrange
            val converter = converter(
                currency = cardano,
                stakingBalance = MockStaking.stakeKitBalance(
                    items = listOf(
                        MockStaking.balanceItem(BalanceType.STAKED, BigDecimal("10"), token = MockStaking.adaToken),
                        MockStaking.balanceItem(BalanceType.REWARDS, BigDecimal("0.5"), token = MockStaking.adaToken),
                    ),
                    integrationId = MockStaking.CARDANO_ID,
                ),
                integration = cardanoIntegration,
            )

            // Act
            val actual = converter.convert(stakeKitEntry(amount = BigDecimal("10")))

            // Assert
            assertThat(actual?.cryptoAmount).isEquivalentAccordingToCompareTo(BigDecimal("9.5"))
            assertThat(actual?.fiatAmount).isEquivalentAccordingToCompareTo(BigDecimal("19"))
        }

        @Test
        fun `GIVEN cardano without staking balance WHEN convert THEN full amount`() {
            // Arrange
            val converter = converter(currency = cardano, integration = cardanoIntegration)

            // Act
            val actual = converter.convert(stakeKitEntry(amount = BigDecimal("10")))

            // Assert
            assertThat(actual?.cryptoAmount).isEquivalentAccordingToCompareTo(BigDecimal("10"))
        }

        @Test
        fun `GIVEN solana with rewards WHEN convert THEN full amount`() {
            // Arrange
            val converter = converter(
                stakingBalance = MockStaking.stakeKitBalance(
                    items = listOf(MockStaking.balanceItem(BalanceType.REWARDS, BigDecimal("0.5"))),
                ),
            )

            // Act
            val actual = converter.convert(stakeKitEntry(amount = BigDecimal("10")))

            // Assert
            assertThat(actual?.cryptoAmount).isEquivalentAccordingToCompareTo(BigDecimal("10"))
        }

        @Test
        fun `GIVEN no fiat rate WHEN convert THEN fiat amount null`() {
            // Act
            val actual = converter(fiatRate = null).convert(stakeKitEntry())

            // Assert
            assertThat(actual?.fiatAmount).isNull()
        }
    }

    @Nested
    inner class Subtitles {

        private val tron = coin(Blockchain.Tron)
        private val tronIntegration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Tron)

        @Test
        fun `GIVEN withdrawable WHEN convert THEN tap to withdraw subtitle`() {
            // Act
            val actual = converter().convert(stakeKitEntry(type = StakingEntryType.WITHDRAWABLE))

            // Assert
            assertThat(actual?.subtitle).isEqualTo(resourceReference(R.string.staking_tap_to_withdraw))
        }

        @Test
        fun `GIVEN tron locked with vote locked action WHEN convert THEN tap to unlock or vote subtitle`() {
            // Arrange
            val entry = stakeKitEntry(
                type = StakingEntryType.LOCKED,
                pendingActions = listOf(
                    MockStaking.pendingAction(StakingActionType.VOTE_LOCKED),
                    MockStaking.pendingAction(StakingActionType.UNLOCK_LOCKED),
                ),
            )

            // Act
            val actual = converter(currency = tron, integration = tronIntegration).convert(entry)

            // Assert
            assertThat(actual?.subtitle).isEqualTo(resourceReference(R.string.staking_tap_to_unlock_or_vote))
        }

        @Test
        fun `GIVEN locked without vote locked WHEN convert THEN tap to unlock subtitle`() {
            // Arrange
            val entry = stakeKitEntry(
                type = StakingEntryType.LOCKED,
                pendingActions = listOf(MockStaking.pendingAction(StakingActionType.UNLOCK_LOCKED)),
            )

            // Act
            val actual = converter(currency = tron, integration = tronIntegration).convert(entry)

            // Assert
            assertThat(actual?.subtitle).isEqualTo(resourceReference(R.string.staking_tap_to_unlock))
        }

        @Test
        fun `GIVEN preparing with warmup days WHEN convert THEN warmup days subtitle`() {
            // Arrange
            val integration = warmupIntegration(Yield.Metadata.Period(days = 2, seconds = null))

            // Act
            val actual = converter(integration = integration).convert(stakeKitEntry(type = StakingEntryType.PREPARING))

            // Assert
            assertThat(actual?.subtitle).isEqualTo(
                warmupSubtitle(pluralReference(R.plurals.common_days, count = 2, formatArgs = wrappedList(2))),
            )
        }

        @Test
        fun `GIVEN preparing with warmup seconds WHEN convert THEN warmup hours subtitle`() {
            // Arrange
            val integration = warmupIntegration(Yield.Metadata.Period(days = 0, seconds = 7200))

            // Act
            val actual = converter(integration = integration).convert(stakeKitEntry(type = StakingEntryType.PREPARING))

            // Assert
            assertThat(actual?.subtitle).isEqualTo(
                warmupSubtitle(pluralReference(R.plurals.common_hours, count = 2, formatArgs = wrappedList(2))),
            )
        }

        @Test
        fun `GIVEN staked WHEN convert THEN no subtitle`() {
            // Act
            val actual = converter().convert(stakeKitEntry(type = StakingEntryType.STAKED))

            // Assert
            assertThat(actual?.subtitle).isNull()
        }

        private fun warmupIntegration(warmup: Yield.Metadata.Period) = stakeKitIntegration(
            yield = MockStaking.yield(metadata = MockStaking.metadata(warmupPeriod = warmup)),
        )

        private fun warmupSubtitle(period: TextReference): TextReference = TextReference.Combined(
            wrappedList(resourceReference(R.string.staking_details_warmup_period), stringReference(" "), period),
        )
    }

    /**
     * The converter reads `Calendar.getInstance()` (not injectable), so dates are anchored to today's UTC midnight
     * plus 12 hours: the computed day difference cannot flip when the test runs close to midnight.
     */
    @Nested
    inner class Unbonding {

        private lateinit var defaultTimeZone: TimeZone

        @BeforeEach
        fun setUp() {
            defaultTimeZone = TimeZone.getDefault()
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        }

        @AfterEach
        fun tearDown() {
            TimeZone.setDefault(defaultTimeZone)
        }

        @Test
        fun `GIVEN unstaking without date WHEN convert THEN unbonding period subtitle`() {
            // Act
            val actual = converter().convert(stakeKitEntry(type = StakingEntryType.UNSTAKING))

            // Assert
            assertThat(actual?.subtitle).isEqualTo(unbondingSubtitle(CooldownPeriod.Fixed(Period.Days(3))))
        }

        @Test
        fun `GIVEN unstaking without cooldown WHEN convert THEN no subtitle`() {
            // Arrange
            val integration = stakeKitIntegration(
                yield = MockStaking.yield(metadata = MockStaking.metadata(cooldownPeriod = null)),
            )

            // Act
            val actual = converter(integration = integration).convert(stakeKitEntry(type = StakingEntryType.UNSTAKING))

            // Assert
            assertThat(actual?.subtitle).isNull()
        }

        @Test
        fun `GIVEN unstaking ending tomorrow WHEN convert THEN one day left subtitle`() {
            // Act
            val actual = converter().convert(unstakingAt(middayInDays(1)))

            // Assert
            assertThat(actual?.subtitle).isEqualTo(
                resourceReference(
                    R.string.common_left,
                    wrappedList(pluralReference(R.plurals.common_days, count = 1, formatArgs = wrappedList(1))),
                ),
            )
        }

        @Test
        fun `GIVEN unstaking ending today WHEN convert THEN today subtitle`() {
            // Act
            val actual = converter().convert(unstakingAt(middayInDays(0)))

            // Assert
            assertThat(actual?.subtitle).isEqualTo(resourceReference(R.string.common_today))
        }

        @Test
        fun `GIVEN unstaking date in past WHEN convert THEN today subtitle`() {
            // Act
            val actual = converter().convert(unstakingAt(middayInDays(-2)))

            // Assert
            assertThat(actual?.subtitle).isEqualTo(resourceReference(R.string.common_today))
        }

        private fun unstakingAt(date: Instant) = stakeKitEntry(type = StakingEntryType.UNSTAKING, date = date)

        private fun middayInDays(days: Int): Instant {
            val todayMidnight = System.currentTimeMillis() / DAY_MILLIS * DAY_MILLIS
            return Instant.fromEpochMilliseconds(todayMidnight + days * DAY_MILLIS + DAY_MILLIS / 2)
        }

        private fun unbondingSubtitle(cooldown: CooldownPeriod): TextReference = TextReference.Combined(
            wrappedList(
                resourceReference(R.string.staking_details_unbonding_period),
                stringReference(" "),
                cooldown.toTextReference(),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Clickability {

        private val ton = coin(Blockchain.TON)
        private val tonIntegration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Ton)
        private val withdraw = MockStaking.pendingAction(StakingActionType.WITHDRAW)

        @Test
        fun `GIVEN ton preparing with withdraw action WHEN convert THEN clickable`() {
            // Arrange
            val entry = stakeKitEntry(type = StakingEntryType.PREPARING, pendingActions = listOf(withdraw))

            // Act
            val actual = converter(currency = ton, integration = tonIntegration).convert(entry)

            // Assert
            assertThat(actual?.isClickable).isTrue()
            assertThat(actual?.pendingActions).containsExactly(withdraw)
        }

        // Pins current behaviour: the TON preparing branch ignores isPending, unlike every other clickable type.
        @Test
        fun `GIVEN ton preparing pending with withdraw action WHEN convert THEN clickable ignoring pending`() {
            // Arrange
            val entry = stakeKitEntry(
                type = StakingEntryType.PREPARING,
                pendingActions = listOf(withdraw),
                isPending = true,
            )

            // Act
            val actual = converter(currency = ton, integration = tonIntegration).convert(entry)

            // Assert
            assertThat(actual?.isClickable).isTrue()
        }

        @Test
        fun `GIVEN ton preparing without withdraw WHEN convert THEN not clickable`() {
            // Act
            val actual = converter(currency = ton, integration = tonIntegration)
                .convert(stakeKitEntry(type = StakingEntryType.PREPARING))

            // Assert
            assertThat(actual?.isClickable).isFalse()
        }

        @Test
        fun `GIVEN ton preparing with only non withdraw action WHEN convert THEN not clickable`() {
            // Act
            val actual = converter(currency = ton, integration = tonIntegration).convert(
                stakeKitEntry(
                    type = StakingEntryType.PREPARING,
                    pendingActions = listOf(MockStaking.pendingAction(StakingActionType.CLAIM_REWARDS)),
                ),
            )

            // Assert
            assertThat(actual?.isClickable).isFalse()
        }

        @Test
        fun `GIVEN solana preparing with withdraw WHEN convert THEN not clickable`() {
            // Act
            val actual = converter()
                .convert(stakeKitEntry(type = StakingEntryType.PREPARING, pendingActions = listOf(withdraw)))

            // Assert
            assertThat(actual?.isClickable).isFalse()
        }

        @ParameterizedTest
        @EnumSource(value = StakingEntryType::class, names = ["STAKED", "WITHDRAWABLE", "LOCKED"])
        fun `GIVEN clickable type not pending WHEN convert THEN clickable`(type: StakingEntryType) {
            // Act & Assert
            assertThat(converter().convert(stakeKitEntry(type = type))?.isClickable).isTrue()
        }

        @Test
        fun `GIVEN staked pending WHEN convert THEN not clickable`() {
            // Act & Assert
            assertThat(converter().convert(stakeKitEntry(isPending = true))?.isClickable).isFalse()
        }

        @Test
        fun `GIVEN unstaking WHEN convert THEN not clickable`() {
            // Act & Assert
            assertThat(converter().convert(stakeKitEntry(type = StakingEntryType.UNSTAKING))?.isClickable).isFalse()
        }

        @Test
        fun `GIVEN p2p withdrawable WHEN convert THEN stub withdraw action`() {
            // Arrange
            val converter = converter(currency = coin(Blockchain.Ethereum), integration = p2pIntegration())

            // Act
            val actual = converter.convert(p2pEntry(type = StakingEntryType.WITHDRAWABLE))

            // Assert
            assertThat(actual?.pendingActions)
                .containsExactly(PendingAction(StakingActionType.WITHDRAW, passthrough = "", args = null))
        }

        @Test
        fun `GIVEN p2p staked WHEN convert THEN no pending actions`() {
            // Arrange
            val converter = converter(currency = coin(Blockchain.Ethereum), integration = p2pIntegration())

            // Act
            val actual = converter.convert(p2pEntry(type = StakingEntryType.STAKED))

            // Assert
            assertThat(actual?.pendingActions).isEmpty()
        }
    }

    private fun converter(
        currency: CryptoCurrency = coin(Blockchain.Solana),
        stakingBalance: StakingBalance? = null,
        integration: StakingIntegration = stakeKitIntegration(),
        fiatRate: BigDecimal? = BigDecimal("2"),
    ) = StakingBalanceEntryConverter(
        cryptoCurrencyStatus = currencyStatus(currency, BigDecimal("100"), fiatRate, stakingBalance),
        appCurrencyProvider = Provider { AppCurrency.Default },
        integration = integration,
    )

    private fun createStakedEntry(validatorAddress: String) = StakingBalanceEntry(
        id = "group",
        type = StakingEntryType.STAKED,
        amount = BigDecimal.ONE,
        validator = ValidatorInfo(address = validatorAddress, name = null),
        date = null,
        actions = StakingEntryActions.StakeKit(pendingActions = emptyList(), pendingActionsConstraints = emptyList()),
        isPending = false,
        rawCurrencyId = null,
    )

    private companion object {
        const val JAILED_ADDRESS = "cosmosvaloper1jailed"
        const val UNKNOWN_ADDRESS = "cosmosvaloper1unknownwxyz"
        const val DAY_MILLIS = 86_400_000L
    }
}