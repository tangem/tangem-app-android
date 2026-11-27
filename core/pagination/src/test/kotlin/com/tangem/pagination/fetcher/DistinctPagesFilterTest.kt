package com.tangem.pagination.fetcher

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class DistinctPagesFilterTest {

    private val filter = DistinctPagesFilter<String, String> { it }

    @Test
    fun `GIVEN an item returned by the previous page WHEN the next page is filtered THEN the item is dropped`() {
        // Arrange
        filter.startPage(isFirstPage = true).distinct(listOf("btc", "tcy"))

        // Act
        val actual = filter.startPage(isFirstPage = false).distinct(listOf("tcy", "eth"))

        // Assert
        assertThat(actual).containsExactly("eth")
    }

    @Test
    fun `GIVEN an item repeated within one page WHEN it is filtered THEN the first occurrence is kept`() {
        // Act
        val actual = filter.startPage(isFirstPage = true).distinct(listOf("btc", "eth", "btc"))

        // Assert
        assertThat(actual).containsExactly("btc", "eth").inOrder()
    }

    @Test
    fun `GIVEN a loaded session WHEN a first page starts again THEN earlier items are returned again`() {
        // Arrange
        filter.startPage(isFirstPage = true).distinct(listOf("btc", "eth"))

        // Act
        val actual = filter.startPage(isFirstPage = true).distinct(listOf("btc", "eth"))

        // Assert
        assertThat(actual).containsExactly("btc", "eth").inOrder()
    }

    @Test
    fun `GIVEN a page started before a reload WHEN its response lands after it THEN the new session is unaffected`() {
        // Arrange
        filter.startPage(isFirstPage = true).distinct(listOf("btc"))
        val stalePage = filter.startPage(isFirstPage = false)
        val reloadedPage = filter.startPage(isFirstPage = true)

        // Act
        stalePage.distinct(listOf("eth"))
        val actual = reloadedPage.distinct(listOf("btc", "eth"))

        // Assert
        assertThat(actual).containsExactly("btc", "eth").inOrder()
    }
}