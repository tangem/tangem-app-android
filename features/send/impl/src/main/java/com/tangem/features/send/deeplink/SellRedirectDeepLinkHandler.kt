package com.tangem.features.send.deeplink

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.raise.option
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.account.status.utils.CryptoCurrencyOperations.getCryptoCurrency
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.offramp.repository.OfframpRepository
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import kotlinx.coroutines.launch
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class SellRedirectDeepLinkHandler @Inject constructor(
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val offrampRepository: OfframpRepository,
) : DeepLinkHandler {

    override val isOpenableFromPush: Boolean = false

    @Suppress("ComplexCondition")
    override fun handle(request: DeepLinkRequest) {
        val queryParams = request.queryParams
        val currencyId = queryParams[CURRENCY_ID_KEY]
        val transactionId = queryParams[TRANSACTION_ID_KEY]
        // Universal parser accounts for regional separators
        val amount = queryParams[AMOUNT_KEY]?.parseBigDecimalOrNull()?.takeIf { it.signum() > 0 }
        val destinationAddress = queryParams[DESTINATION_ADDRESS_KEY]
        val memo = queryParams[MEMO_KEY]
        val requestId = queryParams[REQUEST_ID_KEY]

        getSelectedWalletSyncUseCase()
            .fold(
                ifLeft = {
                    TangemLogger.e("Error on getting user wallet: $it")
                },
                ifRight = { userWallet ->
                    if (currencyId.isNullOrEmpty() || transactionId.isNullOrEmpty() ||
                        amount == null || destinationAddress.isNullOrEmpty() ||
                        requestId.isNullOrEmpty()
                    ) {
                        // Do not log the params: they contain the deposit address and request_id.
                        TangemLogger.e("Invalid parameters for SELL deeplink")
                        return@fold
                    }

                    request.scope.launch {
                        // Only trust the redirect if it carries a request_id we issued for a sell this
                        // app actually started (bound to the wallet + currency, valid until it expires). Otherwise an
                        // external deeplink could inject a locked attacker recipient/amount into the Send confirm
                        // screen. The record is kept until expiry so the user can re-open the redirect within that
                        // window.
                        val pendingOfframp = offrampRepository.resolvePendingOfframp(
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
                        request.router.push(
                            AppRoute.Send(
                                currency = cryptoCurrency,
                                userWalletId = userWallet.walletId,
                                transactionId = transactionId,
                                destinationAddress = destinationAddress,
                                amount = amount.parseBigDecimal(cryptoCurrency.decimals),
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

    companion object {
        const val DEEPLINK_KEY = "tangem://redirect_sell"

        private const val TRANSACTION_ID_KEY = "transactionId"
        private const val CURRENCY_ID_KEY = "currency_id"
        private const val AMOUNT_KEY = "baseCurrencyAmount"
        private const val DESTINATION_ADDRESS_KEY = "depositWalletAddress"
        private const val MEMO_KEY = "depositWalletAddressTag"
        private const val REQUEST_ID_KEY = "request_id"
    }
}