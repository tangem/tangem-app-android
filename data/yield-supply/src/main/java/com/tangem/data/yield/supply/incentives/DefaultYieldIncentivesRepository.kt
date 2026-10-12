package com.tangem.data.yield.supply.incentives

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.yield.supply.incentives.cache.YieldIncentivesCache
import com.tangem.data.yield.supply.incentives.converters.YieldAprHistoryConverter
import com.tangem.data.yield.supply.incentives.converters.YieldClaimCalldataConverter
import com.tangem.data.yield.supply.incentives.converters.YieldClaimHistoryConverter
import com.tangem.data.yield.supply.incentives.converters.YieldIncentivesConverter
import com.tangem.data.yield.supply.converters.YieldMarketTokenConverter
import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository
import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository.Companion.APR_HISTORY_CACHE_SECONDS
import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository.Companion.CLAIM_HISTORY_CACHE_SECONDS
import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository.Companion.INCENTIVES_CACHE_SECONDS
import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository.Companion.INCENTIVE_MARKETS_CACHE_SECONDS
import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository.Companion.MAX_APR_HISTORY_DAYS
import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository.Companion.MIN_APR_HISTORY_DAYS
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.incentives.YieldAprHistory
import com.tangem.domain.yield.supply.models.incentives.YieldClaimCalldata
import com.tangem.domain.yield.supply.models.incentives.YieldClaimHistoryItem
import com.tangem.domain.yield.supply.models.incentives.YieldIncentives
import com.tangem.domain.yield.supply.models.incentives.YieldIncentivesError
import com.tangem.grow.datasource.yield.YieldIncentivesApi
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

internal class DefaultYieldIncentivesRepository(
    private val api: YieldIncentivesApi,
    private val cache: YieldIncentivesCache,
    private val dispatchers: CoroutineDispatcherProvider,
    private val isMerklIncentivesEnabled: Boolean,
) : YieldIncentivesRepository {

    override suspend fun getIncentiveMarkets(
        chainIds: List<Int>,
    ): Either<YieldIncentivesError, List<YieldMarketToken>> {
        if (!isMerklIncentivesEnabled) return YieldIncentivesError.FeatureAbsent.left()

        val chains = chainIds.takeIf(List<Int>::isNotEmpty)?.sorted()?.joinToString(separator = ",")

        return cache.getOrFetch(key = "markets:$chains", window = INCENTIVE_MARKETS_CACHE_SECONDS.seconds) {
            request(
                call = { api.getIncentiveMarkets(chainId = chains) },
                convert = { response -> response.marketDtos.map(YieldMarketTokenConverter::convert) },
            )
        }
    }

    override suspend fun getIncentives(
        chainId: Int,
        userAddress: String,
    ): Either<YieldIncentivesError, YieldIncentives> {
        if (!isMerklIncentivesEnabled) return YieldIncentivesError.FeatureAbsent.left()

        val key = "incentives:$chainId:${userAddress.lowercase()}"

        return cache.getOrFetch(key = key, window = INCENTIVES_CACHE_SECONDS.seconds) {
            request(
                call = { api.getIncentives(chainId = chainId, userAddress = userAddress) },
                convert = YieldIncentivesConverter::convert,
            )
        }
    }

    override suspend fun getAprHistory(
        chainId: Int,
        tokenAddress: String,
        days: Int,
    ): Either<YieldIncentivesError, YieldAprHistory> {
        if (!isMerklIncentivesEnabled) return YieldIncentivesError.FeatureAbsent.left()

        val boundedDays = days.coerceIn(MIN_APR_HISTORY_DAYS, MAX_APR_HISTORY_DAYS)
        val key = "apr-history:$chainId:${tokenAddress.lowercase()}:$boundedDays"

        return cache.getOrFetch(key = key, window = APR_HISTORY_CACHE_SECONDS.seconds) {
            request(
                call = { api.getAprHistory(chainId = chainId, tokenAddress = tokenAddress, days = boundedDays) },
                convert = YieldAprHistoryConverter::convert,
            )
        }
    }

    override suspend fun getClaimCalldata(
        chainId: Int,
        userAddress: String,
    ): Either<YieldIncentivesError, YieldClaimCalldata> {
        if (!isMerklIncentivesEnabled) return YieldIncentivesError.FeatureAbsent.left()

        return request(
            call = { api.getClaimCalldata(chainId = chainId, userAddress = userAddress) },
            convert = YieldClaimCalldataConverter::convert,
        )
    }

    override suspend fun getClaimHistory(
        chainId: Int,
        userAddress: String,
    ): Either<YieldIncentivesError, List<YieldClaimHistoryItem>> {
        if (!isMerklIncentivesEnabled) return YieldIncentivesError.FeatureAbsent.left()

        val key = "claim-history:$chainId:${userAddress.lowercase()}"

        return cache.getOrFetch(key = key, window = CLAIM_HISTORY_CACHE_SECONDS.seconds) {
            request(
                call = { api.getClaimHistory(chainId = chainId, userAddress = userAddress) },
                convert = YieldClaimHistoryConverter::convert,
            )
        }
    }

    private suspend fun <D : Any, T : Any> request(
        call: suspend () -> ApiResponse<D>,
        convert: (D) -> T,
    ): Either<YieldIncentivesError, T> = withContext(dispatchers.io) {
        when (val response = call()) {
            is ApiResponse.Success -> runSuspendCatching { convert(response.data) }
                .fold(
                    onSuccess = { it.right() },
                    onFailure = { error ->
                        TangemLogger.e("Unable to read a yield incentives payload", error)
                        YieldIncentivesError.Unavailable.left()
                    },
                )
            is ApiResponse.Error -> response.cause.toDomainError().left()
        }
    }

    private fun ApiResponseError.toDomainError(): YieldIncentivesError {
        val isUnsupportedChain = this is ApiResponseError.HttpException &&
            code == ApiResponseError.HttpException.Code.BAD_REQUEST

        return if (isUnsupportedChain) {
            YieldIncentivesError.FeatureAbsent
        } else {
            TangemLogger.e("Yield incentives request failed", this)
            YieldIncentivesError.Unavailable
        }
    }
}