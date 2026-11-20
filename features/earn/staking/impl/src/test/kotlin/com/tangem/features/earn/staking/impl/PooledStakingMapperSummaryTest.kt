package com.tangem.features.earn.staking.impl

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.earn.ResolveEarnRateUseCase
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingOption
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.product.api.product.EarnPositionSummary
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnRisk
import com.tangem.features.earn.product.api.product.ProductAvailability
import com.tangem.features.earn.staking.impl.fixtures.coin
import com.tangem.features.earn.staking.impl.fixtures.currencyStatus
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PooledStakingMapperSummaryTest {

    private val mapper = PooledStakingMapper()
    private val userWalletId = UserWalletId("1234567890ABCDEF")
    private val ethereum = coin(Blockchain.Ethereum)
    private var previousLocale: Locale = Locale.getDefault()

    @BeforeEach
    fun pinLocale() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(previousLocale)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN availability and position WHEN summary THEN entry or null per table`(model: SummaryModel) {
        // Arrange
        val status = status(hasPosition = model.hasPosition)

        // Act
        val actual = mapper.summary(userWalletId, status, model.availability)

        // Assert
        val expected = model.expectedAvailability?.let { availability ->
            EarnProductSummary(
                productId = POOLED_STAKING_ID,
                rateText = stringReference("3.00% APY"),
                risk = EarnRisk.Low,
                availability = availability,
                rate = EarnProductRate(BigDecimal("0.03"), EarnRateType.APY),
                position = EarnPositionSummary(BigDecimal("2")).takeIf { model.hasPosition },
                source = StatusSource.ACTUAL,
                onClickAction = PooledStakingAction(
                    stringReference(if (model.hasPosition) "Manage" else "Start earning"),
                ),
                route = stakingRoute(userWalletId, ethereum),
            )
        }
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN vaults with different apy WHEN summary THEN rate is the highest apy`() {
        // Arrange
        val option = StakingOption.P2PEthPool(
            vaults = listOf(
                MockStaking.vault(vaultAddress = "0xvault1", apy = BigDecimal("0.03")),
                MockStaking.vault(vaultAddress = "0xvault2", apy = BigDecimal("0.055")),
            ),
        )

        // Act
        val actual = mapper.summary(userWalletId, status(hasPosition = false), StakingAvailability.Available(option))

        // Assert
        assertThat(actual?.rate).isEqualTo(EarnProductRate(BigDecimal("0.055"), EarnRateType.APY))
        assertThat(actual?.rateText).isEqualTo(stringReference("5.50% APY"))
    }

    @Test
    fun `GIVEN a failed balance without position WHEN summary THEN position is null and click action is Manage`() {
        // Arrange
        val status = status(hasPosition = false)

        // Act
        val actual = mapper.summary(
            userWalletId = userWalletId,
            status = status,
            availability = StakingAvailability.Available(option()),
            isBalanceFailed = true,
        )

        // Assert
        assertThat(actual?.position).isNull()
        assertThat(actual?.onClickAction).isEqualTo(PooledStakingAction(stringReference("Manage")))
    }

    @Test
    fun `GIVEN a failed balance and a known position WHEN summary THEN the known position is kept`() {
        // Arrange
        val status = status(hasPosition = true)

        // Act
        val actual = mapper.summary(
            userWalletId = userWalletId,
            status = status,
            availability = StakingAvailability.Available(option()),
            isBalanceFailed = true,
        )

        // Assert
        assertThat(actual?.position).isEqualTo(EarnPositionSummary(BigDecimal("2")))
        assertThat(actual?.onClickAction).isEqualTo(PooledStakingAction(stringReference("Manage")))
    }

    @Test
    fun `GIVEN a failed balance and a Full pool without position WHEN summary THEN no entry`() {
        // Arrange
        val status = status(hasPosition = false)

        // Act
        val actual = mapper.summary(
            userWalletId = userWalletId,
            status = status,
            availability = StakingAvailability.Full(option()),
            isBalanceFailed = true,
        )

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN staking balance source ONLY_CACHE WHEN summary THEN summary source is ONLY_CACHE`() {
        // Arrange
        val status = currencyStatus(
            currency = ethereum,
            stakingBalance = MockStaking.p2pBalance(source = StatusSource.ONLY_CACHE),
        )

        // Act
        val actual = mapper.summary(userWalletId, status, StakingAvailability.Available(option()))

        // Assert
        assertThat(actual?.source).isEqualTo(StatusSource.ONLY_CACHE)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN same inputs WHEN mapper summary and ResolveEarnRateUseCase THEN rate and activity agree`(
        model: SummaryModel,
    ) {
        // Arrange
        val status = status(hasPosition = model.hasPosition)

        // Act
        val summary = mapper.summary(userWalletId, status, model.availability)
        val legacyRate = ResolveEarnRateUseCase().invoke(
            currencyStatus = status,
            yieldSupplyApy = emptyMap(),
            stakingAvailability = mapOf(status.currency to model.availability),
        )

        // Assert
        if (legacyRate == null) {
            assertThat(summary).isNull()
        } else {
            assertThat(summary?.rate?.fraction).isEqualTo(legacyRate.apy)
            assertThat(summary?.position != null).isEqualTo(legacyRate.isActive)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StatusWithoutBalances {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a status without balances WHEN summary THEN no entry`(model: NoBalancesModel) {
            // Arrange
            val status = CryptoCurrencyStatus(currency = ethereum, value = model.value)

            // Act
            val actual = mapper.summary(userWalletId, status, StakingAvailability.Available(option()))

            // Assert
            assertThat(actual).isNull()
        }

        private fun provideTestModels() = listOf(
            NoBalancesModel("Loading", CryptoCurrencyStatus.Loading),
            NoBalancesModel(
                name = "Unreachable",
                value = CryptoCurrencyStatus.Unreachable(priceChange = null, fiatRate = null, networkAddress = null),
            ),
            NoBalancesModel("NoAmount", CryptoCurrencyStatus.NoAmount(priceChange = null, fiatRate = null)),
            NoBalancesModel(
                name = "MissedDerivation",
                value = CryptoCurrencyStatus.MissedDerivation(priceChange = null, fiatRate = null),
            ),
        )
    }

    private fun status(hasPosition: Boolean): CryptoCurrencyStatus = currencyStatus(
        currency = ethereum,
        stakingBalance = MockStaking.p2pBalance(
            accounts = listOf(MockStaking.p2pAccount(staked = BigDecimal("2"))),
        ).takeIf { hasPosition },
    )

    private fun option(): StakingOption.P2PEthPool = StakingOption.P2PEthPool(vaults = listOf(MockStaking.vault()))

    private fun provideTestModels(): List<SummaryModel> {
        val available = StakingAvailability.Available(option())
        val full = StakingAvailability.Full(option())
        val unavailable = listOf(
            StakingAvailability.Unavailable,
            StakingAvailability.TemporaryUnavailable,
            StakingAvailability.RegionUnavailable,
        )
        return listOf(
            SummaryModel(available, hasPosition = false, expectedAvailability = ProductAvailability.Available),
            SummaryModel(available, hasPosition = true, expectedAvailability = ProductAvailability.Available),
            SummaryModel(full, hasPosition = true, expectedAvailability = ProductAvailability.Full),
            SummaryModel(full, hasPosition = false, expectedAvailability = null),
        ) + unavailable.flatMap { availability ->
            listOf(
                SummaryModel(availability, hasPosition = false, expectedAvailability = null),
                SummaryModel(availability, hasPosition = true, expectedAvailability = null),
            )
        }
    }

    internal data class NoBalancesModel(val name: String, val value: CryptoCurrencyStatus.Value) {
        override fun toString(): String = name
    }

    internal data class SummaryModel(
        val availability: StakingAvailability,
        val hasPosition: Boolean,
        val expectedAvailability: ProductAvailability?,
    ) {
        override fun toString(): String =
            "${availability::class.simpleName}, position=$hasPosition -> ${expectedAvailability ?: "no entry"}"
    }
}