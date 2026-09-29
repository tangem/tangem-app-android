package com.tangem.features.tangempay.withdraw.bank.form

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.features.tangempay.eligibility.TangemPayCountrySelectorComponent
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormResult
import com.tangem.features.tangempay.withdraw.bank.form.model.CountrySelectorConfig
import com.tangem.features.tangempay.withdraw.bank.form.model.TangemPayBankWithdrawFormModel
import com.tangem.features.tangempay.withdraw.bank.form.ui.TangemPayBankWithdrawFormScreen
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow

internal class TangemPayBankWithdrawFormComponent(
    appComponentContext: AppComponentContext,
    params: Params,
    private val countrySelectorFactory: TangemPayCountrySelectorComponent.Factory,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val selectorNavigation = SlotNavigation<CountrySelectorConfig>()

    private val model: TangemPayBankWithdrawFormModel = getOrCreateModel(
        params = TangemPayBankWithdrawFormModel.Params(
            form = params,
            onOpenSelector = selectorNavigation::activate,
        ),
    )

    private val selectorSlot = childSlot(
        key = "countrySelector",
        source = selectorNavigation,
        serializer = CountrySelectorConfig.serializer(),
        handleBackButton = false,
        childFactory = ::selectorChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val selector by selectorSlot.subscribeAsState()
        val selectorChild = selector.child?.instance

        if (selectorChild != null) {
            selectorChild.Content(modifier = modifier)
        } else {
            val state by model.uiState.collectAsStateWithLifecycle()
            BackHandler(onBack = state.onBackClick)
            TangemPayBankWithdrawFormScreen(state = state, modifier = modifier)
        }
    }

    private fun selectorChild(
        config: CountrySelectorConfig,
        componentContext: ComponentContext,
    ): TangemPayCountrySelectorComponent = countrySelectorFactory.create(
        context = childByContext(componentContext),
        params = when (config) {
            is CountrySelectorConfig.Country -> TangemPayCountrySelectorComponent.Params.Countries(
                onDismiss = selectorNavigation::dismiss,
                onSelected = { country, region ->
                    model.onCountrySelected(field = config.field, country = country, region = region)
                },
                selectedCountryCode = config.selectedCountryCode,
                selectedRegionCode = config.selectedRegionCode,
            )
            is CountrySelectorConfig.Region -> TangemPayCountrySelectorComponent.Params.Regions(
                countryCode = config.countryCode,
                onDismiss = selectorNavigation::dismiss,
                onSelected = { region -> model.onRegionSelected(field = config.field, region = region) },
                selectedRegionCode = config.selectedRegionCode,
            )
        },
    )

    data class Params(
        val draft: StateFlow<BankWithdrawDraft>,
        val step: TangemPayBankWithdrawInnerRoute,
        val fields: ImmutableList<BankWithdrawFormField>,
        val savedAccounts: StateFlow<List<BankAccount>>,
        val onBack: (result: BankWithdrawFormResult) -> Unit,
        val onContinue: (result: BankWithdrawFormResult) -> Unit,
        val onSavedRecipientSelected: (account: BankAccount) -> Unit,
        val onCloseFlow: () -> Unit,
    )
}