package com.tangem.data.staking

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.staking.converters.error.StakeKitErrorConverter
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakingErrorResolver

internal class DefaultStakingErrorResolver(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val stakeKitErrorConverter: StakeKitErrorConverter,
) : StakingErrorResolver {

    override fun resolve(throwable: Throwable): StakingError {
        val error = if (throwable is ApiResponseError.HttpException) {
            stakeKitErrorConverter.convert(throwable.errorBody.orEmpty())
        } else {
            StakingError.DomainError(throwable.message)
        }

        when (error) {
            is StakingError.StakeKitApiError -> {
                analyticsEventHandler.send(StakingAnalyticsEvent.StakeKitApiError(error))
            }
            is StakingError.StakeKitUnknownError -> {
                analyticsEventHandler.send(StakingAnalyticsEvent.StakeKitApiUnknownError(error))
            }
            is StakingError.DomainError -> {
                analyticsEventHandler.send(StakingAnalyticsEvent.DomainError(error))
            }
        }

        return error
    }
}