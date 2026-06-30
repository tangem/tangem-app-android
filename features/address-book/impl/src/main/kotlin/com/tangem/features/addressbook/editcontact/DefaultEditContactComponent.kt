package com.tangem.features.addressbook.editcontact

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.addressbook.model.ContactId
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
        handleBackButton = false,
        childFactory = { _, componentContext -> portfolioSelectorChild(componentContext) },
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

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        val selectorSlot by portfolioSelectorSlot.subscribeAsState()
        BackHandler {
            if (selectorSlot.child != null) {
                model.portfolioSelectorCallback.onBack()
            } else {
                state.onCloseClick()
            }
        }
        EditContactContent(
            state = state,
            modifier = modifier,
        )
        selectorSlot.child?.instance?.BottomSheet()
    }

    data class Params(
        val contactId: ContactId?,
        val predefinedAddress: ValidatedAddress? = null,
        val onBackClick: () -> Unit,
        val onAddAddressClick: () -> Unit,
    )
}