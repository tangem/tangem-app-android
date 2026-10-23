package com.tangem.domain.staking.earn

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.common.RewardInfo
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class ResolveEarnRateUseCaseTest {

    private val useCase = ResolveEarnRateUseCase()

    private val factory = MockCryptoCurrencyFactory()
    private val solana = factory.createCoin(Blockchain.Solana)
    private val ethereum = factory.createCoin(Blockchain.Ethereum)
    private val usdcEvm = factory.createToken(
        blockchain = Blockchain.Ethereum,
        id = "usd-coin",
        contractAddress = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
    )
    private val usdcSolana = factory.createToken(
        blockchain = Blockchain.Solana,
        id = "usd-coin",
        contractAddress = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
    )

    private val vPref7 = MockStaking.validator(
        address = "validator-1",
        preferred = true,
        rewardInfo = RewardInfo(BigDecimal("0.07"), RewardType.APR),
    )
    private val vPref8 = MockStaking.validator(
        address = "validator-3",
        preferred = true,
        rewardInfo = RewardInfo(BigDecimal("0.08"), RewardType.APR),
    )
    private val vOther5 = MockStaking.validator(
        address = "validator-2",
        preferred = false,
        rewardInfo = RewardInfo(BigDecimal("0.05"), RewardType.APR),
    )
    private val vOther99 = MockStaking.validator(
        address = "validator-4",
        preferred = false,
        rewardInfo = RewardInfo(BigDecimal("0.99"), RewardType.APR),
    )

    private val evmKeyUpper = "ETHEREUM_0XA0B86991C6218B36C1D19D4A2E9EB0CE3606EB48"
    private val solanaKeyOriginal = "solana_EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
    private val solanaKeyLower = usdcSolana.yieldSupplyKey()

    private fun status(
        currency: CryptoCurrency,
        stakingBalance: StakingBalance.Data? = null,
        isYieldActive: Boolean? = null,
    ) = CryptoCurrencyStatus(
        currency,
        CryptoCurrencyStatus.Loaded(
            amount = BigDecimal.ONE,
            fiatAmount = BigDecimal.ONE,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            yieldSupplyStatus = isYieldActive?.let { YieldSupplyStatus(it, true, true, null) },
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                NetworkAddress.Address("address", NetworkAddress.Address.Type.Primary),
            ),
            sources = CryptoCurrencyStatus.Sources(),
            contributions = listOfNotNull(stakingBalance),
        ),
    )

    private fun stakeKitOption(
        vararg validators: Yield.Validator,
        allValidators: List<Yield.Validator> = validators.toList(),
    ) = StakingOption.StakeKit(
        StakingIntegrationID.StakeKit.Coin.Solana,
        MockStaking.yield(validators = validators.toList(), allValidators = allValidators),
    )

    private fun stakedOn(address: String?) = MockStaking.stakeKitBalance(
        items = listOf(MockStaking.balanceItem(validatorAddress = address)),
    )

    private fun solanaStaking(apy: String, isActive: Boolean, type: RewardType = RewardType.APR) = EarnRate(
        apy = BigDecimal(apy),
        isActive = isActive,
        source = EarnRate.Source.Staking(StakingIntegrationID.StakeKit.Coin.Solana, type),
    )

    private fun yieldSupplyRate(isActive: Boolean = false) = EarnRate(
        apy = BigDecimal("0.055"),
        isActive = isActive,
        source = EarnRate.Source.YieldSupply(BigDecimal("5.5")),
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class YieldSupply {

        @Test
        fun `GIVEN evm token and upper case key WHEN invoke THEN matched case insensitively`() {
            // Act
            val actual = useCase(status(usdcEvm), mapOf(evmKeyUpper to BigDecimal("5.5")), emptyMap())

            // Assert
            assertThat(actual).isEqualTo(yieldSupplyRate())
        }

        // Pins current behaviour: yieldSupplyKey lowercases the contract, so a case-sensitive key in original case never matches
        @Test
        fun `GIVEN solana token and original case key WHEN invoke THEN not matched`() {
            // Act
            val actual = useCase(status(usdcSolana), mapOf(solanaKeyOriginal to BigDecimal("5.5")), emptyMap())

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN solana token and lowercase key WHEN invoke THEN matched`() {
            // Act
            val actual = useCase(status(usdcSolana), mapOf(solanaKeyLower to BigDecimal("5.5")), emptyMap())

            // Assert
            assertThat(actual).isEqualTo(yieldSupplyRate())
        }

        @Test
        fun `GIVEN long scale percent WHEN invoke THEN fraction keeps every digit`() {
            // Act
            val actual = useCase(
                status(usdcEvm),
                mapOf(evmKeyUpper to BigDecimal("5.851295672613467")),
                emptyMap(),
            )

            // Assert
            assertThat(actual).isEqualTo(
                EarnRate(
                    apy = BigDecimal("0.05851295672613467"),
                    isActive = false,
                    source = EarnRate.Source.YieldSupply(BigDecimal("5.851295672613467")),
                ),
            )
        }

        @Test
        fun `GIVEN active yield supply WHEN invoke THEN rate is active`() {
            // Act
            val actual = useCase(
                status(usdcEvm, isYieldActive = true),
                mapOf(evmKeyUpper to BigDecimal("5.5")),
                emptyMap(),
            )

            // Assert
            assertThat(actual).isEqualTo(yieldSupplyRate(isActive = true))
        }

        @Test
        fun `GIVEN inactive yield supply status WHEN invoke THEN rate is not active`() {
            // Act
            val actual = useCase(
                status(usdcEvm, isYieldActive = false),
                mapOf(evmKeyUpper to BigDecimal("5.5")),
                emptyMap(),
            )

            // Assert
            assertThat(actual).isEqualTo(yieldSupplyRate(isActive = false))
        }

        @Test
        fun `GIVEN key of another network WHEN invoke THEN falls back to staking and returns null`() {
            // Act
            val actual = useCase(
                status(usdcEvm),
                mapOf("polygon_0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48" to BigDecimal("5.5")),
                emptyMap(),
            )

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN coin with non empty yield map WHEN invoke THEN yield supply skipped and staking rate used`() {
            // Arrange
            val staking = mapOf<CryptoCurrency, StakingAvailability>(
                solana to StakingAvailability.Available(stakeKitOption(vPref7)),
            )

            // Act
            val actual = useCase(status(solana), mapOf(evmKeyUpper to BigDecimal("5.5")), staking)

            // Assert
            assertThat(actual).isEqualTo(solanaStaking("0.07", isActive = false))
        }

        @Test
        fun `GIVEN token in yield map and staking map WHEN invoke THEN yield supply wins`() {
            // Arrange
            val staking = mapOf<CryptoCurrency, StakingAvailability>(
                usdcEvm to StakingAvailability.Available(stakeKitOption(vPref7)),
            )

            // Act
            val actual = useCase(status(usdcEvm), mapOf(evmKeyUpper to BigDecimal("5.5")), staking)

            // Assert
            assertThat(actual).isEqualTo(yieldSupplyRate())
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StakeKit {

        private fun resolve(option: StakingOption.StakeKit, balance: StakingBalance.Data? = null): EarnRate? {
            return useCase(
                currencyStatus = status(solana, balance),
                yieldSupplyApy = emptyMap(),
                stakingAvailability = mapOf(solana to StakingAvailability.Available(option)),
            )
        }

        @Test
        fun `GIVEN no stake WHEN invoke THEN best preferred validator rate returned`() {
            // Act
            val actual = resolve(stakeKitOption(vPref7, vPref8, vOther99))

            // Assert
            assertThat(actual).isEqualTo(solanaStaking("0.08", isActive = false))
        }

        @Test
        fun `GIVEN stake on non preferred validator WHEN invoke THEN that validator rate returned`() {
            // Act
            val actual = resolve(stakeKitOption(vPref7, vOther5), stakedOn("validator-2"))

            // Assert
            assertThat(actual).isEqualTo(solanaStaking("0.05", isActive = true))
        }

        @Test
        fun `GIVEN stake on unknown validator WHEN invoke THEN best preferred rate and active`() {
            // Act
            val actual = resolve(stakeKitOption(vPref7, vPref8), stakedOn("unknown"))

            // Assert
            assertThat(actual).isEqualTo(solanaStaking("0.08", isActive = true))
        }

        @Test
        fun `GIVEN stake on unknown then known validator WHEN invoke THEN known validator rate returned`() {
            // Arrange
            val balance = MockStaking.stakeKitBalance(
                items = listOf(
                    MockStaking.balanceItem(validatorAddress = "unknown"),
                    MockStaking.balanceItem(validatorAddress = "validator-2"),
                ),
            )

            // Act
            val actual = resolve(stakeKitOption(vPref7, vOther5), balance)

            // Assert
            assertThat(actual).isEqualTo(solanaStaking("0.05", isActive = true))
        }

        @Test
        fun `GIVEN stake item without validator WHEN invoke THEN best preferred rate and active`() {
            // Act
            val actual = resolve(stakeKitOption(vPref7), stakedOn(null))

            // Assert
            assertThat(actual).isEqualTo(solanaStaking("0.07", isActive = true))
        }

        @Test
        fun `GIVEN preferred validator with APY reward WHEN invoke THEN APY reward type returned`() {
            // Arrange
            val validator = MockStaking.validator(
                preferred = true,
                rewardInfo = RewardInfo(BigDecimal("0.06"), RewardType.APY),
            )

            // Act
            val actual = resolve(stakeKitOption(validator))

            // Assert
            assertThat(actual).isEqualTo(solanaStaking("0.06", isActive = false, type = RewardType.APY))
        }

        @Test
        fun `GIVEN no preferred validator WHEN invoke THEN null`() {
            // Act
            val actual = resolve(stakeKitOption(vOther5))

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN preferred validator without reward info WHEN invoke THEN null`() {
            // Arrange
            val validator = MockStaking.validator(preferred = true, rewardInfo = null)

            // Act
            val actual = resolve(stakeKitOption(validator))

            // Assert
            assertThat(actual).isNull()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class P2PEthPool {

        private val option = StakingOption.P2PEthPool(
            listOf(
                MockStaking.vault(apy = BigDecimal("0.03")),
                MockStaking.vault(vaultAddress = "0xvault2", apy = BigDecimal("0.045")),
            ),
        )

        private fun p2pRate(isActive: Boolean) = EarnRate(
            apy = BigDecimal("0.045"),
            isActive = isActive,
            source = EarnRate.Source.Staking(StakingIntegrationID.P2PEthPool, RewardType.APY),
        )

        @Test
        fun `GIVEN p2p option and no stake WHEN invoke THEN max vault apy and not active`() {
            // Act
            val actual = useCase(
                status(ethereum),
                emptyMap(),
                mapOf(ethereum to StakingAvailability.Available(option)),
            )

            // Assert
            assertThat(actual).isEqualTo(p2pRate(isActive = false))
        }

        @Test
        fun `GIVEN p2p option and p2p stake WHEN invoke THEN rate is active`() {
            // Act
            val actual = useCase(
                status(ethereum, MockStaking.p2pBalance()),
                emptyMap(),
                mapOf(ethereum to StakingAvailability.Available(option)),
            )

            // Assert
            assertThat(actual).isEqualTo(p2pRate(isActive = true))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Availability {

        private val p2pOption = StakingOption.P2PEthPool(
            listOf(
                MockStaking.vault(apy = BigDecimal("0.03")),
                MockStaking.vault(vaultAddress = "0xvault2", apy = BigDecimal("0.045")),
            ),
        )

        @Test
        fun `GIVEN empty staking map WHEN invoke THEN null`() {
            // Act
            val actual = useCase(status(solana), emptyMap(), emptyMap())

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN currency missing in staking map WHEN invoke THEN null`() {
            // Arrange
            val staking = mapOf<CryptoCurrency, StakingAvailability>(
                ethereum to StakingAvailability.Available(stakeKitOption(vPref7)),
            )

            // Act
            val actual = useCase(status(solana), emptyMap(), staking)

            // Assert
            assertThat(actual).isNull()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN availability without option WHEN invoke THEN null`(model: StakingAvailability) {
            // Act
            val actual = useCase(status(solana), emptyMap(), mapOf(solana to model))

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN Full availability and no stake WHEN invoke THEN null`() {
            // Arrange
            val staking = mapOf<CryptoCurrency, StakingAvailability>(
                solana to StakingAvailability.Full(stakeKitOption(vPref7)),
            )

            // Act
            val actual = useCase(status(solana), emptyMap(), staking)

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN Full availability and stakeKit stake WHEN invoke THEN active rate returned`() {
            // Arrange
            val staking = mapOf<CryptoCurrency, StakingAvailability>(
                solana to StakingAvailability.Full(stakeKitOption(vPref7)),
            )

            // Act
            val actual = useCase(status(solana, stakedOn("validator-1")), emptyMap(), staking)

            // Assert
            assertThat(actual).isEqualTo(solanaStaking("0.07", isActive = true))
        }

        @Test
        fun `GIVEN Full p2p availability and p2p stake WHEN invoke THEN active rate returned`() {
            // Arrange
            val staking = mapOf<CryptoCurrency, StakingAvailability>(
                ethereum to StakingAvailability.Full(p2pOption),
            )

            // Act
            val actual = useCase(status(ethereum, MockStaking.p2pBalance()), emptyMap(), staking)

            // Assert
            assertThat(actual).isEqualTo(
                EarnRate(
                    apy = BigDecimal("0.045"),
                    isActive = true,
                    source = EarnRate.Source.Staking(StakingIntegrationID.P2PEthPool, RewardType.APY),
                ),
            )
        }

        @Test
        fun `GIVEN loading status and Available option WHEN invoke THEN not active rate`() {
            // Arrange
            val staking = mapOf<CryptoCurrency, StakingAvailability>(
                solana to StakingAvailability.Available(stakeKitOption(vPref7)),
            )

            // Act
            val actual = useCase(CryptoCurrencyStatus(solana, CryptoCurrencyStatus.Loading), emptyMap(), staking)

            // Assert
            assertThat(actual).isEqualTo(solanaStaking("0.07", isActive = false))
        }

        private fun provideTestModels() = listOf<StakingAvailability>(
            StakingAvailability.Unavailable,
            StakingAvailability.TemporaryUnavailable,
            StakingAvailability.RegionUnavailable,
        )
    }
}