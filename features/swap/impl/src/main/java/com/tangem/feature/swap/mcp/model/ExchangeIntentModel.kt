package com.tangem.feature.swap.mcp.model

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.domain.mcp.repository.McpRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.domain.swap.models.ExchangeIntentError
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
 * carries it out. Processed and unavailable intents keep a status sheet open until dismissed; only network failures
 * and timeouts offer retry. Without a selected wallet, the queue slot is released without showing a sheet.
 */
@Suppress("LongParameterList")
@ModelScoped
internal class ExchangeIntentModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val getExchangeIntentUseCase: GetExchangeIntentUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val holdingsResolver: ExchangeIntentHoldingsResolver,
    private val mcpRepository: McpRepository,
    private val offerConverter: ExchangeIntentOfferConverter,
    private val processedConverter: ExchangeIntentProcessedConverter,
    private val cantReachConverter: ExchangeIntentCantReachConverter,
    private val swapRouteConverter: ExchangeIntentSwapRouteConverter,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<ExchangeIntentComponent.Params>()

    private var loadJob: Job? = null
    private var loaded: LoadedExchange? = null
    private var isFinished = false

    val sheet: StateFlow<ExchangeIntentUM?>
        field = MutableStateFlow(null)

    init {
        TangemLogger.i("Exchange intent received: id=${params.intentId}")
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
        loaded = null
        loadJob?.cancel()
        loadJob = null
        return true
    }

    private suspend fun loadIntent() {
        val walletId = userWalletsListRepository.selectedUserWalletSync()?.walletId
            ?: return closeWithoutSheet(reason = "no wallet is selected")

        val intent = getExchangeIntentUseCase(userWalletId = walletId, intentId = params.intentId)
            .getOrElse { error ->
                return showCantReach(
                    reason = "the intent cannot be read",
                    error = error,
                    canRetry = error is ExchangeIntentError.Unreachable,
                )
            }
        if (intent.isHandled) return showProcessed(walletId = walletId, intent = intent)

        val holdings = holdingsResolver.resolve(userWalletId = walletId, intent = intent)
        if (holdings !is ExchangeIntentHoldings.Both) {
            return showCantReach(
                reason = "the wallet lacks a token: $holdings",
            )
        }

        val offer = offerConverter.convert(
            ExchangeIntentOfferConverter.Input(
                intent = intent,
                from = holdings.from,
                to = holdings.to,
                agent = getAgent(intent),
                onProceed = ::onProceed,
            ),
        ) ?: return showCantReach(reason = "the amount of the intent is malformed")

        currentCoroutineContext().ensureActive()
        loaded = LoadedExchange(walletId = walletId, intent = intent, holdings = holdings)
        sheet.value = offer
    }

    private suspend fun showProcessed(walletId: UserWalletId, intent: ExchangeIntent) {
        TangemLogger.i("Exchange intent ${params.intentId} has already been carried out")
        val holdings = holdingsResolver.resolve(
            userWalletId = walletId,
            intent = intent,
        ) as? ExchangeIntentHoldings.Both
        val status = processedConverter.convert(
            ExchangeIntentProcessedConverter.Input(
                intent = intent,
                from = holdings?.from,
                to = holdings?.to,
                onClose = ::onClose,
            ),
        )

        currentCoroutineContext().ensureActive()
        sheet.value = status
    }

    /** Not knowing the agent must not hold the offer back, so a slow or failed lookup gives the unknown agent. */
    private suspend fun getAgent(intent: ExchangeIntent): McpAgent {
        val clientId = intent.clientId.orEmpty()

        return withTimeoutOrNull(AGENT_LOOKUP_TIMEOUT_MILLIS) { mcpRepository.getAgent(clientId) }
            ?: McpAgent.unknown(clientId)
    }

    private suspend fun showCantReach(reason: String, error: Throwable? = null, canRetry: Boolean = false) {
        TangemLogger.e("Exchange intent ${params.intentId} is not offered: $reason", error)
        val status = cantReachConverter.convert(
            ExchangeIntentCantReachConverter.Input(onRetry = if (canRetry) ::onRetry else null, onClose = ::onClose),
        )

        currentCoroutineContext().ensureActive()
        sheet.value = status
    }

    private fun closeWithoutSheet(reason: String) {
        if (!finish()) return
        TangemLogger.i("Exchange intent ${params.intentId} is not offered: $reason")
        router.pop()
    }

    /** The sheet stays on screen until the new answer replaces it: removing it would dismiss the model. */
    private fun onRetry() {
        if (isFinished || loadJob?.isActive == true) return
        loadJob = modelScope.launch { loadIntent() }
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
        router.navigate(
            transformer = { stack ->
                val currentRoute = stack.lastOrNull()
                val isAccountFlowOnTop = currentRoute is AppRoute.Swap && currentRoute.accountFlow != null
                when {
                    isAccountFlowOnTop && route !in stack -> stack + route
                    currentRoute == route -> stack
                    route in stack -> stack.take(stack.lastIndexOf(route) + 1)
                    currentRoute is AppRoute.Swap -> stack.dropLast(1) + route
                    else -> stack + route
                }
            },
        )
    }

    private data class LoadedExchange(
        val walletId: UserWalletId,
        val intent: ExchangeIntent,
        val holdings: ExchangeIntentHoldings.Both,
    )
}