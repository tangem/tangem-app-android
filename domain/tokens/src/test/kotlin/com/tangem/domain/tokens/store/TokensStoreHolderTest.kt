package com.tangem.domain.tokens.store

import arrow.core.nonEmptySetOf
import arrow.core.toNonEmptySetOrNull
import com.tangem.domain.models.userwallet.UserWalletId
import com.tangem.domain.tokens.EmptyTokens
import com.tangem.domain.tokens.GroupingTypeNotFetched
import com.tangem.domain.tokens.SortingTypeNotFetched
import com.tangem.domain.tokens.TokensNotFetched
import com.tangem.domain.tokens.mock.MockNetworks
import com.tangem.domain.tokens.mock.MockQuotes
import com.tangem.domain.tokens.mock.MockTokens
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.utils.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class TokensStoreHolderTest {

    private val storeHolder = ScopedTokensStoreHolder()
    private val userWalletId = UserWalletId.mock()

    private val token = MockTokens.token1
    private val quote = MockQuotes.quote1
    private val networkStatus = MockNetworks.networkStatus1

    @Test
    fun `when adding new store to holder then it must be in it`() = runTest {
        // Given
        assertEquals(0, storeHolder.size)

        // When
        storeHolder[userWalletId] = TokensStore()

        // Then
        assertEquals(1, storeHolder.size)
        assertTrue(storeHolder.containsKey(userWalletId))
    }

    @Test
    fun `when getting store from holder then it must be received`() = runTest {
        // Given
        val expectedResult = TokensStore()

        storeHolder[userWalletId] = expectedResult

        // When
        val receivedStore = storeHolder[userWalletId]

        // Then
        assertEquals(expectedResult, receivedStore)
    }

    @Test
    fun `when getting data on not initialized store then correct result should be received`() = runTest {
        // Given
        val tokensStore = storeHolder.getOrPut(userWalletId) { TokensStore() }

        // When
        val tokens = tokensStore.getTokens()
        val isGrouped = tokensStore.getIsGrouped()
        val isSortedByBalance = tokensStore.getIsSortedByBalance()
        val quotes = tokensStore.getQuotes()
        val networkStatuses = tokensStore.getNetworkStatuses()

        // Then
        assert(tokens.isLeft { it is TokensNotFetched })
        assert(isGrouped.isLeft { it is GroupingTypeNotFetched })
        assert(isSortedByBalance.isLeft { it is SortingTypeNotFetched })
        assert(quotes.isRight { it.isEmpty() })
        assert(networkStatuses.isRight { it.isEmpty() })
    }

    @Test
    fun `when getting data from initialized store then correct data should be received`() = runTest {
        // Given
        val expectedTokens = nonEmptySetOf(token)
        val expectedQuotes = nonEmptySetOf(quote)
        val expectedNetworkStatuses = setOf(networkStatus)
        val expectedIsGrouped = true
        val expectedIsSortedByBalance = true

        val tokensStore = storeHolder.getOrPut(userWalletId) { TokensStore() }
        tokensStore.setTokens(expectedTokens)
        tokensStore.setQuotes(expectedQuotes)
        tokensStore.addOrUpdateNetworkStatus(networkStatus)
        tokensStore.setIsTokensGrouped(expectedIsGrouped)
        tokensStore.setIsTokensSortedByBalance(expectedIsSortedByBalance)

        // When
        val tokens = tokensStore.getTokens()
        val quotes = tokensStore.getQuotes()
        val networkStatuses = tokensStore.getNetworkStatuses()
        val isGrouped = tokensStore.getIsGrouped()
        val isSortedByBalance = tokensStore.getIsSortedByBalance()

        // Then
        assertEquals(expectedTokens, tokens.getOrNull())
        assertEquals(expectedQuotes.toSet(), quotes.getOrNull())
        assertEquals(expectedNetworkStatuses, networkStatuses.getOrNull())
        assertEquals(expectedIsGrouped, isGrouped.getOrNull())
        assertEquals(expectedIsSortedByBalance, isSortedByBalance.getOrNull())
    }

    @Test
    fun `when setting data then correct data should be received`() = runTest {
        // Given
        val expectedTokens = nonEmptySetOf(token, MockTokens.token2)
        val expectedQuotes = setOf(quote, MockQuotes.quote2)
        val newNetworkStatus = MockNetworks.networkStatus2
        val expectedNetworkStatuses = setOf(networkStatus, newNetworkStatus)
        val expectedIsGrouped = true
        val expectedIsSortedByBalance = true

        val tokensStore = storeHolder.getOrPut(userWalletId) { TokensStore() }

        tokensStore.setTokens(nonEmptySetOf(token))
        tokensStore.setQuotes(nonEmptySetOf(quote))
        tokensStore.addOrUpdateNetworkStatus(networkStatus)
        tokensStore.setIsTokensGrouped(false)
        tokensStore.setIsTokensSortedByBalance(false)

        // When
        tokensStore.setTokens(expectedTokens.toNonEmptySetOrNull()!!)
        tokensStore.setQuotes(expectedQuotes.toNonEmptySetOrNull()!!)
        tokensStore.addOrUpdateNetworkStatus(newNetworkStatus)
        tokensStore.setIsTokensGrouped(expectedIsGrouped)
        tokensStore.setIsTokensSortedByBalance(expectedIsSortedByBalance)

        // Then
        assertEquals(expectedTokens, tokensStore.getTokens().getOrNull())
        assertEquals(expectedQuotes, tokensStore.getQuotes().getOrNull())
        assertEquals(expectedNetworkStatuses, tokensStore.getNetworkStatuses().getOrNull())
        assertEquals(expectedIsGrouped, tokensStore.getIsGrouped().getOrNull())
        assertEquals(expectedIsSortedByBalance, tokensStore.getIsSortedByBalance().getOrNull())
    }

    @Test
    fun `when setting error then error should be received`() = runTest {
        // Given
        val tokensStore = storeHolder.getOrPut(userWalletId) { TokensStore() }
        tokensStore.setTokens(nonEmptySetOf(token))

        // When
        tokensStore.setError(EmptyTokens)

        // Then
        assertTrue(tokensStore.getTokens().isLeft { it is EmptyTokens })
    }

    @Test
    fun `when setting data after error then data should be received`() = runTest {
        // Given
        val expectedTokens = nonEmptySetOf(token)

        val tokensStore = storeHolder.getOrPut(userWalletId) { TokensStore() }
        tokensStore.setError(EmptyTokens)

        // When
        tokensStore.setTokens(expectedTokens)

        // Then
        assertEquals(expectedTokens, tokensStore.getTokens().getOrNull())
    }

    @Test
    fun `when adding network status then correct data should be received`() = runTest {
        // Given
        val tokensStore = storeHolder.getOrPut(userWalletId) { TokensStore() }

        val initialNetworkStatus = networkStatus
        val newNetworkStatus1 = initialNetworkStatus.copy(
            value = NetworkStatus.Unreachable,
        )
        val newNetworkStatus2 = MockNetworks.networkStatus2

        tokensStore.addOrUpdateNetworkStatus(initialNetworkStatus)

        // When
        tokensStore.addOrUpdateNetworkStatus(newNetworkStatus1)
        tokensStore.addOrUpdateNetworkStatus(newNetworkStatus2)

        // Then
        val expectedNetworkStatuses = setOf(
            initialNetworkStatus,
            newNetworkStatus1,
            newNetworkStatus2,
        )
        assertEquals(expectedNetworkStatuses, tokensStore.getNetworkStatuses().getOrNull())
    }
}
