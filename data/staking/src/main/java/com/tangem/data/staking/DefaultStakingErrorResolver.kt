package com.tangem.data.staking

import com.tangem.data.staking.converters.error.StakeKitErrorConverter
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakingErrorResolver

internal class DefaultStakingErrorResolver(
    private val stakeKitErrorConverter: StakeKitErrorConverter,
) : StakingErrorResolver {

    override fun resolve(throwable: Throwable): StakingError {
        return if (throwable is ApiResponseError.HttpException) {
            stakeKitErrorConverter.convert(throwable.errorBody.orEmpty())
        } else {
            StakingError.UnknownError
        }
    }
}
