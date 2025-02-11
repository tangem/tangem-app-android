package com.tangem.domain.visa.repository

import com.tangem.domain.visa.model.*

interface VisaActivationRepository {

    suspend fun getActivationRemoteState(): VisaActivationRemoteState

    suspend fun getActivationRemoteStateLongPoll(): VisaActivationRemoteState

    suspend fun getCardWalletAcceptanceData(request: VisaCardWalletDataToSignRequest): VisaDataToSignByCardWallet

    suspend fun getCustomerWalletAcceptanceData(
        request: VisaCustomerWalletDataToSignRequest,
    ): VisaDataToSignByCustomerWallet

    suspend fun activateCard(signedData: VisaSignedActivationDataByCardWallet)

    suspend fun approveByCustomerWallet(signedData: VisaSignedDataByCustomerWallet)

    suspend fun sendPinCode(pinCode: VisaEncryptedPinCode)

    interface Factory {
        fun create(cardId: VisaCardId): VisaActivationRepository
    }
}