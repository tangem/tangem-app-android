package com.tangem.domain.tokens.utils

import arrow.core.NonEmptySet
import arrow.core.raise.Raise
import arrow.core.raise.ensureNotNull
import arrow.core.toNonEmptySetOrNull
import com.tangem.domain.tokens.error.TokensError
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkGroup
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.model.TokenStatus
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.TokensRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal

@Suppress("LongParameterList")
internal class TokenListOperations(
    private val tokensRepository: TokensRepository,
    private val networksRepository: NetworksRepository,
    private val userWalletId: UserWalletId,
    private val totalFiatBalance: TokenList.FiatBalance,
    private val isAnyTokenLoading: Boolean,
    private val dispatchers: CoroutineDispatcherProvider,
    raise: Raise<TokensError>,
) : Raise<TokensError> by raise {

    fun getTokenListFlow(tokens: Set<TokenStatus>): Flow<TokenList> {
        return combine(getIsGrouped(), getIsSortedByBalance()) { isGrouped, isSortedByBalance ->
            createTokenList(tokens, isGrouped, isSortedByBalance)
        }
    }

    private suspend fun createTokenList(
        tokens: Set<TokenStatus>,
        isGrouped: Boolean,
        isSortedByBalance: Boolean,
    ): TokenList = withContext(dispatchers.single) {
        val tokensNes = tokens.toNonEmptySetOrNull()

        when {
            tokensNes == null -> TokenList.NotInitialized
            isGrouped -> {
                val networks = getNetworks(tokensNes)

                createGroupedTokenList(tokensNes, networks, isSortedByBalance)
            }
            else -> createUngroupedTokenList(tokensNes, isSortedByBalance)
        }
    }

    private fun createUngroupedTokenList(
        tokens: NonEmptySet<TokenStatus>,
        isSortedByBalance: Boolean,
    ): TokenList.Ungrouped {
        return TokenList.Ungrouped(
            sortedBy = getSortType(isSortedByBalance),
            totalFiatBalance = totalFiatBalance,
            tokens = if (isSortedByBalance) sortTokensByBalance(tokens) else tokens,
        )
    }

    private fun createGroupedTokenList(
        tokens: NonEmptySet<TokenStatus>,
        networks: NonEmptySet<Network>,
        isSortedByBalance: Boolean,
    ): TokenList.GroupedByNetwork {
        return TokenList.GroupedByNetwork(
            sortedBy = getSortType(isSortedByBalance),
            totalFiatBalance = totalFiatBalance,
            groups = if (isSortedByBalance) groupAndSortTokens(tokens, networks) else groupTokens(tokens, networks),
        )
    }

    private fun groupAndSortTokens(
        tokens: NonEmptySet<TokenStatus>,
        networks: NonEmptySet<Network>,
    ): NonEmptySet<NetworkGroup> {
        val groupsWithSortedTokens = groupTokens(tokens, networks)
            .map { group ->
                group.copy(tokens = sortTokensByBalance(group.tokens as NonEmptySet<TokenStatus>))
            }
            .toNonEmptySet()
        val sortedGroups = if (isAnyTokenLoading) {
            groupsWithSortedTokens
        } else {
            sortGroupsByBalance(groupsWithSortedTokens)
        }

        return sortedGroups
    }

    private fun sortTokensByBalance(tokens: NonEmptySet<TokenStatus>): NonEmptySet<TokenStatus> {
        val sortedTokens = if (isAnyTokenLoading) {
            tokens
        } else {
            tokens
                .sortedByDescending { it.value.fiatAmount ?: BigDecimal.ZERO }
                .toNonEmptySetOrNull()
        }

        return sortedTokens!!
    }

    private fun groupTokens(
        tokens: NonEmptySet<TokenStatus>,
        networks: NonEmptySet<Network>,
    ): NonEmptySet<NetworkGroup> {
        val groups = tokens
            .groupBy { it.networkId }
            .map { (networkId, tokens) ->
                val network = ensureNotNull(networks.firstOrNull { it.id == networkId }) {
                    TokensError.NetworkNotFound(networkId)
                }

                NetworkGroup(
                    networkId = network.id,
                    name = network.name,
                    tokens = tokens.toNonEmptySetOrNull()!!,
                )
            }
            .toNonEmptySetOrNull()

        return groups!!
    }

    private suspend fun getNetworks(tokens: NonEmptySet<TokenStatus>): NonEmptySet<Network> {
        return withContext(dispatchers.io) {
            val networksIds = tokens.map { it.networkId }.toNonEmptySet()
            val networks = networksRepository.getNetworks(networksIds).bind()
            ensureNotNull(networks.toNonEmptySetOrNull()) { TokensError.EmptyNetworks }
        }
    }

    private fun getIsGrouped(): Flow<Boolean> {
        return tokensRepository.isTokensGrouped(userWalletId)
            .map { it.bind() }
            .flowOn(dispatchers.io)
    }

    private fun getIsSortedByBalance(): Flow<Boolean> {
        return tokensRepository.isTokensSortedByBalance(userWalletId)
            .map { it.bind() }
            .flowOn(dispatchers.io)
    }

    private fun sortGroupsByBalance(groupsWithSortedTokens: NonEmptySet<NetworkGroup>): NonEmptySet<NetworkGroup> {
        return groupsWithSortedTokens
            .sortedByDescending { group ->
                group.tokens.sumOf { it.value.fiatAmount ?: BigDecimal.ZERO }
            }
            .toNonEmptySetOrNull()!!
    }

    private fun getSortType(isSortedByBalance: Boolean) =
        if (isSortedByBalance) TokenList.SortType.BALANCE else TokenList.SortType.NONE
}