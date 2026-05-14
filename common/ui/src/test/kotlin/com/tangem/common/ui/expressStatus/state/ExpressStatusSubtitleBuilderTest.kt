package com.tangem.common.ui.expressStatus.state

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.WrappedList
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.utils.StringsSigns
import org.junit.jupiter.api.Test

internal class ExpressStatusSubtitleBuilderTest {

    private val status = stringReference("Confirming")

    private val minutesAgo = TextReference.PluralRes(
        id = R.plurals.common_minutes_time_ago,
        count = 30,
        formatArgs = wrappedList(30),
    )

    private val hoursAgo = TextReference.PluralRes(
        id = R.plurals.common_hours_time_ago,
        count = 3,
        formatArgs = wrappedList(3),
    )

    private val today = TextReference.Combined(
        refs = WrappedList(
            data = listOf(
                TextReference.Res(R.string.common_today),
                TextReference.Str(StringsSigns.COMA_SIGN),
                TextReference.Str(StringsSigns.WHITE_SPACE),
                TextReference.Str("03:00"),
            ),
        ),
    )

    private val fullDate = TextReference.Str("14 Oct 2025")

    @Test
    fun `GIVEN empty activeStatus AND empty date WHEN buildExpressStatusSubtitle THEN return EMPTY`() {
        val result = buildExpressStatusSubtitle(
            activeStatus = TextReference.EMPTY,
            date = TextReference.EMPTY,
        )

        assertThat(result).isEqualTo(TextReference.EMPTY)
    }

    @Test
    fun `GIVEN non-empty activeStatus AND empty date WHEN buildExpressStatusSubtitle THEN return activeStatus`() {
        val result = buildExpressStatusSubtitle(activeStatus = status, date = TextReference.EMPTY)

        assertThat(result).isEqualTo(status)
    }

    @Test
    fun `GIVEN empty activeStatus AND MinutesAgo date WHEN buildExpressStatusSubtitle THEN return date as is`() {
        val result = buildExpressStatusSubtitle(activeStatus = TextReference.EMPTY, date = minutesAgo)

        assertThat(result).isEqualTo(minutesAgo)
    }

    @Test
    fun `GIVEN status AND MinutesAgo date WHEN buildExpressStatusSubtitle THEN return Combined with tilde separator`() {
        val result = buildExpressStatusSubtitle(activeStatus = status, date = minutesAgo)

        assertThat(result).isEqualTo(
            TextReference.Combined(refs = wrappedList(status, stringReference(" ~ "), minutesAgo)),
        )
    }

    @Test
    fun `GIVEN status AND HoursAgo date WHEN buildExpressStatusSubtitle THEN return Combined with tilde separator`() {
        val result = buildExpressStatusSubtitle(activeStatus = status, date = hoursAgo)

        assertThat(result).isEqualTo(
            TextReference.Combined(refs = wrappedList(status, stringReference(" ~ "), hoursAgo)),
        )
    }

    @Test
    fun `GIVEN status AND Today date WHEN buildExpressStatusSubtitle THEN return Combined with space AND decapitalized today`() {
        val result = buildExpressStatusSubtitle(activeStatus = status, date = today)

        val expectedToday = TextReference.Combined(
            refs = WrappedList(
                data = listOf(
                    TextReference.Res(id = R.string.common_today, shouldDecapitalize = true),
                    TextReference.Str(StringsSigns.COMA_SIGN),
                    TextReference.Str(StringsSigns.WHITE_SPACE),
                    TextReference.Str("03:00"),
                ),
            ),
        )
        assertThat(result).isEqualTo(
            TextReference.Combined(refs = wrappedList(status, stringReference(" "), expectedToday)),
        )
    }

    @Test
    fun `GIVEN status AND FullDate date WHEN buildExpressStatusSubtitle THEN return Combined with space separator`() {
        val result = buildExpressStatusSubtitle(activeStatus = status, date = fullDate)

        assertThat(result).isEqualTo(
            TextReference.Combined(refs = wrappedList(status, stringReference(" "), fullDate)),
        )
    }

    @Test
    fun `GIVEN empty activeStatus AND Today date WHEN buildExpressStatusSubtitle THEN return Today as is without decapitalize`() {
        val result = buildExpressStatusSubtitle(activeStatus = TextReference.EMPTY, date = today)

        assertThat(result).isEqualTo(today)
    }
}