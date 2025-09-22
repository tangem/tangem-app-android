package com.tangem.domain.tokens

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.domain.tokens.mock.MockTokenLists
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ToggleTokenListSortingUseCaseTest {

    private val useCase = ToggleTokenListSortingUseCase(
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @Test
    fun `when list is empty then error should be received`() = runTest {
        // Given
        val expected = TokenListSortingError.TokenListIsEmpty.left()

        // When
        val actual = useCase(MockTokenLists.emptyTokenList)

        // Then
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when list is grouped and loading then error should be received`() = runTest {
        // Given
        val expected = TokenListSortingError.TokenListIsLoading.left()

        // When
        val actual = useCase(MockTokenLists.loadingGroupedTokenList)

        // Then
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when list is ungrouped and loading then error should be received`() = runTest {
        // Given
        val expected = TokenListSortingError.TokenListIsLoading.left()

        // When
        val actual = useCase(MockTokenLists.loadingUngroupedTokenList)

        // Then
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when list is grouped and unsorted then grouped and sorted list should be received`() = runTest {
        // Given
        val expected = MockTokenLists.sortedGroupedTokenList.right()

        // When
        val actual = useCase(MockTokenLists.unsortedGroupedTokenList)

        // Then
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when list is ungrouped and unsorted then ungrouped and sorted list should be received`() = runTest {
        // Given
        val expected = MockTokenLists.sortedUngroupedTokenList.right()

        // When
        val actual = useCase(MockTokenLists.unsortedUngroupedTokenList)

        // Then
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when list is grouped and sorted then grouped and unsorted list should be received`() = runTest {
        // Given
        val expected = MockTokenLists.sortedGroupedTokenList.right()

        // When
        val actual = useCase(MockTokenLists.unsortedGroupedTokenList)

        // Then
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when list is ungrouped and sorted then ungrouped and unsorted list should be received`() = runTest {
        // Given
        val expected = MockTokenLists.sortedUngroupedTokenList.right()

        // When
        val actual = useCase(MockTokenLists.unsortedUngroupedTokenList)

        // Then
        Truth.assertThat(actual).isEqualTo(expected)
    }
}