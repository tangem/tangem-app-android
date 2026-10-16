package com.tangem.feature.swap.mcp.model

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.utils.pushOrReplaceCurrent
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
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.domain.swap.usecase.GetExchangeIntentUseCase
import com.tangem.feature.swap.mcp.ExchangeIntentSwapRouteConverter
import com.tangem.feature.swap.mcp.holdings.ExchangeIntentHoldings
import com.tangem.feature.swap.mcp.holdings.ExchangeIntentHoldingsResolver
import com.tangem.features.swap.mcp.ExchangeIntentComponent
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
 * Reads the exchange intent as the selected wallet, offers it to the owner and hands it over to the Swap screen that
 * carries it out. An intent the wallet cannot act on yet closes the sheet.
 */
@Suppress("LongParameterList")
@ModelScoped
internal class ExchangeIntentModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val appRouter: AppRouter,
    private val messageSender: UiMessageSender,
    private val getExchangeIntentUseCase: GetExchangeIntentUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val holdingsResolver: ExchangeIntentHoldingsResolver,
    private val mcpRepository: McpRepository,
    private val offerConverter: ExchangeIntentOfferConverter,
    private val swapRouteConverter: ExchangeIntentSwapRouteConverter,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<ExchangeIntentComponent.Params>()

    private var loadJob: Job? = null
    private var loaded: LoadedExchange? = null
    private var isFinished = false

    val uiState: StateFlow<ExchangeIntentUM>
        field = MutableStateFlow<ExchangeIntentUM>(ExchangeIntentUM.Loading)

    init {
        TangemLogger.i("Exchange intent received: id=${params.intentId}")
        loadJob = modelScope.launch { loadIntent() }
    }

    fun onClose() {
        if (finish()) router.pop()
    }

    override fun onDestroy() {
        finish()
        super.onDestroy()
    }

    private fun finish(): Boolean {
        if (isFinished) return false
        isFinished = true
        loaded = null
        loadJob?.cancel()
        loadJob = null
        return true
    }

    private suspend fun loadIntent() {
        val walletId = userWalletsListRepository.selectedUserWalletSync()?.walletId
            ?: return close(reason = "no wallet is selected")

        val intent = getExchangeIntentUseCase(userWalletId = walletId, intentId = params.intentId)
            .getOrElse { error -> return close(reason = "the intent cannot be read", error = error) }
        if (intent.isHandled) return close(reason = "the intent has already been carried out")

        val holdings = holdingsResolver.resolve(userWalletId = walletId, intent = intent)
        if (holdings !is ExchangeIntentHoldings.Both) return close(reason = "the wallet lacks a token: $holdings")

        val offer = offerConverter.convert(
            ExchangeIntentOfferConverter.Input(
                intent = intent,
                from = holdings.from,
                to = holdings.to,
                agent = getAgent(intent),
                onProceed = ::onProceed,
            ),
        ) ?: return close(reason = "the amount of the intent is malformed")

        currentCoroutineContext().ensureActive()
        loaded = LoadedExchange(walletId = walletId, intent = intent, holdings = holdings)
        uiState.value = offer
    }

    /** Not knowing the agent must not hold the offer back, so a slow or failed lookup gives the unknown agent. */
    private suspend fun getAgent(intent: ExchangeIntent): McpAgent {
        val clientId = intent.clientId.orEmpty()

        return withTimeoutOrNull(AGENT_LOOKUP_TIMEOUT_MILLIS) { mcpRepository.getAgent(clientId) }
            ?: McpAgent.unknown(clientId)
    }

    private fun close(reason: String, error: Throwable? = null) {
        if (!finish()) return
        TangemLogger.e("Exchange intent ${params.intentId} is not offered: $reason", error)
        messageSender.send(ToastMessage(message = resourceReference(R.string.common_something_went_wrong)))
        router.pop()
    }

    private fun onProceed() {
        val (walletId, intent, holdings) = loaded ?: return
        if (!finish()) return
        val route = swapRouteConverter.convert(
            ExchangeIntentSwapRouteConverter.Input(
                intent = intent,
                userWalletId = walletId,
                from = holdings.from,
                to = holdings.to,
            ),
        )

        openSwap(route)
        router.pop()
    }

    /** Opens Swap in place of an open one, unless that one is an account flow: it is a different one using Swap. */
    private fun openSwap(route: AppRoute.Swap) {
        val currentStack = appRouter.stack
        val currentRoute = currentStack.lastOrNull()
        if (currentRoute is AppRoute.Swap && currentRoute.accountFlow != null && route !in currentStack) {
            appRouter.push(route)
        } else {
            appRouter.pushOrReplaceCurrent(route)
        }
    }

    private data class LoadedExchange(
        val walletId: UserWalletId,
        val intent: ExchangeIntent,
        val holdings: ExchangeIntentHoldings.Both,
    )
}