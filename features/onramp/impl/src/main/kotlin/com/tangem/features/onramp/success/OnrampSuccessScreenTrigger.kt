package com.tangem.features.onramp.success

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

interface OnrampSuccessScreenListener {
    val onrampSuccessTriggerFlow: Flow<Boolean>
}

interface OnrampSuccessScreenTrigger {
    suspend fun triggerOnrampSuccess(result: Boolean)
}

@Singleton
internal class DefaultOnrampSuccessScreenTrigger @Inject constructor() :
    OnrampSuccessScreenTrigger,
    OnrampSuccessScreenListener {

    override val onrampSuccessTriggerFlow = MutableSharedFlow<Boolean>()

    override suspend fun triggerOnrampSuccess(result: Boolean) {
        onrampSuccessTriggerFlow.emit(result)
    }
}