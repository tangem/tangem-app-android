package com.tangem.features.staking.impl.presentation.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.remove
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.components.list.RoundedListWithDividersItemData
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.StakingBalanceEntry
import com.tangem.domain.staking.model.CooldownPeriod
import com.tangem.domain.staking.model.Period
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.common.RewardInfo
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.fixtures.coin
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.fixtures.p2pIntegration
import com.tangem.features.staking.impl.fixtures.stakeKitIntegration
import com.tangem.features.staking.impl.presentation.model.StakingClickIntents
import com.tangem.features.staking.impl.presentation.state.InnerYieldBalanceState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.bottomsheet.InfoType
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import com.tangem.features.staking.impl.presentation.state.utils.toTextReference
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import com.tangem.utils.Provider
import com.tangem.utils.StringsSigns.DASH_SIGN
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class SetInitialDataStateTransformerTest {

    private val clickIntents: StakingClickIntents = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(clickIntents)
    }

    @Nested
    inner class TopLevel {

        @Test
        fun `GIVEN loading status WHEN transform THEN amount state empty`() {
            // Arrange
            val status = CryptoCurrencyStatus(coin(Blockchain.Solana), CryptoCurrencyStatus.Loading)

            // Act
            val actual = transform(status = status)

            // Assert
            assertThat(actual.amountState).isEqualTo(AmountState.Empty)
        }
    }

    @Nested
    inner class PrimaryAndBanner {

        @Test
        fun `GIVEN positive amount and actual sources WHEN transform THEN primary enabled`() {
            // Act & Assert
            assertThat(info(transform()).isPrimaryButtonEnabled).isTrue()
        }

        @Test
        fun `GIVEN zero amount WHEN transform THEN primary disabled`() {
            // Act & Assert
            assertThat(info(transform(status = currencyStatus(amount = BigDecimal.ZERO))).isPrimaryButtonEnabled)
                .isFalse()
        }

        @Test
        fun `GIVEN no amount WHEN transform THEN primary disabled`() {
            // Act & Assert
            assertThat(info(transform(status = currencyStatus(amount = null))).isPrimaryButtonEnabled).isFalse()
        }

        @Test
        fun `GIVEN cached network source WHEN transform THEN primary disabled`() {
            // Act
            val actual = transform(status = currencyStatus(networkSource = StatusSource.CACHE))

            // Assert
            assertThat(info(actual).isPrimaryButtonEnabled).isFalse()
        }

        @Test
        fun `GIVEN cached staking source WHEN transform THEN primary disabled`() {
            // Arrange
            val status = currencyStatus(stakingBalance = MockStaking.stakeKitBalance(source = StatusSource.CACHE))

            // Act
            val actual = transform(status = status)

            // Assert
            assertThat(info(actual).isPrimaryButtonEnabled).isFalse()
        }

        @Test
        fun `GIVEN nothing staked anywhere WHEN transform THEN banner visible`() {
            // Act
            val actual = info(transform(isAnyTokenStaked = false))

            // Assert
            assertThat(actual.isBannerVisible).isTrue()
            assertThat(actual.yieldBalance).isEqualTo(InnerYieldBalanceState.Empty)
        }

        @Test
        fun `GIVEN another token staked WHEN transform THEN banner hidden`() {
            // Act & Assert
            assertThat(info(transform(isAnyTokenStaked = true)).isBannerVisible).isFalse()
        }

        @Test
        fun `GIVEN own staking balance WHEN transform THEN banner hidden`() {
            // Act
            val actual = info(transform(status = currencyStatus(stakingBalance = MockStaking.stakeKitBalance())))

            // Assert
            assertThat(actual.isBannerVisible).isFalse()
            assertThat(actual.yieldBalance).isInstanceOf(InnerYieldBalanceState.Data::class.java)
        }
    }

    @Nested
    inner class AnnualPercentage {

        private val id = R.string.staking_details_annual_percentage_rate

        @Test
        fun `GIVEN single preferred rate WHEN transform THEN apr start text and single percent`() {
            // Act
            val actual = item(transform(), id)

            // Assert
            assertThat(actual.startText).isEqualTo(TextReference.Res(R.string.staking_details_annual_percentage_rate))
            assertThat(actual.endText).isEqualTo(stringReference(pct("0.05").remove("%") + "%"))
            assertThat(actual.isEndTextHighlighted).isFalse()
        }

        @Test
        fun `GIVEN preferred rates range WHEN transform THEN range text of max type`() {
            // Arrange
            val integration = validatorsIntegration(
                MockStaking.validator(),
                MockStaking.validator(
                    address = MockStaking.SECOND_VALIDATOR_ADDRESS,
                    rewardInfo = RewardInfo(BigDecimal("0.07"), RewardType.APY),
                ),
            )

            // Act
            val actual = item(transform(integration = integration), id)

            // Assert
            assertThat(actual.startText).isEqualTo(TextReference.Res(R.string.staking_details_annual_percentage_yield))
            assertThat(actual.endText).isEqualTo(
                resourceReference(R.string.common_range, wrappedList(pct("0.05").remove("%"), pct("0.07"))),
            )
        }

        @Test
        fun `GIVEN rates differing exactly by equality threshold WHEN transform THEN range text`() {
            // Arrange
            val maxRate = BigDecimal("0.05") + BigDecimal(1E-10)
            val integration = validatorsIntegration(
                MockStaking.validator(),
                MockStaking.validator(
                    address = MockStaking.SECOND_VALIDATOR_ADDRESS,
                    rewardInfo = RewardInfo(maxRate, RewardType.APR),
                ),
            )

            // Act
            val actual = item(transform(integration = integration), id)

            // Assert
            assertThat(actual.endText).isEqualTo(
                resourceReference(
                    R.string.common_range,
                    wrappedList(pct("0.05").remove("%"), maxRate.format { percent() }),
                ),
            )
        }

        @Test
        fun `GIVEN no preferred targets WHEN transform THEN apr dash`() {
            // Arrange
            val integration = validatorsIntegration(MockStaking.validator(preferred = false))

            // Act
            val actual = item(transform(integration = integration), id)

            // Assert
            assertThat(actual.startText).isEqualTo(TextReference.Res(R.string.staking_details_annual_percentage_rate))
            assertThat(actual.endText).isEqualTo(stringReference(DASH_SIGN))
        }

        @Test
        fun `GIVEN preferred without reward info WHEN transform THEN apr dash`() {
            // Arrange
            val integration = validatorsIntegration(MockStaking.validator(rewardInfo = null))

            // Act
            val actual = item(transform(integration = integration), id)

            // Assert
            assertThat(actual.endText).isEqualTo(stringReference(DASH_SIGN))
        }

        @Test
        fun `GIVEN apr item WHEN icon clicked THEN apr info requested`() {
            // Act
            requireNotNull(item(transform(), id).iconClick).invoke()

            // Assert
            verify(exactly = 1) { clickIntents.onInfoClick(InfoType.ANNUAL_PERCENTAGE_RATE) }
        }

        @Test
        fun `GIVEN apy item WHEN icon clicked THEN apy info requested`() {
            // Arrange
            val integration = validatorsIntegration(
                MockStaking.validator(rewardInfo = RewardInfo(BigDecimal("0.05"), RewardType.APY)),
            )

            // Act
            requireNotNull(item(transform(integration = integration), id).iconClick).invoke()

            // Assert
            verify(exactly = 1) { clickIntents.onInfoClick(InfoType.ANNUAL_PERCENTAGE_YIELD) }
        }

        @Test
        fun `GIVEN p2p vault WHEN transform THEN apy of vault`() {
            // Act
            val actual = item(
                transform(integration = p2pIntegration(), status = currencyStatus(coin(Blockchain.Ethereum))),
                id,
            )

            // Assert
            assertThat(actual.startText).isEqualTo(TextReference.Res(R.string.staking_details_annual_percentage_yield))
            assertThat(actual.endText).isEqualTo(stringReference(pct("0.03").remove("%") + "%"))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OptionalRows {

        @Test
        fun `GIVEN solana defaults WHEN transform THEN info item order`() {
            // Act
            val actual = info(transform()).infoItems.map { it.id }

            // Assert
            assertThat(actual).containsExactly(
                R.string.staking_details_annual_percentage_rate,
                R.string.staking_details_available,
                R.string.staking_details_unbonding_period,
                R.string.staking_details_reward_claiming,
                R.string.staking_details_reward_schedule,
            ).inOrder()
        }

        @Test
        fun `GIVEN balance WHEN transform THEN available item formatted and hideable`() {
            // Arrange
            val status = currencyStatus()

            // Act
            val actual = item(transform(status = status), R.string.staking_details_available)

            // Assert
            assertThat(actual.endText).isEqualTo(TextReference.Str(BigDecimal("10").format { crypto(status.currency) }))
            assertThat(actual.isEndTextHideable).isTrue()
            assertThat(actual.iconClick).isNull()
        }

        @Test
        fun `GIVEN cooldown WHEN transform THEN unbonding item with cooldown text`() {
            // Act
            val actual = item(transform(), R.string.staking_details_unbonding_period)
            requireNotNull(actual.iconClick).invoke()

            // Assert
            assertThat(actual.endText).isEqualTo(CooldownPeriod.Fixed(Period.Days(3)).toTextReference())
            verify(exactly = 1) { clickIntents.onInfoClick(InfoType.UNBONDING_PERIOD) }
        }

        @Test
        fun `GIVEN no cooldown WHEN transform THEN no unbonding item`() {
            // Act
            val actual = transform(integration = metadataIntegration(MockStaking.metadata(cooldownPeriod = null)))

            // Assert
            assertThat(ids(actual)).doesNotContain(R.string.staking_details_unbonding_period)
        }

        @Test
        fun `GIVEN cardano with minimum WHEN transform THEN minimum requirement item`() {
            // Arrange
            val cardano = coin(Blockchain.Cardano)
            val integration = stakeKitIntegration(
                id = StakingIntegrationID.StakeKit.Coin.Cardano,
                yield = MockStaking.yield(
                    id = MockStaking.CARDANO_ID,
                    token = MockStaking.adaToken,
                    enter = MockStaking.enterArgs(minimum = BigDecimal("5")),
                ),
            )

            // Act
            val actual = item(
                transform(integration = integration, status = currencyStatus(cardano)),
                R.string.staking_details_minimum_requirement,
            )

            // Assert
            assertThat(actual.endText).isEqualTo(TextReference.Str(BigDecimal("5").format { crypto(cardano) }))
        }

        // Pins current behaviour: Polkadot shows the minimum row although no Polkadot integration exists.
        @Test
        fun `GIVEN polkadot with minimum WHEN transform THEN minimum requirement item`() {
            // Arrange
            val integration = stakeKitIntegration(
                yield = MockStaking.yield(enter = MockStaking.enterArgs(minimum = BigDecimal("5"))),
            )

            // Act
            val actual = transform(integration = integration, status = currencyStatus(coin(Blockchain.Polkadot)))

            // Assert
            assertThat(ids(actual)).contains(R.string.staking_details_minimum_requirement)
        }

        @Test
        fun `GIVEN solana with minimum WHEN transform THEN no minimum item`() {
            // Act & Assert
            assertThat(ids(transform())).doesNotContain(R.string.staking_details_minimum_requirement)
        }

        @Test
        fun `GIVEN cardano without minimum WHEN transform THEN no minimum item`() {
            // Arrange
            val integration = stakeKitIntegration(
                id = StakingIntegrationID.StakeKit.Coin.Cardano,
                yield = MockStaking.yield(
                    id = MockStaking.CARDANO_ID,
                    token = MockStaking.adaToken,
                    enter = MockStaking.enterArgs(minimum = null),
                ),
            )

            // Act
            val actual = transform(integration = integration, status = currencyStatus(coin(Blockchain.Cardano)))

            // Assert
            assertThat(ids(actual)).doesNotContain(R.string.staking_details_minimum_requirement)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN reward claiming WHEN transform THEN claiming text`(model: ClaimingModel) {
            // Arrange
            val integration = metadataIntegration(MockStaking.metadata(rewardClaiming = model.claiming))

            // Act
            val actual = item(transform(integration = integration), R.string.staking_details_reward_claiming)
            requireNotNull(actual.iconClick).invoke()

            // Assert
            assertThat(actual.endText).isEqualTo(TextReference.Res(model.expectedId))
            verify(exactly = 1) { clickIntents.onInfoClick(InfoType.REWARD_CLAIMING) }
        }

        @Test
        fun `GIVEN unknown reward claiming WHEN transform THEN no claiming item`() {
            // Act
            val actual = transform(
                integration = metadataIntegration(
                    MockStaking.metadata(rewardClaiming = Yield.Metadata.RewardClaiming.UNKNOWN),
                ),
            )

            // Assert
            assertThat(ids(actual)).doesNotContain(R.string.staking_details_reward_claiming)
        }

        @Test
        fun `GIVEN warmup days WHEN transform THEN warmup item in days`() {
            // Arrange
            val integration = metadataIntegration(
                MockStaking.metadata(warmupPeriod = Yield.Metadata.Period(days = 2, seconds = null)),
            )

            // Act
            val actual = item(transform(integration = integration), R.string.staking_details_warmup_period)
            requireNotNull(actual.iconClick).invoke()

            // Assert
            assertThat(actual.endText).isEqualTo(pluralReference(R.plurals.common_days, 2, wrappedList(2)))
            verify(exactly = 1) { clickIntents.onInfoClick(InfoType.WARMUP_PERIOD) }
        }

        @Test
        fun `GIVEN warmup seconds WHEN transform THEN warmup item in hours`() {
            // Arrange
            val integration = metadataIntegration(
                MockStaking.metadata(warmupPeriod = Yield.Metadata.Period(days = 0, seconds = 7200)),
            )

            // Act
            val actual = item(transform(integration = integration), R.string.staking_details_warmup_period)

            // Assert
            assertThat(actual.endText).isEqualTo(pluralReference(R.plurals.common_hours, 2, wrappedList(2)))
        }

        @Test
        fun `GIVEN reward schedule WHEN transform THEN schedule item`() {
            // Act
            val actual = item(transform(), R.string.staking_details_reward_schedule)
            requireNotNull(actual.iconClick).invoke()

            // Assert
            assertThat(actual.startText).isEqualTo(resourceReference(R.string.staking_details_reward_schedule))
            assertThat(actual.endText).isEqualTo(resourceReference(R.string.staking_reward_schedule_day))
            verify(exactly = 1) { clickIntents.onInfoClick(InfoType.REWARD_SCHEDULE) }
        }

        @Test
        fun `GIVEN unknown reward schedule WHEN transform THEN no schedule item`() {
            // Act
            val actual = transform(
                integration = metadataIntegration(
                    MockStaking.metadata(rewardSchedule = Yield.Metadata.RewardSchedule.UNKNOWN),
                ),
            )

            // Assert
            assertThat(ids(actual)).doesNotContain(R.string.staking_details_reward_schedule)
        }

        private fun provideTestModels() = listOf(
            ClaimingModel(Yield.Metadata.RewardClaiming.AUTO, R.string.staking_reward_claiming_auto),
            ClaimingModel(Yield.Metadata.RewardClaiming.MANUAL, R.string.staking_reward_claiming_manual),
        )
    }

    private fun validatorsIntegration(vararg validators: Yield.Validator) =
        stakeKitIntegration(yield = MockStaking.yield(validators = validators.toList()))

    private fun metadataIntegration(metadata: Yield.Metadata) =
        stakeKitIntegration(yield = MockStaking.yield(metadata = metadata))

    private fun pct(value: String) = BigDecimal(value).format { percent() }

    private fun transform(
        integration: StakingIntegration = stakeKitIntegration(),
        status: CryptoCurrencyStatus = currencyStatus(),
        isAnyTokenStaked: Boolean = false,
        entries: List<StakingBalanceEntry> = emptyList(),
    ): StakingUiState = SetInitialDataStateTransformer(
        clickIntents = clickIntents,
        integration = integration,
        isAnyTokenStaked = isAnyTokenStaked,
        cryptoCurrencyStatus = status,
        userWalletProvider = Provider { MockUserWalletFactory.create() },
        appCurrencyProvider = Provider { AppCurrency.Default },
        balancesToShowProvider = Provider { entries },
        isAccountsModeEnabled = false,
        account = null,
        isBalanceHidden = false,
    ).transform(stakingUiState())

    private fun info(state: StakingUiState) = state.initialInfoState as StakingStates.InitialInfoState.Data

    private fun ids(state: StakingUiState) = info(state).infoItems.map { it.id }

    private fun item(state: StakingUiState, id: Int): RoundedListWithDividersItemData =
        info(state).infoItems.single { it.id == id }

    internal data class ClaimingModel(val claiming: Yield.Metadata.RewardClaiming, val expectedId: Int)
}