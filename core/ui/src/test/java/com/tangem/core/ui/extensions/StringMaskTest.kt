package com.tangem.core.ui.extensions

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StringMaskTest {

    @Test
    fun GIVEN_empty_string_WHEN_mask_THEN_returns_empty_string() {
        // GIVEN
        val input = ""

        // WHEN
        val actual = input.mask()

        // THEN
        assertThat(actual).isEqualTo("")
    }

    @Test
    fun GIVEN_one_char_WHEN_mask_THEN_returns_asterisk() {
        // GIVEN
        val input = "a"

        // WHEN
        val actual = input.mask()

        // THEN
        assertThat(actual).isEqualTo("*")
    }

    @Test
    fun GIVEN_two_chars_WHEN_mask_THEN_returns_two_asterisks() {
        // GIVEN
        val input = "ab"

        // WHEN
        val actual = input.mask()

        // THEN
        assertThat(actual).isEqualTo("**")
    }

    @Test
    fun GIVEN_three_chars_WHEN_mask_THEN_masks_middle_only() {
        // GIVEN
        val input = "abc"

        // WHEN
        val actual = input.mask()

        // THEN
        assertThat(actual).isEqualTo("a*c")
    }

    @Test
    fun GIVEN_four_chars_WHEN_mask_THEN_masks_middle_two() {
        // GIVEN
        val input = "abcd"

        // WHEN
        val actual = input.mask()

        // THEN
        assertThat(actual).isEqualTo("a**d")
    }

    @Test
    fun GIVEN_five_chars_WHEN_mask_THEN_keeps_edges_and_two_stars_in_middle() {
        // GIVEN
        val input = "abcde"

        // WHEN
        val actual = input.mask()

        // THEN
        assertThat(actual).isEqualTo("ab**de")
    }

    @Test
    fun GIVEN_seven_chars_WHEN_mask_THEN_keeps_two_on_each_side_and_two_stars() {
        // GIVEN
        val input = "abcdefg"

        // WHEN
        val actual = input.mask()

        // THEN
        assertThat(actual).isEqualTo("ab**fg")
    }
}