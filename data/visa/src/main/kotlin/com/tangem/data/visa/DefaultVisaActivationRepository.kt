package com.tangem.data.visa

import com.tangem.data.visa.converter.VisaActivationStatusConverter
import com.tangem.datasource.api.visa.TangemVisaApi
import com.tangem.domain.visa.model.VisaActivationRemoteState
import com.tangem.domain.visa.repository.VisaActivationRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultVisaActivationRepository @Inject constructor(
    private val visaApi: TangemVisaApi,
    private val dispatcherProvider: CoroutineDispatcherProvider,
    private val visaActivationStatusConverter: VisaActivationStatusConverter,
) : VisaActivationRepository {

    override suspend fun getActivationRemoteState(): VisaActivationRemoteState = withContext(dispatcherProvider.io) {
        visaActivationStatusConverter.convert(visaApi.getRemoteActivationStatus())
        // TODO implement refreshing access token if it's expired
    }
}