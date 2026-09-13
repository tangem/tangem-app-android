package com.tangem.domain.models.yield.supply

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Tests [YieldSupplyContribution] as a `BalanceContribution`.
 *
 * It is the one contribution that adds nothing: the supplied principal is already inside the network amount, so the
 * wrapper exists only to carry freshness and the partition data ("Axis 2") through the same generic channel as every
 * other extra balance. A non-zero delta here would double-count every supplied balance in the app, which is why the
 * zero is pinned rather than assumed.
 *
 * The wrapper also exists because [YieldSupplyStatus] carries no freshness of its own — [YieldSupplyContribution.source]
 * has to come from the network status it arrived with, so the pass-through is asserted with a non-default value.
 */
internal class YieldSupplyContributionTest {

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