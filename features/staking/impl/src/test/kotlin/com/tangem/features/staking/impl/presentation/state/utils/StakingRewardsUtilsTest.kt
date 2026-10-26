package com.tangem.features.staking.impl.presentation.state.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.staking.model.common.RewardSchedule
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.features.staking.impl.R
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.StringsSigns.MINUS
import com.tangem.utils.StringsSigns.NON_BREAKING_SPACE
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class StakingRewardsUtilsTest {

    private val each = resourceReference(R.string.staking_reward_schedule_each_plural, decapitalize = true)
    private val nbsp = stringReference(NON_BREAKING_SPACE.toString())

    @Nested
    inner class CustomSchedules {

        @Nested
        inner class Solana {

            @ParameterizedTest
            @EnumSource(value = RewardSchedule::class, names = ["BLOCK", "EPOCH", "ERA"])
            fun `GIVEN solana custom schedule WHEN getRewardScheduleText THEN two to three days`(
                schedule: RewardSchedule,
            ) {
                // Act
                val actual = getRewardScheduleText(schedule, Blockchain.Solana.toNetworkId(), decapitalize = true)

                // Assert
                assertThat(actual).isEqualTo(
                    combinedReference(
                        each,
                        nbsp,
                        stringReference("2${MINUS}3$NON_BREAKING_SPACE"),
                        pluralReference(R.plurals.common_days_no_param, count = 3),
                    ),
                )
            }
        }

        @Nested
        inner class Cosmos {

            @ParameterizedTest
            @EnumSource(value = RewardSchedule::class, names = ["BLOCK", "EPOCH", "ERA"])
            fun `GIVEN cosmos custom schedule WHEN getRewardScheduleText THEN five to twelve seconds`(
                schedule: RewardSchedule,
            ) {
                // Act
                val actual = getRewardScheduleText(schedule, Blockchain.Cosmos.toNetworkId(), decapitalize = true)

                // Assert
                assertThat(actual).isEqualTo(
                    combinedReference(
                        each,
                        nbsp,
                        stringReference("5${MINUS}12$NON_BREAKING_SPACE"),
                        resourceReference(R.string.common_second_no_param),
                    ),
                )
            }
        }

        @Nested
        inner class Tron {

            @ParameterizedTest
            @EnumSource(value = RewardSchedule::class, names = ["BLOCK", "EPOCH", "ERA"])
            fun `GIVEN tron custom schedule WHEN getRewardScheduleText THEN daily schedule`(schedule: RewardSchedule) {
                // Act
                val actual = getRewardScheduleText(schedule, Blockchain.Tron.toNetworkId(), decapitalize = true)

                // Assert
                assertThat(actual).isEqualTo(
                    resourceReference(R.string.staking_reward_schedule_day, decapitalize = true),
                )
            }
        }

        @Nested
        inner class Ton {

            @ParameterizedTest
            @EnumSource(value = RewardSchedule::class, names = ["BLOCK", "EPOCH", "ERA"])
            fun `GIVEN ton custom schedule WHEN getRewardScheduleText THEN one to two days`(schedule: RewardSchedule) {
                // Act
                val actual = getRewardScheduleText(schedule, Blockchain.TON.toNetworkId(), decapitalize = true)

                // Assert
                assertThat(actual).isEqualTo(
                    combinedReference(
                        each,
                        nbsp,
                        stringReference("1${MINUS}2$NON_BREAKING_SPACE"),
                        pluralReference(R.plurals.common_days_no_param, count = 2),
                    ),
                )
            }
        }

        @Nested
        inner class Cardano {

            @ParameterizedTest
            @EnumSource(value = RewardSchedule::class, names = ["BLOCK", "EPOCH", "ERA"])
            fun `GIVEN cardano custom schedule WHEN getRewardScheduleText THEN five days`(schedule: RewardSchedule) {
                // Act
                val actual = getRewardScheduleText(schedule, Blockchain.Cardano.toNetworkId(), decapitalize = true)

                // Assert
                assertThat(actual).isEqualTo(
                    combinedReference(
                        each,
                        nbsp,
                        stringReference("5$NON_BREAKING_SPACE"),
                        pluralReference(R.plurals.common_days_no_param, count = 5),
                    ),
                )
            }
        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        inner class Fallback {

            @ParameterizedTest
            @ProvideTestModels
            fun `GIVEN chain without custom schedule WHEN getRewardScheduleText THEN capitalized schedule name`(
                model: FallbackModel,
            ) {
                // Act
                val actual = getRewardScheduleText(model.schedule, model.blockchain.toNetworkId(), decapitalize = true)

                // Assert
                assertThat(actual).isEqualTo(stringReference(model.expected))
            }

            private fun provideTestModels() = listOf(Blockchain.BSC, Blockchain.Ethereum).flatMap { blockchain ->
                listOf(
                    FallbackModel(blockchain, RewardSchedule.BLOCK, "Block"),
                    FallbackModel(blockchain, RewardSchedule.EPOCH, "Epoch"),
                    FallbackModel(blockchain, RewardSchedule.ERA, "Era"),
                )
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UnknownSchedule {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN unknown schedule WHEN getRewardScheduleText THEN null`(blockchain: Blockchain) {
            // Act
            val actual = getRewardScheduleText(RewardSchedule.UNKNOWN, blockchain.toNetworkId(), decapitalize = true)

            // Assert
            assertThat(actual).isNull()
        }

        private fun provideTestModels() = listOf(Blockchain.Solana, Blockchain.BSC)
    }

    @Nested
    inner class RewardTypeTexts {

        @Test
        fun `GIVEN unknown reward type WHEN reward type texts THEN empty texts`() {
            // Act
            val short = getRewardTypeShortText(RewardType.UNKNOWN)
            val long = getRewardTypeLongText(RewardType.UNKNOWN)

            // Assert
            assertThat(short).isEqualTo(TextReference.EMPTY)
            assertThat(long).isEqualTo(TextReference.EMPTY)
        }
    }

    internal data class FallbackModel(val blockchain: Blockchain, val schedule: RewardSchedule, val expected: String)
}