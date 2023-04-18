package com.tangem.tap.domain.tokens

import com.squareup.moshi.JsonAdapter
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.services.Result
import com.tangem.datasource.api.common.MoshiConverter
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.datasource.asset.AssetReader
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

class LoadAvailableCoinsService(
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val assetReader: AssetReader,
) {
    private val currenciesAdapter: JsonAdapter<CurrenciesFromJson> =
        MoshiConverter.networkMoshi.adapter(CurrenciesFromJson::class.java)

    suspend fun getSupportedTokens(
        isTestNet: Boolean,
        supportedBlockchains: List<Blockchain>,
        page: Int,
        searchInput: String?,
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
        return when (val result = loadCoins(supportedBlockchains, offset, searchInput)) {
            is Result.Success -> {
                val data = result.data

                Result.Success(
                    LoadedCoins(
                        currencies = data.coins.map {
                            Currency.fromCoinResponse(currency = it, imageHost = data.imageHost)
                        },
                        moreAvailable = data.total > offset + LOAD_PER_PAGE,
                    ),
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
        searchInput: String?,
    ): Result<CoinsResponse> {
        return withContext(dispatchers.io) {
            runCatching {
                tangemTechApi.getCoins(
                    networkIds = supportedBlockchains.joinToString(
                        separator = ",",
                        transform = Blockchain::toNetworkId,
                    ),
                    active = true,
                    searchText = searchInput,
                    offset = offset,
                    limit = LOAD_PER_PAGE,
                )
            }.fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Failure(it) },
            )
        }
    }

    private fun getTestnetCoins(): List<Currency> {
        val json = assetReader.readJson(FILE_NAME_TESTNET_COINS)
        return currenciesAdapter.fromJson(json)!!.coins
            .map { Currency.fromJsonObject(it) }
    }

    private fun List<Currency>.filter(searchInput: String?): List<Currency> {
        if (searchInput.isNullOrBlank()) return this

        return filter { currency ->
            currency.symbol.contains(searchInput, ignoreCase = true) ||
                currency.name.contains(searchInput, ignoreCase = true)
        }
    }

    private companion object {
        const val LOAD_PER_PAGE = 100
        const val FILE_NAME_TESTNET_COINS = "testnet_tokens"
    }
}

data class LoadedCoins(
    val currencies: List<Currency>,
    val moreAvailable: Boolean,
)
