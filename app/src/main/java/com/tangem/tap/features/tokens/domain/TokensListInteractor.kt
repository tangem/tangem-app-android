package com.tangem.tap.features.tokens.domain

import androidx.paging.PagingData
import com.tangem.tap.features.tokens.domain.models.Token
import kotlinx.coroutines.flow.Flow

/**
 * Interactor of tokens list feature
 *
 * @author Andrew Khokhlov on 12/04/2023
 */
internal interface TokensListInteractor {

    /** Get tokens list using filter by text [searchText] */
    fun getTokensList(searchText: String): Flow<PagingData<Token>>
}
