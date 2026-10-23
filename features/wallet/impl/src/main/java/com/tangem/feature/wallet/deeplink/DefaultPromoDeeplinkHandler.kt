package com.tangem.feature.wallet.deeplink

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.common.routing.deeplink.DeeplinkConst
import com.tangem.common.routing.deeplink.DeeplinkConst.PROMO_CODE_KEY
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.mask
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.GlobalLoadingMessage
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.domain.wallets.models.PromoCodeActivation
import com.tangem.domain.wallets.models.PromoPayout
import com.tangem.domain.wallets.models.ReferralParamValidator
import com.tangem.domain.wallets.models.errors.ActivatePromoCodeError
import com.tangem.domain.wallets.usecase.ActivatePromoCodeUseCase
import com.tangem.domain.wallets.usecase.BindRefcodeWithWalletUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.wallet.deeplink.analytics.PromoActivationError
import com.tangem.feature.wallet.deeplink.analytics.PromoBannerAnalytics
import com.tangem.feature.wallet.impl.R
import com.tangem.features.wallet.deeplink.PromoDeeplinkHandler
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList")
internal class DefaultPromoDeeplinkHandler @AssistedInject constructor(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val queryParams: Map<String, String>,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
    private val multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val activatePromoCodeUseCase: ActivatePromoCodeUseCase,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val appsFlyerRepository: AppsFlyerRepository,
    private val bindRefcodeWithWalletUseCase: BindRefcodeWithWalletUseCase,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    private val dispatchers: CoroutineDispatcherProvider,
) : PromoDeeplinkHandler {

    private val logger = TangemLogger.withTag(LOG_TAG)

    private val refcode: String? = ReferralParamValidator.sanitize(queryParams[DeeplinkConst.REF_KEY])
    private val campaign: String? = ReferralParamValidator.sanitize(queryParams[DeeplinkConst.CAMPAIGN_KEY])
    private val promoCode: String = queryParams[PROMO_CODE_KEY].orEmpty()

    private val linkNetworkId: String? = queryParams[PAYMENT_NETWORK_KEY]?.takeIf { it.isNotBlank() }
    private val linkCurrencySymbol: String? = queryParams[PAYMENT_CURRENCY_KEY]?.takeIf { it.isNotBlank() }

    /** Legacy links carry only the promo code: those campaigns pay out in Bitcoin and must not send a payout */
    private val isLegacyLink: Boolean =
        PAYMENT_NETWORK_KEY !in queryParams && PAYMENT_CURRENCY_KEY !in queryParams
    private val payout: PromoPayout? = if (linkNetworkId != null && linkCurrencySymbol != null) {
        PromoPayout(networkId = linkNetworkId, currencySymbol = linkCurrencySymbol)
    } else {
        null
    }
    private val payoutNetworkId: String = if (isLegacyLink) {
        Blockchain.Bitcoin.toNetworkId()
    } else {
        linkNetworkId.orEmpty()
    }
    private val payoutSymbol: String = if (isLegacyLink) Blockchain.Bitcoin.currency else linkCurrencySymbol.orEmpty()
    private val payoutBlockchain: Blockchain? = Blockchain.fromNetworkId(payoutNetworkId)
    private val payoutTokenName: String = payoutBlockchain?.fullName
        ?.takeIf { it.isNotBlank() }
        ?: payoutNetworkId

    /** A non-native payout token is named by its symbol: the network name would point to the coin instead */
    private val missingCurrencyName: String =
        if (payout == null || payout.currencySymbol.equals(payoutBlockchain?.currency, ignoreCase = true)) {
            payoutTokenName
        } else {
            payout.currencySymbol
        }

    private val userWallet: UserWallet? = getSelectedWalletSyncUseCase()
        .onLeft { logger.e("Error on getting user wallet: $it") }
        .getOrNull()
    private val walletId: String? = userWallet?.walletId?.stringValue

    init {
        saveAndBindRefcode()

        analyticsEventsHandler.send(PromoBannerAnalytics.BannerOpen(walletId = walletId, promoCode = promoCode))

        when {
            promoCode.isEmpty() -> showFailure(PromoActivationError.InvalidCode)
            !isLegacyLink && payout == null -> {
                logger.e(
                    "Incomplete payout in promo link: network=$linkNetworkId, currency=$linkCurrencySymbol",
                )
                showFailure(PromoActivationError.InvalidCode)
            }
            userWallet == null -> showFailure(PromoActivationError.InternalError)
            else -> {
                logger.d("SelectedUserWallet ${walletId.orEmpty().mask()}")
                findPayoutAddress(userWallet)
            }
        }
    }

    private fun findPayoutAddress(userWallet: UserWallet) {
        scope.launch(context = dispatchers.default) {
            val networkStatuses = withTimeoutOrNull(
                FETCH_TIMEOUT_SECONDS.seconds,
                {
                    multiNetworkStatusSupplier
                        .invoke(MultiNetworkStatusProducer.Params(userWallet.walletId))
                        .first { statuses -> statuses.any { status -> status.network.rawId == payoutNetworkId } }
                },
            )

            logger.d("All user network statuses ${networkStatuses?.size}")

            val cryptoCurrencies = multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                MultiWalletCryptoCurrenciesProducer.Params(userWallet.walletId),
            )

            logger.d("All user cryptoCurrencies on main ${cryptoCurrencies?.size}")

            val payoutCurrency = cryptoCurrencies?.firstOrNull { it.isPayoutCurrency() }
            logger.d("PayoutCurrency $payoutCurrency")

            val payoutStatus = networkStatuses?.firstOrNull { status ->
                status.network.id == payoutCurrency?.network?.id
            }
            logger.d("PayoutStatus $payoutStatus")

            val address = when (val value = payoutStatus?.value) {
                is NetworkStatus.Verified -> value.address.defaultAddress.value
                is NetworkStatus.NoAccount -> value.address.defaultAddress.value
                else -> null
            }

            if (address != null) {
                logger.d("Start activation promoCode ${promoCode.mask()} address ${address.mask()}")
                activatePromoCode(userWallet = userWallet, address = address)
            } else {
                uiMessageSender.send(GlobalLoadingMessage(false))
                delay(DEFAULT_MESSAGE_SENDER_DELAY)
                logger.d("No $payoutSymbol address, status ${payoutStatus?.value}")
                showFailure(PromoActivationError.AddressMissing)
            }
        }
    }

    /** Legacy Bitcoin campaigns are matched by network only, campaigns with a payout also by the currency */
    private fun CryptoCurrency.isPayoutCurrency(): Boolean {
        return id.rawNetworkId == payoutNetworkId &&
            (payout == null || symbol.equals(payout.currencySymbol, ignoreCase = true))
    }

    private suspend fun activatePromoCode(userWallet: UserWallet, address: String) {
        analyticsEventsHandler.send(
            PromoBannerAnalytics.ActivationAttempt(
                walletId = userWallet.walletId.stringValue,
                promoCode = promoCode,
                network = payoutNetworkId,
                token = payoutSymbol,
            ),
        )
        uiMessageSender.send(GlobalLoadingMessage(true))

        val result = activatePromoCodeUseCase(
            userWalletId = userWallet.walletId,
            address = address,
            promoCode = promoCode,
            payout = payout,
        )

        delay(DEFAULT_MESSAGE_SENDER_DELAY)
        uiMessageSender.send(GlobalLoadingMessage(false))
        delay(DEFAULT_MESSAGE_SENDER_DELAY)

        result.onRight { activation ->
            logger.d("${promoCode.mask()} activation success on address ${address.mask()}")
            showSuccess(walletId = userWallet.walletId.stringValue, activation = activation)
        }.onLeft { error ->
            logger.e("${promoCode.mask()} activation failed $error")
            showFailure(error.toPromoActivationError())
        }
    }

    private fun ActivatePromoCodeError.toPromoActivationError(): PromoActivationError = when (this) {
        ActivatePromoCodeError.InvalidPromoCode -> PromoActivationError.InvalidCode
        ActivatePromoCodeError.PromocodeAlreadyUsed -> PromoActivationError.AlreadyUsed
        ActivatePromoCodeError.CampaignExpired -> PromoActivationError.CampaignExpired
        ActivatePromoCodeError.InvalidRequest -> PromoActivationError.InvalidAddress
        ActivatePromoCodeError.NetworkError -> PromoActivationError.NetworkError
        ActivatePromoCodeError.ActivationFailed -> PromoActivationError.InternalError
    }

    private fun showSuccess(walletId: String, activation: PromoCodeActivation) {
        analyticsEventsHandler.send(
            PromoBannerAnalytics.ActivationSuccess(
                walletId = walletId,
                promoCode = promoCode,
                network = payoutNetworkId,
                token = payoutSymbol,
                campaignId = activation.programId,
                activationTime = DateTime.now(DateTimeZone.UTC).toString(),
            ),
        )

        showDialog(
            title = resourceReference(R.string.bitcoin_promo_activation_success_title),
            message = resourceReference(
                id = R.string.promo_code_activation_success,
                formatArgs = wrappedList(payoutTokenName),
            ),
        )
    }

    private fun showFailure(error: PromoActivationError) {
        analyticsEventsHandler.send(
            PromoBannerAnalytics.ActivationFailed(
                walletId = walletId,
                promoCode = promoCode,
                network = payoutNetworkId,
                token = payoutSymbol,
                error = error,
            ),
        )

        when (error) {
            PromoActivationError.InvalidCode -> showDialog(
                title = resourceReference(R.string.bitcoin_promo_invalid_code_title),
                message = resourceReference(R.string.bitcoin_promo_invalid_code),
            )
            PromoActivationError.AlreadyUsed -> showDialog(
                title = resourceReference(R.string.bitcoin_promo_already_activated_title),
                message = resourceReference(R.string.bitcoin_promo_already_activated),
            )
            PromoActivationError.AddressMissing -> showDialog(
                title = resourceReference(
                    id = R.string.promo_code_no_address_title,
                    formatArgs = wrappedList(missingCurrencyName),
                ),
                message = resourceReference(
                    id = R.string.promo_code_no_address,
                    formatArgs = wrappedList(missingCurrencyName),
                ),
            )
            PromoActivationError.CampaignExpired,
            PromoActivationError.InvalidAddress,
            PromoActivationError.NetworkError,
            PromoActivationError.InternalError,
            -> showDialog(
                title = resourceReference(R.string.bitcoin_promo_activation_error_title),
                message = resourceReference(R.string.bitcoin_promo_activation_error),
            )
        }
    }

    private fun showDialog(title: TextReference, message: TextReference) {
        uiMessageSender.send(
            DialogMessage(
                title = title,
                message = message,
                dismissOnFirstAction = true,
                firstActionBuilder = {
                    okAction {
                        uiMessageSender.send(GlobalLoadingMessage(false))
                    }
                },
            ),
        )
    }

    @Suppress("NullableToStringCall")
    private fun saveAndBindRefcode() {
        if (refcode == null) return

        scope.launch(dispatchers.default) {
            logger.i("saveAndBindRefcode: refcode = $refcode, campaign = $campaign")

            val conversionData = AppsFlyerConversionData(refcode = refcode, campaign = campaign)

            appsFlyerRepository.storeIfAbsent(value = conversionData)

            bindRefcodeWithWalletUseCase(conversionData)
                .onLeft { logger.e("Failed to bind refcode with wallets: $it") }
        }
    }

    @AssistedFactory
    interface Factory : PromoDeeplinkHandler.Factory {
        override fun create(
            coroutineScope: CoroutineScope,
            queryParams: Map<String, String>,
        ): DefaultPromoDeeplinkHandler
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://promo"

        private const val PAYMENT_NETWORK_KEY = "payment_network"
        private const val PAYMENT_CURRENCY_KEY = "payment_currency"

        private const val LOG_TAG = "PromoCodeActivation"
        private const val DEFAULT_MESSAGE_SENDER_DELAY = 500L
        private const val FETCH_TIMEOUT_SECONDS = 5
    }
}