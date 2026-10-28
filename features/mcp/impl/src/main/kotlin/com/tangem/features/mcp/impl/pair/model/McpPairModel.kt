package com.tangem.features.mcp.impl.pair.model

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceAll
import com.tangem.common.ui.account.PortfolioSelectUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.notifications.SystemNotificationsStateProvider
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.mcp.models.McpDeviceVerification
import com.tangem.domain.mcp.repository.McpRepository
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.EnableAddressesSharingUseCase
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.mcp.impl.R
import com.tangem.features.mcp.impl.pair.McpPairComponent
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class McpPairModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val appScope: AppCoroutineScope,
    private val router: Router,
    private val systemNotificationsStateProvider: SystemNotificationsStateProvider,
    private val settingsManager: SettingsManager,
    private val mcpRepository: McpRepository,
    private val enableAddressesSharingUseCase: EnableAddressesSharingUseCase,
    private val messageSender: UiMessageSender,
    val selectorController: PortfolioSelectorController,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<McpPairComponent.Params>()

    val stackNavigation = StackNavigation<McpPairRoutes>()

    val portfolioFetcher: PortfolioFetcher = portfolioFetcherFactory.create(
        mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = true, isOnlyRegistered = true),
        scope = modelScope,
    )

    private val connectState = MutableStateFlow(ConnectState())
    private val verification = MutableStateFlow<McpDeviceVerification?>(value = null)

    private val notificationsWarning = McpNotificationsWarningUM(
        onOpenSettingsClick = settingsManager::openAppNotificationSettings,
    )

    private val requestPushPermissionChannel = Channel<Unit>(Channel.BUFFERED)

    /** One-shot requests to launch the system push permission prompt, consumed by the component. */
    val requestPushPermission: Flow<Unit> = requestPushPermissionChannel.receiveAsFlow()

    val uiState: StateFlow<McpPairUM>
        field = MutableStateFlow<McpPairUM>(McpPairUM.Loading)

    init {
        modelScope.launch {
            if (selectInitialWallet()) observeSelectedWallet()
        }
        loadVerification()
    }

    private fun loadVerification() {
        modelScope.launch {
            mcpRepository.getDeviceVerification(userCode = params.request.userCode)
                .fold(
                    ifLeft = { onPairFailed() },
                    ifRight = { verification.value = it },
                )
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

    fun onDecline() = onClose()

    fun onClose() {
        rejectVerification()
        router.pop()
    }

    /**
     * Dismissing the sheet turns the request down, exactly like Decline: left alone the user code
     * stays valid until its TTL, so the same deep link would open the confirmation all over again.
     *
     * Sent on [appScope] and not awaited — the sheet is already gone, a model-bound scope would
     * cancel the request halfway, and a failure has no screen left to be reported on.
     */
    private fun rejectVerification() {
        if (connectState.value.isConnected) return
        val verification = verification.value ?: return

        appScope.launch { mcpRepository.rejectDeviceVerification(verification) }
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
                    ifRight = { if (it.isSuccess) onPaired(walletId) else onPairFailed() },
                )
        }
    }

    private fun onPaired(walletId: UserWalletId) {
        shareAddresses(walletId)
        connectState.update { it.copy(isConnecting = false, isConnected = true) }
        // Replaces instead of pushing: there is nothing to go back to once the agent is connected.
        stackNavigation.replaceAll(McpPairRoutes.Success)
    }

    /**
     * The agent reads the portfolio by the wallet's addresses, so they are pushed as soon as it is authorized.
     *
     * Sent on [appScope]: the success screen is shown right away and the user may close the sheet before the
     * push completes. A failure is retried by the saver itself, and every later token push carries the
     * addresses anyway, so there is nothing to report here.
     */
    private fun shareAddresses(walletId: UserWalletId) {
        appScope.launch { enableAddressesSharingUseCase(walletId) }
    }

    /**
     * Every failure of the flow reads the same to the user, so there is nothing to branch on: the
     * server answers an unknown, expired and already used code alike, and neither end of the pairing
     * can be resumed in place. The sheet goes away and the alert carries the reason.
     */
    private fun onPairFailed() {
        connectState.update { it.copy(isConnecting = false) }
        messageSender.send(
            DialogMessage(
                title = resourceReference(R.string.common_something_went_wrong),
                message = resourceReference(R.string.common_try_again_later),
            ),
        )
        router.pop()
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
            transform = ::createState,
        )
            .onEach { state -> uiState.value = state }
            .launchIn(modelScope)
    }

    private fun createState(
        portfolios: PortfolioFetcher.Data,
        selected: Pair<UserWallet, AccountStatus.CryptoPortfolio>,
        connect: ConnectState,
        verification: McpDeviceVerification?,
    ): McpPairUM {
        verification ?: return McpPairUM.Loading
        val agent = createAgent(verification)
        if (connect.isConnected) return McpPairUM.Success(agentName = agent.name, onDone = ::onClose)

        val wallet = selected.first
        return McpPairUM.AgentInfo(
            agent = agent,
            wallet = PortfolioSelectUM(
                icon = null,
                name = stringReference(wallet.name),
                isAccountMode = false,
                isMultiChoice = portfolios.balances.size > 1,
                onClick = { stackNavigation.pushNew(McpPairRoutes.PortfolioSelector) },
            ),
            notificationsWarning = if (connect.isPushDenied) notificationsWarning else null,
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

    private fun createAgent(verification: McpDeviceVerification): McpAgentUM {
        val clientName = verification.clientName
        return if (clientName != null) {
            McpAgentUM(
                name = stringReference(clientName),
                origin = verification.clientOrigin?.let(::stringReference),
                iconUrl = verification.clientIconUrl,
            )
        } else {
            McpAgentUM(
                name = stringReference(verification.clientOrigin ?: verification.userCode),
                origin = null,
                iconUrl = verification.clientIconUrl,
            )
        }
    }

    private data class ConnectState(
        val isConnecting: Boolean = false,
        val isPushDenied: Boolean = false,
        val isConnected: Boolean = false,
    )
}