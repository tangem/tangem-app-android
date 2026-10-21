package com.tangem.domain.staking.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.staking.model.stakekit.Yield.Validator.ValidatorStatus
import com.tangem.test.mock.staking.MockStaking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class StakingTargetTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ValidatorTarget {

        @ParameterizedTest
        @EnumSource(ValidatorStatus::class)
        fun `GIVEN validator status WHEN wrapped THEN only ACTIVE is active`(status: ValidatorStatus) {
            // Arrange
            val validator = MockStaking.validator(status = status)

            // Act
            val target = validator.toStakingTarget()

            // Assert
            assertThat(target.isActive).isEqualTo(status == ValidatorStatus.ACTIVE)
        }

        @Test
        fun `GIVEN validator with image WHEN wrapped THEN image wrapped in Url`() {
            // Act
            val target = MockStaking.validator(image = "https://img/v1.png").toStakingTarget()

            // Assert
            assertThat(target.image).isEqualTo(StakingTargetImage.Url("https://img/v1.png"))
        }

        @Test
        fun `GIVEN validator without image WHEN wrapped THEN image is null`() {
            // Act
            val target = MockStaking.validator(image = null).toStakingTarget()

            // Assert
            assertThat(target.image).isNull()
        }
    }
}
