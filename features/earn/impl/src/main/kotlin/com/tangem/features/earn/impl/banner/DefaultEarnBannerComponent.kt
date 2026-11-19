package com.tangem.features.earn.impl.banner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.earn.api.EarnBannerComponent
import com.tangem.features.earn.impl.banner.model.EarnBannerModel
import com.tangem.features.earn.impl.banner.ui.EarnBannerContent
import com.tangem.features.earn.impl.sheet.EarnYieldSheetComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultEarnBannerComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: EarnBannerComponent.Params,
    private val sheetFactory: EarnYieldSheetComponent.Factory,
) : EarnBannerComponent, AppComponentContext by context {

    private val model: EarnBannerModel = getOrCreateModel(params)

    private val sheetSlot = childSlot(
        key = SHEET_SLOT_KEY,
        source = model.sheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = { _, ctx ->
            sheetFactory.create(
                context = childByContext(ctx),
                params = EarnYieldSheetComponent.Params(
                    userWalletId = params.userWalletId,
                    cryptoCurrency = params.cryptoCurrency,
                    callback = model,
                ),
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(state::class) { model.onShown() }

        EarnBannerContent(state = state, modifier = modifier)
    }

    @Composable
    override fun BottomSheet() {
        val sheet by sheetSlot.subscribeAsState()

        sheet.child?.instance?.BottomSheet()
    }

    @AssistedFactory
    interface Factory : EarnBannerComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: EarnBannerComponent.Params,
        ): DefaultEarnBannerComponent
    }

    private companion object {
        const val SHEET_SLOT_KEY = "earnYieldSheetSlot"
    }
}