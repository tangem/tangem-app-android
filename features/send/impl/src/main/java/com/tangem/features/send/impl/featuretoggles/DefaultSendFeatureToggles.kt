package com.tangem.features.send.impl.featuretoggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.features.send.api.featuretoggles.SendFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

/**
 * Default implementation of Send feature toggles
 *
 * @property featureTogglesManager manager for getting information about the availability of feature toggles
 * @property tangemTechApi api to get remote feature toggle for send
 * @property dispatchers coroutine dispatchers
 */
internal class DefaultSendFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : SendFeatureToggles {

    private val remoteSendEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true)

    override val isRedesignedSendEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "REDESIGNED_SEND_SCREEN_ENABLED") &&
            remoteSendEnabled.value

    override suspend fun fetchNewSendEnabled() {
        runCatching(dispatchers.io) {
            tangemTechApi.getFeatures().getOrThrow()
        }.onSuccess { response ->
            remoteSendEnabled.update { response.isNewSendEnabled }
        }.onFailure {
            Timber.e(it.localizedMessage, "Unable to fetch new send toggle")
        }
    }
}
