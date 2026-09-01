package com.tangem.feature.swap.model

import com.tangem.common.routing.AppRoute.Swap.AccountFlow
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.swap.domain.account.AccountUnderlyingCurrencies
import com.tangem.features.swap.SwapComponent.Params.CurrencyPosition
import com.tangem.utils.extensions.orZero
import com.tangem.utils.isNullOrZero
import javax.inject.Inject

/**
 * Resolves the initial FROM and TO currencies when the swap screen opens.
 *
 * Selection rules when [initialCryptoCurrency][CryptoCurrency] is provided:
 * - [CurrencyPosition.FROM] — places the currency as FROM, TO is null.
 * - [CurrencyPosition.TO] — places the currency as TO, FROM is null.
 * - [CurrencyPosition.ANY] — auto-places based on availability and balance:
 *   - available with balance → FROM.
 *   - available without balance or unavailable without balance → TO,
 *     and the best candidate from the SAME account as the initial currency is selected as FROM
 *     (the search is scoped to that account only, not the whole portfolio).
 *   - unavailable with balance → FROM.
 *
 * When no initial currency is provided, selects the best token from crypto portfolio accounts:
 * 1. If available tokens with balance exist — the available token with the highest fiat balance.
 * 2. If available tokens exist but none have balance — the first token from the first account.
 * 3. If no available tokens exist but tokens with balance exist — the token with the highest fiat balance.
 * 4. If no tokens have balance — the first token from the first account.
 */
internal class InitialCurrenciesResolver @Inject constructor(
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val rampStateManager: RampStateManager,
    private val accountUnderlyingCurrencies: AccountUnderlyingCurrencies,
) {

    /**
     * Orders currencies by the funds they hold: by fiat value first, then by the crypto amount — the only
     * dimension left for a currency whose quote has not arrived, since it reports no fiat value at all.
     */
    private val fundsComparator = compareBy<SwapCurrencyStatus>(
        { it.status.value.fiatAmount.orZero() },
        { it.status.value.amount.orZero() },
    )

    /**
     * Whether the currency holds funds. The crypto amount alone counts: a currency with a balance but without
     * a quote yet has no fiat value, and skipping it would silently hand the slot to an unrelated token.
     */
    private val SwapCurrencyStatus.hasBalance: Boolean
        get() = !status.value.fiatAmount.isNullOrZero() || !status.value.amount.isNullOrZero()

    /**
     * Resolves the initial FROM/TO currency pair for the swap screen.
     *
     * @param userWalletId the wallet to resolve currencies for
     * @param initialCryptoCurrency pre-selected currency, or null to auto-select
     * @param swapCurrencyPosition preferred position for the initial currency
     * @param accountFlow non-null when the swap screen was opened for an account top-up/withdraw flow;
     *  `null` for a regular swap. Determines both where [initialCryptoCurrency] is looked up
     *  (payment accounts vs. crypto portfolio accounts) and, combined with [isAccountFlowEnabled],
     *  which account FROM auto-fill priority applies.
     * @param initialToCryptoCurrency optional currency to pre-select as TO. It is placed into the TO slot
     *  ONLY if it already exists in the user's crypto portfolio (and the TO slot wasn't filled otherwise);
     *  if the currency is not added to the wallet, the TO slot stays empty.
     * @param isAccountFlowEnabled the account-swap-flow toggle. Decides which currencies a payment account
     *  contributes (its issued networks vs. its single legacy currency) and overrides the resolved FROM with
     *  the account flow's own priority. For [AccountFlow.TopUp]: (a) the most-funded wallet token that is one of the account's own
     *  underlying currencies ([AccountUnderlyingCurrencies]), if it has a balance — this turns the operation
     *  into a same-wallet transfer; (b) otherwise the regular most-funded wallet token; (c) otherwise `null`
     *  (falls through to "Choose token"). For [AccountFlow.Withdraw]: the account's most-funded token.
     * @return pair of (from, to) [SwapCurrencyStatus]; either or both may be null
     */
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        initialCryptoCurrency: CryptoCurrency?,
        swapCurrencyPosition: CurrencyPosition,
        accountFlow: AccountFlow?,
        initialToCryptoCurrency: CryptoCurrency? = null,
        isAccountFlowEnabled: Boolean = false,
    ): Pair<SwapCurrencyStatus?, SwapCurrencyStatus?> {
        val isPaymentAccount = accountFlow != null
        val walletAccountList = getWalletAccountCurrencyStatusList(userWalletId, isAccountFlowEnabled)
        val cryptoPortfolioAccounts = walletAccountList.filterKeys { accountStatus ->
            accountStatus is AccountStatus.CryptoPortfolio
        }.mapKeys { (key, _) -> key as AccountStatus.CryptoPortfolio }
        val cryptoPaymentAccounts = walletAccountList.filterKeys { accountStatus ->
            accountStatus is AccountStatus.Payment
        }

        val cryptoCurrencyList = cryptoPortfolioAccounts.values.flatten()

        val (from, to) = if (initialCryptoCurrency != null) {
            val selectedSwapCurrencyStatus = if (isPaymentAccount) {
                cryptoPaymentAccounts
            } else {
                cryptoPortfolioAccounts
            }.firstNotNullOfOrNull { (_, currencyList) ->
                currencyList.firstOrNull { currencyStatus ->
                    currencyStatus.currency.id == initialCryptoCurrency.id
                }
            }

            if (selectedSwapCurrencyStatus == null) {
                null to null
            } else {
                placeSelectedCurrency(
                    selectedSwapCurrencyStatus = selectedSwapCurrencyStatus,
                    swapCurrencyPosition = swapCurrencyPosition,
                    cryptoPortfolioAccountsMap = cryptoPortfolioAccounts,
                )
            }
        } else {
            selectCryptoCurrency(
                cryptoPortfolioAccountsMap = cryptoPortfolioAccounts,
                cryptoCurrencyList = cryptoCurrencyList,
            ) to null
        }

        val resolvedTo = to ?: resolveExplicitToCurrency(
            initialToCryptoCurrency = initialToCryptoCurrency,
            from = from,
            cryptoPortfolioAccountsMap = cryptoPortfolioAccounts,
        )

        val prioritizedFrom = when {
            !isAccountFlowEnabled -> from
            accountFlow is AccountFlow.TopUp -> resolveAccountTopUpFromPriority(
                userWalletId = userWalletId,
                cryptoPortfolioAccounts = cryptoPortfolioAccounts,
                cryptoCurrencyList = cryptoCurrencyList,
            )
            accountFlow is AccountFlow.Withdraw -> mostFundedAccountCurrency(cryptoPaymentAccounts) ?: from
            else -> from
        }

        // Add funds anchors TO to the account, but passes in its legacy currency, which a multichain account
        // may not hold: fall back to the account's own tokens instead of leaving the slot empty.
        val anchoredTo = if (isAccountFlowEnabled && accountFlow is AccountFlow.TopUp) {
            resolvedTo ?: mostFundedAccountCurrency(cryptoPaymentAccounts)
        } else {
            resolvedTo
        }

        return prioritizedFrom to anchoredTo
    }

    /**
     * The account's own token holding the most funds, preferring one the user can actually swap. Used wherever
     * the entry point's currency is only an intent: it is the account's hardcoded legacy currency, which a
     * multichain account may not hold at all.
     */
    private fun mostFundedAccountCurrency(
        cryptoPaymentAccounts: Map<AccountStatus, List<SwapCurrencyStatus>>,
    ): SwapCurrencyStatus? = cryptoPaymentAccounts.values.flatten()
        .maxWithOrNull(compareBy<SwapCurrencyStatus> { it.isAvailableForSwap }.then(fundsComparator))

    /**
     * Account top-up FROM auto-fill priority: (a) the most-funded wallet token that is one of the
     * account's own underlying currencies, if it holds a balance (the swap then becomes a same-wallet
     * transfer); (b) otherwise the regular most-funded wallet token; (c) otherwise `null`.
     */
    private suspend fun resolveAccountTopUpFromPriority(
        userWalletId: UserWalletId,
        cryptoPortfolioAccounts: Map<AccountStatus.CryptoPortfolio, List<SwapCurrencyStatus>>,
        cryptoCurrencyList: List<SwapCurrencyStatus>,
    ): SwapCurrencyStatus? {
        val hood = accountUnderlyingCurrencies.get(userWalletId)
        val hoodMatch = cryptoCurrencyList
            .filter { walletToken -> hood.any { it.currency.isSameTokenAs(walletToken.currency) } }
            .filter { it.hasBalance }
            .maxWithOrNull(fundsComparator)

        return hoodMatch ?: selectCryptoCurrency(
            cryptoPortfolioAccountsMap = cryptoPortfolioAccounts,
            cryptoCurrencyList = cryptoCurrencyList,
        )
    }

    /**
     * Resolves the optional explicit TO currency, but only if it is already present in the user's crypto
     * portfolio. Matches by token identity ([isSameTokenAs]) rather than full id, since the passed currency
     * may come from a different account/derivation. Prefers the instance from the FROM account, then falls
     * back to the first match across the portfolio. Never returns the same token as FROM.
     */
    private fun resolveExplicitToCurrency(
        initialToCryptoCurrency: CryptoCurrency?,
        from: SwapCurrencyStatus?,
        cryptoPortfolioAccountsMap: Map<AccountStatus.CryptoPortfolio, List<SwapCurrencyStatus>>,
    ): SwapCurrencyStatus? {
        if (initialToCryptoCurrency == null) return null

        val fromCurrency = from?.currency
        fun matches(status: SwapCurrencyStatus): Boolean {
            return status.currency.isSameTokenAs(initialToCryptoCurrency) &&
                (fromCurrency == null || !status.currency.isSameTokenAs(fromCurrency))
        }

        val fromAccountMatch = from?.account?.accountId?.let { fromAccountId ->
            cryptoPortfolioAccountsMap.entries
                .firstOrNull { (accountStatus, _) -> accountStatus.account.accountId == fromAccountId }
                ?.value
                ?.firstOrNull(::matches)
        }

        return fromAccountMatch ?: cryptoPortfolioAccountsMap.values.flatten().firstOrNull(::matches)
    }

    /**
     * Builds a map of [AccountStatus] to their [SwapCurrencyStatus] lists,
     * enriching each currency with its swap availability from [RampStateManager].
     */
    private suspend fun getWalletAccountCurrencyStatusList(
        userWalletId: UserWalletId,
        isAccountFlowEnabled: Boolean,
    ): Map<AccountStatus, List<SwapCurrencyStatus>> {
        val userWallet = getUserWalletUseCase(userWalletId).getOrNull() ?: return emptyMap()

        val walletAccountCurrencyStatuses = singleAccountStatusListSupplier.getSyncOrNull(
            SingleAccountStatusListProducer.Params(userWalletId),
        )?.accountStatuses.orEmpty()

        return walletAccountCurrencyStatuses.associateWith { accountStatus ->
            val currencyStatuses = when (accountStatus) {
                is AccountStatus.CryptoPortfolio -> accountStatus.flattenCurrencies()
                is AccountStatus.Payment -> getPaymentAccountCurrencies(accountStatus, isAccountFlowEnabled)
                // Virtual account isn't a swap source in the MVP (withdrawal reuses the send flow)
                is AccountStatus.Virtual -> emptyList()
                // Prediction account isn't a swap source either — the same MVP rule
                is AccountStatus.Prediction -> emptyList()
                // Joint currencies live on the Safe contract and never swap through the participant's EOA
                is AccountStatus.Joint -> emptyList()
            }
            val availabilityStates = rampStateManager.availableForSwap(
                userWalletId,
                currencyStatuses.map { it.currency },
            )
            currencyStatuses.map { cryptoCurrencyStatus ->
                SwapCurrencyStatus(
                    userWallet = userWallet,
                    account = accountStatus.account,
                    status = cryptoCurrencyStatus,
                    isAvailableForSwap = availabilityStates[cryptoCurrencyStatus.currency] ==
                        ScenarioUnavailabilityReason.None,
                )
            }
        }
    }

    /**
     * Every currency the payment account is issued on — the same set the token selector lists — or, while the
     * account flow is off, only the account's legacy currency, which is what the pre-multichain screens resolve
     * against. The issued set collapses to that same legacy currency when the account has no issued networks.
     */
    private fun getPaymentAccountCurrencies(
        accountStatus: AccountStatus.Payment,
        isAccountFlowEnabled: Boolean,
    ): List<CryptoCurrencyStatus> {
        return when (val statusValue = accountStatus.value) {
            is PaymentAccountStatusValue.Loaded -> statusValue.currencies(isAccountFlowEnabled)
            is PaymentAccountStatusValue.Deactivated -> statusValue.currencies(isAccountFlowEnabled)
            else -> emptyList()
        }
    }

    private fun PaymentAccountStatusValue.Loaded.currencies(isAccountFlowEnabled: Boolean) =
        if (isAccountFlowEnabled) cryptoCurrencyStatuses else listOfNotNull(cryptoCurrencyStatus)

    private fun PaymentAccountStatusValue.Deactivated.currencies(isAccountFlowEnabled: Boolean) =
        if (isAccountFlowEnabled) cryptoCurrencyStatuses else listOfNotNull(cryptoCurrencyStatus)

    /**
     * Places the [selectedSwapCurrencyStatus] into the FROM or TO slot based on [swapCurrencyPosition].
     *
     * For [CurrencyPosition.ANY], the position is determined by availability and balance:
     * currencies that are available with balance go to FROM; otherwise, the selected currency
     * goes to TO and a best-candidate FROM is resolved via [selectCryptoCurrency] — scoped to the
     * SAME account that the selected currency belongs to, so we never pull a FROM candidate from a
     * different account in the portfolio.
     */
    private fun placeSelectedCurrency(
        selectedSwapCurrencyStatus: SwapCurrencyStatus,
        swapCurrencyPosition: CurrencyPosition,
        cryptoPortfolioAccountsMap: Map<AccountStatus.CryptoPortfolio, List<SwapCurrencyStatus>>,
    ): Pair<SwapCurrencyStatus?, SwapCurrencyStatus?> {
        return when (swapCurrencyPosition) {
            CurrencyPosition.FROM -> {
                selectedSwapCurrencyStatus to null
            }
            CurrencyPosition.TO -> {
                null to selectedSwapCurrencyStatus
            }
            CurrencyPosition.ANY -> {
                val isAvailable = selectedSwapCurrencyStatus.isAvailableForSwap
                val hasFiatBalance = !selectedSwapCurrencyStatus.status.value.fiatAmount.isNullOrZero()
                if (isAvailable && hasFiatBalance) {
                    selectedSwapCurrencyStatus to null
                } else if (isAvailable || !hasFiatBalance) {
                    val selectedCurrency = selectedSwapCurrencyStatus.currency
                    val selectedAccountId = selectedSwapCurrencyStatus.account.accountId
                    val sameAccountEntry = cryptoPortfolioAccountsMap.entries
                        .firstOrNull { (accountStatus, _) -> accountStatus.account.accountId == selectedAccountId }

                    if (sameAccountEntry == null) {
                        null to selectedSwapCurrencyStatus
                    } else {
                        val scopedList = sameAccountEntry.value
                            .filterNot { it.currency.isSameTokenAs(selectedCurrency) }
                        selectCryptoCurrency(
                            cryptoPortfolioAccountsMap = mapOf(sameAccountEntry.key to scopedList),
                            cryptoCurrencyList = scopedList,
                        ) to selectedSwapCurrencyStatus
                    }
                } else {
                    selectedSwapCurrencyStatus to null
                }
            }
        }
    }

    /**
     * Checks whether two currencies refer to the same asset on the same network, regardless of the
     * owning account. Two instances of the same token in different accounts have distinct
     * [CryptoCurrency.ID] values (their derivation path differs), so id equality is not sufficient
     * to detect duplicates when auto-picking a FROM candidate.
     */
    private fun CryptoCurrency.isSameTokenAs(other: CryptoCurrency): Boolean {
        return id.rawNetworkId == other.id.rawNetworkId &&
            id.contractAddress == other.id.contractAddress
    }

    /**
     * Selects the best token from the crypto portfolio when no initial currency is specified.
     *
     * Prioritizes available-for-swap tokens. Among the candidates, picks the one with the highest
     * [fiatAmount][CryptoCurrencyStatus.Value.fiatAmount]. Falls back to the first token from the
     * first account if no candidate has a positive balance.
     */
    private fun selectCryptoCurrency(
        cryptoPortfolioAccountsMap: Map<AccountStatus.CryptoPortfolio, List<SwapCurrencyStatus>>,
        cryptoCurrencyList: List<SwapCurrencyStatus>,
    ): SwapCurrencyStatus? {
        return if (cryptoCurrencyList.isEmpty()) {
            null
        } else {
            val hasAvailable = cryptoCurrencyList.any { it.isAvailableForSwap }
            val candidates = if (hasAvailable) {
                cryptoCurrencyList.filter { it.isAvailableForSwap }
            } else {
                cryptoCurrencyList
            }
            candidates
                .filter { !it.status.value.fiatAmount.isNullOrZero() }
                .maxByOrNull { it.status.value.fiatAmount.orZero() }
                ?: cryptoPortfolioAccountsMap.entries.firstOrNull()?.value?.firstOrNull()
        }
    }
}