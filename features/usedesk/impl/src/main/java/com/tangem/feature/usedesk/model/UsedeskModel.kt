package com.tangem.feature.usedesk.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ru.usedesk.chat_sdk.entity.UsedeskChatConfiguration
import javax.inject.Inject

@Stable
@ModelScoped
internal class UsedeskModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val _state = MutableStateFlow(getInitialState())

    val state: StateFlow<UsedeskState> = _state

    private fun getInitialState(): UsedeskState {
        return UsedeskState(
            UsedeskChatConfiguration(
                companyId = COMPANY_ID,
                channelId = CHANNEL_ID,
            ),
        )
    }

    private companion object {
        const val COMPANY_ID = "170509"
        const val CHANNEL_ID = "65637"
    }
}