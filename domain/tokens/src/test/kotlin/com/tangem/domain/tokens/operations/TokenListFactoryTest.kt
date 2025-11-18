package com.tangem.domain.tokens.operations

import com.google.common.truth.Truth
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.tokens.mock.MockTokens
import com.tangem.domain.tokens.mock.MockTokensStates
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TokenListFactoryTest {

    @Test
    fun `statuses is empty, return TokenList Empty`() {
        // Act
        val actual = TokenListFactory.create(
            statuses = emptyList(),
            groupType = TokensGroupType.NONE, // doesn't matter
            sortType = TokensSortType.BALANCE, // doesn't matter
        )

        // Assert
        val expected = TokenList.Empty
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `create UNGROUPED token list WITHOUT sorting`() {
        // Act
        val actual = TokenListFactory.create(
            statuses = MockTokensStates.loadedTokensStates,
            groupType = TokensGroupType.NONE,
            sortType = TokensSortType.NONE,
        )

        // Assert
        val expected = TokenList.Ungrouped(
            totalFiatBalance = TotalFiatBalance.Loaded(amount = BigDecimal("599.50"), source = StatusSource.ACTUAL),
            sortedBy = TokensSortType.NONE,
            currencies = MockTokensStates.loadedTokensStates,
        )
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `create UNGROUPED token list WITH SORTING BY BALANCE`() {
        // Act
        val actual = TokenListFactory.create(
            statuses = MockTokensStates.loadedTokensStates,
            groupType = TokensGroupType.NONE,
            sortType = TokensSortType.BALANCE,
        )

        // Assert
        val expected = TokenList.Ungrouped(
            totalFiatBalance = TotalFiatBalance.Loaded(amount = BigDecimal("599.50"), source = StatusSource.ACTUAL),
            sortedBy = TokensSortType.BALANCE,
            currencies = MockTokensStates.loadedTokensStates.reversed(),
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `create GROUPED token list WITHOUT sorting`() {
        // Act
        val actual = TokenListFactory.create(
            statuses = MockTokensStates.loadedTokensStates,
            groupType = TokensGroupType.NETWORK,
            sortType = TokensSortType.NONE,
        )

        // Assert
        val expected = TokenList.GroupedByNetwork(
            totalFiatBalance = TotalFiatBalance.Loaded(amount = BigDecimal("599.50"), source = StatusSource.ACTUAL),
            sortedBy = TokensSortType.NONE,
            groups = listOf(
                TokenList.GroupedByNetwork.NetworkGroup(
                    network = MockTokens.token3.network,
                    currencies = listOf(
                        MockTokensStates.loadedTokensStates[0],
                        MockTokensStates.loadedTokensStates[1],
                        MockTokensStates.loadedTokensStates[2],
                    ),
                ),
                TokenList.GroupedByNetwork.NetworkGroup(
                    network = MockTokens.token6.network,
                    currencies = listOf(
                        MockTokensStates.loadedTokensStates[3],
                        MockTokensStates.loadedTokensStates[4],
                        MockTokensStates.loadedTokensStates[5],
                    ),
                ),
                TokenList.GroupedByNetwork.NetworkGroup(
                    network = MockTokens.token10.network,
                    currencies = listOf(
                        MockTokensStates.loadedTokensStates[6],
                        MockTokensStates.loadedTokensStates[7],
                        MockTokensStates.loadedTokensStates[8],
                        MockTokensStates.loadedTokensStates[9],
                    ),
                ),
            ),
        )
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `create GROUPED token list WITH SORTING BY BALANCE`() {
        // Act
        val actual = TokenListFactory.create(
            statuses = MockTokensStates.loadedTokensStates,
            groupType = TokensGroupType.NETWORK,
            sortType = TokensSortType.BALANCE,
        )

        // Assert
        val expected = TokenList.GroupedByNetwork(
            totalFiatBalance = TotalFiatBalance.Loaded(amount = BigDecimal("599.50"), source = StatusSource.ACTUAL),
            sortedBy = TokensSortType.BALANCE,
            groups = listOf(
                TokenList.GroupedByNetwork.NetworkGroup(
                    network = MockTokens.token10.network,
                    currencies = listOf(
                        MockTokensStates.loadedTokensStates[9],
                        MockTokensStates.loadedTokensStates[8],
                        MockTokensStates.loadedTokensStates[7],
                        MockTokensStates.loadedTokensStates[6],
                    ),
                ),
                TokenList.GroupedByNetwork.NetworkGroup(
                    network = MockTokens.token6.network,
                    currencies = listOf(
                        MockTokensStates.loadedTokensStates[5],
                        MockTokensStates.loadedTokensStates[4],
                        MockTokensStates.loadedTokensStates[3],
                    ),
                ),
                TokenList.GroupedByNetwork.NetworkGroup(
                    network = MockTokens.token3.network,
                    currencies = listOf(
                        MockTokensStates.loadedTokensStates[2],
                        MockTokensStates.loadedTokensStates[1],
                        MockTokensStates.loadedTokensStates[0],
                    ),
                ),
            ),
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `sorting will not be applied if any status is Loading`() {
        // Arrange
        val statuses = listOf(
            CryptoCurrencyStatus(currency = MockTokens.token1, value = CryptoCurrencyStatus.Loading),
            MockTokensStates.tokenState10,
        )

        // Act
        val actual = TokenListFactory.create(
            statuses = statuses,
            groupType = TokensGroupType.NONE,
            sortType = TokensSortType.BALANCE,
        )

        // Assert
        val expected = TokenList.Ungrouped(
            totalFiatBalance = TotalFiatBalance.Loading,
            sortedBy = TokensSortType.BALANCE,
            currencies = statuses,
        )
        Truth.assertThat(actual).isEqualTo(expected)
    }
}