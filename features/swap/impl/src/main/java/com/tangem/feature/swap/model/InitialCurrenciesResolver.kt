package com.tangem.feature.swap.model

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
) {

    /**
     * Resolves the initial FROM/TO currency pair for the swap screen.
     *
     * @param userWalletId the wallet to resolve currencies for
     * @param initialCryptoCurrency pre-selected currency, or null to auto-select
     * @param swapCurrencyPosition preferred position for the initial currency
     * @return pair of (from, to) [SwapCurrencyStatus]; either or both may be null
     */
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        initialCryptoCurrency: CryptoCurrency?,
        swapCurrencyPosition: CurrencyPosition,
        isPaymentAccount: Boolean,
    ): Pair<SwapCurrencyStatus?, SwapCurrencyStatus?> {
        val walletAccountList = getWalletAccountCurrencyStatusList(userWalletId)
        val cryptoPortfolioAccounts = walletAccountList.filterKeys { accountStatus ->
            accountStatus is AccountStatus.CryptoPortfolio
        }.mapKeys { (key, _) -> key as AccountStatus.CryptoPortfolio }
        val cryptoPaymentAccounts = walletAccountList.filterKeys { accountStatus ->
            accountStatus is AccountStatus.Payment
        }

        val cryptoCurrencyList = cryptoPortfolioAccounts.values.flatten()

        return if (initialCryptoCurrency != null) {
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
    }

    /**
     * Builds a map of [AccountStatus] to their [SwapCurrencyStatus] lists,
     * enriching each currency with its swap availability from [RampStateManager].
     */
    private suspend fun getWalletAccountCurrencyStatusList(
        userWalletId: UserWalletId,
    ): Map<AccountStatus, List<SwapCurrencyStatus>> {
        val userWallet = getUserWalletUseCase(userWalletId).getOrNull() ?: return emptyMap()

        val walletAccountCurrencyStatuses = singleAccountStatusListSupplier.getSyncOrNull(
            SingleAccountStatusListProducer.Params(userWalletId),
        )?.accountStatuses.orEmpty()

        return walletAccountCurrencyStatuses.associateWith { accountStatus ->
            val currencyStatuses = when (accountStatus) {
                is AccountStatus.CryptoPortfolio -> accountStatus.flattenCurrencies()
                is AccountStatus.Payment -> getPaymentAccountCurrencies(accountStatus)
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

    private fun getPaymentAccountCurrencies(accountStatus: AccountStatus.Payment): List<CryptoCurrencyStatus> {
        val paymentCryptoCurrencyStatus = when (val statusValue = accountStatus.value) {
            is PaymentAccountStatusValue.Loaded -> statusValue.cryptoCurrencyStatus
            is PaymentAccountStatusValue.Deactivated -> statusValue.cryptoCurrencyStatus
            else -> null
        }

        return listOfNotNull(paymentCryptoCurrencyStatus)
    }

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
                val hasBalance = !selectedSwapCurrencyStatus.status.value.fiatAmount.isNullOrZero()
                if (isAvailable && hasBalance) {
                    selectedSwapCurrencyStatus to null
                } else if (isAvailable || !hasBalance) {
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