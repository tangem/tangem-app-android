package com.tangem.data.staking.converters.error

import com.squareup.moshi.JsonAdapter
import com.tangem.datasource.api.stakekit.models.response.model.error.StakeKitErrorResponse
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.utils.converter.Converter

internal class StakeKitErrorConverter(
    private val jsonAdapter: JsonAdapter<StakeKitErrorResponse>,
) : Converter<String, StakingError> {

    override fun convert(value: String): StakingError {
        return try {
            val stakeKitErrorResponse = jsonAdapter.fromJson(value)
                ?: return StakingError.StakeKitUnknownError(value)

            return StakingError.StakeKitApiError(
                message = stakeKitErrorResponse.message,
                code = stakeKitErrorResponse.code,
                methodName = stakeKitErrorResponse.path,
                details = stakeKitErrorResponse.details?.let(StakeKitErrorDetailsConverter::convert),
            )
        } catch (e: Exception) {
            StakingError.StakeKitUnknownError(value)
        }
    }
}