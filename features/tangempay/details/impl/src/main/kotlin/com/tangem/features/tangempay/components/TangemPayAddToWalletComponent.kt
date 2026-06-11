package com.tangem.features.tangempay.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.components.cardDetails.DefaultTangemPayCardDetailsBlockComponent
import com.tangem.features.tangempay.components.cardDetails.TangemPayCardDetailsBlockComponent
import com.tangem.features.tangempay.model.TangemPayAddToWalletModel
import com.tangem.features.tangempay.ui.TangemPayAddToWalletScreen
import com.tangem.features.tangempay.ui.TangemPayAddToWalletScreenV2
import com.tangem.features.tangempay.utils.userWalletId

internal class TangemPayAddToWalletComponent(
    private val appComponentContext: AppComponentContext,
    private val params: TangemPayDetailsContainerComponent.Params,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: TangemPayAddToWalletModel = getOrCreateModel()

    private val cardDetailsBlockComponent = DefaultTangemPayCardDetailsBlockComponent(
        appComponentContext = child("cardDetailsBlockComponent"),
        params = TangemPayCardDetailsBlockComponent.Params(
            initialStatus = params.initialStatus,
            userWalletId = params.initialStatus.userWalletId,
            isEditingNameEnabled = false,
            shouldShowCardDetailsButtonOnCard = true,
        ),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        BackHandler(onBack = router::pop)
        if (model.isRedesignEnabled()) {
            TangemPayAddToWalletScreenV2(
                state = state,
                cardDetailsBlockComponent = cardDetailsBlockComponent,
            )
        } else {
            TangemPayAddToWalletScreen(
                state = state,
                cardDetailsBlockComponent = cardDetailsBlockComponent,
            )
        }
    }
}