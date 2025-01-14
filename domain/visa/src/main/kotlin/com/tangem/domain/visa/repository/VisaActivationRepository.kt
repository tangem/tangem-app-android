package com.tangem.domain.visa.repository

import com.tangem.domain.visa.model.VisaActivationRemoteState

interface VisaActivationRepository {

    suspend fun getActivationRemoteState(): VisaActivationRemoteState
}