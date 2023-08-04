package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.core.error.DataError
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.domain.tokens.mock.MockNetworks
import com.tangem.domain.tokens.mock.MockTokenLists
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.tokens.repository.MockNetworksRepository
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class ToggleTokenListGroupingTest {

    @Test
    fun `when grouped list is empty then error should be received`() = runTest {
        // Given
        val expectedResult = TokenListSortingError.TokenListIsEmpty.left()

        val useCase = getUseCase()

        // When
        val result = useCase(MockTokenLists.emptyGroupedTokenList)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when ungrouped list is empty then error should be received`() = runTest {
        // Given
        val expectedResult = TokenListSortingError.TokenListIsEmpty.left()

        val useCase = getUseCase()

        // When
        val result = useCase(MockTokenLists.emptyUngroupedTokenList)

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
    fun `when list is not initialized then error should be received`() = runTest {
        // Given
        val expectedResult = TokenListSortingError.TokenListIsLoading.left()

        val useCase = getUseCase()

        // When
        val result = useCase(MockTokenLists.notInitializedTokenList)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when list is ungrouped and sorted then sorted grouped list should be received`() = runTest {
        // Given
        val expectedResult = MockTokenLists.sortedGroupedTokenList.right()

        val useCase = getUseCase()

        // When
        val result = useCase(MockTokenLists.sortedUngroupedTokenList)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when list is ungrouped and unsorted then unsorted grouped list should be received`() = runTest {
        // Given
        val expectedResult = MockTokenLists.unsortedGroupedTokenList.right()

        val useCase = getUseCase()

        // When
        val result = useCase(MockTokenLists.unsortedUngroupedTokenList)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when list is grouped and sorted then sorted ungrouped list should be received`() = runTest {
        // Given
        val expectedResult = MockTokenLists.sortedUngroupedTokenList.right()

        val useCase = getUseCase()

        // When
        val result = useCase(MockTokenLists.sortedGroupedTokenList)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when list is grouped and unsorted then unsorted ungrouped list should be received`() = runTest {
        // Given
        val expectedResult = MockTokenLists.unsortedUngroupedTokenList.right()

        val useCase = getUseCase()

        // When
        val result = useCase(MockTokenLists.unsortedGroupedTokenList)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when list is ungrouped but networks is empty then error should be received`() = runTest {
        // Given
        val expectedResult = TokenListSortingError.UnableToSortTokenList.left()

        val useCase = getUseCase(networks = emptySet<Network>().right())

        // When
        val result = useCase(MockTokenLists.unsortedUngroupedTokenList)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when list is ungrouped but networks getting failed then error should be received`() = runTest {
        // Given
        val error = DataError.NetworkError.NoInternetConnection
        val expectedResult = TokenListSortingError.DataError(error).left()

        val useCase = getUseCase(networks = error.left())

        // When
        val result = useCase(MockTokenLists.unsortedUngroupedTokenList)

        // Then
        assertEquals(expectedResult, result)
    }

    private fun getUseCase(networks: Either<DataError, Set<Network>> = MockNetworks.networks.right()) =
        ToggleTokenListGroupingUseCase(
            networksRepository = MockNetworksRepository(networks, statuses = flowOf()),
            dispatchers = TestingCoroutineDispatcherProvider(),
        )
}
