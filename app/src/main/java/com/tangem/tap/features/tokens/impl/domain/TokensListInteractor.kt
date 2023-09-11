package com.tangem.tap.features.tokens.impl.domain

import androidx.paging.PagingData
import com.tangem.tap.features.tokens.impl.domain.models.Token
import kotlinx.coroutines.flow.Flow

/**
 * Interactor of tokens list feature
 *
[REDACTED_AUTHOR]
 */
internal interface TokensListInteractor {

    /** Get tokens list using filter by text [searchText] */
    fun getTokensList(searchText: String): Flow<PagingData<Token>>
}