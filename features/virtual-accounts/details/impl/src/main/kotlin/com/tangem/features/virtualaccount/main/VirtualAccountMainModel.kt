package com.tangem.features.virtualaccount.main

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.virtualaccount.details.component.VirtualAccountMainComponent
import com.tangem.features.virtualaccount.details.impl.R
import com.tangem.features.virtualaccount.main.addfunds.VirtualAccountAddFundsBottomSheetComponent
import com.tangem.features.virtualaccount.main.addfunds.VirtualAccountAddFundsListener
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class VirtualAccountMainModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
) : Model(), VirtualAccountAddFundsListener {

    @Suppress("UnusedPrivateProperty")
    private val params = paramsContainer.require<VirtualAccountMainComponent.Params>()

    val bottomSheetNavigation: SlotNavigation<VirtualAccountMainNavigationBottomSheetConfig> = SlotNavigation()

    val uiState: StateFlow<VirtualAccountMainUM>
        field = MutableStateFlow(
            createInitialState(),
        )

    override fun onAddFundsDismiss() {
        bottomSheetNavigation.dismiss()
    }

    private fun createInitialState(): VirtualAccountMainUM = VirtualAccountMainUM(
        title = resourceReference(R.string.virtual_account_title),
        subtitle = resourceReference(R.string.tangempay_usdc_on_polygon_network),
        balance = VirtualAccountBalanceBlockState.Content(
            fiatBalance = stringReference("$0.00"),
            isBalanceFlickering = false,
        ),
        isBalanceHidden = false,
        onBackClick = { router.pop() },
        onMenuClick = {},
        onAddFundsClick = ::onAddFundsClick,
        onSendClick = {},
    )

    private fun buildRequisites(details: VirtualAccountDepositDetails) = listOf(
        VirtualAccountAddFundsBottomSheetComponent.RequisitesRow(
            title = stringReference("Beneficiary name and address"),
            titleForShare = "Beneficiary name and address",
            value = "${details.beneficiaryName}\n${details.beneficiaryAddress}",
        ),
        VirtualAccountAddFundsBottomSheetComponent.RequisitesRow(
            title = stringReference("Bank name and address"),
            titleForShare = "Bank name and address",
            value = "${details.bankName}\n${details.bankAddress}",
        ),
        VirtualAccountAddFundsBottomSheetComponent.RequisitesRow(
            title = stringReference("Account number"),
            titleForShare = "Account number",
            value = details.accountNumber,
        ),
        VirtualAccountAddFundsBottomSheetComponent.RequisitesRow(
            title = stringReference("Routing number"),
            titleForShare = "Routing number",
            value = details.routingNumber,
        ),
    )

    private fun onAddFundsClick() {
        val details = getDepositDetails()
        bottomSheetNavigation.activate(
            VirtualAccountMainNavigationBottomSheetConfig.AddFunds(
                requisites = buildRequisites(details),
                dailyDepositLimit = details.dailyDepositLimit,
            ),
        )
    }

    // TODO v_rodionov: HARDCODE - get this data from backend
    private fun getDepositDetails(): VirtualAccountDepositDetails {
        return VirtualAccountDepositDetails(
            beneficiaryName = "Ivan Ivanov",
            beneficiaryAddress = "18, Rue Rubens 20, Paris, Ile-de-France 75013, US",
            bankName = "SSB Bank",
            bankAddress = "8700 Perry Highway, Pittsburgh, PA 15237, US",
            accountNumber = "707613210122",
            routingNumber = "043087080",
            dailyDepositLimit = "$10,000",
        )
    }

    private data class VirtualAccountDepositDetails(
        val beneficiaryName: String,
        val beneficiaryAddress: String,
        val bankName: String,
        val bankAddress: String,
        val accountNumber: String,
        val routingNumber: String,
        val dailyDepositLimit: String,
    )
}