package com.tangem.domain.search.usecase

import com.tangem.domain.search.repository.SearchRepository

/**
 * Saves the current search query text to the local search history.
 * Blank queries are ignored. The history is limited to 3 entries; oldest entries are evicted automatically.
 * Should be called when the user selects any asset from the search results.
 *
 * @property searchRepository local search history storage
 */
class SaveSearchQueryUseCase(
    private val searchRepository: SearchRepository,
) {

    /**
     * @param query the search text to persist; blank values are silently ignored
     */
    suspend operator fun invoke(query: String) {
        if (query.isBlank()) return
        searchRepository.saveTextHint(query.trim())
    }
}