package com.tangem.features.tangempay.orderCard.impl.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.usecase.GetCustomerOffersUseCase
import com.tangem.features.tangempay.card.issue.TangemPayIssueAdditionalCardComponent
import com.tangem.features.tangempay.common.balanceOrNull
import com.tangem.features.tangempay.orderCard.api.TangemPayOrderCardComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayOrderCardModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val getCustomerOffers: GetCustomerOffersUseCase,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
) : Model(), TangemPayIssueAdditionalCardComponent.Listener {

    private val params = paramsContainer.require<TangemPayOrderCardComponent.Params>()
    private val issueJobHolder = JobHolder()

    val bottomSheetNavigation: SlotNavigation<TangemPayOrderCardNavigation> = SlotNavigation()

    private var availableFiatBalance: BigDecimal = BigDecimal.ZERO

    init {
        modelScope.launch {
            paymentAccountStatusSupplier(params.userWalletId)
                .mapNotNull { it.balanceOrNull()?.fiatBalance?.availableBalance }
                .collect { availableFiatBalance = it }
        }
    }

    fun onSelectVirtual() {
        modelScope.launch {
            val offer = getCustomerOffers
                .additionalCardOffer(params.userWalletId)
                .getOrNull()
                ?: return@launch
            bottomSheetNavigation.activate(
                TangemPayOrderCardNavigation.IssueVirtual(
                    walletId = params.userWalletId,
                    feeAmount = offer.fee.amount,
                    feeCurrency = offer.fee.currency,
                    fiatBalance = availableFiatBalance,
                ),
            )
        }.saveIn(issueJobHolder)
    }

    override fun onIssueAdditionalCardDismissed() {
        bottomSheetNavigation.dismiss()
    }

    override fun onIssueAdditionalCardSucceeded() {
        bottomSheetNavigation.dismiss()
        modelScope.launch { paymentAccountStatusFetcher.invoke(params.userWalletId) }
        router.pop()
    }

    override fun onAddFundsForCardIssue() {
        bottomSheetNavigation.dismiss()
        router.pop()
    }
}