package com.tangem.tap.features.tokens.impl.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.tangem.datasource.local.testnet.TestnetTokensStorage
import com.tangem.datasource.local.testnet.models.TestnetTokensConfig
import com.tangem.tap.features.tokens.impl.data.converters.TestnetTokensConfigConverter
import com.tangem.tap.features.tokens.impl.domain.models.Token

/**
 * Paging source that get tokens by testnet tokens config
 *
 * @property testnetTokensStorage testnet tokens config storage
 * @property searchText           search text
 */
internal class TestnetTokensPagingSource(
    private val testnetTokensStorage: TestnetTokensStorage,
    private val searchText: String?,
) : PagingSource<Int, Token>() {

    override fun getRefreshKey(state: PagingState<Int, Token>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Token> {
        return LoadResult.Page(
            data = TestnetTokensConfigConverter.convert(
                value = testnetTokensStorage.getConfig().searchByText(searchText),
            ),
            prevKey = null,
            nextKey = null,
        )
    }

    private fun TestnetTokensConfig.searchByText(searchText: String?): TestnetTokensConfig {
        if (searchText.isNullOrBlank()) return this

        return copy(
            tokens = tokens.filter { token ->
                token.symbol.contains(other = searchText, ignoreCase = true) ||
                    token.name.contains(other = searchText, ignoreCase = true)
            },
        )
    }
}
