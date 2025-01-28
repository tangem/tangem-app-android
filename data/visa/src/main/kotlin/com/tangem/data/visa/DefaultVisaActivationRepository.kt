package com.tangem.data.visa

import com.tangem.common.extensions.toHexString
import com.tangem.crypto.CryptoUtils
import com.tangem.data.visa.converter.VisaActivationStatusConverter
import com.tangem.datasource.api.common.visa.TangemVisaAuthProvider
import com.tangem.datasource.api.visa.TangemVisaApi
import com.tangem.domain.visa.model.ActivationOrder
import com.tangem.domain.visa.model.VisaActivationRemoteState
import com.tangem.domain.visa.repository.VisaActivationRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext

@Suppress("UnusedPrivateMember")
internal class DefaultVisaActivationRepository @AssistedInject constructor(
    @Assisted private val cardId: String,
    private val visaApi: TangemVisaApi,
    private val dispatcherProvider: CoroutineDispatcherProvider,
    private val visaActivationStatusConverter: VisaActivationStatusConverter,
    private val visaAuthProvider: TangemVisaAuthProvider,
) : VisaActivationRepository {

    override suspend fun getActivationRemoteState(): VisaActivationRemoteState = withContext(dispatcherProvider.io) {
        // visaActivationStatusConverter.convert(visaApi.getRemoteActivationStatus(visaAuthProvider.getAuthHeader(cardId)))
        VisaActivationRemoteState.CardWalletSignatureRequired // mock
        // TODO implement refreshing access token if it's expired
    }

    override suspend fun getActivationRemoteStateLongPoll(): VisaActivationRemoteState =
        withContext(dispatcherProvider.io) {
            // visaActivationStatusConverter.convert(
            //     visaApi.getRemoteActivationStatusLongPoll(visaAuthProvider.getAuthHeader(cardId)),
            // )

            VisaActivationRemoteState.WaitingPinCode
        }

    override suspend fun getActivationOrderToSign(): ActivationOrder = withContext(dispatcherProvider.io) {
        ActivationOrder(CryptoUtils.generateRandomBytes(length = 32).toHexString())
    }

    @AssistedFactory
    interface Factory : VisaActivationRepository.Factory {
        override fun create(cardId: String): DefaultVisaActivationRepository
    }
}