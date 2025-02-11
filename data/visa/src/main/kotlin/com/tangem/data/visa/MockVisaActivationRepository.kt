package com.tangem.data.visa

import com.tangem.common.extensions.toHexString
import com.tangem.crypto.CryptoUtils
import com.tangem.domain.visa.model.*
import com.tangem.domain.visa.repository.VisaActivationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("UnusedPrivateMember")
class MockVisaActivationRepository @AssistedInject constructor(
    @Assisted private val visaCardId: VisaCardId,
) : VisaActivationRepository {

    override suspend fun getActivationRemoteState(): VisaActivationRemoteState {
        return VisaActivationRemoteState.PaymentAccountDeploying
    }

    override suspend fun getActivationRemoteStateLongPoll(): VisaActivationRemoteState {
        return VisaActivationRemoteState.WaitingPinCode
    }

    override suspend fun getCardWalletAcceptanceData(
        request: VisaCardWalletDataToSignRequest,
    ): VisaDataToSignByCardWallet {
        return VisaDataToSignByCardWallet(
            request = request,
            hashToSign = CryptoUtils.generateRandomBytes(length = 32).toHexString(),
        )
    }

    override suspend fun getCustomerWalletAcceptanceData(
        request: VisaCustomerWalletDataToSignRequest,
    ): VisaDataToSignByCustomerWallet {
        return VisaDataToSignByCustomerWallet(
            request = request,
            CryptoUtils.generateRandomBytes(length = 32).toHexString(),
        )
    }

    override suspend fun activateCard(signedData: VisaSignedActivationDataByCardWallet) {}

    override suspend fun approveByCustomerWallet(signedData: VisaSignedDataByCustomerWallet) {}

    override suspend fun sendPinCode(pinCode: VisaEncryptedPinCode) {}

    @AssistedFactory
    interface Factory : VisaActivationRepository.Factory {
        override fun create(cardId: VisaCardId): MockVisaActivationRepository
    }
}