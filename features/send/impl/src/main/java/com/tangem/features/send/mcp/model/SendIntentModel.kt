package com.tangem.features.send.mcp.model

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.domain.mcp.repository.McpRepository
import com.tangem.domain.transaction.models.SendIntent
import com.tangem.domain.transaction.usecase.GetSendIntentUseCase
import com.tangem.features.send.api.mcp.SendIntentComponent
import com.tangem.features.send.mcp.SendIntentSendRouteConverter
import com.tangem.features.send.mcp.SendIntentSource
import com.tangem.features.send.mcp.SendIntentSourceResolver
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

private const val AGENT_LOOKUP_TIMEOUT_MILLIS = 3_000L

/**
 * Reads the send intent for the selected wallet, offers it to the owner and hands it over to the Send screen that
 * carries it out. An intent the wallet cannot act on closes the sheet.
 */
@Suppress("LongParameterList")
@ModelScoped
internal class SendIntentModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val messageSender: UiMessageSender,
    private val getSendIntentUseCase: GetSendIntentUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val sourceResolver: SendIntentSourceResolver,
    private val mcpRepository: McpRepository,
    private val offerConverter: SendIntentOfferConverter,
    private val sendRouteConverter: SendIntentSendRouteConverter,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<SendIntentComponent.Params>()

    private var loadJob: Job? = null
    private var loadedRoute: AppRoute.Send? = null
    private var isFinished = false

    val offer: StateFlow<SendIntentUM?>
        field = MutableStateFlow(null)

    init {
        TangemLogger.i("Send intent received: id=${params.intentId}")
        loadJob = modelScope.launch { loadIntent() }
    }

    fun onClose() {
        if (!finish()) return
        router.pop()
    }

    override fun onDestroy() {
        finish()
        super.onDestroy()
    }

    private fun finish(): Boolean {
        if (isFinished) return false
        isFinished = true
        loadedRoute = null
        loadJob?.cancel()
        loadJob = null
        return true
    }

    private suspend fun loadIntent() {
        val walletId = userWalletsListRepository.selectedUserWalletSync()?.walletId
            ?: return close(reason = "no wallet is selected")

        val intent = getSendIntentUseCase(intentId = params.intentId)
            .getOrElse { error -> return close(reason = "the intent cannot be read", error = error) }
        if (intent.isHandled) return close(reason = "the intent has already been carried out")

        val source = sourceResolver.resolve(userWalletId = walletId, intent = intent)
        if (source !is SendIntentSource.Found) return close(reason = "the wallet does not send from it: $source")

        val route = sendRouteConverter.convert(
            SendIntentSendRouteConverter.Input(intent = intent, userWalletId = walletId, currency = source.currency),
        ).getOrElse { error -> return close(reason = "the amount of the intent is invalid", error = error) }

        val offer = offerConverter.convert(
            SendIntentOfferConverter.Input(
                intent = intent,
                currency = source.currency,
                agent = getAgent(intent),
                onProceed = ::onProceed,
            ),
        ) ?: return close(reason = "the amount of the intent is malformed")

        currentCoroutineContext().ensureActive()
        loadedRoute = route
        this.offer.value = offer
    }

    /** Not knowing the agent must not hold the offer back, so a slow or failed lookup gives the unknown agent. */
    private suspend fun getAgent(intent: SendIntent): McpAgent {
        val clientId = intent.clientId.orEmpty()

        return withTimeoutOrNull(AGENT_LOOKUP_TIMEOUT_MILLIS) { mcpRepository.getAgent(clientId) }
            ?: McpAgent.unknown(clientId)
    }

    private fun close(reason: String, error: Throwable? = null) {
        if (!finish()) return
        TangemLogger.e("Send intent ${params.intentId} is not offered: $reason", error)
        messageSender.send(ToastMessage(message = resourceReference(R.string.common_something_went_wrong)))
        router.pop()
    }

    private fun onProceed() {
        val route = loadedRoute ?: return
        var isCoveredByOtherScreen = false

        router.navigate(
            transformer = { stack ->
                val isOnTop = stack.lastOrNull() == route
                isCoveredByOtherScreen = !isOnTop && route in stack
                // An existing Send may be broadcasting even when the Card SDK UI is no longer visible, so it is
                // neither replaced nor popped back to
                if (isOnTop || isCoveredByOtherScreen) stack else stack + route
            },
            onComplete = { isSuccess ->
                when {
                    !isSuccess -> close(reason = "the send could not be opened")
                    isCoveredByOtherScreen -> close(reason = "the send is already open behind another screen")
                    else -> onSendOpened()
                }
            },
        )
    }

    private fun onSendOpened() {
        if (!finish()) return
        router.pop()
    }
}