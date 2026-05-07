package com.tangem.domain.staking.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.models.staking.YieldToken
import com.tangem.domain.staking.model.stakekit.AddressArgument
import com.tangem.domain.staking.model.stakekit.Yield
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * Tests for [StakeKitIntegration] — specifically the Period/CooldownPeriod mapping from [Yield.Metadata].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StakeKitIntegrationTest {

    // region helpers

    private val dummyToken = YieldToken(
        name = "Test Token",
        network = NetworkType.SOLANA,
        symbol = "SOL",
        decimals = 9,
        address = null,
        coinGeckoId = null,
        logoURI = null,
        isPoints = false,
    )

    private val dummyEnter = Yield.Args.Enter(
        addresses = Yield.Args.Enter.Addresses(
            address = AddressArgument(required = false),
        ),
        args = emptyMap(),
    )

    private val dummyArgs = Yield.Args(enter = dummyEnter, exit = null)

    private val dummyStatus = Yield.Status(enter = true, exit = null)

    private val dummyEnabled = Yield.Metadata.Enabled(enabled = true)

    private fun buildYield(
        warmupPeriod: Yield.Metadata.Period,
        cooldownPeriod: Yield.Metadata.Period?,
    ): Yield {
        return Yield(
            id = "test-integration",
            token = dummyToken,
            tokens = emptyList(),
            args = dummyArgs,
            status = dummyStatus,
            apy = BigDecimal("5.0"),
            rewardRate = 5.0,
            rewardType = com.tangem.domain.staking.model.common.RewardType.APY,
            metadata = Yield.Metadata(
                name = "Test Staking",
                logoUri = "https://example.com/logo.png",
                description = "Test staking integration",
                documentation = null,
                gasFeeToken = dummyToken,
                token = dummyToken,
                tokens = emptyList(),
                type = "liquid",
                rewardSchedule = Yield.Metadata.RewardSchedule.DAY,
                cooldownPeriod = cooldownPeriod,
                warmupPeriod = warmupPeriod,
                rewardClaiming = Yield.Metadata.RewardClaiming.AUTO,
                defaultValidator = null,
                minimumStake = null,
                supportsMultipleValidators = false,
                revshare = dummyEnabled,
                fee = dummyEnabled,
            ),
            validators = emptyList(),
            isAvailable = true,
        )
    }

    private fun buildIntegration(
        warmupPeriod: Yield.Metadata.Period,
        cooldownPeriod: Yield.Metadata.Period?,
    ): StakeKitIntegration {
        return StakeKitIntegration(
            integrationId = StakingIntegrationID.StakeKit.Coin.Solana,
            yield = buildYield(warmupPeriod = warmupPeriod, cooldownPeriod = cooldownPeriod),
        )
    }

    // endregion

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class `warmupPeriod mapping` {

        @Test
        fun `should produce Period Seconds when seconds is non-null`() {
            // given
            val warmup = Yield.Metadata.Period(days = 3, seconds = 7200)

            // when
            val integration = buildIntegration(warmupPeriod = warmup, cooldownPeriod = null)

            // then
            assertThat(integration.warmupPeriod).isEqualTo(Period.Seconds(7200))
        }

        @Test
        fun `should produce Period Days when seconds is null`() {
            // given
            val warmup = Yield.Metadata.Period(days = 5, seconds = null)

            // when
            val integration = buildIntegration(warmupPeriod = warmup, cooldownPeriod = null)

            // then
            assertThat(integration.warmupPeriod).isEqualTo(Period.Days(5))
        }

        @Test
        fun `should prefer seconds over days when both are present`() {
            // given — days is non-zero but seconds takes priority
            val warmup = Yield.Metadata.Period(days = 10, seconds = 3600)

            // when
            val integration = buildIntegration(warmupPeriod = warmup, cooldownPeriod = null)

            // then
            assertThat(integration.warmupPeriod).isInstanceOf(Period.Seconds::class.java)
            assertThat((integration.warmupPeriod as Period.Seconds).value).isEqualTo(3600)
        }

        @Test
        fun `should produce Period Days with zero value when days is zero and seconds is null`() {
            // given
            val warmup = Yield.Metadata.Period(days = 0, seconds = null)

            // when
            val integration = buildIntegration(warmupPeriod = warmup, cooldownPeriod = null)

            // then
            assertThat(integration.warmupPeriod).isEqualTo(Period.Days(0))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class `cooldownPeriod mapping` {

        @Test
        fun `should be null when yield cooldownPeriod is null`() {
            // given
            val warmup = Yield.Metadata.Period(days = 1, seconds = null)

            // when
            val integration = buildIntegration(warmupPeriod = warmup, cooldownPeriod = null)

            // then
            assertThat(integration.cooldownPeriod).isNull()
        }

        @Test
        fun `should produce Fixed Period Seconds when cooldown seconds is non-null`() {
            // given
            val warmup = Yield.Metadata.Period(days = 1, seconds = null)
            val cooldown = Yield.Metadata.Period(days = 2, seconds = 86400)

            // when
            val integration = buildIntegration(warmupPeriod = warmup, cooldownPeriod = cooldown)

            // then
            assertThat(integration.cooldownPeriod).isEqualTo(CooldownPeriod.Fixed(Period.Seconds(86400)))
        }

        @Test
        fun `should produce Fixed Period Days when cooldown seconds is null`() {
            // given
            val warmup = Yield.Metadata.Period(days = 1, seconds = null)
            val cooldown = Yield.Metadata.Period(days = 3, seconds = null)

            // when
            val integration = buildIntegration(warmupPeriod = warmup, cooldownPeriod = cooldown)

            // then
            assertThat(integration.cooldownPeriod).isEqualTo(CooldownPeriod.Fixed(Period.Days(3)))
        }

        @Test
        fun `should prefer seconds over days in cooldown when both are present`() {
            // given
            val warmup = Yield.Metadata.Period(days = 1, seconds = null)
            val cooldown = Yield.Metadata.Period(days = 7, seconds = 604800)

            // when
            val integration = buildIntegration(warmupPeriod = warmup, cooldownPeriod = cooldown)

            // then
            val period = integration.cooldownPeriod
            assertThat(period).isInstanceOf(CooldownPeriod.Fixed::class.java)
            assertThat((period as CooldownPeriod.Fixed).period).isInstanceOf(Period.Seconds::class.java)
            assertThat((period.period as Period.Seconds).value).isEqualTo(604800)
        }
    }
}