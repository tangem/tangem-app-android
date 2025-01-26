package com.tangem.data.visa

import com.tangem.data.visa.converter.VisaActivationStatusConverter
import com.tangem.datasource.api.visa.TangemVisaApi
import com.tangem.domain.visa.model.ActivationOrder
import com.tangem.domain.visa.model.VisaActivationRemoteState
import com.tangem.domain.visa.repository.VisaActivationRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext

internal class DefaultVisaActivationRepository @AssistedInject constructor(
    @Assisted private val cardId: String,
    private val visaApi: TangemVisaApi,
    private val dispatcherProvider: CoroutineDispatcherProvider,
    private val visaActivationStatusConverter: VisaActivationStatusConverter,
) : VisaActivationRepository {

    override suspend fun getActivationRemoteState(): VisaActivationRemoteState = withContext(dispatcherProvider.io) {
        visaActivationStatusConverter.convert(visaApi.getRemoteActivationStatus(cardId))
        VisaActivationRemoteState.CardWalletSignatureRequired // mock
// [REDACTED_TODO_COMMENT]
    }

    override suspend fun getActivationOrderToSign(): ActivationOrder {
        return ActivationOrder("TODO implement")
    }

    @AssistedFactory
    interface Factory : VisaActivationRepository.Factory {
        override fun create(cardId: String): DefaultVisaActivationRepository
    }
}
