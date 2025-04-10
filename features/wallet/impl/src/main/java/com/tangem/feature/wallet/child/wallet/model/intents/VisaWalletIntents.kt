package com.tangem.feature.wallet.child.wallet.model.intents

import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.visa.GetVisaCurrencyUseCase
import com.tangem.domain.visa.GetVisaTxDetailsUseCase
import com.tangem.domain.visa.model.VisaTxDetails
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.BalancesAndLimitsBottomSheetConverter
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.VisaTxDetailsBottomSheetConverter
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletEventSender
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal interface VisaWalletIntents {

    fun onBalancesAndLimitsClick()

    fun onVisaTransactionClick(id: String)

    fun onExploreClick(exploreUrl: String)

    fun onDisputeClick(txDetails: VisaTxDetails)
}

@Suppress("LongParameterList")
@ModelScoped
internal class VisaWalletIntentsImplementor @Inject constructor(
    private val stateController: WalletStateController,
    private val eventSender: WalletEventSender,
    private val getVisaCurrencyUseCase: GetVisaCurrencyUseCase,
    private val getVisaTxDetailsUseCase: GetVisaTxDetailsUseCase,
    private val getCardInfoUseCase: GetCardInfoUseCase,
    private val getUserWalletsUseCase: GetWalletsUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : BaseWalletClickIntents(), VisaWalletIntents {

    private val balancesAndLimitsBottomSheetConverter by lazy(mode = LazyThreadSafetyMode.NONE) {
        BalancesAndLimitsBottomSheetConverter(eventSender, analyticsEventHandler)
    }

    override fun onBalancesAndLimitsClick() {
        analyticsEventHandler.send(MainScreenAnalyticsEvent.LimitsClicked)
        modelScope.launch(dispatchers.main) {
            val userWalletId = stateController.getSelectedWalletId()
            val balancesAndLimits = getVisaCurrencyUseCase(userWalletId)
                .getOrElse {
                    Timber.e("Unable to get balances and limits: $it")
                    return@launch
                }

            val bottomSheetContent = balancesAndLimitsBottomSheetConverter.convert(
                value = balancesAndLimits,
            )

            stateController.showBottomSheet(bottomSheetContent)
        }
    }

    override fun onVisaTransactionClick(id: String) {
        modelScope.launch(dispatchers.main) {
            val userWalletId = stateController.getSelectedWalletId()
            val visaCurrency = getVisaCurrencyUseCase(userWalletId)
                .getOrElse {
                    Timber.e(it, "Failed to get visa currency")
                    return@launch
                }
            val transactionDetails = getVisaTxDetailsUseCase(userWalletId, id)
                .getOrElse {
                    Timber.e(it, "Failed to get transaction details")
                    return@launch
                }

            val converter = VisaTxDetailsBottomSheetConverter(
                visaCurrency,
                clickIntents = this@VisaWalletIntentsImplementor,
            )

            stateController.showBottomSheet(content = converter.convert(transactionDetails))
        }
    }

    override fun onExploreClick(exploreUrl: String) {
        analyticsEventHandler.send(MainScreenAnalyticsEvent.ButtonExplore)
        router.openUrl(exploreUrl)
    }

    override fun onDisputeClick(txDetails: VisaTxDetails) {
        modelScope.launch {
            val userWalletId = stateController.getSelectedWalletId()
            val userWallet = getUserWalletsUseCase.invokeSync()
                .firstOrNull { it.walletId == userWalletId } ?: return@launch
            val cardInfo = getCardInfoUseCase.invoke(userWallet.scanResponse).getOrNull() ?: return@launch

            sendFeedbackEmailUseCase(
                FeedbackEmailType.Visa.Dispute(
                    cardInfo = cardInfo,
                    visaTxDetails = txDetails,
                ),
            )
        }
    }
}