package com.tangem.data.visa.converter

import com.tangem.datasource.api.visa.models.response.CardActivationRemoteStateResponse
import com.tangem.domain.visa.model.VisaActivationRemoteState
import com.tangem.utils.converter.Converter
import javax.inject.Inject

class VisaActivationStatusConverter @Inject constructor() :
    Converter<CardActivationRemoteStateResponse, VisaActivationRemoteState> {

    override fun convert(value: CardActivationRemoteStateResponse): VisaActivationRemoteState {
        // TODO Will be implemented in the future
        return VisaActivationRemoteState.Activated
    }
}