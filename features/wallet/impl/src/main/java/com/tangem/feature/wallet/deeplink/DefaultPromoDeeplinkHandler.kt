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
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.wallets.PromoCodeActivationResult
import com.tangem.domain.wallets.PromoCodeActivationResult.*
import com.tangem.domain.wallets.models.errors.ActivatePromoCodeError
import com.tangem.domain.wallets.usecase.ActivateBitcoinPromocodeUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.wallet.deeplink.analytics.PromoActivationAnalytics
import com.tangem.feature.wallet.impl.R
import com.tangem.features.wallet.deeplink.PromoDeeplinkHandler
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList")
internal class DefaultPromoDeeplinkHandler @AssistedInject constructor(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val queryParams: Map<String, String>,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
    private val multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val activateBitcoinPromocodeUseCase: ActivateBitcoinPromocodeUseCase,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    private val dispatchers: CoroutineDispatcherProvider,
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
        scope.launch(context = dispatchers.default) {
            val networkStatuses = withTimeoutOrNull(
                FETCH_TIMEOUT_SECONDS.seconds,
                {
                    multiNetworkStatusSupplier
                        .invoke(MultiNetworkStatusProducer.Params(userWallet.walletId))
                        .first { statuses ->
                            statuses.any { status -> status.network.rawId == Blockchain.Bitcoin.id }
                        }
                },
            )

            Timber.tag(LOG_TAG).d("All user network statuses ${networkStatuses?.size}")

            val cryptoCurrencies = multiWalletCryptoCurrenciesSupplier
                .getSyncOrNull(
                    MultiWalletCryptoCurrenciesProducer.Params(
                        userWallet
                            .walletId,
                    ),
                )

            Timber.tag(LOG_TAG).d("All user cryptoCurrencies on main ${cryptoCurrencies?.size}")

            val bitcoinCurrency = cryptoCurrencies?.firstOrNull { it.id.rawNetworkId == Blockchain.Bitcoin.id }
            Timber.tag(LOG_TAG).d("BitcoinCurrency $bitcoinCurrency")

            val bitcoinStatus = networkStatuses?.firstOrNull { status ->
                status.network.id == bitcoinCurrency
                    ?.network?.id
            }
            Timber.tag(LOG_TAG).d("BitcoinStatus $bitcoinStatus")

            if (bitcoinStatus == null) {
                Timber.tag(LOG_TAG).d("No bitcoin, bitcoin network status == null")
                showAlert(NoBitcoinAddress)
            } else {
                val networkAddress = when (bitcoinStatus.value) {
                    is NetworkStatus.Verified -> (bitcoinStatus.value as NetworkStatus.Verified).address
                    is NetworkStatus.NoAccount -> (bitcoinStatus.value as NetworkStatus.NoAccount).address
                    else -> null
                }

                val bitcoinAddress = networkAddress
                    ?.defaultAddress?.value

                if (bitcoinAddress != null) {
                    Timber.tag(LOG_TAG).d(
                        "Start activation promoCode ${promoCode.mask()} address ${bitcoinAddress.mask()}",
                    )
                    activatePromoCode(bitcoinAddress = bitcoinAddress, promoCode = promoCode)
                } else {
                    uiMessageSender.send(GlobalLoadingMessage(false))
                    delay(DEFAULT_MESSAGE_SENDER_DELAY)
                    Timber.tag(LOG_TAG).d("No Bitcoin address $bitcoinStatus.value")
                    showAlert(NoBitcoinAddress)
                }
            }
        }
    }

    private suspend fun activatePromoCode(bitcoinAddress: String, promoCode: String) {
        uiMessageSender.send(GlobalLoadingMessage(true))
        activateBitcoinPromocodeUseCase(bitcoinAddress, promoCode).onRight {
            delay(DEFAULT_MESSAGE_SENDER_DELAY)
            uiMessageSender.send(GlobalLoadingMessage(false))
            delay(DEFAULT_MESSAGE_SENDER_DELAY)
            Timber.tag(LOG_TAG).d("${promoCode.mask()} activation success on address ${bitcoinAddress.mask()}")
            showAlert(Activated)
        }.onLeft { error ->
            delay(DEFAULT_MESSAGE_SENDER_DELAY)
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
        private const val FETCH_TIMEOUT_SECONDS = 5
    }
}