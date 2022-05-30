package com.tangem.tap.domain.tokens

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.services.Result
import com.tangem.domain.common.extensions.getListOfCoins
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.network.api.tangemTech.CoinsResponse
import com.tangem.network.api.tangemTech.TangemTechService

class LoadAvailableCoinsService(
    private val networkService: TangemTechService,
    private val currenciesRepository: CurrenciesRepository
) {

    suspend fun getSupportedTokens(
        isTestNet: Boolean = false,
        supportedBlockchains: List<Blockchain>,
        page: Int,
        searchInput: String? = null
    ): Result<LoadedCoins> {
        if (isTestNet) {
            return Result.Success(
                LoadedCoins(
                    currencies = currenciesRepository.getTestnetCoins(),
                    moreAvailable = false,
                )
            )
        }
        val offset = page * LOAD_PER_PAGE
        val result = loadCoins(supportedBlockchains, offset, searchInput)

        return when (result) {
            is Result.Success -> {
                val data = result.data
                Result.Success(
                    LoadedCoins(
                        currencies = data.coins.map { Currency.fromCoinResponse(it, data.imageHost) },
                        moreAvailable = data.total > offset + LOAD_PER_PAGE,
                    )
                )
            }
            is Result.Failure -> {
                Result.Failure(result.error)
            }
        }
    }

    private suspend fun loadCoins(
        supportedBlockchains: List<Blockchain>,
        offset: Int,
        searchInput: String? = null
    ): Result<CoinsResponse> {
        val networkIds = supportedBlockchains.toSet().map { it.toNetworkId() }
        return networkService.getListOfCoins(
            networkIds = networkIds,
            offset = offset,
            limit = LOAD_PER_PAGE,
            searchText = searchInput
        )
    }

    companion object {
        const val LOAD_PER_PAGE = 100
    }
}


data class LoadedCoins(
    val currencies: List<Currency>,
    val moreAvailable: Boolean,
)