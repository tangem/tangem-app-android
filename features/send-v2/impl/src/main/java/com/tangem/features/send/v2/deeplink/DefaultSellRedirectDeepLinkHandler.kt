package com.tangem.features.send.v2.deeplink

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.tokens.GetCryptoCurrencyUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.send.v2.api.deeplink.SellRedirectDeepLinkHandler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("ComplexCondition")
internal class DefaultSellRedirectDeepLinkHandler @AssistedInject constructor(
    @Assisted scope: CoroutineScope,
    @Assisted queryParams: Map<String, String>,
    appRouter: AppRouter,
    getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    getCryptoCurrencyUseCase: GetCryptoCurrencyUseCase,
) : SellRedirectDeepLinkHandler {

    init {
        val currencyId = queryParams[CURRENCY_ID_KEY]
        val transactionId = queryParams[TRANSACTION_ID_KEY]
        val amount = queryParams[AMOUNT_KEY]
        val destinationAddress = queryParams[DESTINATION_ADDRESS_KEY]
        val memo = queryParams[MEMO_KEY]

        // It is okay here, we are navigating from outside, and there is no other way to getting UserWallet
        getSelectedWalletSyncUseCase()
            .fold(
                ifLeft = {
                    Timber.e("Error on getting user wallet: $it")
                },
                ifRight = { userWallet ->
                    if (currencyId.isNullOrEmpty() || transactionId.isNullOrEmpty() ||
                        amount.isNullOrEmpty() || destinationAddress.isNullOrEmpty()
                    ) {
                        Timber.e(
                            """
                               Invalid parameters for SELL deeplink
                               |- Params: $queryParams
                            """.trimIndent(),
                        )
                        return@fold
                    }

                    scope.launch {
                        val cryptoCurrency = getCryptoCurrencyUseCase(userWallet, currencyId).getOrElse {
                            Timber.e("Error on getting cryptoCurrency: $it")
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
    }
}