package com.tangem.features.earn.staking.impl

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingBalance
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingOption
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.product.api.product.EarnActionItem
import com.tangem.features.earn.product.api.product.EarnLimits
import com.tangem.features.earn.product.api.product.EarnPosition
import com.tangem.features.earn.product.api.product.EarnPositionState
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.product.EarnValue
import com.tangem.features.earn.staking.impl.fixtures.coin
import com.tangem.features.earn.staking.impl.fixtures.currencyStatus
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class PooledStakingMapperStateTest {

    private val mapper = PooledStakingMapper()
    private val ethereum = coin(Blockchain.Ethereum)

    @Test
    fun `GIVEN several vaults WHEN state THEN rate and fee come from the vault with the highest apy`() {
        // Arrange
        val option = StakingOption.P2PEthPool(
            vaults = listOf(
                MockStaking.vault(vaultAddress = "0xvault1", apy = BigDecimal("0.03")),
                MockStaking.vault(vaultAddress = "0xvault2", apy = BigDecimal("0.06")).copy(
                    feePercent = BigDecimal("5"),
                ),
            ),
        )

        // Act
        val actual = mapper.state(status(), StakingAvailability.Available(option)) as EarnProductState.Ready

        // Assert
        assertThat(actual.rate).isEqualTo(EarnProductRate(BigDecimal("0.06"), EarnRateType.APY))
        assertThat(actual.details).contains(row("Performance fee", "5%"))
        assertThat(actual.projection?.netRate).isEquivalentAccordingToCompareTo(BigDecimal("0.057"))
    }

    @Test
    fun `GIVEN ready state WHEN reading fixed fields THEN contract values of the pooled staking product`() {
        // Arrange
        val availability = StakingAvailability.Available(option(apy = "0.03", feePercent = "10"))

        // Act
        val actual = mapper.state(status(), availability) as EarnProductState.Ready

        // Assert
        val expected = EarnProductState.Ready(
            rate = EarnProductRate(BigDecimal("0.03"), EarnRateType.APY),
            description = stringReference("Earn rewards by staking ETH in a P2P.org pool"),
            projection = null,
            benefits = emptyList(),
            howItWorks = null,
            details = listOf(row("Risk", "Low"), row("Protocol", "P2P.org"), row("Performance fee", "10%")),
            limits = EarnLimits(minDeposit = BigDecimal("0.01"), remainingCap = null),
            apyPeriods = emptyList(),
            primary = EarnActionItem(PooledStakingAction(stringReference("Start earning"))),
            secondary = emptyList(),
            freshness = null,
            position = EarnPositionState.None,
        )
        assertThat(actual.copy(projection = null)).isEqualTo(expected)
        assertThat(actual.projection).isNotNull()
    }

    @Test
    fun `GIVEN any vaults WHEN state THEN minimum deposit is the 0_01 ETH the staking flow enforces`() {
        // Arrange
        val availability = StakingAvailability.Available(option())

        // Act
        val actual = mapper.state(status(), availability) as EarnProductState.Ready

        // Assert
        assertThat(actual.limits).isEqualTo(EarnLimits(minDeposit = BigDecimal("0.01"), remainingCap = null))
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NetRate {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN apy and fee percent WHEN state THEN projection netRate is apy after fee`(model: NetRateModel) {
            // Arrange
            val availability = StakingAvailability.Available(option(model.apy, model.feePercent))

            // Act
            val actual = mapper.state(status(), availability) as EarnProductState.Ready

            // Assert
            assertThat(actual.projection?.netRate).isEquivalentAccordingToCompareTo(BigDecimal(model.expectedNetRate))
        }

        private fun provideTestModels() = listOf(
            NetRateModel(apy = "0.04", feePercent = "8", expectedNetRate = "0.0368"),
            NetRateModel(apy = "0.04", feePercent = "0", expectedNetRate = "0.04"),
            NetRateModel(apy = "0.04", feePercent = "100", expectedNetRate = "0"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Offer {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN no offer WHEN state THEN Unavailable`(model: NoOfferModel) {
            // Act
            val actual = mapper.state(status(), model.availability)

            // Assert
            assertThat(actual).isEqualTo(EarnProductState.Unavailable)
        }

        @Test
        fun `GIVEN Full with position WHEN state THEN Ready`() {
            // Arrange
            val status = status(position = MockStaking.p2pBalance())

            // Act
            val actual = mapper.state(status, StakingAvailability.Full(option()))

            // Assert
            assertThat(actual).isInstanceOf(EarnProductState.Ready::class.java)
        }

        @Test
        fun `GIVEN availability flips from Available to TemporaryUnavailable WHEN state of the same status THEN Ready then Unavailable`() {
            // Arrange
            val status = status()

            // Act
            val before = mapper.state(status, StakingAvailability.Available(option()))
            val after = mapper.state(status, StakingAvailability.TemporaryUnavailable)

            // Assert
            assertThat(before).isInstanceOf(EarnProductState.Ready::class.java)
            assertThat(after).isEqualTo(EarnProductState.Unavailable)
        }

        private fun provideTestModels() = listOf(
            NoOfferModel("Unavailable", StakingAvailability.Unavailable),
            NoOfferModel("TemporaryUnavailable", StakingAvailability.TemporaryUnavailable),
            NoOfferModel("RegionUnavailable", StakingAvailability.RegionUnavailable),
            NoOfferModel("Full without position", StakingAvailability.Full(option())),
            NoOfferModel(
                "Available StakeKit option",
                StakingAvailability.Available(
                    StakingOption.StakeKit(StakingIntegrationID.StakeKit.Coin.Solana, MockStaking.solanaYield),
                ),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Position {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN balance contribution WHEN state THEN position and button label`(model: PositionModel) {
            // Arrange
            val status = status(position = model.contribution)

            // Act
            val actual = mapper.state(status, StakingAvailability.Available(option())) as EarnProductState.Ready

            // Assert
            assertThat(actual.position).isEqualTo(model.expectedPosition)
            assertThat(actual.primary).isEqualTo(
                EarnActionItem(PooledStakingAction(stringReference(model.expectedLabel))),
            )
        }

        private fun provideTestModels(): List<PositionModel> {
            val stakingId = MockStaking.stakingId(MockStaking.P2P_ID)
            val staked = MockStaking.p2pBalance(
                accounts = listOf(MockStaking.p2pAccount(staked = BigDecimal("2"), earned = BigDecimal("0.1"))),
            )
            return listOf(
                PositionModel(
                    name = "P2P staking balance",
                    contribution = staked,
                    expectedPosition = EarnPositionState.Known(
                        EarnPosition(
                            cryptoAmount = BigDecimal("2"),
                            earned = BigDecimal("0.1"),
                            description = null,
                            rows = emptyList(),
                            rewards = null,
                            freshness = null,
                        ),
                    ),
                    expectedLabel = "Manage",
                ),
                PositionModel("Error balance", StakingBalance.Error(stakingId), EarnPositionState.Unknown, "Manage"),
                PositionModel(
                    name = "Empty balance",
                    contribution = StakingBalance.Empty(stakingId, StatusSource.ACTUAL),
                    expectedPosition = EarnPositionState.None,
                    expectedLabel = "Start earning",
                ),
                PositionModel("No balance", null, EarnPositionState.None, "Start earning"),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class LoadingStatus {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a Loading status WHEN state THEN Loading whatever the availability`(model: AvailabilityModel) {
            // Arrange
            val status = CryptoCurrencyStatus(currency = ethereum, value = CryptoCurrencyStatus.Loading)

            // Act
            val actual = mapper.state(status, model.availability)

            // Assert
            assertThat(actual).isEqualTo(EarnProductState.Loading)
        }

        private fun provideTestModels() = listOf(
            AvailabilityModel("Available", StakingAvailability.Available(option())),
            AvailabilityModel("Full", StakingAvailability.Full(option())),
            AvailabilityModel("Unavailable", StakingAvailability.Unavailable),
        )
    }

    private fun status(position: StakingBalance? = null): CryptoCurrencyStatus =
        currencyStatus(currency = ethereum, stakingBalance = position)

    private fun option(apy: String = "0.03", feePercent: String = "10"): StakingOption.P2PEthPool =
        StakingOption.P2PEthPool(
            vaults = listOf(MockStaking.vault(apy = BigDecimal(apy)).copy(feePercent = BigDecimal(feePercent))),
        )

    private fun row(label: String, value: String): EarnRow = EarnRow(
        label = stringReference(label),
        value = EarnValue.Text(stringReference(value)),
    )

    internal data class NetRateModel(val apy: String, val feePercent: String, val expectedNetRate: String) {
        override fun toString(): String = "apy=$apy, fee=$feePercent% -> $expectedNetRate"
    }

    internal data class NoOfferModel(val name: String, val availability: StakingAvailability) {
        override fun toString(): String = name
    }

    internal data class AvailabilityModel(val name: String, val availability: StakingAvailability) {
        override fun toString(): String = name
    }

    internal data class PositionModel(
        val name: String,
        val contribution: StakingBalance?,
        val expectedPosition: EarnPositionState,
        val expectedLabel: String,
    ) {
        override fun toString(): String = name
    }
}