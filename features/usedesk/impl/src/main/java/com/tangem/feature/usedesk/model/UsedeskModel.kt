package com.tangem.feature.usedesk.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.feature.usedesk.analytics.UsedeskAnalyticsEvents
import com.tangem.feature.usedesk.api.UsedeskComponent
import com.tangem.usedesk.chat_sdk.entity.UsedeskChatConfiguration
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

@Stable
@ModelScoped
internal class UsedeskModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val appPreferencesStore: AppPreferencesStore,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val feedbackRepository: FeedbackRepository,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<UsedeskComponent.Params>()

    private val _state = MutableStateFlow(UsedeskState(usedeskChatConfiguration = null))

    val state: StateFlow<UsedeskState> = _state

    // The chat screen opened/error event is sent only once.
    private var isScreenLoadEventSent = false

    init {
        modelScope.launch {
            val clientId = getOrCreateClientId()
            _state.value = UsedeskState(
                UsedeskChatConfiguration(
                    urlChat = URL_CHAT,
                    urlChatApi = URL_CHAT_API,
                    companyId = COMPANY_ID,
                    channelId = CHANNEL_ID,
                    clientId = clientId,
                    clientEmail = params.userWalletId,
                ),
            )
        }
    }

    fun onChatLoaded() {
        if (isScreenLoadEventSent) return
        isScreenLoadEventSent = true
        analyticsEventHandler.send(
            UsedeskAnalyticsEvents.ChatScreenOpened(source = AnalyticsParam.ScreensSources.Settings),
        )
    }

    fun onChatLoadError() {
        if (isScreenLoadEventSent) return
        isScreenLoadEventSent = true
        analyticsEventHandler.send(UsedeskAnalyticsEvents.ChatScreenError())
    }

    /**
     * Collects the app logs into a zip archive (the same one attached to the support email)
     * and returns it via [onReady] on the main thread, or null if there are no logs.
     */
    fun provideLogsFile(onReady: (File?) -> Unit) {
        modelScope.launch {
            val file = runSuspendCatching { feedbackRepository.getZipLogFile() }.getOrNull()
            withContext(dispatchers.main) { onReady(file) }
        }
    }

    override fun onDestroy() {
        analyticsEventHandler.send(UsedeskAnalyticsEvents.ChatScreenClosed())
        super.onDestroy()
    }

    private suspend fun getOrCreateClientId(): String {
        var clientId = ""
        appPreferencesStore.editData { preferences ->
            val existing = preferences[PreferencesKeys.USEDESK_CLIENT_ID_KEY]
            clientId = if (existing.isNullOrBlank()) {
                UUID.randomUUID().toString().also { preferences[PreferencesKeys.USEDESK_CLIENT_ID_KEY] = it }
            } else {
                existing
            }
        }
        return clientId
    }

    private companion object {
        const val URL_CHAT = "https://pubsub.tangem.org"
        const val URL_CHAT_API = "https://ud.tangem.org"
        const val COMPANY_ID = "2"
        const val CHANNEL_ID = "54"
    }
}