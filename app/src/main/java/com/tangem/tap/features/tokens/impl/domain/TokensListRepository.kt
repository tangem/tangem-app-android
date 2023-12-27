package com.tangem.tap.features.tokens.impl.domain

import androidx.paging.PagingData
import com.tangem.tap.features.tokens.impl.domain.models.Token
import kotlinx.coroutines.flow.Flow

/**
 * Repository of tokens list feature
 *
[REDACTED_AUTHOR]
 */
internal interface TokensListRepository {

    /**
     * Get available tokens list
     *
     * @param searchText search text
     *
     * @throws com.tangem.datasource.api.common.response.ApiResponseError
     */
    fun getAvailableTokens(searchText: String?): Flow<PagingData<Token>>
}