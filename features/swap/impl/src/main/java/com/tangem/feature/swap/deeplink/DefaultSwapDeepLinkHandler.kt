package com.tangem.feature.swap.deeplink

import arrow.core.Either
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeeplinkConst.FROM_AMOUNT_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.FROM_NETWORK_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.FROM_TOKEN_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.FROM_USER_ACCOUNT_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.FROM_USER_WALLET_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.PROVIDER_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TO_NETWORK_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TO_TOKEN_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TO_USER_ACCOUNT_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TO_USER_WALLET_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.WALLET_ID_KEY
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.account.status.usecase.GetWalletTotalBalanceUseCase
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.wallets.usecase.SelectWalletUseCase
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.features.swap.SwapFeatureToggles
import com.tangem.features.swap.deeplink.SwapDeepLinkHandler
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.math.BigDecimal
import kotlin.time.Duration.Companion.seconds

/**
 * Handles the `tangem://swap` deep link.
 *
 * Toggle OFF ([SwapFeatureToggles.isSwapDeeplinkEnabled]) preserves the v1 behavior: a bare
 * [AppRoute.Swap] is pushed for the currently selected wallet, ignoring all query params.
 *
 * Toggle ON resolves a target wallet from the AI-MCP / broadcast wallet id params (see
 * [resolveTargetWalletId]), switching the selected wallet if needed, then resolves concrete
 * FROM/TO tokens from the wallet's accounts (or leaves them null to let the model degrade — see
 * [resolveToken]), enforces the corner-case gating matrix, applies `from_amount` only for a
 * fully-resolved explicit pair, and pre-checks `provider_id` against the resolved pair's providers
 * (see [findPairProviderIds]) before pushing the final [AppRoute.Swap].
 */
@Suppress("LongParameterList")
internal class DefaultSwapDeepLinkHandler @AssistedInject constructor(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val queryParams: Map<String, String>,
    private val router: AppRouter,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val selectWalletUseCase: SelectWalletUseCase,
    private val getWalletTotalBalanceUseCase: GetWalletTotalBalanceUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val rampStateManager: RampStateManager,
    private val swapInteractor: SwapInteractor,
    private val swapFeatureToggles: SwapFeatureToggles,
) : SwapDeepLinkHandler {

    init {
        handleDeepLink()
    }

    private fun handleDeepLink() {
        val selectedResult = getSelectedWalletSyncUseCase()

        if (!swapFeatureToggles.isSwapDeeplinkEnabled) {
            navigateToSelectedWalletOrReturn(selectedResult)
            return
        }

        scope.launch {
            val targetWalletId = resolveTargetWalletId(selectedResult) ?: return@launch
            resolveTokensAndNavigate(targetWalletId)
        }
    }

    private fun navigateToSelectedWalletOrReturn(selectedResult: Either<GetUserWalletError, UserWallet>) {
        val selectedWalletId = selectedResult.fold(
            ifLeft = { error ->
                TangemLogger.e("Error on getting user wallet: $error")
                null
            },
            ifRight = { wallet -> wallet.walletId },
        ) ?: return
        navigateToSwap(selectedWalletId)
    }

    /**
     * Resolves the wallet the swap should open for.
     *
     * Order: an existing pinned id from `user_wallet_id`/`from_user_wallet_id`/`to_user_wallet_id`
     * (non-existent ids are dropped — degrade, not Main) -> the currently selected wallet ->
     * (cold-start, no selected wallet) the wallet with the largest total fiat balance.
     *
     * A hard inconsistency (two or more distinct *existing* pinned ids) pushes a bare Main [AppRoute.Swap]
     * for the currently selected wallet (or the first pinned id if there is none) and returns `null`.
     * Returns `null` also when there is no wallet to open at all, or when switching to the resolved
     * target wallet fails.
     */
    private suspend fun resolveTargetWalletId(selectedResult: Either<GetUserWalletError, UserWallet>): UserWalletId? {
        val wallets = getWalletsUseCase.invokeSync()
        if (wallets.isEmpty()) return null
        val existingIds = wallets.map { it.walletId }.toSet()

        // AI-MCP / broadcast wallet ids; drop non-existent ones (degrade, not Main)
        val fromWalletId = queryParams[FROM_USER_WALLET_ID_KEY]?.let(::UserWalletId)?.takeIf { it in existingIds }
        val toWalletId = queryParams[TO_USER_WALLET_ID_KEY]?.let(::UserWalletId)?.takeIf { it in existingIds }
        val broadcastWalletId = queryParams[WALLET_ID_KEY]?.let(::UserWalletId)?.takeIf { it in existingIds }

        val selectedWalletId = selectedResult.getOrNull()?.walletId

        // Hard inconsistency: existing pinned ids disagree -> Main
        val pinned = listOfNotNull(fromWalletId, toWalletId, broadcastWalletId).distinct()
        if (pinned.size > 1) {
            navigateToSwap(selectedWalletId ?: pinned.first())
            return null
        }

        val targetWalletId = pinned.firstOrNull()
            ?: selectedWalletId
            ?: pickLargestBalanceWalletId(existingIds) // cold-start
            ?: return null

        if (targetWalletId != selectedWalletId) {
            selectWalletUseCase(targetWalletId).onLeft { error ->
                TangemLogger.e("Swap deeplink: failed to select wallet $targetWalletId: $error")
                navigateToSwap(selectedWalletId ?: targetWalletId)
                return null
            }
        }
        return targetWalletId
    }

    /** Picks the wallet with the largest [TotalFiatBalance.Loaded] amount, for cold-start resolution. */
    private suspend fun pickLargestBalanceWalletId(walletIds: Set<UserWalletId>): UserWalletId? {
        // Wait for a settled emission (at least one wallet balance is Loaded), not the first (Loading) one,
        // still bounded by the timeout below.
        val balances = withTimeoutOrNull(BALANCE_LOOKUP_TIMEOUT_SECONDS.seconds) {
            getWalletTotalBalanceUseCase(walletIds)
                .mapNotNull { it.getOrNull() }
                .firstOrNull { balances -> balances.values.any { it is TotalFiatBalance.Loaded } }
        }.orEmpty()

        return balances.entries
            .mapNotNull { (id, balance) -> (balance as? TotalFiatBalance.Loaded)?.let { id to it.amount } }
            .maxByOrNull { it.second }
            ?.first
            ?: walletIds.firstOrNull()
    }

    private fun navigateToSwap(userWalletId: UserWalletId) {
        router.push(
            AppRoute.Swap(
                userWalletId = userWalletId,
                screenSource = AnalyticsParam.ScreensSources.Main.value,
            ),
        )
    }

    /**
     * Resolves FROM/TO concrete tokens from [targetWalletId]'s accounts (or leaves them `null` to let
     * the model degrade), enforces the corner-case gating matrix (cases 2, 4, 6, 9 push a bare Main
     * [AppRoute.Swap]), applies `from_amount` only when both FROM and TO are explicitly requested AND
     * both resolve to concrete tokens (assumption A2), pre-checks `provider_id` against the resolved
     * pair's providers when a concrete pair exists (§7 of the spec; unavailable -> Main, best-effort
     * dropped otherwise per assumption A3), and pushes the resulting [AppRoute.Swap].
     */
    private suspend fun resolveTokensAndNavigate(targetWalletId: UserWalletId) {
        val isFromRequested = !queryParams[FROM_TOKEN_ID_KEY].isNullOrBlank()
        val isToRequested = !queryParams[TO_TOKEN_ID_KEY].isNullOrBlank()

        // case 2: FROM token without network -> Main
        if (isFromRequested && queryParams[FROM_NETWORK_ID_KEY].isNullOrBlank()) {
            navigateToSwap(targetWalletId)
            return
        }

        // case 1: nothing requested -> bare Swap; cases 6 & 9: amount/provider with no token at all
        // -> Main (inconsistent). Both resolve to the same bare Main route, and skip account loading.
        if (!isFromRequested && !isToRequested) {
            navigateToSwap(targetWalletId)
            return
        }

        val accounts = loadAccountCurrencies(targetWalletId)
        val (from, to) = resolvePair(accounts, isFromRequested, isToRequested)

        // case 4 last row: both explicitly requested but neither resolved in the portfolio -> Main
        if (isUnresolvedPair(isFromRequested = isFromRequested, isToRequested = isToRequested, from = from, to = to)) {
            navigateToSwap(targetWalletId)
            return
        }

        // A2: from_amount is applied only when FROM and TO are both explicitly requested AND both
        // resolved to concrete tokens; otherwise it's dropped (cases 7/8 degrade one side).
        val isConcretePair = isFromRequested && isToRequested && from != null && to != null
        val amount = queryParams[FROM_AMOUNT_KEY]?.toBigDecimalOrNull()?.takeIf { isConcretePair }

        // §7: provider pre-check, only when a concrete pair was resolved.
        val requestedProviderId = queryParams[PROVIDER_ID_KEY]
        val providerId = if (isConcretePair && !requestedProviderId.isNullOrBlank()) {
            val available = findPairProviderIds(requireNotNull(from), requireNotNull(to))
            val matched = available.firstOrNull { it.equals(requestedProviderId, ignoreCase = true) }
            if (matched == null) {
                // provider_id doesn't serve the resolved pair -> Main
                navigateToSwap(targetWalletId)
                return
            }
            matched
        } else {
            // No concrete pair (or no provider requested): best-effort drop (A3). Provider-only
            // (no tokens at all) is already gated to Main above.
            null
        }

        router.push(
            AppRoute.Swap(
                userWalletId = targetWalletId,
                fromCryptoCurrency = from?.currency,
                toCryptoCurrency = to?.currency,
                fromAmount = amount,
                providerId = providerId,
                fromCurrencyPosition = resolvePosition(from, to),
                screenSource = AnalyticsParam.ScreensSources.Main.value,
            ),
        )
    }

    /** Resolves the FROM/TO concrete tokens (or `null` to degrade), TO preferring FROM's account. */
    private fun resolvePair(
        accounts: List<SwapCurrencyStatus>,
        isFromRequested: Boolean,
        isToRequested: Boolean,
    ): Pair<SwapCurrencyStatus?, SwapCurrencyStatus?> {
        val from = if (isFromRequested) {
            resolveToken(
                accounts = accounts,
                tokenId = queryParams[FROM_TOKEN_ID_KEY],
                networkId = queryParams[FROM_NETWORK_ID_KEY],
                accountIdParam = queryParams[FROM_USER_ACCOUNT_ID_KEY],
                preferAccountId = null,
            )
        } else {
            null
        }
        val to = if (isToRequested) {
            resolveToken(
                accounts = accounts,
                tokenId = queryParams[TO_TOKEN_ID_KEY],
                networkId = queryParams[TO_NETWORK_ID_KEY],
                accountIdParam = queryParams[TO_USER_ACCOUNT_ID_KEY],
                preferAccountId = from?.account?.accountId,
            )
        } else {
            null
        }
        return from to to
    }

    /** True when both sides were explicitly requested but neither resolved in the portfolio (case 4). */
    private fun isUnresolvedPair(
        isFromRequested: Boolean,
        isToRequested: Boolean,
        from: SwapCurrencyStatus?,
        to: SwapCurrencyStatus?,
    ): Boolean {
        val isBothRequested = isFromRequested && isToRequested
        val isBothUnresolved = from == null && to == null
        return isBothRequested && isBothUnresolved
    }

    private fun resolvePosition(from: SwapCurrencyStatus?, to: SwapCurrencyStatus?): AppRoute.Swap.CurrencyPosition {
        return when {
            from != null && to != null -> AppRoute.Swap.CurrencyPosition.FROM
            to != null -> AppRoute.Swap.CurrencyPosition.TO
            else -> AppRoute.Swap.CurrencyPosition.ANY
        }
    }

    /** Loads all crypto-portfolio currencies of [walletId]'s accounts, enriched with swap availability. */
    private suspend fun loadAccountCurrencies(walletId: UserWalletId): List<SwapCurrencyStatus> {
        val userWallet = getUserWalletUseCase(walletId).getOrNull() ?: return emptyList()
        val accountList = singleAccountStatusListSupplier.getSyncOrNull(walletId) ?: return emptyList()
        val portfolios = accountList.accountStatuses.filterIsInstance<AccountStatus.CryptoPortfolio>()
        val allCurrencies = portfolios.flatMap { portfolio -> portfolio.flattenCurrencies().map { it.currency } }
        val availability = rampStateManager.availableForSwap(walletId, allCurrencies)

        return portfolios.flatMap { portfolio ->
            portfolio.flattenCurrencies().map { status ->
                SwapCurrencyStatus(
                    userWallet = userWallet,
                    status = status,
                    account = portfolio.account,
                    isAvailableForSwap = availability[status.currency] == ScenarioUnavailabilityReason.None,
                )
            }
        }
    }

    /**
     * Resolves a requested token to a concrete portfolio instance. Scopes to [accountIdParam] when it
     * matches an existing account; prefers [preferAccountId] (FROM's resolved account, for TO
     * resolution); among the remaining candidates picks the most funded one. Returns `null` when the
     * token isn't present in the portfolio (caller lets the model degrade).
     */
    private fun resolveToken(
        accounts: List<SwapCurrencyStatus>,
        tokenId: String?,
        networkId: String?,
        accountIdParam: String?,
        preferAccountId: AccountId?,
    ): SwapCurrencyStatus? {
        if (tokenId.isNullOrBlank() || networkId.isNullOrBlank()) return null

        val matches = accounts.filter { candidate -> candidate.matchesToken(tokenId, networkId) }
        if (matches.isEmpty()) return null

        val scoped = accountIdParam
            ?.let { id -> matches.filter { it.account.accountId.value == id }.ifEmpty { matches } }
            ?: matches
        val preferred = preferAccountId?.let { pref -> scoped.filter { it.account.accountId == pref } }.orEmpty()
        val pool = preferred.ifEmpty { scoped }

        return pool.maxByOrNull { it.status.value.fiatAmount ?: BigDecimal.ZERO }
    }

    /** Matches by network raw id and raw currency id (custom tokens, which have no raw currency id, never match). */
    private fun SwapCurrencyStatus.matchesToken(tokenId: String, networkId: String): Boolean {
        val isNetworkMatch = currency.network.rawId.equals(networkId, ignoreCase = true)
        val rawCurrencyId = currency.id.rawCurrencyId?.value
        val isTokenMatch = rawCurrencyId?.equals(tokenId, ignoreCase = true) == true
        return isNetworkMatch && isTokenMatch
    }

    /**
     * Provider ids that actually serve the [from]->[to] pair. A [SwapInteractor.getPair] failure
     * (e.g. no network) is treated as "no known providers" — the requested `provider_id` can't be
     * confirmed, so it falls back to Main just like an unavailable provider (see call site).
     */
    private suspend fun findPairProviderIds(from: SwapCurrencyStatus, to: SwapCurrencyStatus): List<String> {
        val pairs = swapInteractor.getPair(
            fromSwapCurrencyStatus = from,
            toSwapCurrencyStatus = to,
            filterProviderTypes = ExchangeProviderType.getSwapProviderTypes(),
        ).getOrNull().orEmpty()
        return swapInteractor.findProvidersForPairWithCheck(from, to, pairs).map { it.providerId }
    }

    @AssistedFactory
    interface Factory : SwapDeepLinkHandler.Factory {
        override fun create(
            coroutineScope: CoroutineScope,
            queryParams: Map<String, String>,
        ): DefaultSwapDeepLinkHandler
    }

    private companion object {
        /** Max time to wait for [GetWalletTotalBalanceUseCase] before falling back to the first wallet. */
        const val BALANCE_LOOKUP_TIMEOUT_SECONDS = 5
    }
}