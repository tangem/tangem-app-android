package com.tangem.features.onramp.main.entity.transformer

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampAvailability
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.features.onramp.main.entity.OnrampMainBottomSheetConfig
import com.tangem.features.onramp.main.entity.OnrampMainComponentUM
import com.tangem.utils.transformer.Transformer
import timber.log.Timber

internal class OnrampAvailabilityTransformer(
    private val maybeAvailabilityStatus: Either<Throwable, OnrampAvailability>,
    private val openBottomSheet: (OnrampMainBottomSheetConfig) -> Unit,
) : Transformer<OnrampMainComponentUM> {

    override fun transform(prevState: OnrampMainComponentUM): OnrampMainComponentUM {
        return maybeAvailabilityStatus.fold(
            ifLeft = {
                Timber.e(it)
                prevState
            },
            ifRight = { availability ->
                when (availability) {
                    OnrampAvailability.Available -> handleAvailableStatus(prevState)
                    is OnrampAvailability.ConfirmResidency -> handleShowingConfirmResidency(
                        prevState = prevState,
                        country = availability.country,
                    )
                    is OnrampAvailability.NotSupported -> handleShowingConfirmResidency(
                        prevState = prevState,
                        country = availability.country,
                    )
                }
            },
        )
    }

    private fun handleAvailableStatus(prevState: OnrampMainComponentUM): OnrampMainComponentUM {
        val endButton = prevState.topBarConfig.endButtonUM.copy(enabled = true)
        return OnrampMainComponentUM.Content(
            topBarConfig = prevState.topBarConfig.copy(endButtonUM = endButton),
            buyButtonConfig = prevState.buyButtonConfig,
        )
    }

    private fun handleShowingConfirmResidency(
        prevState: OnrampMainComponentUM,
        country: OnrampCountry,
    ): OnrampMainComponentUM {
        openBottomSheet.invoke(OnrampMainBottomSheetConfig.ConfirmResidency(country))
        return prevState
    }
}
