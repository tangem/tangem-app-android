package com.tangem.domain.staking.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.staking.YieldToken
import com.tangem.domain.staking.fixtures.stakeKitIntegration
import com.tangem.domain.staking.model.common.RewardClaiming
import com.tangem.domain.staking.model.common.RewardSchedule
import com.tangem.domain.staking.model.common.StakingActionArgs
import com.tangem.domain.staking.model.common.StakingAmountRequirement
import com.tangem.domain.staking.model.stakekit.AddressArgument
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class StakeKitIntegrationTest {

    private fun integration(
        metadata: Yield.Metadata = MockStaking.metadata(),
        tokens: List<YieldToken>? = null,
    ) = stakeKitIntegration(
        id = StakingIntegrationID.StakeKit.Coin.Solana,
        yield = MockStaking.yield(metadata = metadata).let { y -> tokens?.let { y.copy(tokens = it) } ?: y },
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class YieldDelegation {

        private val id = StakingIntegrationID.StakeKit.Coin.Solana

        private val token: YieldToken = MockStaking.yieldFor(id.value).token

        private val v1 = MockStaking.validator(address = "validator-1", preferred = true)
        private val v2 = MockStaking.validator(address = "validator-2", name = "Validator 2", preferred = false)
        private val v3 = MockStaking.validator(
            address = "validator-3",
            name = "Validator 3",
            status = Yield.Validator.ValidatorStatus.JAILED,
            preferred = false,
        )
        private val full1 = MockStaking.validator(
            address = "validator-1",
            status = Yield.Validator.ValidatorStatus.FULL,
        )
        private val full2 = MockStaking.validator(
            address = "validator-2",
            status = Yield.Validator.ValidatorStatus.FULL,
        )
        private val noAmountEnter = Yield.Args.Enter(
            addresses = Yield.Args.Enter.Addresses(address = AddressArgument(isRequired = true)),
            args = emptyMap(),
        )
        private val secondary = MockStaking.yieldToken(network = token.network, symbol = "ALT", coinGeckoId = "alt-gecko")

        private fun testYield(
            validators: List<Yield.Validator> = listOf(MockStaking.validator()),
            allValidators: List<Yield.Validator> = validators,
            enter: Yield.Args.Enter = MockStaking.enterArgs(),
            exit: Yield.Args.Enter? = MockStaking.enterArgs(),
        ) = MockStaking.yield(
            id = id.value,
            token = token,
            metadata = MockStaking.metadata(token),
            validators = validators,
            allValidators = allValidators,
            enter = enter,
            exit = exit,
        )

        private fun testIntegration(yield: Yield = testYield()) = stakeKitIntegration(id = id, yield = yield)

        @Test
        fun `GIVEN active and jailed validators WHEN targets read THEN only active validators returned`() {
            // Arrange
            val yield = testYield(validators = listOf(v1, v2), allValidators = listOf(v1, v2, v3))

            // Act
            val actual = testIntegration(yield).targets

            // Assert
            assertThat(actual)
                .containsExactly(StakingTarget.Validator(v1), StakingTarget.Validator(v2))
                .inOrder()
        }

        @Test
        fun `GIVEN active and jailed validators WHEN allTargets read THEN jailed validator included and inactive`() {
            // Arrange
            val yield = testYield(validators = listOf(v1, v2), allValidators = listOf(v1, v2, v3))

            // Act
            val actual = testIntegration(yield).allTargets

            // Assert
            assertThat(actual).isEqualTo(
                listOf(StakingTarget.Validator(v1), StakingTarget.Validator(v2), StakingTarget.Validator(v3)),
            )
            assertThat(actual[2].isActive).isFalse()
        }

        @Test
        fun `GIVEN all validators FULL WHEN areAllTargetsFull read THEN true`() {
            // Arrange
            val yield = testYield(validators = listOf(full1, full2))

            // Act
            val actual = testIntegration(yield).areAllTargetsFull

            // Assert
            assertThat(actual).isTrue()
        }

        @Test
        fun `GIVEN one FULL and one active validator WHEN areAllTargetsFull read THEN false`() {
            // Arrange
            val yield = testYield(validators = listOf(full1, v2))

            // Act
            val actual = testIntegration(yield).areAllTargetsFull

            // Assert
            assertThat(actual).isFalse()
        }

        @Test
        fun `GIVEN enter minimum 1 and exit minimum 2 WHEN minimum amounts read THEN both mapped`() {
            // Arrange
            val yield = testYield(
                enter = MockStaking.enterArgs(minimum = BigDecimal.ONE),
                exit = MockStaking.enterArgs(minimum = BigDecimal("2")),
            )

            // Act
            val actual = testIntegration(yield)

            // Assert
            assertThat(actual.enterMinimumAmount).isEqualTo(BigDecimal.ONE)
            assertThat(actual.exitMinimumAmount).isEqualTo(BigDecimal("2"))
        }

        @Test
        fun `GIVEN no exit args WHEN exit values read THEN exitMinimumAmount and exitArgs are null`() {
            // Arrange
            val yield = testYield(exit = null)

            // Act
            val actual = testIntegration(yield)

            // Assert
            assertThat(actual.exitMinimumAmount).isNull()
            assertThat(actual.exitArgs).isNull()
        }

        @Test
        fun `GIVEN exit without amount argument WHEN exitMinimumAmount read THEN null`() {
            // Arrange
            val yield = testYield(exit = noAmountEnter)

            // Act
            val actual = testIntegration(yield).exitMinimumAmount

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN enter without amount argument WHEN enter values read THEN minimum and requirement are null`() {
            // Arrange
            val yield = testYield(enter = noAmountEnter)

            // Act
            val actual = testIntegration(yield)

            // Assert
            assertThat(actual.enterMinimumAmount).isNull()
            assertThat(actual.enterArgs).isEqualTo(
                StakingActionArgs(amountRequirement = null, isPartialAmountDisabled = false),
            )
        }

        @Test
        fun `GIVEN amount argument with minimum and maximum WHEN enterArgs read THEN requirement mapped`() {
            // Arrange
            val yield = testYield(enter = MockStaking.enterArgs(minimum = BigDecimal.ONE, maximum = BigDecimal("100")))

            // Act
            val actual = testIntegration(yield).enterArgs

            // Assert
            assertThat(actual).isEqualTo(
                StakingActionArgs(
                    amountRequirement = StakingAmountRequirement(
                        isRequired = true,
                        minimum = BigDecimal.ONE,
                        maximum = BigDecimal("100"),
                    ),
                    isPartialAmountDisabled = false,
                ),
            )
        }

        @Test
        fun `GIVEN optional exit amount argument WHEN exitArgs read THEN requirement not required`() {
            // Arrange
            val yield = testYield(
                exit = Yield.Args.Enter(
                    addresses = Yield.Args.Enter.Addresses(address = AddressArgument(isRequired = true)),
                    args = mapOf(
                        Yield.Args.ArgType.AMOUNT to MockStaking.amountArgument(
                            minimum = null,
                            maximum = null,
                            required = false,
                        ),
                    ),
                ),
            )

            // Act
            val actual = testIntegration(yield).exitArgs

            // Assert
            assertThat(actual).isEqualTo(
                StakingActionArgs(
                    amountRequirement = StakingAmountRequirement(isRequired = false, minimum = null, maximum = null),
                    isPartialAmountDisabled = false,
                ),
            )
        }

        @Test
        fun `GIVEN negative enter maximum WHEN isPartialAmountDisabled read THEN true`() {
            // Arrange
            val yield = testYield(enter = MockStaking.enterArgs(maximum = BigDecimal("-1")))

            // Act
            val actual = testIntegration(yield)

            // Assert
            assertThat(actual.isPartialAmountDisabled).isTrue()
            assertThat(actual.enterArgs.isPartialAmountDisabled).isTrue()
        }

        @Test
        fun `GIVEN negative exit maximum only WHEN partial flags read THEN only exitArgs flag is true`() {
            // Arrange
            val yield = testYield(
                enter = MockStaking.enterArgs(),
                exit = MockStaking.enterArgs(maximum = BigDecimal("-1")),
            )

            // Act
            val actual = testIntegration(yield)

            // Assert
            assertThat(actual.isPartialAmountDisabled).isFalse()
            assertThat(actual.exitArgs!!.isPartialAmountDisabled).isTrue()
        }

        @Test
        fun `GIVEN secondary token raw id WHEN getCurrentToken THEN secondary token returned`() {
            // Arrange
            val actual = testIntegration(testYield().copy(tokens = listOf(token, secondary)))

            // Act
            val result = actual.getCurrentToken(CryptoCurrency.RawID("alt-gecko"))

            // Assert
            assertThat(result).isEqualTo(secondary)
        }

        @Test
        fun `GIVEN unknown raw id WHEN getCurrentToken THEN main token returned`() {
            // Arrange
            val actual = testIntegration(testYield().copy(tokens = listOf(token, secondary)))

            // Act
            val result = actual.getCurrentToken(CryptoCurrency.RawID("unknown"))

            // Assert
            assertThat(result).isEqualTo(token)
        }

        @Test
        fun `GIVEN null raw id WHEN getCurrentToken THEN main token returned`() {
            // Arrange
            val actual = testIntegration(testYield().copy(tokens = listOf(token, secondary)))

            // Act
            val result = actual.getCurrentToken(null)

            // Assert
            assertThat(result).isEqualTo(token)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class WarmupPeriodMapping {

        @Test
        fun `GIVEN warmup with seconds WHEN integration created THEN warmupPeriod is Seconds`() {
            // Arrange
            val warmup = Yield.Metadata.Period(days = 3, seconds = 7200)

            // Act
            val actual = integration(metadata = MockStaking.metadata(warmupPeriod = warmup))

            // Assert
            assertThat(actual.warmupPeriod).isEqualTo(Period.Seconds(7200))
        }

        @Test
        fun `GIVEN warmup without seconds WHEN integration created THEN warmupPeriod is Days`() {
            // Arrange
            val warmup = Yield.Metadata.Period(days = 5, seconds = null)

            // Act
            val actual = integration(metadata = MockStaking.metadata(warmupPeriod = warmup))

            // Assert
            assertThat(actual.warmupPeriod).isEqualTo(Period.Days(5))
        }

        @Test
        fun `GIVEN warmup with days and seconds WHEN integration created THEN seconds win`() {
            // Arrange
            val warmup = Yield.Metadata.Period(days = 10, seconds = 3600)

            // Act
            val actual = integration(metadata = MockStaking.metadata(warmupPeriod = warmup))

            // Assert
            assertThat(actual.warmupPeriod).isEqualTo(Period.Seconds(3600))
        }

        @Test
        fun `GIVEN zero warmup days WHEN integration created THEN warmupPeriod is zero Days`() {
            // Arrange
            val warmup = Yield.Metadata.Period(days = 0, seconds = null)

            // Act
            val actual = integration(metadata = MockStaking.metadata(warmupPeriod = warmup))

            // Assert
            assertThat(actual.warmupPeriod).isEqualTo(Period.Days(0))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CooldownPeriodMapping {

        @Test
        fun `GIVEN no cooldown WHEN integration created THEN cooldownPeriod is null`() {
            // Arrange
            val metadata = MockStaking.metadata(cooldownPeriod = null)

            // Act
            val actual = integration(metadata = metadata)

            // Assert
            assertThat(actual.cooldownPeriod).isNull()
        }

        @Test
        fun `GIVEN cooldown with seconds WHEN integration created THEN Fixed Seconds`() {
            // Arrange
            val cooldown = Yield.Metadata.Period(days = 2, seconds = 86400)

            // Act
            val actual = integration(metadata = MockStaking.metadata(cooldownPeriod = cooldown))

            // Assert
            assertThat(actual.cooldownPeriod).isEqualTo(CooldownPeriod.Fixed(Period.Seconds(86400)))
        }

        @Test
        fun `GIVEN cooldown without seconds WHEN integration created THEN Fixed Days`() {
            // Arrange
            val cooldown = Yield.Metadata.Period(days = 3, seconds = null)

            // Act
            val actual = integration(metadata = MockStaking.metadata(cooldownPeriod = cooldown))

            // Assert
            assertThat(actual.cooldownPeriod).isEqualTo(CooldownPeriod.Fixed(Period.Days(3)))
        }

        @Test
        fun `GIVEN cooldown with days and seconds WHEN integration created THEN Fixed Seconds win`() {
            // Arrange
            val cooldown = Yield.Metadata.Period(days = 7, seconds = 604800)

            // Act
            val actual = integration(metadata = MockStaking.metadata(cooldownPeriod = cooldown))

            // Assert
            assertThat(actual.cooldownPeriod).isEqualTo(CooldownPeriod.Fixed(Period.Seconds(604800)))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RewardScheduleMapping {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN yield reward schedule WHEN integration created THEN mapped to domain schedule`(
            model: ScheduleModel,
        ) {
            // Arrange
            val metadata = MockStaking.metadata(rewardSchedule = model.source)

            // Act
            val actual = integration(metadata = metadata)

            // Assert
            assertThat(actual.rewardSchedule).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ScheduleModel(Yield.Metadata.RewardSchedule.BLOCK, RewardSchedule.BLOCK),
            ScheduleModel(Yield.Metadata.RewardSchedule.HOUR, RewardSchedule.HOUR),
            ScheduleModel(Yield.Metadata.RewardSchedule.DAY, RewardSchedule.DAY),
            ScheduleModel(Yield.Metadata.RewardSchedule.WEEK, RewardSchedule.WEEK),
            ScheduleModel(Yield.Metadata.RewardSchedule.MONTH, RewardSchedule.MONTH),
            ScheduleModel(Yield.Metadata.RewardSchedule.ERA, RewardSchedule.ERA),
            ScheduleModel(Yield.Metadata.RewardSchedule.EPOCH, RewardSchedule.EPOCH),
            ScheduleModel(Yield.Metadata.RewardSchedule.UNKNOWN, RewardSchedule.UNKNOWN),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RewardClaimingMapping {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN yield reward claiming WHEN integration created THEN mapped to domain claiming`(
            model: ClaimingModel,
        ) {
            // Arrange
            val metadata = MockStaking.metadata(rewardClaiming = model.source)

            // Act
            val actual = integration(metadata = metadata)

            // Assert
            assertThat(actual.rewardClaiming).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ClaimingModel(Yield.Metadata.RewardClaiming.AUTO, RewardClaiming.AUTO),
            ClaimingModel(Yield.Metadata.RewardClaiming.MANUAL, RewardClaiming.MANUAL),
            ClaimingModel(Yield.Metadata.RewardClaiming.UNKNOWN, RewardClaiming.UNKNOWN),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetCurrentTokenEdgeCases {

        private val points = MockStaking.yieldToken(symbol = "PTS", coinGeckoId = null)

        // Pins current behaviour: null rawId equals a null coinGeckoId, so a token without gecko id beats the main token
        @Test
        fun `GIVEN null raw id and a token without gecko id WHEN getCurrentToken THEN that token returned instead of main`() {
            // Arrange
            val integration = integration(tokens = listOf(MockStaking.solToken, points))

            // Act
            val actual = integration.getCurrentToken(null)

            // Assert
            assertThat(actual).isEqualTo(points)
        }

        @Test
        fun `GIVEN raw id equal to main gecko id WHEN getCurrentToken THEN main token returned`() {
            // Arrange
            val integration = integration(tokens = listOf(MockStaking.solToken, points))

            // Act
            val actual = integration.getCurrentToken(CryptoCurrency.RawID("solana"))

            // Assert
            assertThat(actual).isEqualTo(MockStaking.solToken)
        }
    }
}

internal data class ScheduleModel(val source: Yield.Metadata.RewardSchedule, val expected: RewardSchedule)

internal data class ClaimingModel(val source: Yield.Metadata.RewardClaiming, val expected: RewardClaiming)