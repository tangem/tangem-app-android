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
    fun `GIVEN qualificationEndDate less than a day away WHEN resolve THEN DaysLeft one`() {
        val result = resolveBoostBlockState(
            qualificationEndDate = Instant.parse("2026-05-28T18:00:00Z"),
            now = now,
        )

        assertThat(result).isEqualTo(BoostBlockState.DaysLeft(days = 1))
    }

    @Test
    fun `GIVEN qualificationEndDate just over a day away WHEN resolve THEN DaysLeft rounds up`() {
        val result = resolveBoostBlockState(
            // 1d 1h away — rounds up to 2
            qualificationEndDate = Instant.parse("2026-05-29T01:00:00Z"),
            now = now,
        )

        assertThat(result).isEqualTo(BoostBlockState.DaysLeft(days = 2))
    }

    @Test
    fun `GIVEN qualificationEndDate exactly whole days away WHEN resolve THEN DaysLeft exact`() {
        val result = resolveBoostBlockState(
            qualificationEndDate = Instant.parse("2026-05-30T00:00:00Z"),
            now = now,
        )

        assertThat(result).isEqualTo(BoostBlockState.DaysLeft(days = 2))
    }

    @Test
    fun `GIVEN qualificationEndDate equal to now WHEN resolve THEN AwaitingPayout`() {
        val result = resolveBoostBlockState(qualificationEndDate = now, now = now)

        assertThat(result).isEqualTo(BoostBlockState.AwaitingPayout)
    }

    @Test
    fun `GIVEN qualificationEndDate passed within 14 days WHEN resolve THEN AwaitingPayout`() {
        val result = resolveBoostBlockState(
            // 13d 23h 59m 59s ago — just inside the 14-day window
            qualificationEndDate = Instant.parse("2026-05-14T00:00:01Z"),
            now = now,
        )

        assertThat(result).isEqualTo(BoostBlockState.AwaitingPayout)
    }

    @Test
    fun `GIVEN qualificationEndDate passed exactly 14 days ago WHEN resolve THEN Hidden`() {
        val result = resolveBoostBlockState(
            qualificationEndDate = Instant.parse("2026-05-14T00:00:00Z"),
            now = now,
        )

        assertThat(result).isEqualTo(BoostBlockState.Hidden)
    }

    @Test
    fun `GIVEN qualificationEndDate passed more than 14 days ago WHEN resolve THEN Hidden`() {
        val result = resolveBoostBlockState(
            qualificationEndDate = Instant.parse("2026-05-01T00:00:00Z"),
            now = now,
        )

        assertThat(result).isEqualTo(BoostBlockState.Hidden)
    }
}