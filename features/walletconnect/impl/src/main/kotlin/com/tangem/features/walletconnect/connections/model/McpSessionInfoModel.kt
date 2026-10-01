package com.tangem.features.walletconnect.connections.model

import com.tangem.common.ui.account.PortfolioSelectUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.mcp.models.McpSession
import com.tangem.domain.mcp.repository.McpRepository
import com.tangem.features.walletconnect.connections.components.McpSessionInfoComponent
import com.tangem.features.walletconnect.connections.entity.McpSessionInfoUM
import com.tangem.features.walletconnect.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import javax.inject.Inject

@ModelScoped
internal class McpSessionInfoModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val mcpRepository: McpRepository,
    private val messageSender: UiMessageSender,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<McpSessionInfoComponent.Params>()

    /** `null` only until the session is picked out of the repository's list, which is cached. */
    val uiState: StateFlow<McpSessionInfoUM?>
        field = MutableStateFlow<McpSessionInfoUM?>(value = null)

    init {
        loadSession()
    }

    // There is no endpoint for a single session, so it is picked out of the list the repository keeps
    private fun loadSession() {
        mcpRepository.getSessions()
            .mapNotNull { sessions -> sessions.firstOrNull { it.id == params.sessionId } }
            .onEach { session -> uiState.value = session.toUM(isRevoking = uiState.value?.isRevoking == true) }
            .launchIn(modelScope)
    }

    private fun McpSession.toUM(isRevoking: Boolean) = McpSessionInfoUM(
        agentName = clientName ?: clientOrigin ?: clientId,
        agentOrigin = clientOrigin.orEmpty(),
        agentIconUrl = clientIconUrl,
        connectedAt = DateTimeFormatters.formatDate(
            date = DateTime(createdAtMillis),
            formatter = DateTimeFormatters.dateTimeMMMdYYYY,
        ),
        wallet = PortfolioSelectUM(
            icon = null,
            name = stringReference(params.walletName),
            isAccountMode = false,
            isMultiChoice = false,
            onClick = {},
        ),
        isRevoking = isRevoking,
        onRevokeClick = ::revokeSession,
        onDismiss = params.onDismiss,
    )

    private fun revokeSession() {
        modelScope.launch {
            uiState.update { state -> state?.copy(isRevoking = true) }
            val isRevoked = mcpRepository.revokeSession(params.sessionId).isRight()
            uiState.update { state -> state?.copy(isRevoking = false) }

            if (isRevoked) {
                params.onDismiss()
            } else {
                messageSender.send(SnackbarMessage(message = resourceReference(R.string.mcp_session_revoke_failed)))
            }
        }
    }
}