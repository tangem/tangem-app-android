package com.tangem.domain.visa.repository

import com.tangem.domain.visa.model.ActivationOrder
import com.tangem.domain.visa.model.VisaActivationRemoteState

interface VisaActivationRepository {

    suspend fun getActivationRemoteState(): VisaActivationRemoteState

    suspend fun getActivationRemoteStateLongPoll(): VisaActivationRemoteState

    suspend fun getActivationOrderToSign(): ActivationOrder

    interface Factory {
        fun create(cardId: String): VisaActivationRepository
    }
}