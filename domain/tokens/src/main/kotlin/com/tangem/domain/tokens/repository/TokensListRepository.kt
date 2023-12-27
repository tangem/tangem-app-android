package com.tangem.domain.tokens.repository

import androidx.paging.PagingData
import com.tangem.domain.tokens.model.Token
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing operations related to a complete set of tokens
 * (not associated with any specific card).
 * */
interface TokensListRepository {

    /**
     * Retrieves a list of available tokens with quotes, can be filtered based on the provided search text.
     *
     * @param searchText The search text used to filter tokens.
     * @return A [Flow] emitting [PagingData] containing the tokens with quotes matching the search criteria.
     * @throws com.tangem.datasource.api.common.response.ApiResponseError
     */
    fun getTokens(searchText: String?): Flow<PagingData<Token>>
}