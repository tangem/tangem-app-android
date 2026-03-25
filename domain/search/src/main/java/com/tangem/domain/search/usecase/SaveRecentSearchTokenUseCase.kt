package com.tangem.domain.search.usecase

import com.tangem.domain.search.model.RecentSearchToken
import com.tangem.domain.search.repository.SearchRepository

/**
 * Saves a market token to the "recently viewed" search history.
 * Only market assets should be saved (user's own assets are ignored).
 * The history is limited to 3 entries; oldest entries are evicted automatically.
 *
 * @property searchRepository local search history storage
 */
class SaveRecentSearchTokenUseCase(
    private val searchRepository: SearchRepository,
) {

    /**
     * @param token the market token to persist as a recent search entry
     */
    suspend operator fun invoke(token: RecentSearchToken) {
        searchRepository.saveRecentToken(token)
    }
}