package com.tangem.features.earn.staking.impl

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.routing.AppRoute
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.earn.ResolveEarnRateUseCase
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingBalance
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.common.RewardInfo
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.product.api.product.EarnRateEntry
import com.tangem.features.earn.staking.impl.fixtures.coin
import com.tangem.features.earn.staking.impl.fixtures.currencyStatus
import com.tangem.features.earn.staking.impl.fixtures.polygonToken
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal

internal class StakingRateMapperTest {

    private val mapper = StakingRateMapper()
    private val userWalletId = UserWalletId("1234567890ABCDEF")
    private val solana = coin(Blockchain.Solana)
    private val solanaRoute = AppRoute.Staking(userWalletId, solana, StakingIntegrationID.StakeKit.Coin.Solana)

    @Test
    fun `GIVEN SOL with the preferred validator and no position WHEN entry THEN rate of the best preferred validator, inactive`() {
        // Arrange
        val option = option(
            validator("v1", "0.05"),
            validator("v2", "0.08"),
            validator("v3", "0.20", preferred = false),
        )

        // Act
        val actual = mapper.entry(userWalletId, currencyStatus(solana), StakingAvailability.Available(option))

        // Assert
        val expected = EarnRateEntry(
            rate = EarnProductRate(BigDecimal("0.08"), EarnRateType.APR),
            isActive = false,
            source = StatusSource.ACTUAL,
            route = solanaRoute,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN position at the second validator WHEN entry THEN rate of the staked validator and active`() {
        // Arrange
        val option = option(validator("v1", "0.05"), validator("v2", "0.08"))

        // Act
        val actual = mapper.entry(userWalletId, stakedAt(solana, "v1"), StakingAvailability.Available(option))

        // Assert
        val expected = EarnRateEntry(
            rate = EarnProductRate(BigDecimal("0.05"), EarnRateType.APR),
            isActive = true,
            source = StatusSource.ACTUAL,
            route = solanaRoute,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RewardTypes {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN reward type WHEN entry THEN earn rate type per table`(model: RewardTypeModel) {
            // Arrange
            val option = option(validator("v1", "0.05", model.rewardType))

            // Act
            val actual = mapper.entry(userWalletId, currencyStatus(solana), StakingAvailability.Available(option))

            // Assert
            assertThat(actual?.rate?.type).isEqualTo(model.expectedType)
        }

        private fun provideTestModels() = listOf(
            RewardTypeModel(RewardType.APR, EarnRateType.APR),
            RewardTypeModel(RewardType.APY, EarnRateType.APY),
            RewardTypeModel(RewardType.UNKNOWN, EarnRateType.APY),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NoUsableValidator {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN no usable validator WHEN entry THEN null`(model: ValidatorsModel) {
            // Act
            val actual = mapper.entry(userWalletId, currencyStatus(solana), StakingAvailability.Available(model.option))

            // Assert
            assertThat(actual).isNull()
        }

        private fun provideTestModels() = listOf(
            ValidatorsModel("no preferred validators", option(validator("v1", "0.05", preferred = false))),
            ValidatorsModel(
                name = "preferred validator without reward info",
                option = option(MockStaking.validator(address = "v1", preferred = true, rewardInfo = null)),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Availability {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN availability WHEN entry THEN entry or null per table`(model: AvailabilityModel) {
            // Act
            val actual = mapper.entry(userWalletId, model.status, model.availability)

            // Assert
            assertThat(actual != null).isEqualTo(model.hasEntry)
        }

        private fun provideTestModels(): List<AvailabilityModel> {
            val stakeKit = option(MockStaking.validator())
            val ethereum = currencyStatus(coin(Blockchain.Ethereum))
            val pool = StakingOption.P2PEthPool(vaults = listOf(MockStaking.vault()))
            return listOf(
                AvailabilityModel("Available StakeKit", currencyStatus(solana), StakingAvailability.Available(stakeKit), true),
                AvailabilityModel("Full StakeKit without position", currencyStatus(solana), StakingAvailability.Full(stakeKit), false),
                AvailabilityModel(
                    name = "Full StakeKit with position",
                    status = stakedAt(solana, MockStaking.VALIDATOR_ADDRESS),
                    availability = StakingAvailability.Full(stakeKit),
                    hasEntry = true,
                ),
                AvailabilityModel("P2P pool on ETH", ethereum, StakingAvailability.Available(pool), false),
                AvailabilityModel("Unavailable", currencyStatus(solana), StakingAvailability.Unavailable, false),
                AvailabilityModel("TemporaryUnavailable", currencyStatus(solana), StakingAvailability.TemporaryUnavailable, false),
                AvailabilityModel("RegionUnavailable", currencyStatus(solana), StakingAvailability.RegionUnavailable, false),
            )
        }
    }

    @Test
    fun `GIVEN balance Error WHEN entry THEN entry is inactive`() {
        // Arrange
        val status = currencyStatus(solana, stakingBalance = StakingBalance.Error(MockStaking.stakingId()))

        // Act
        val actual = mapper.entry(userWalletId, status, StakingAvailability.Available(option(MockStaking.validator())))

        // Assert
        assertThat(actual?.isActive).isFalse()
    }

    @ParameterizedTest
    @EnumSource(StatusSource::class)
    fun `GIVEN a staked balance source WHEN entry THEN entry source is the balance source`(source: StatusSource) {
        // Arrange
        val status = currencyStatus(
            currency = solana,
            stakingBalance = MockStaking.stakeKitBalance(source = source),
        )

        // Act
        val actual = mapper.entry(userWalletId, status, StakingAvailability.Available(option(MockStaking.validator())))

        // Assert
        assertThat(actual?.source).isEqualTo(source)
    }

    @ParameterizedTest
    @EnumSource(StatusSource::class)
    fun `GIVEN no position and a staking balance source WHEN entry THEN entry source is that source`(
        source: StatusSource,
    ) {
        // Arrange
        val status = currencyStatus(currency = solana, stakingBalanceSource = source)

        // Act
        val actual = mapper.entry(userWalletId, status, StakingAvailability.Available(option(MockStaking.validator())))

        // Assert
        assertThat(actual?.source).isEqualTo(source)
    }

    @Test
    fun `GIVEN a StakeKit token WHEN entry THEN route carries the token integration`() {
        // Arrange
        val token = polygonToken()
        val integration = StakingIntegrationID.StakeKit.EthereumToken.Polygon
        val availability = StakingAvailability.Available(StakingOption.StakeKit(integration, MockStaking.polygonYield))

        // Act
        val actual = mapper.entry(userWalletId, currencyStatus(token), availability)

        // Assert
        assertThat(actual?.route).isEqualTo(AppRoute.Staking(userWalletId, token, integration))
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Parity {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN same inputs WHEN mapper entry and ResolveEarnRateUseCase THEN rate and activity agree`(
            model: AvailabilityModel,
        ) {
            // Act
            val legacy = ResolveEarnRateUseCase().invoke(
                currencyStatus = model.status,
                yieldSupplyApy = emptyMap(),
                stakingAvailability = mapOf(model.status.currency to model.availability),
            )
            val actual = mapper.entry(userWalletId, model.status, model.availability)

            // Assert
            if (legacy == null) {
                assertThat(actual).isNull()
            } else {
                assertThat(actual?.rate?.fraction).isEqualTo(legacy.apy)
                assertThat(actual?.isActive).isEqualTo(legacy.isActive)
            }
        }

        private fun provideTestModels(): List<AvailabilityModel> {
            val two = option(validator("v1", "0.05"), validator("v2", "0.08"))
            val unusable = option(validator("v1", "0.05", preferred = false))
            val unknown = option(validator("v1", "0.05", RewardType.UNKNOWN))
            return listOf(
                AvailabilityModel("best preferred", currencyStatus(solana), StakingAvailability.Available(two), true),
                AvailabilityModel("staked validator", stakedAt(solana, "v1"), StakingAvailability.Available(two), true),
                AvailabilityModel("Full without position", currencyStatus(solana), StakingAvailability.Full(two), false),
                AvailabilityModel("Unavailable", currencyStatus(solana), StakingAvailability.Unavailable, false),
                AvailabilityModel("no preferred validator", currencyStatus(solana), StakingAvailability.Available(unusable), false),
                AvailabilityModel("UNKNOWN reward type", currencyStatus(solana), StakingAvailability.Available(unknown), true),
            )
        }
    }

    private fun option(vararg validators: Yield.Validator): StakingOption.StakeKit = StakingOption.StakeKit(
        integrationId = StakingIntegrationID.StakeKit.Coin.Solana,
        yield = MockStaking.yield(validators = validators.toList()),
    )

    private fun validator(
        address: String,
        rate: String,
        type: RewardType = RewardType.APR,
        preferred: Boolean = true,
    ): Yield.Validator = MockStaking.validator(
        address = address,
        preferred = preferred,
        rewardInfo = RewardInfo(BigDecimal(rate), type),
    )

    private fun stakedAt(currency: CryptoCurrency, address: String) = currencyStatus(
        currency = currency,
        stakingBalance = MockStaking.stakeKitBalance(items = listOf(MockStaking.balanceItem(validatorAddress = address))),
    )

    internal data class RewardTypeModel(val rewardType: RewardType, val expectedType: EarnRateType) {
        override fun toString(): String = "$rewardType -> $expectedType"
    }

    internal data class ValidatorsModel(val name: String, val option: StakingOption.StakeKit) {
        override fun toString(): String = name
    }

    internal data class AvailabilityModel(
        val name: String,
        val status: CryptoCurrencyStatus,
        val availability: StakingAvailability,
        val hasEntry: Boolean,
    ) {
        override fun toString(): String = name
    }
}