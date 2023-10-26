package com.tangem.domain.tokens

import arrow.core.left
import arrow.core.right
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.domain.tokens.mock.MockTokenLists
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class ToggleTokenListSortingUseCaseTest {

    @Test
    fun `when list is empty then error should be received`() = runTest {
        // Given
        val expectedResult = TokenListSortingError.TokenListIsEmpty.left()

        val useCase = getUseCase()

        // When
        val result = useCase(MockTokenLists.emptyTokenList)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when list is grouped and loading then error should be received`() = runTest {
        // Given
        val expectedResult = TokenListSortingError.TokenListIsLoading.left()

        val useCase = getUseCase()

        // When
        val result = useCase(MockTokenLists.loadingGroupedTokenList)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when list is ungrouped and loading then error should be received`() = runTest {
        // Given
        val expectedResult = TokenListSortingError.TokenListIsLoading.left()

        val useCase = getUseCase()

        // When
        val result = useCase(MockTokenLists.loadingUngroupedTokenList)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when list is grouped and unsorted then grouped and sorted list should be received`() = runTest {
        // Given
        val expectedResult = MockTokenLists.sortedGroupedTokenList.right()

        val useCase = getUseCase()

        // When
        val result = useCase(MockTokenLists.unsortedGroupedTokenList)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when list is ungrouped and unsorted then ungrouped and sorted list should be received`() = runTest {
        // Given
        val expectedResult = MockTokenLists.sortedUngroupedTokenList.right()

        val useCase = getUseCase()

        // When
        val result = useCase(MockTokenLists.unsortedUngroupedTokenList)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when list is grouped and sorted then grouped and unsorted list should be received`() = runTest {
        // Given
        val expectedResult = MockTokenLists.sortedGroupedTokenList.right()

        val useCase = getUseCase()

        // When
        val result = useCase(MockTokenLists.unsortedGroupedTokenList)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when list is ungrouped and sorted then ungrouped and unsorted list should be received`() = runTest {
        // Given
        val expectedResult = MockTokenLists.sortedUngroupedTokenList.right()

        val useCase = getUseCase()

        // When
        val result = useCase(MockTokenLists.unsortedUngroupedTokenList)

        // Then
        assertEquals(expectedResult, result)
    }

    private fun getUseCase() = ToggleTokenListSortingUseCase(
        dispatchers = TestingCoroutineDispatcherProvider(),
    )
}