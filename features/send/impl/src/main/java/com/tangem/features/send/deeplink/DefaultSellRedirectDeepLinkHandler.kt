package com.tangem.features.send.deeplink

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.raise.option
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.account.status.utils.CryptoCurrencyOperations.getCryptoCurrency
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.offramp.repository.OfframpRepository
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.send.api.deeplink.SellRedirectDeepLinkHandler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.tangem.utils.logging.TangemLogger

@Suppress("ComplexCondition", "LongParameterList")
internal class DefaultSellRedirectDeepLinkHandler @AssistedInject constructor(
    @Assisted scope: CoroutineScope,
    @Assisted queryParams: Map<String, String>,
    appRouter: AppRouter,
    getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val offrampRepository: OfframpRepository,
) : SellRedirectDeepLinkHandler {

    init {
        val currencyId = queryParams[CURRENCY_ID_KEY]
        val transactionId = queryParams[TRANSACTION_ID_KEY]
        val amount = queryParams[AMOUNT_KEY]
        val destinationAddress = queryParams[DESTINATION_ADDRESS_KEY]
        val memo = queryParams[MEMO_KEY]
        val requestId = queryParams[REQUEST_ID_KEY]

        // It is okay here, we are navigating from outside, and there is no other way to getting UserWallet
        getSelectedWalletSyncUseCase()
            .fold(
                ifLeft = {
                    TangemLogger.e("Error on getting user wallet: $it")
                },
                ifRight = { userWallet ->
                    if (currencyId.isNullOrEmpty() || transactionId.isNullOrEmpty() ||
                        amount.isNullOrEmpty() || destinationAddress.isNullOrEmpty() ||
                        requestId.isNullOrEmpty()
                    ) {
                        // Do not log the params: they contain the deposit address and request_id.
                        TangemLogger.e("Invalid parameters for SELL deeplink")
                        return@fold
                    }

                    scope.launch {
                        // Only trust the redirect if it carries a request_id we issued for a sell this
                        // app actually started (single-use, bound to the wallet + currency). Otherwise an external
                        // deeplink could inject a locked attacker recipient/amount into the Send confirm screen.
                        val pendingOfframp = offrampRepository.consumePendingOfframp(
                            requestId = requestId,
                            userWalletId = userWallet.walletId,
                            currencyId = currencyId,
                        )
                        if (pendingOfframp == null) {
                            TangemLogger.e("Rejected SELL deeplink: no matching app-initiated sell")
                            return@launch
                        }

                        val cryptoCurrency = getCryptoCurrency(userWallet.walletId, currencyId).getOrElse {
                            TangemLogger.e("Error on getting cryptoCurrency for SELL deeplink")
                            return@launch
                        }
                        // Convert using universal parser to account for regional separators
                        val amountValue = amount.parseBigDecimalOrNull()?.parseBigDecimal(cryptoCurrency.decimals)
                        appRouter.push(
                            AppRoute.Send(
                                currency = cryptoCurrency,
                                userWalletId = userWallet.walletId,
                                transactionId = transactionId,
                                destinationAddress = destinationAddress,
                                amount = amountValue,
                                tag = memo,
                            ),
                        )
                    }
                },
            )
    }

    private suspend fun getCryptoCurrency(userWalletId: UserWalletId, currencyId: String): Option<CryptoCurrency> =
        option {
            val accountStatusList = singleAccountListSupplier.getSyncOrNull(userWalletId)

            ensureNotNull(accountStatusList)

            return accountStatusList.getCryptoCurrency(currencyIdValue = currencyId)
        }

    @AssistedFactory
    interface Factory : SellRedirectDeepLinkHandler.Factory {
        override fun create(
            coroutineScope: CoroutineScope,
            queryParams: Map<String, String>,
        ): DefaultSellRedirectDeepLinkHandler
    }

    private companion object {
        const val TRANSACTION_ID_KEY = "transactionId"
        const val CURRENCY_ID_KEY = "currency_id"
        const val AMOUNT_KEY = "baseCurrencyAmount"
        const val DESTINATION_ADDRESS_KEY = "depositWalletAddress"
        const val MEMO_KEY = "depositWalletAddressTag"
        const val REQUEST_ID_KEY = "request_id"
    }
}