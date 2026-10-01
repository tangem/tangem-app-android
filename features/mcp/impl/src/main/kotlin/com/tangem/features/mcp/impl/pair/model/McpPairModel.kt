package com.tangem.features.mcp.impl.pair.model

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.tangem.common.ui.account.PortfolioSelectUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.notifications.SystemNotificationsStateProvider
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.mcp.models.McpDeviceVerification
import com.tangem.domain.mcp.repository.McpRepository
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.mcp.impl.pair.McpPairComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class McpPairModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val systemNotificationsStateProvider: SystemNotificationsStateProvider,
    private val settingsManager: SettingsManager,
    private val mcpRepository: McpRepository,
    private val messageSender: UiMessageSender,
    val selectorController: PortfolioSelectorController,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<McpPairComponent.Params>()

    val stackNavigation = StackNavigation<McpPairRoutes>()

    val portfolioFetcher: PortfolioFetcher = portfolioFetcherFactory.create(
        mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = true),
        scope = modelScope,
    )

    private val connectState = MutableStateFlow(ConnectState())
    private val verification = MutableStateFlow<McpDeviceVerification?>(value = null)

    private val pushNotificationsWarning = McpPushNotificationsWarningUM(
        onOpenSettingsClick = settingsManager::openAppNotificationSettings,
    )

    private val requestPushPermissionChannel = Channel<Unit>(Channel.BUFFERED)

    /** One-shot requests to launch the system push permission prompt, consumed by the component. */
    val requestPushPermission: Flow<Unit> = requestPushPermissionChannel.receiveAsFlow()

    val uiState: StateFlow<McpPairUM>
        field = MutableStateFlow<McpPairUM>(McpPairUM.Loading(onDecline = ::onDecline))

    init {
        modelScope.launch {
            if (selectInitialWallet()) observeSelectedWallet()
        }
        loadVerification()
    }

    private fun loadVerification() {
        modelScope.launch {
            mcpRepository.getDeviceVerification(userCode = params.request.userCode)
                .onRight { verification.value = it }
        }
    }

    fun onResume() {
        // The user may have come back from the system settings with notifications turned on.
        if (!connectState.value.isPushDenied) return
        if (systemNotificationsStateProvider.areNotificationsEnabled()) {
            connectState.update { it.copy(isPushDenied = false) }
        }
    }

    fun onPushPermissionResult() {
        // The granted flag alone is unreliable: an already-granted-but-OS-disabled app returns true
        // instantly, and pre-Android 13 there is no runtime permission at all.
        if (systemNotificationsStateProvider.areNotificationsEnabled()) {
            connectState.update { it.copy(isPushDenied = false) }
            selectorController.selectedAccountSync?.userWalletId
                ?.let { pair(it) }
        } else {
            connectState.value = ConnectState(isConnecting = false, isPushDenied = true)
        }
    }

    fun onDecline() {
        // TODO reject the pairing request via the MCP domain layer once it exists
        router.pop()
    }

    /**
     * The button stays in loading for the whole chain: the push check first, then the pairing request.
     */
    private fun onConnect(wallet: UserWallet) {
        connectState.update { it.copy(isConnecting = true) }
        if (systemNotificationsStateProvider.areNotificationsEnabled()) {
            pair(wallet.walletId)
        } else {
            requestPushPermissionChannel.trySend(Unit)
        }
    }

    /** The client and the scope are never chosen here, they are echoed back from the verification. */
    private fun pair(walletId: UserWalletId) {
        val verification = verification.value
        if (verification == null) {
            onPairFailed()
            return
        }

        modelScope.launch {
            mcpRepository.approveDeviceVerification(verification = verification, walletId = walletId)
                .fold(
                    ifLeft = { onPairFailed() },
                    ifRight = { if (it.isSuccess) router.pop() else onPairFailed() },
                )
        }
    }

    private fun onPairFailed() {
        connectState.update { it.copy(isConnecting = false) }
        messageSender.send(SnackbarMessage(message = McpPairTexts.connectFailed))
    }

    private suspend fun selectInitialWallet(): Boolean {
        val accountId = portfolioFetcher.data.first()
            .balances.values.firstOrNull()
            ?.accountsBalance?.mainAccount?.accountId
        if (accountId == null) {
            router.pop()
            return false
        }
        if (selectorController.selectedAccountSync == null) selectorController.selectAccount(accountId)
        return true
    }

    private fun observeSelectedWallet() {
        combine(
            flow = portfolioFetcher.data,
            flow2 = selectorController.selectedAccountWithData(portfolioFetcher)
                .filterNotNull()
                // Re-selecting a wallet closes the selector and returns to the agent info screen.
                .runningReduce { _, new ->
                    stackNavigation.pop()
                    new
                },
            flow3 = connectState,
            flow4 = verification,
            transform = ::createContent,
        )
            .onEach { content -> uiState.value = content }
            .launchIn(modelScope)
    }

    private fun createContent(
        portfolios: PortfolioFetcher.Data,
        selected: Pair<UserWallet, AccountStatus.CryptoPortfolio>,
        connect: ConnectState,
        verification: McpDeviceVerification?,
    ): McpPairUM.Content {
        val wallet = selected.first
        return McpPairUM.Content(
            agent = McpAgentUM(
                name = McpPairTexts.agentName,
                subtitle = stringReference(params.request.userCode),
                iconUrl = null,
                verificationText = verification?.toString()?.toPrettyDebugString().orEmpty(),
            ),
            portfolioSelectRow = PortfolioSelectUM(
                icon = null,
                name = stringReference(wallet.name),
                isAccountMode = false,
                isMultiChoice = !portfolios.isSingleChoice,
                onClick = { stackNavigation.pushNew(McpPairRoutes.PortfolioSelector) },
            ),
            pushNotificationsWarning = if (connect.isPushDenied) pushNotificationsWarning else null,
            connectButton = McpConnectButtonUM(
                // The agent asks for every transaction approval through a push, so a connection
                // established without notifications would be unusable.
                isEnabled = !connect.isPushDenied,
                isLoading = connect.isConnecting,
                onClick = { onConnect(wallet) },
            ),
            onDecline = ::onDecline,
        )
    }

    private data class ConnectState(val isConnecting: Boolean = false, val isPushDenied: Boolean = false)
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