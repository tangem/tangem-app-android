package com.tangem.tap.features.tokens.impl.domain

import androidx.paging.PagingData
import com.tangem.tap.features.tokens.impl.domain.models.Token
import kotlinx.coroutines.flow.Flow

/**
 * Default implementation of tokens list interactor
 *
 * @property repository repository of tokens list feature
 */
internal class DefaultTokensListInteractor(private val repository: TokensListRepository) : TokensListInteractor {

    override fun getTokensList(searchText: String): Flow<PagingData<Token>> {
        return repository.getAvailableTokens(searchText = searchText.ifBlank(defaultValue = { null }))
    }
}