package com.tangem.domain.tokens

import androidx.paging.PagingData
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.tokens.repository.TokensListRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for retrieving a complete set of tokens with their quotes (filtered by a search text if provided).
 *
 * @property repository The repository responsible for fetching and preparing the list of tokens with their quotes.
 */
class GetGlobalTokenListUseCase(private val repository: TokensListRepository) {

    /**
     * @param searchText The text used for filtering the list of tokens (not required).
     * @return A [Flow] emitting [PagingData] containing the tokens with their quotes.
     */
    operator fun invoke(searchText: String?): Flow<PagingData<Token>> {
        return repository.getTokens(searchText = searchText?.ifBlank(defaultValue = { null }))
    }
}
