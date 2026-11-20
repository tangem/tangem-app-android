package com.tangem.domain.models.yield.supply

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * Tests [YieldSupplyContribution] as a `BalanceContribution`.
 *
 * Supplied funds are spendable, so they never add to the total on top of `amount`: they are either already inside the
 * network amount, or reach it through the spendable delta the provider resolved and the status factory folds in. A
 * non-zero total delta would double-count every supplied balance in the app, which is why it is pinned rather than
 * assumed.
 *
 * The wrapper also exists because [YieldSupplyStatus] carries no freshness of its own — [YieldSupplyContribution.source]
 * has to come from what delivered it, so the pass-through is asserted with a non-default value.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldSupplyContributionTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a resolved delta WHEN deltas THEN spendable only while active and never on top of amount`(
        model: SpendableModel,
    ) {
        // Arrange
        val contribution = YieldSupplyContribution(
            status = YieldSupplyStatus(
                isActive = model.isActive,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = BigDecimal("7"),
            ),
            source = StatusSource.ACTUAL,
            spendableDelta = model.spendableDelta,
        )

        // Act
        val actual = Deltas(
            spendable = contribution.spendableDeltaCryptoAmount(),
            total = contribution.totalDeltaCryptoAmount(),
        )

        // Assert
        assertThat(actual).isEqualTo(Deltas(spendable = model.expectedSpendable, total = BigDecimal.ZERO))
    }

    @Suppress("unused")
    private fun provideTestModels() = listOf(
        SpendableModel(
            name = "active, supplied inside the amount",
            isActive = true,
            spendableDelta = BigDecimal.ZERO,
            expectedSpendable = BigDecimal.ZERO,
        ),
        SpendableModel(
            name = "active, supplied outside the amount",
            isActive = true,
            spendableDelta = BigDecimal("7"),
            expectedSpendable = BigDecimal("7"),
        ),
        SpendableModel(
            name = "active, network amount ahead of the position",
            isActive = true,
            spendableDelta = BigDecimal("-2"),
            expectedSpendable = BigDecimal("-2"),
        ),
        SpendableModel(
            name = "inactive",
            isActive = false,
            spendableDelta = BigDecimal("7"),
            expectedSpendable = BigDecimal.ZERO,
        ),
    )

    data class SpendableModel(
        val name: String,
        val isActive: Boolean,
        val spendableDelta: BigDecimal,
        val expectedSpendable: BigDecimal,
    ) {
        override fun toString(): String = name
    }

    data class Deltas(val spendable: BigDecimal, val total: BigDecimal)

    @Test
    fun `GIVEN any yield supply WHEN totalDeltaCryptoAmount THEN zero`() {
        // Arrange
        val contribution = YieldSupplyContribution(status = mockk(relaxed = true), source = StatusSource.ACTUAL)

        // Act
        val actual = contribution.totalDeltaCryptoAmount()

        // Assert — yield supply partitions a balance, it never adds to one
        assertThat(actual).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `GIVEN any yield supply WHEN kind THEN it is the yield supply discriminator`() {
        // Arrange
        val contribution = YieldSupplyContribution(status = mockk(relaxed = true), source = StatusSource.ACTUAL)

        // Act
        val actual = contribution.kind

        // Assert — the literal, not the constant: comparing the constant to itself can never fail, and `kind` is
        // the cross-module discriminator, so its value is part of the contract
        assertThat(actual).isEqualTo("yield_supply")
        assertThat(YieldSupplyContribution.CONTRIBUTION_KIND).isEqualTo("yield_supply")
    }

    @Test
    fun `GIVEN a cached network status WHEN source THEN it is carried over from the network status`() {
        // Arrange — the status itself has no freshness, so a hardcoded ACTUAL would go unnoticed without this
        val contribution = YieldSupplyContribution(status = mockk(relaxed = true), source = StatusSource.ONLY_CACHE)

        // Act
        val actual = contribution.source

        // Assert
        assertThat(actual).isEqualTo(StatusSource.ONLY_CACHE)
    }

    @Test
    fun `GIVEN a status WHEN wrapped THEN it is exposed unchanged for the type-specific breakdown`() {
        // Arrange
        val status = mockk<YieldSupplyStatus>(relaxed = true)

        // Act
        val contribution = YieldSupplyContribution(status = status, source = StatusSource.ACTUAL)

        // Assert — "Axis 2" readers downcast to this type and read the partition off `status`
        assertThat(contribution.status).isSameInstanceAs(status)
    }
}