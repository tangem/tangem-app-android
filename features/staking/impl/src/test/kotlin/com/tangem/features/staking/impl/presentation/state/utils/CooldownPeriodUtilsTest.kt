package com.tangem.features.staking.impl.presentation.state.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.SECONDS_IN_HOUR
import com.tangem.domain.staking.model.CooldownPeriod
import com.tangem.domain.staking.model.Period
import com.tangem.features.staking.impl.R
import com.tangem.utils.StringsSigns.MINUS
import com.tangem.utils.StringsSigns.NON_BREAKING_SPACE
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Tests for [CooldownPeriod.toTextReference] extension function.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CooldownPeriodUtilsTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class `Fixed with Days` {

        @Test
        fun `should return plural days reference for Fixed Period Days`() {
            // given
            val cooldown = CooldownPeriod.Fixed(Period.Days(7))

            // when
            val result = cooldown.toTextReference()

            // then
            val expected = pluralReference(
                id = R.plurals.common_days,
                count = 7,
                formatArgs = wrappedList(7),
            )
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `should return plural days with zero for Fixed Period Days zero`() {
            // given
            val cooldown = CooldownPeriod.Fixed(Period.Days(0))

            // when
            val result = cooldown.toTextReference()

            // then
            val expected = pluralReference(
                id = R.plurals.common_days,
                count = 0,
                formatArgs = wrappedList(0),
            )
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `should pass day count as both count and format arg`() {
            // given
            val days = 14
            val cooldown = CooldownPeriod.Fixed(Period.Days(days))

            // when
            val result = cooldown.toTextReference()

            // then
            assertThat(result).isInstanceOf(TextReference.PluralRes::class.java)
            val plural = result as TextReference.PluralRes
            assertThat(plural.count).isEqualTo(days)
            assertThat(plural.formatArgs.first()).isEqualTo(days)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class `Fixed with Seconds` {

        @Test
        fun `should return plural hours reference for Fixed Period Seconds`() {
            // given — 2 hours worth of seconds
            val cooldown = CooldownPeriod.Fixed(Period.Seconds(2 * SECONDS_IN_HOUR))

            // when
            val result = cooldown.toTextReference()

            // then
            val expectedHours = 2
            val expected = pluralReference(
                id = R.plurals.common_hours,
                count = expectedHours,
                formatArgs = wrappedList(expectedHours),
            )
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `should convert exactly one hour worth of seconds to 1 hour`() {
            // given
            val cooldown = CooldownPeriod.Fixed(Period.Seconds(SECONDS_IN_HOUR))

            // when
            val result = cooldown.toTextReference()

            // then
            assertThat(result).isInstanceOf(TextReference.PluralRes::class.java)
            val plural = result as TextReference.PluralRes
            assertThat(plural.count).isEqualTo(1)
            assertThat(plural.formatArgs.first()).isEqualTo(1)
        }

        @Test
        fun `should floor to 1 hour when seconds are not divisible evenly`() {
            // given — 5000 seconds = 1.388... hours → integer division → 1
            val cooldown = CooldownPeriod.Fixed(Period.Seconds(5000))

            // when
            val result = cooldown.toTextReference()

            // then
            assertThat(result).isInstanceOf(TextReference.PluralRes::class.java)
            val plural = result as TextReference.PluralRes
            assertThat(plural.count).isEqualTo(1)
        }

        @Test
        fun `should return 0 hours for zero seconds`() {
            // given
            val cooldown = CooldownPeriod.Fixed(Period.Seconds(0))

            // when
            val result = cooldown.toTextReference()

            // then
            assertThat(result).isInstanceOf(TextReference.PluralRes::class.java)
            val plural = result as TextReference.PluralRes
            assertThat(plural.count).isEqualTo(0)
            assertThat(plural.formatArgs.first()).isEqualTo(0)
        }

        @Test
        fun `should use R plurals common_hours resource id`() {
            // given
            val cooldown = CooldownPeriod.Fixed(Period.Seconds(SECONDS_IN_HOUR))

            // when
            val result = cooldown.toTextReference()

            // then
            assertThat(result).isInstanceOf(TextReference.PluralRes::class.java)
            assertThat((result as TextReference.PluralRes).id).isEqualTo(R.plurals.common_hours)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Range {

        @Test
        fun `should return combined reference for Range`() {
            // given
            val minDays = 2
            val maxDays = 5
            val cooldown = CooldownPeriod.Range(minDays = minDays, maxDays = maxDays)

            // when
            val result = cooldown.toTextReference()

            // then
            val expected = combinedReference(
                stringReference("$minDays$MINUS$maxDays$NON_BREAKING_SPACE"),
                pluralReference(
                    id = R.plurals.common_days_no_param,
                    count = maxDays,
                ),
            )
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `should use maxDays as count for plural in Range`() {
            // given
            val maxDays = 10
            val cooldown = CooldownPeriod.Range(minDays = 3, maxDays = maxDays)

            // when
            val result = cooldown.toTextReference()

            // then
            assertThat(result).isInstanceOf(TextReference.Combined::class.java)
            val combined = result as TextReference.Combined
            val pluralPart = combined.refs[1]
            assertThat(pluralPart).isInstanceOf(TextReference.PluralRes::class.java)
            assertThat((pluralPart as TextReference.PluralRes).count).isEqualTo(maxDays)
        }

        @Test
        fun `should include minDays and maxDays with minus and non-breaking-space in string part`() {
            // given
            val minDays = 1
            val maxDays = 7
            val cooldown = CooldownPeriod.Range(minDays = minDays, maxDays = maxDays)

            // when
            val result = cooldown.toTextReference()

            // then
            assertThat(result).isInstanceOf(TextReference.Combined::class.java)
            val combined = result as TextReference.Combined
            val stringPart = combined.refs[0]
            assertThat(stringPart).isInstanceOf(TextReference.Str::class.java)
            assertThat((stringPart as TextReference.Str).value)
                .isEqualTo("$minDays$MINUS$maxDays$NON_BREAKING_SPACE")
        }
    }
}