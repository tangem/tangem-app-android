package com.tangem.features.send.v2.deeplink

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.domain.tokens.GetCryptoCurrencyUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.send.v2.api.deeplink.SellDeepLinkHandler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("ComplexCondition")
internal class DefaultSellDeepLinkHandler @AssistedInject constructor(
    @Assisted scope: CoroutineScope,
    @Assisted params: Map<String, String>,
    appRouter: AppRouter,
    getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    getCryptoCurrencyUseCase: GetCryptoCurrencyUseCase,
) : SellDeepLinkHandler {

    init {
        val currencyId = params[CURRENCY_ID_KEY]
        val transactionId = params[TRANSACTION_ID_KEY]
        val amount = params[AMOUNT_KEY]
        val destinationAddress = params[DESTINATION_ADDRESS_KEY]
        val memo = params[MEMO_KEY]

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
                               |- Params: $params
                            """.trimIndent(),
                        )
                        return@fold
                    }

                    scope.launch {
                        val cryptoCurrency = getCryptoCurrencyUseCase(userWallet, currencyId).getOrElse {
                            Timber.e("Error on getting cryptoCurrency: $it")
                            return@launch
                        }

                        appRouter.push(
                            AppRoute.Send(
                                currency = cryptoCurrency,
                                userWalletId = userWallet.walletId,
                                transactionId = transactionId,
                                destinationAddress = destinationAddress,
                                amount = amount,
                                tag = memo,
                            ),
                        )
                    }
                },
            )
    }

    @AssistedFactory
    interface Factory : SellDeepLinkHandler.Factory {
        override fun create(coroutineScope: CoroutineScope, params: Map<String, String>): DefaultSellDeepLinkHandler
    }

    private companion object {
        const val TRANSACTION_ID_KEY = "transactionId"
        const val CURRENCY_ID_KEY = "currency_id"
        const val AMOUNT_KEY = "baseCurrencyAmount"
        const val DESTINATION_ADDRESS_KEY = "depositWalletAddress"
        const val MEMO_KEY = "depositWalletAddressTag"
    }
}