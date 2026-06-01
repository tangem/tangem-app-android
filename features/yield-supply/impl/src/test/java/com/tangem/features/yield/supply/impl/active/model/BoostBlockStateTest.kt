package com.tangem.features.yield.supply.impl.active.model

import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

internal class BoostBlockStateTest {

    private val now = Instant.parse("2026-05-28T00:00:00Z")

    @Test
    fun `GIVEN null qualificationEndDate WHEN resolve THEN Hidden`() {
        val result = resolveBoostBlockState(qualificationEndDate = null, now = now)

        assertThat(result).isEqualTo(BoostBlockState.Hidden)
    }

    @Test
    fun `GIVEN future qualificationEndDate WHEN resolve THEN DaysLeft with whole days`() {
        val result = resolveBoostBlockState(
            qualificationEndDate = Instant.parse("2026-06-01T00:00:00Z"),
            now = now,
        )

        assertThat(result).isEqualTo(BoostBlockState.DaysLeft(days = 4))
    }

    @Test
    fun `GIVEN qualificationEndDate less than a day away WHEN resolve THEN DaysLeft zero`() {
        val result = resolveBoostBlockState(
            qualificationEndDate = Instant.parse("2026-05-28T18:00:00Z"),
            now = now,
        )

        assertThat(result).isEqualTo(BoostBlockState.DaysLeft(days = 0))
    }

    @Test
    fun `GIVEN qualificationEndDate equal to now WHEN resolve THEN AwaitingPayout`() {
        val result = resolveBoostBlockState(qualificationEndDate = now, now = now)

        assertThat(result).isEqualTo(BoostBlockState.AwaitingPayout)
    }

    @Test
    fun `GIVEN past qualificationEndDate WHEN resolve THEN AwaitingPayout`() {
        val result = resolveBoostBlockState(
            qualificationEndDate = Instant.parse("2026-05-01T00:00:00Z"),
            now = now,
        )

        assertThat(result).isEqualTo(BoostBlockState.AwaitingPayout)
    }
}