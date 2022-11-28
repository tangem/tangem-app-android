package com.tangem.tap.domain.tokens

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.services.Result
import com.tangem.domain.common.extensions.getListOfCoins
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.datasource.api.tangemTech.CoinsResponse
import com.tangem.datasource.api.tangemTech.TangemTechService
import com.tangem.datasource.api.common.MoshiConverter
import com.tangem.tap.common.AssetReader

class LoadAvailableCoinsService(
    private val networkService: TangemTechService,
    private val assetReader: AssetReader,
) {
    private val moshi: Moshi by lazy { MoshiConverter.defaultMoshi() }
    private val currenciesAdapter: JsonAdapter<CurrenciesFromJson> =
        moshi.adapter(CurrenciesFromJson::class.java)

    suspend fun getSupportedTokens(
        isTestNet: Boolean = false,
        supportedBlockchains: List<Blockchain>,
        page: Int,
        searchInput: String? = null,
    ): Result<LoadedCoins> {
        if (isTestNet) {
            return Result.Success(
                LoadedCoins(
                    currencies = getTestnetCoins().filter(searchInput),
                    moreAvailable = false,
                ),
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
            active = true,
            offset = offset,
            limit = LOAD_PER_PAGE,
            searchText = searchInput,
        )
    }

    fun getTestnetCoins(): List<Currency> {
        val json = assetReader.readAssetAsString(FILE_NAME_TESTNET_COINS)
        return currenciesAdapter.fromJson(json)!!.coins
            .map { Currency.fromJsonObject(it) }
    }

    private fun List<Currency>.filter(searchInput: String?): List<Currency> {
        if (searchInput.isNullOrBlank()) return this

        return filter {
            it.symbol.contains(searchInput, ignoreCase = true) ||
                it.name.contains(searchInput, ignoreCase = true)
        }
    }

    companion object {
        const val LOAD_PER_PAGE = 100
        private const val FILE_NAME_TESTNET_COINS = "testnet_tokens"
    }
}


data class LoadedCoins(
    val currencies: List<Currency>,
    val moreAvailable: Boolean,
)
