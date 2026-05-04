package com.tangem.domain.search.usecase

import com.tangem.domain.search.repository.SearchRepository

/**
 * Clears the entire search history, removing both text hints and recently viewed tokens.
 *
 * @property searchRepository local search history storage
 */
class ClearSearchHistoryUseCase(
    private val searchRepository: SearchRepository,
) {

    suspend operator fun invoke() {
        searchRepository.clearHistory()
    }
}