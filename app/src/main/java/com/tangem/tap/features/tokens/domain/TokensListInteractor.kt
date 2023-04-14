package com.tangem.tap.features.tokens.domain

import androidx.paging.PagingData
import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.features.tokens.domain.models.Token
import com.tangem.tap.features.tokens.redux.TokenWithBlockchain
import kotlinx.coroutines.flow.Flow

/**
 * Interactor of tokens list feature
 *
[REDACTED_AUTHOR]
 */
internal interface TokensListInteractor {

    /** Get tokens list using filter by text [searchText] */
    fun getTokensList(searchText: String): Flow<PagingData<Token>>

    /**
     * Save added tokens
     *
     * @param tokens      tokens list that need to save
     * @param blockchains blockchains list that need to save
     */
    suspend fun saveChanges(tokens: List<TokenWithBlockchain>, blockchains: List<Blockchain>)
}