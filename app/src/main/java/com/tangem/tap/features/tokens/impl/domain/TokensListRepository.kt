package com.tangem.tap.features.tokens.impl.domain

import androidx.paging.PagingData
import com.tangem.tap.features.tokens.impl.domain.models.Token
import kotlinx.coroutines.flow.Flow

/**
 * Repository of tokens list feature
 *
 * @author Andrew Khokhlov on 07/04/2023
 */
internal interface TokensListRepository {

    /**
     * Get available tokens list
     *
     * @param searchText search text
     */
    fun getAvailableTokens(searchText: String?): Flow<PagingData<Token>>
}
