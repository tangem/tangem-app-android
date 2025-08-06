package com.tangem.features.swap.v2.impl.chooseprovider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.features.swap.v2.impl.chooseprovider.model.SwapChooseProviderModel
import com.tangem.features.swap.v2.impl.chooseprovider.ui.SwapChooseProviderBottomSheet
import com.tangem.features.swap.v2.impl.chooseprovider.ui.SwapChooseProviderContent
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import kotlinx.collections.immutable.ImmutableList

internal class SwapChooseProviderComponent(
    context: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by context {

    private val model: SwapChooseProviderModel = getOrCreateModel(params = params)

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state = model.uiState.collectAsStateWithLifecycle()

        val bottomSheetConfig = remember(key1 = this) {
            TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = TangemBottomSheetConfigContent.Empty,
            )
        }

        SwapChooseProviderBottomSheet(config = bottomSheetConfig) {
            SwapChooseProviderContent(
                contentUM = state.value,
                onProviderClick = model::onProviderClick,
            )
        }
    }

    data class Params(
        val cryptoCurrency: CryptoCurrency,
        val selectedProvider: ExpressProvider,
        val providers: ImmutableList<SwapQuoteUM>,
        val userCountry: UserCountry,
        val callback: ModelCallback,
        val onDismiss: () -> Unit,
    )

    interface ModelCallback {
        fun onProviderResult(quoteUM: SwapQuoteUM)
    }
}