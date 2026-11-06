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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CooldownPeriodUtilsTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FixedDays {

        @Test
        fun `GIVEN fixed 7 days WHEN toTextReference THEN plural days reference`() {
            // Arrange
            val cooldown = CooldownPeriod.Fixed(Period.Days(7))

            // Act
            val result = cooldown.toTextReference()

            // Assert
            val expected = pluralReference(
                id = R.plurals.common_days,
                count = 7,
                formatArgs = wrappedList(7),
            )
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `GIVEN fixed 0 days WHEN toTextReference THEN plural days with zero`() {
            // Arrange
            val cooldown = CooldownPeriod.Fixed(Period.Days(0))

            // Act
            val result = cooldown.toTextReference()

            // Assert
            val expected = pluralReference(
                id = R.plurals.common_days,
                count = 0,
                formatArgs = wrappedList(0),
            )
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `GIVEN fixed 14 days WHEN toTextReference THEN count and format arg are days`() {
            // Arrange
            val days = 14
            val cooldown = CooldownPeriod.Fixed(Period.Days(days))

            // Act
            val result = cooldown.toTextReference()

            // Assert
            assertThat(result).isInstanceOf(TextReference.PluralRes::class.java)
            val plural = result as TextReference.PluralRes
            assertThat(plural.count).isEqualTo(days)
            assertThat(plural.formatArgs.first()).isEqualTo(days)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FixedSeconds {

        @Test
        fun `GIVEN fixed two hours of seconds WHEN toTextReference THEN plural hours reference`() {
            // Arrange
            val cooldown = CooldownPeriod.Fixed(Period.Seconds(2 * SECONDS_IN_HOUR))

            // Act
            val result = cooldown.toTextReference()

            // Assert
            val expectedHours = 2
            val expected = pluralReference(
                id = R.plurals.common_hours,
                count = expectedHours,
                formatArgs = wrappedList(expectedHours),
            )
            assertThat(result).isEqualTo(expected)
        }

        @Test
        fun `GIVEN fixed one hour of seconds WHEN toTextReference THEN one hour`() {
            // Arrange
            val cooldown = CooldownPeriod.Fixed(Period.Seconds(SECONDS_IN_HOUR))

            // Act
            val result = cooldown.toTextReference()

            // Assert
            assertThat(result).isInstanceOf(TextReference.PluralRes::class.java)
            val plural = result as TextReference.PluralRes
            assertThat(plural.count).isEqualTo(1)
            assertThat(plural.formatArgs.first()).isEqualTo(1)
        }

        @Test
        fun `GIVEN 5000 seconds WHEN toTextReference THEN floored to one hour`() {
            // Arrange
            val cooldown = CooldownPeriod.Fixed(Period.Seconds(5000))

            // Act
            val result = cooldown.toTextReference()

            // Assert
            assertThat(result).isInstanceOf(TextReference.PluralRes::class.java)
            val plural = result as TextReference.PluralRes
            assertThat(plural.count).isEqualTo(1)
        }

        @Test
        fun `GIVEN zero seconds WHEN toTextReference THEN zero hours`() {
            // Arrange
            val cooldown = CooldownPeriod.Fixed(Period.Seconds(0))

            // Act
            val result = cooldown.toTextReference()

            // Assert
            assertThat(result).isInstanceOf(TextReference.PluralRes::class.java)
            val plural = result as TextReference.PluralRes
            assertThat(plural.count).isEqualTo(0)
            assertThat(plural.formatArgs.first()).isEqualTo(0)
        }

        @Test
        fun `GIVEN fixed seconds WHEN toTextReference THEN common hours plural id`() {
            // Arrange
            val cooldown = CooldownPeriod.Fixed(Period.Seconds(SECONDS_IN_HOUR))

            // Act
            val result = cooldown.toTextReference()

            // Assert
            assertThat(result).isInstanceOf(TextReference.PluralRes::class.java)
            assertThat((result as TextReference.PluralRes).id).isEqualTo(R.plurals.common_hours)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Range {

        @Test
        fun `GIVEN range WHEN toTextReference THEN combined range reference`() {
            // Arrange
            val minDays = 2
            val maxDays = 5
            val cooldown = CooldownPeriod.Range(minDays = minDays, maxDays = maxDays)

            // Act
            val result = cooldown.toTextReference()

            // Assert
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
        fun `GIVEN range WHEN toTextReference THEN max days used as plural count`() {
            // Arrange
            val maxDays = 10
            val cooldown = CooldownPeriod.Range(minDays = 3, maxDays = maxDays)

            // Act
            val result = cooldown.toTextReference()

            // Assert
            assertThat(result).isInstanceOf(TextReference.Combined::class.java)
            val combined = result as TextReference.Combined
            val pluralPart = combined.refs[1]
            assertThat(pluralPart).isInstanceOf(TextReference.PluralRes::class.java)
            assertThat((pluralPart as TextReference.PluralRes).count).isEqualTo(maxDays)
        }

        @Test
        fun `GIVEN range WHEN toTextReference THEN string part has min minus max and nbsp`() {
            // Arrange
            val minDays = 1
            val maxDays = 7
            val cooldown = CooldownPeriod.Range(minDays = minDays, maxDays = maxDays)

            // Act
            val result = cooldown.toTextReference()

            // Assert
            assertThat(result).isInstanceOf(TextReference.Combined::class.java)
            val combined = result as TextReference.Combined
            val stringPart = combined.refs[0]
            assertThat(stringPart).isInstanceOf(TextReference.Str::class.java)
            assertThat((stringPart as TextReference.Str).value)
                .isEqualTo("$minDays$MINUS$maxDays$NON_BREAKING_SPACE")
        }
    }
}