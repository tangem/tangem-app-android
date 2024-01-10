package com.tangem.domain.tokens.repository

import androidx.paging.PagingData
import com.tangem.domain.tokens.model.FoundToken
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

    /**
     * Retrieves a token information with the specified contract address on the provided network.
     *
     * @param contractAddress   contract address of the token
     * @param networkId         network of the token
     * @return [FoundToken]     object containing token information or null if no token is found with the provided
     * contract address
     */
    @Throws
    suspend fun findToken(contractAddress: String, networkId: String): FoundToken?

    /**
     * Validates a contract address on a particular network.
     *
     * @param contractAddress   contract address of the token
     * @param networkId         network of the token
     * @return [Boolean]        true is address is valid (possible on the network), false if its format is not
     * supported on the network
     */
    fun validateAddress(contractAddress: String, networkId: String): Boolean
}