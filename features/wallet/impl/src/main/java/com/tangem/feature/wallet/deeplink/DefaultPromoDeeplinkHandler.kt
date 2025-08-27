package com.tangem.feature.wallet.deeplink

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.routing.deeplink.DeeplinkConst.PROMO_CODE_KEY
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.mask
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.GlobalLoadingMessage
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.GetMultiCryptoCurrencyStatusUseCase
import com.tangem.domain.wallets.PromoCodeActivationResult
import com.tangem.domain.wallets.PromoCodeActivationResult.*
import com.tangem.domain.wallets.models.errors.ActivatePromoCodeError
import com.tangem.domain.wallets.usecase.ActivateBitcoinPromocodeUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.wallet.deeplink.analytics.PromoActivationAnalytics
import com.tangem.feature.wallet.impl.R
import com.tangem.features.wallet.deeplink.PromoDeeplinkHandler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("LongParameterList")
internal class DefaultPromoDeeplinkHandler @AssistedInject constructor(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val queryParams: Map<String, String>,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
    private val getMultiCryptoCurrencyStatusUseCase: GetMultiCryptoCurrencyStatusUseCase,
    private val activateBitcoinPromocodeUseCase: ActivateBitcoinPromocodeUseCase,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val analyticsEventsHandler: AnalyticsEventHandler,
) : PromoDeeplinkHandler {

    init {
        analyticsEventsHandler.send(PromoActivationAnalytics.PromoDeepLinkActivationStart)
        val promoCode = queryParams[PROMO_CODE_KEY].orEmpty()
        if (promoCode.isEmpty()) {
            showAlert(InvalidPromoCode)
        } else {
            findSelectedWallet(promoCode)
        }
    }

    private fun findSelectedWallet(promoCode: String) {
        getSelectedWalletSyncUseCase().fold(
            ifLeft = {
                Timber.tag(LOG_TAG).e("Error on getting user wallet: $it")
                showAlert(Failed)
            },
            ifRight = { userWallet ->
                Timber.tag(LOG_TAG).d("SelectedUserWallet ${userWallet.walletId.stringValue.mask()}")
                findBitcoinAddress(userWallet = userWallet, promoCode = promoCode)
            },
        )
    }

    private fun findBitcoinAddress(userWallet: UserWallet, promoCode: String) {
        scope.launch {
            getMultiCryptoCurrencyStatusUseCase.invokeMultiWalletSync(userWallet.walletId).onRight { currencies ->
                val bitcoinAddress = currencies.find { it.currency.id.rawNetworkId == Blockchain.Bitcoin.id }
                    ?.value
                    ?.networkAddress
                    ?.defaultAddress
                    ?.value

                if (bitcoinAddress != null) {
                    Timber.tag(LOG_TAG).d(
                        "Start activation promoCode ${promoCode.mask()} address ${bitcoinAddress.mask()}",
                    )
                    activatePromoCode(bitcoinAddress = bitcoinAddress, promoCode = promoCode)
                } else {
                    Timber.tag(LOG_TAG).d("no Bitcoin address")
                    showAlert(NoBitcoinAddress)
                }
            }.onLeft {
                Timber.tag(LOG_TAG).e("Error on getting userWallet currencies: $it")
                showAlert(Failed)
            }
        }
    }

    private suspend fun activatePromoCode(bitcoinAddress: String, promoCode: String) {
        uiMessageSender.send(GlobalLoadingMessage(true))
        activateBitcoinPromocodeUseCase(bitcoinAddress, promoCode).onRight {
            uiMessageSender.send(GlobalLoadingMessage(false))
            delay(DEFAULT_MESSAGE_SENDER_DELAY)
            Timber.tag(LOG_TAG).d("${promoCode.mask()} activation success on address ${bitcoinAddress.mask()}")
            showAlert(Activated)
        }.onLeft { error ->
            uiMessageSender.send(GlobalLoadingMessage(false))
            delay(DEFAULT_MESSAGE_SENDER_DELAY)
            Timber.tag(LOG_TAG).d("${promoCode.mask()} activation failed $error")
            val alertType = when (error) {
                ActivatePromoCodeError.ActivationFailed -> Failed
                ActivatePromoCodeError.InvalidPromoCode -> InvalidPromoCode
                ActivatePromoCodeError.NoBitcoinAddress -> NoBitcoinAddress
                ActivatePromoCodeError.PromocodeAlreadyUsed -> PromoCodeAlreadyUsed
            }
            showAlert(alertType)
        }
    }

    private fun showAlert(type: PromoCodeActivationResult) {
        analyticsEventsHandler.send(PromoActivationAnalytics.PromoActivation(type))
        val (title, message) = when (type) {
            Failed -> resourceReference(R.string.bitcoin_promo_activation_error_title) to
                resourceReference(R.string.bitcoin_promo_activation_error)
            InvalidPromoCode -> resourceReference(R.string.bitcoin_promo_invalid_code_title) to
                resourceReference(R.string.bitcoin_promo_invalid_code)
            NoBitcoinAddress -> resourceReference(R.string.bitcoin_promo_no_address_title) to
                resourceReference(R.string.bitcoin_promo_no_address)
            PromoCodeAlreadyUsed -> resourceReference(R.string.bitcoin_promo_already_activated_title) to
                resourceReference(R.string.bitcoin_promo_already_activated)
            Activated -> resourceReference(R.string.bitcoin_promo_activation_success_title) to
                resourceReference(R.string.bitcoin_promo_activation_success)
        }
        uiMessageSender.send(
            DialogMessage(
                title = title,
                message = message,
                dismissOnFirstAction = true,
                firstActionBuilder = {
                    okAction { }
                },
            ),
        )
    }

    @AssistedFactory
    interface Factory : PromoDeeplinkHandler.Factory {
        override fun create(
            coroutineScope: CoroutineScope,
            queryParams: Map<String, String>,
        ): DefaultPromoDeeplinkHandler
    }

    companion object {
        private const val LOG_TAG = "PromoCodeActivation"
        private const val DEFAULT_MESSAGE_SENDER_DELAY = 500L
    }
}