package com.tangem.features.feed.components.v2

import com.google.common.truth.Truth.assertThat
import com.tangem.features.feed.search.FeedSearchBarUM
import org.junit.jupiter.api.Test

internal class DefaultFeedSearchBarControllerTest {

    private val controller = DefaultFeedSearchBarController()

    @Test
    fun `GIVEN idle bar WHEN focused THEN bar is active and the search screen is not requested`() {
        // Act
        controller.onFocusChange(isFocused = true)

        // Assert
        assertThat(controller.state.value).isEqualTo(FeedSearchBarUM(isActive = true))
        assertThat(controller.isSearchScreenRequested.value).isFalse()
    }

    @Test
    fun `GIVEN focused bar WHEN first character typed THEN the search screen is requested`() {
        // Arrange
        controller.onFocusChange(isFocused = true)

        // Act
        controller.onQueryChange("b")

        // Assert
        assertThat(controller.isSearchScreenRequested.value).isTrue()
    }

    @Test
    fun `GIVEN focused bar WHEN only whitespace typed THEN the search screen is not requested`() {
        // Arrange
        controller.onFocusChange(isFocused = true)

        // Act
        controller.onQueryChange(" ")

        // Assert
        assertThat(controller.isSearchScreenRequested.value).isFalse()
    }

    @Test
    fun `GIVEN search screen shown WHEN query cleared THEN the search screen is no longer requested`() {
        // Arrange
        controller.onFocusChange(isFocused = true)
        controller.onQueryChange("b")
        controller.setSearchScreenShown(isShown = true)

        // Act
        controller.onQueryChange("")

        // Assert
        assertThat(controller.isSearchScreenRequested.value).isFalse()
    }

    @Test
    fun `GIVEN search not shown yet WHEN typed and cleared at once THEN the search screen is not requested`() {
        // Arrange
        controller.onFocusChange(isFocused = true)

        // Act
        controller.onQueryChange("b")
        controller.onQueryChange("")

        // Assert
        assertThat(controller.isSearchScreenRequested.value).isFalse()
    }

    @Test
    fun `GIVEN focused search screen WHEN it leaves after the query is cleared THEN bar keeps focus`() {
        // Arrange
        controller.onFocusChange(isFocused = true)
        controller.onQueryChange("b")
        controller.setSearchScreenShown(isShown = true)
        controller.onQueryChange("")

        // Act
        controller.setSearchScreenShown(isShown = false)

        // Assert
        assertThat(controller.state.value).isEqualTo(FeedSearchBarUM(isActive = true))
    }

    @Test
    fun `GIVEN focused bar without search screen WHEN unfocused THEN bar returns to idle`() {
        // Arrange
        controller.onFocusChange(isFocused = true)
        controller.onQueryChange(" ")

        // Act
        controller.onFocusChange(isFocused = false)

        // Assert
        assertThat(controller.state.value).isEqualTo(FeedSearchBarUM())
    }

    @Test
    fun `GIVEN search screen shown WHEN unfocused THEN bar stays active with its query`() {
        // Arrange
        controller.onFocusChange(isFocused = true)
        controller.onQueryChange("bitcoin")
        controller.setSearchScreenShown(isShown = true)

        // Act
        controller.onFocusChange(isFocused = false)

        // Assert
        assertThat(controller.state.value).isEqualTo(FeedSearchBarUM(query = "bitcoin", isActive = true))
    }

    @Test
    fun `GIVEN search screen with a query WHEN it leaves the stack THEN bar is idle and search not requested`() {
        // Arrange
        controller.onFocusChange(isFocused = true)
        controller.onQueryChange("bitcoin")
        controller.setSearchScreenShown(isShown = true)

        // Act
        controller.setSearchScreenShown(isShown = false)

        // Assert
        assertThat(controller.state.value).isEqualTo(FeedSearchBarUM())
        assertThat(controller.isSearchScreenRequested.value).isFalse()
    }

    @Test
    fun `GIVEN focused bar without search screen WHEN stack changes THEN bar keeps focus`() {
        // Arrange
        controller.onFocusChange(isFocused = true)

        // Act
        controller.setSearchScreenShown(isShown = false)

        // Assert
        assertThat(controller.state.value).isEqualTo(FeedSearchBarUM(isActive = true))
    }

    @Test
    fun `GIVEN search screen shown WHEN closed THEN search is not requested and query kept until it leaves`() {
        // Arrange
        controller.onFocusChange(isFocused = true)
        controller.onQueryChange("bitcoin")
        controller.setSearchScreenShown(isShown = true)

        // Act
        controller.onClose()

        // Assert
        assertThat(controller.isSearchScreenRequested.value).isFalse()
        assertThat(controller.state.value).isEqualTo(FeedSearchBarUM(query = "bitcoin", isActive = true))
    }

    @Test
    fun `GIVEN focused bar without search screen WHEN closed THEN bar returns to idle`() {
        // Arrange
        controller.onFocusChange(isFocused = true)

        // Act
        controller.onClose()

        // Assert
        assertThat(controller.state.value).isEqualTo(FeedSearchBarUM())
    }

    @Test
    fun `GIVEN active session WHEN reset THEN bar returns to idle and search is not requested`() {
        // Arrange
        controller.onFocusChange(isFocused = true)
        controller.onQueryChange("bitcoin")
        controller.setSearchScreenShown(isShown = true)

        // Act
        controller.reset()

        // Assert
        assertThat(controller.state.value).isEqualTo(FeedSearchBarUM())
        assertThat(controller.isSearchScreenRequested.value).isFalse()
    }
}