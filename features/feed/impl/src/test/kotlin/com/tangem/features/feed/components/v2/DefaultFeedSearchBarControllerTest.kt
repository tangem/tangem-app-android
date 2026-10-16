package com.tangem.features.feed.components.v2

import com.google.common.truth.Truth.assertThat
import com.tangem.features.feed.search.FeedSearchBarUM
import com.tangem.test.core.getEmittedValues
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultFeedSearchBarControllerTest {

    private val controller = DefaultFeedSearchBarController()

    @Test
    fun `GIVEN idle bar WHEN focused THEN bar is active and the search screen is not requested`() = runTest {
        // Arrange
        val requests = collectSearchScreenRequests()

        // Act
        controller.onFocusChange(isFocused = true)

        // Assert
        assertThat(controller.state.value).isEqualTo(FeedSearchBarUM(isActive = true))
        assertThat(requests).isEmpty()
    }

    @Test
    fun `GIVEN focused bar WHEN first character typed THEN the search screen is requested`() = runTest {
        // Arrange
        val requests = collectSearchScreenRequests()
        controller.onFocusChange(isFocused = true)

        // Act
        controller.onQueryChange("b")

        // Assert
        assertThat(requests).containsExactly(true)
    }

    @Test
    fun `GIVEN focused bar WHEN only whitespace typed THEN the search screen is not requested`() = runTest {
        // Arrange
        val requests = collectSearchScreenRequests()
        controller.onFocusChange(isFocused = true)

        // Act
        controller.onQueryChange(" ")

        // Assert
        assertThat(requests).isEmpty()
    }

    @Test
    fun `GIVEN search screen shown WHEN more is typed THEN the search screen is not requested again`() = runTest {
        // Arrange
        val requests = collectSearchScreenRequests()
        controller.onFocusChange(isFocused = true)
        controller.onQueryChange("b")
        controller.setSearchScreenShown(isShown = true)

        // Act
        controller.onQueryChange("bi")

        // Assert
        assertThat(requests).containsExactly(true)
    }

    @Test
    fun `GIVEN focused bar without search screen WHEN unfocused THEN bar returns to idle`() = runTest {
        // Arrange
        controller.onFocusChange(isFocused = true)
        controller.onQueryChange(" ")

        // Act
        controller.onFocusChange(isFocused = false)

        // Assert
        assertThat(controller.state.value).isEqualTo(FeedSearchBarUM())
    }

    @Test
    fun `GIVEN search screen shown WHEN unfocused THEN bar stays active with its query`() = runTest {
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
    fun `GIVEN focused search screen WHEN it leaves the stack THEN bar returns to idle`() = runTest {
        // Arrange
        controller.onFocusChange(isFocused = true)
        controller.onQueryChange("bitcoin")
        controller.setSearchScreenShown(isShown = true)

        // Act
        controller.setSearchScreenShown(isShown = false)

        // Assert
        assertThat(controller.state.value).isEqualTo(FeedSearchBarUM())
    }

    @Test
    fun `GIVEN focused bar without search screen WHEN stack changes THEN bar keeps focus`() = runTest {
        // Arrange
        controller.onFocusChange(isFocused = true)

        // Act
        controller.setSearchScreenShown(isShown = false)

        // Assert
        assertThat(controller.state.value).isEqualTo(FeedSearchBarUM(isActive = true))
    }

    @Test
    fun `GIVEN search screen shown WHEN closed THEN close is requested and query kept until it leaves`() = runTest {
        // Arrange
        val requests = collectSearchScreenRequests()
        controller.onFocusChange(isFocused = true)
        controller.onQueryChange("bitcoin")
        controller.setSearchScreenShown(isShown = true)

        // Act
        controller.onClose()

        // Assert
        assertThat(requests).containsExactly(true, false).inOrder()
        assertThat(controller.state.value).isEqualTo(FeedSearchBarUM(query = "bitcoin", isActive = true))
    }

    @Test
    fun `GIVEN focused bar without search screen WHEN closed THEN bar returns to idle`() = runTest {
        // Arrange
        controller.onFocusChange(isFocused = true)

        // Act
        controller.onClose()

        // Assert
        assertThat(controller.state.value).isEqualTo(FeedSearchBarUM())
    }

    @Test
    fun `GIVEN active session WHEN reset THEN bar returns to idle`() = runTest {
        // Arrange
        controller.onFocusChange(isFocused = true)
        controller.onQueryChange("bitcoin")
        controller.setSearchScreenShown(isShown = true)

        // Act
        controller.reset()

        // Assert
        assertThat(controller.state.value).isEqualTo(FeedSearchBarUM())
    }

    private fun TestScope.collectSearchScreenRequests(): List<Boolean> {
        return getEmittedValues(controller.searchScreenRequests)
    }
}