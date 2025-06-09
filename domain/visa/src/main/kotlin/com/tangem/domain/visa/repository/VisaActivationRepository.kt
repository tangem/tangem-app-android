package com.tangem.domain.visa.repository

import arrow.core.Either
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.*

interface VisaActivationRepository {

    suspend fun getActivationRemoteState(): Either<VisaApiError, VisaActivationRemoteState>

    suspend fun getCardWalletAcceptanceData(
        request: VisaCardWalletDataToSignRequest,
    ): Either<VisaApiError, VisaDataToSignByCardWallet>

    suspend fun getCustomerWalletAcceptanceData(
        request: VisaCustomerWalletDataToSignRequest,
    ): Either<VisaApiError, VisaDataToSignByCustomerWallet>

    suspend fun activateCard(signedData: VisaSignedActivationDataByCardWallet): Either<VisaApiError, Unit>

    suspend fun approveByCustomerWallet(signedData: VisaSignedDataByCustomerWallet): Either<VisaApiError, Unit>

    suspend fun sendPinCode(pinCode: VisaEncryptedPinCode): Either<VisaApiError, Unit>

    suspend fun getPinCodeRsaEncryptionPublicKey(): String

    interface Factory {
        fun create(cardId: VisaCardId): VisaActivationRepository
    }
}