package com.tangem.domain.staking.model.stakekit

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.staking.model.stakekit.Yield.Validator.ValidatorStatus
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class YieldTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AllValidatorsFull {

        @Test
        fun `GIVEN only full validators WHEN allValidatorsFull THEN true`() {
            // Arrange
            val yield = MockStaking.yield(
                validators = listOf(
                    MockStaking.validator(MockStaking.VALIDATOR_ADDRESS, status = ValidatorStatus.FULL),
                    MockStaking.validator(MockStaking.SECOND_VALIDATOR_ADDRESS, status = ValidatorStatus.FULL),
                ),
            )

            // Act & Assert
            assertThat(yield.areAllValidatorsFull).isTrue()
        }

        @Test
        fun `GIVEN full and active validators WHEN allValidatorsFull THEN false`() {
            // Arrange
            val yield = MockStaking.yield(
                validators = listOf(
                    MockStaking.validator(MockStaking.VALIDATOR_ADDRESS, status = ValidatorStatus.FULL),
                    MockStaking.validator(MockStaking.SECOND_VALIDATOR_ADDRESS, status = ValidatorStatus.ACTIVE),
                ),
            )

            // Act & Assert
            assertThat(yield.areAllValidatorsFull).isFalse()
        }

        @Test
        fun `GIVEN full validator only in allValidators WHEN allValidatorsFull THEN false`() {
            // Arrange
            val active = MockStaking.validator(MockStaking.VALIDATOR_ADDRESS, status = ValidatorStatus.ACTIVE)
            val full = MockStaking.validator(MockStaking.SECOND_VALIDATOR_ADDRESS, status = ValidatorStatus.FULL)
            val yield = MockStaking.yield(validators = listOf(active), allValidators = listOf(active, full))

            // Act & Assert
            assertThat(yield.areAllValidatorsFull).isFalse()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PreferredValidators {

        @Test
        fun `GIVEN preferred and non preferred validators WHEN preferredValidators THEN only preferred returned`() {
            // Arrange
            val preferred = MockStaking.validator(MockStaking.VALIDATOR_ADDRESS, preferred = true)
            val other = MockStaking.validator(MockStaking.SECOND_VALIDATOR_ADDRESS, preferred = false)
            val yield = MockStaking.yield(validators = listOf(preferred, other))

            // Act
            val actual = yield.preferredValidators

            // Assert
            assertThat(actual).containsExactly(preferred)
        }

        @Test
        fun `GIVEN preferred validator only in allValidators WHEN preferredValidators THEN it is ignored`() {
            // Arrange
            val other = MockStaking.validator(MockStaking.SECOND_VALIDATOR_ADDRESS, preferred = false)
            val jailed = MockStaking.validator("jailed", status = ValidatorStatus.JAILED, preferred = true)
            val yield = MockStaking.yield(validators = listOf(other), allValidators = listOf(other, jailed))

            // Act
            val actual = yield.preferredValidators

            // Assert
            assertThat(actual).isEmpty()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsPartialAmountDisabled {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN amount maximum WHEN isPartialAmountDisabled THEN true only for negative maximum`(
            model: PartialModel,
        ) {
            // Arrange
            val enter = MockStaking.enterArgs(maximum = model.maximum)

            // Act
            val actual = enter.isPartialAmountDisabled

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        @Test
        fun `GIVEN no amount argument WHEN isPartialAmountDisabled THEN false`() {
            // Arrange
            val enter = MockStaking.enterArgs().copy(
                args = mapOf(
                    Yield.Args.ArgType.ADDRESS to MockStaking.amountArgument(maximum = BigDecimal("-1")),
                ),
            )

            // Act & Assert
            assertThat(enter.isPartialAmountDisabled).isFalse()
        }

        private fun provideTestModels() = listOf(
            PartialModel(maximum = null, expected = false),
            PartialModel(maximum = BigDecimal("-1"), expected = true),
            PartialModel(maximum = BigDecimal("-0.000001"), expected = true),
            PartialModel(maximum = BigDecimal.ZERO, expected = false),
            PartialModel(maximum = BigDecimal("100"), expected = false),
        )
    }

    internal data class PartialModel(val maximum: BigDecimal?, val expected: Boolean)
}