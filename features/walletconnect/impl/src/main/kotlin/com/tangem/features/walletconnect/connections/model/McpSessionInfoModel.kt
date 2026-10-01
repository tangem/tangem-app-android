package com.tangem.features.walletconnect.connections.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.mcp.repository.McpRepository
import com.tangem.features.walletconnect.connections.components.McpSessionInfoComponent
import com.tangem.features.walletconnect.connections.entity.McpSessionInfoUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class McpSessionInfoModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val mcpRepository: McpRepository,
    private val messageSender: UiMessageSender,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<McpSessionInfoComponent.Params>()

    val uiState: StateFlow<McpSessionInfoUM>
        field = MutableStateFlow(
            McpSessionInfoUM(
                debugText = "",
                isRevoking = false,
                onRevokeClick = ::revokeSession,
                onDismiss = params.onDismiss,
            ),
        )

    init {
        loadSession()
    }

    // There is no endpoint for a single session, so it is picked out of the list the repository keeps
    private fun loadSession() {
        mcpRepository.getSessions()
            .mapNotNull { sessions -> sessions.firstOrNull { it.id == params.sessionId } }
            .onEach { session -> uiState.update { state -> state.copy(
                debugText = session.toString().toPrettyDebugString(),
            ) } }
            .launchIn(modelScope)
    }

    private fun revokeSession() {
        modelScope.launch {
            uiState.update { state -> state.copy(isRevoking = true) }
            val isRevoked = mcpRepository.revokeSession(params.sessionId).isRight()
            uiState.update { state -> state.copy(isRevoking = false) }

            if (isRevoked) {
                params.onDismiss()
            } else {
                messageSender.send(SnackbarMessage(message = stringReference("Failed to revoke access")))
            }
        }
    }
}

/**
 * Reflows a `toString()` into an indented tree: `Foo(a=1, b=Bar(x=2))` gets one field per line.
 *
 * Debug-only formatting, so it just walks brackets and commas instead of parsing anything.
 */
private fun String.toPrettyDebugString(): String {
    val builder = StringBuilder()
    var depth = 0
    var index = 0

    fun newLine() {
        builder.append('\n')
        repeat(depth) { builder.append("  ") }
    }

    while (index < length) {
        when (val char = this[index]) {
            '(', '[' -> {
                depth++
                builder.append(char)
                newLine()
            }
            ')', ']' -> {
                depth--
                newLine()
                builder.append(char)
            }
            ',' -> {
                builder.append(char)
                newLine()
                if (index + 1 < length && this[index + 1] == ' ') index++
            }
            else -> builder.append(char)
        }
        index++
    }

    return builder.toString()
}