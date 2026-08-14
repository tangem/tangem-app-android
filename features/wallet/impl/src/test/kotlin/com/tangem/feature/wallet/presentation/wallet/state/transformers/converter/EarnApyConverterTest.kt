package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.BalanceItem
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.common.RewardInfo
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.domain.staking.model.stakekit.Yield
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * Tests the earn-badge APY resolution: which rate a token's badge shows and whether it counts as already earning.
 *
 * Two behaviours are worth pinning. The converter reads the stake through `stakingBalanceData`, so it must reach
 * the same verdict whether the balance arrives as a contribution or through the legacy typed field — with the
 * toggle on, a reader stuck on the legacy field would see no stake and hide the badge for a fully-staked token.
 * And the StakeKit rate selection has real branching: an active stake answers with its own validator's rate, and
 * only falls back to the best preferred validator when the stake doesn't point at a known one.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnApyConverterTest {

    private val currency = MockCryptoCurrencyFactory().createCoin(Blockchain.Solana)

    @Test
    fun `GIVEN Full availability AND the stake arrives as a contribution WHEN convert THEN badge is active`() {
        // Arrange — Full hides the badge for tokens without a stake, so a reader stuck on the legacy typed
        // field would see no stake here and return null
        val converter = converter(
            StakingAvailability.Full(stakeKitOption(validator("v1", preferred = true, rate = "0.07"))),
        )
        val status = status(stakingBalance = stakeKitBalance("v1"), useContributions = true)

        // Act
        val actual = converter.convert(status)

        // Assert
        assertThat(actual).isEqualTo(earnApyInfo(rate = "0.07", isActive = true))
    }

    @Test
    fun `GIVEN the same stake WHEN reached via legacy field or contribution THEN the badge is identical`() {
        // Arrange — one balance delivered through each architecture
        val converter = converter(
            StakingAvailability.Available(
                stakeKitOption(
                    validator("v1", preferred = true, rate = "0.09"),
                    validator("v2", preferred = false, rate = "0.05"),
                ),
            ),
        )
        val stake = stakeKitBalance("v2")

        // Act
        val viaLegacyField = converter.convert(status(stakingBalance = stake))
        val viaContribution = converter.convert(status(stakingBalance = stake, useContributions = true))

        // Assert
        assertThat(viaContribution).isEqualTo(viaLegacyField)
        assertThat(viaContribution).isEqualTo(earnApyInfo(rate = "0.05", isActive = true))
    }

    @Test
    fun `GIVEN an active stake matching a validator WHEN convert THEN that validator's rate wins`() {
        // Arrange — the stake sits on the non-preferred v2, whose rate must beat the better preferred one
        val converter = converter(
            StakingAvailability.Available(
                stakeKitOption(
                    validator("v1", preferred = true, rate = "0.09"),
                    validator("v2", preferred = false, rate = "0.05"),
                ),
            ),
        )
        val status = status(stakingBalance = stakeKitBalance("v2"), useContributions = true)

        // Act
        val actual = converter.convert(status)

        // Assert
        assertThat(actual).isEqualTo(earnApyInfo(rate = "0.05", isActive = true))
    }

    @Test
    fun `GIVEN an active stake with no matching validator WHEN convert THEN best preferred rate is used`() {
        // Arrange — the stake points at an unknown validator, so the fallback must pick the best
        // preferred rate and ignore the higher non-preferred one
        val converter = converter(
            StakingAvailability.Available(
                stakeKitOption(
                    validator("v1", preferred = true, rate = "0.03"),
                    validator("v2", preferred = true, rate = "0.08"),
                    validator("v3", preferred = false, rate = "0.99"),
                ),
            ),
        )
        val status = status(stakingBalance = stakeKitBalance("unknown"), useContributions = true)

        // Act
        val actual = converter.convert(status)

        // Assert
        assertThat(actual).isEqualTo(earnApyInfo(rate = "0.08", isActive = true))
    }

    @Test
    fun `GIVEN no stake WHEN convert THEN best preferred rate is used and badge is not active`() {
        // Arrange
        val converter = converter(
            StakingAvailability.Available(
                stakeKitOption(
                    validator("v1", preferred = true, rate = "0.03"),
                    validator("v2", preferred = true, rate = "0.08"),
                    validator("v3", preferred = false, rate = "0.99"),
                ),
            ),
        )

        // Act
        val actual = converter.convert(status())

        // Assert
        assertThat(actual).isEqualTo(earnApyInfo(rate = "0.08", isActive = false))
    }

    @Test
    fun `GIVEN a P2P option WHEN convert THEN its own APY is used`() {
        // Arrange
        val converter = converter(StakingAvailability.Available(p2pOption(apy = BigDecimal("0.04"))))

        // Act
        val actual = converter.convert(status())

        // Assert
        assertThat(actual).isEqualTo(earnApyInfo(rate = "0.04", isActive = false))
    }

    @Test
    fun `GIVEN an APR validator WHEN convert THEN the APR badge text is used`() {
        // Arrange
        val converter = converter(
            StakingAvailability.Available(
                stakeKitOption(validator("v1", preferred = true, rate = "0.06", type = RewardType.APR)),
            ),
        )

        // Act
        val actual = converter.convert(status())

        // Assert
        val expected = earnApyInfo(rate = "0.06", isActive = false, badgeRes = R.string.staking_apr_earn_badge)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN Full availability AND no stake WHEN convert THEN no badge`() {
        // Arrange — no free capacity and nothing staked: the badge must not advertise an unavailable option
        val converter = converter(
            StakingAvailability.Full(stakeKitOption(validator("v1", preferred = true, rate = "0.07"))),
        )

        // Act
        val actual = converter.convert(status())

        // Assert
        assertThat(actual).isNull()
    }

    // region Fixtures

    private fun converter(availability: StakingAvailability) = EarnApyConverter(
        yieldModuleApyMap = emptyMap(),
        stakingApyMap = mapOf(currency to availability),
    )

    /**
     * [useContributions] switches between the two architectures the way `CryptoCurrencyStatusFactory` does: with
     * the toggle on the balance arrives in `contributions` and the typed field is **null**. Populating both would
     * let a reader stuck on the legacy field still look correct.
     */
    private fun status(
        stakingBalance: StakingBalance.Data? = null,
        useContributions: Boolean = false,
    ): CryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = currency,
        value = CryptoCurrencyStatus.Loaded(
            amount = BigDecimal.ONE,
            fiatAmount = BigDecimal.ONE,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            stakingBalance = stakingBalance.takeIf { !useContributions },
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = mockk(relaxed = true),
            sources = CryptoCurrencyStatus.Sources(),
            contributions = if (useContributions) listOfNotNull(stakingBalance) else emptyList(),
        ),
    )

    /**
     * A real option over a mocked [Yield]: stubbing `integrationId` on an option mock would make MockK retransform
     * `StakingIntegrationID`'s enum implementations, which the JVM rejects ("cannot change the class modifiers").
     */
    private fun stakeKitOption(vararg validatorList: Yield.Validator): StakingOption.StakeKit {
        val yieldModel: Yield = mockk {
            every { validators } returns validatorList.toList()
            every { apy } returns BigDecimal("0.10")
            every { token } returns mockk()
            every { isAvailable } returns true
        }
        return StakingOption.StakeKit(
            integrationId = StakingIntegrationID.StakeKit.Coin.Solana,
            yield = yieldModel,
        )
    }

    private fun p2pOption(apy: BigDecimal): StakingOption.P2PEthPool = mockk {
        every { this@mockk.apy } returns apy
    }

    private fun validator(
        address: String,
        preferred: Boolean,
        rate: String,
        type: RewardType = RewardType.APY,
    ): Yield.Validator = Yield.Validator(
        address = address,
        status = Yield.Validator.ValidatorStatus.ACTIVE,
        name = "validator-$address",
        preferred = preferred,
        isStrategicPartner = false,
        rewardInfo = RewardInfo(rate = BigDecimal(rate), type = type),
    )

    /**
     * A mocked stake is safe here, unlike in the totals tests: the converter reads only `validatorAddress` off the
     * items — no member math for MockK to intercept.
     */
    private fun stakeKitBalance(vararg validatorAddresses: String?): StakingBalance.Data.StakeKit = mockk {
        every { balance } returns mockk {
            every { items } returns validatorAddresses.map { address ->
                mockk<BalanceItem> { every { validatorAddress } returns address }
            }
        }
    }

    /** The converter renders the rate itself; the expected string is produced by the same formatter. */
    private fun earnApyInfo(
        rate: String,
        isActive: Boolean,
        badgeRes: Int = R.string.yield_module_earn_badge,
    ): EarnApyConverter.EarnApyInfo {
        val apyString = BigDecimal(rate).format { percent(withPercentSign = false) }

        return EarnApyConverter.EarnApyInfo(
            text = resourceReference(badgeRes, wrappedList(apyString)),
            isActive = isActive,
            apy = apyString,
        )
    }

    // endregion
}