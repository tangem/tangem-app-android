package com.tangem.features.addressbook.editcontact

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.features.addressbook.addressinfo.DefaultAddressInfoComponent
import com.tangem.features.addressbook.editcontact.model.EditContactModel
import com.tangem.features.addressbook.editcontact.ui.EditContactContent
import com.tangem.features.addressbook.editcontact.ui.state.ValidatedAddress
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import kotlinx.serialization.builtins.serializer

internal class DefaultEditContactComponent(
    appComponentContext: AppComponentContext,
    params: Params,
    private val portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: EditContactModel = getOrCreateModel(params)

    private val portfolioSelectorSlot = childSlot(
        source = model.portfolioSelectorNavigation,
        serializer = Unit.serializer(),
        key = "edit_contact_portfolio_selector_slot",
        handleBackButton = false,
        childFactory = { _, componentContext -> portfolioSelectorChild(componentContext) },
    )

    private val addressInfoSlot = childSlot(
        source = model.addressInfoNavigation,
        serializer = String.serializer(),
        key = "edit_contact_address_info_slot",
        handleBackButton = false,
        childFactory = { address, componentContext -> addressInfoChild(address, componentContext) },
    )

    private fun portfolioSelectorChild(componentContext: ComponentContext): ComposableBottomSheetComponent =
        portfolioSelectorComponentFactory.create(
            context = childByContext(componentContext),
            params = PortfolioSelectorComponent.Params(
                portfolioFetcher = model.portfolioFetcher,
                controller = model.portfolioSelectorController,
                bsCallback = model.portfolioSelectorCallback,
                settings = PortfolioSelectorComponent.Settings(isWalletSelectionOnly = true),
            ),
        )

    private fun addressInfoChild(address: String, componentContext: ComponentContext): ComposableBottomSheetComponent =
        DefaultAddressInfoComponent(
            appComponentContext = childByContext(componentContext),
            params = model.createAddressInfoParams(address),
        )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        val selectorSlot by portfolioSelectorSlot.subscribeAsState()
        val infoSlot by addressInfoSlot.subscribeAsState()
        BackHandler {
            when {
                selectorSlot.child != null -> model.portfolioSelectorCallback.onBack()
                infoSlot.child != null -> model.addressInfoNavigation.dismiss()
                else -> state.onCloseClick()
            }
        }
        EditContactContent(
            state = state,
            modifier = modifier,
        )
        selectorSlot.child?.instance?.BottomSheet()
        infoSlot.child?.instance?.BottomSheet()
    }

    data class Params(
        val contactId: ContactId?,
        val predefinedAddress: ValidatedAddress? = null,
        val onBackClick: () -> Unit,
        val onAddAddressClick: (walletId: String, excludeContactId: String?, prefill: ValidatedAddress?) -> Unit,
    )
}