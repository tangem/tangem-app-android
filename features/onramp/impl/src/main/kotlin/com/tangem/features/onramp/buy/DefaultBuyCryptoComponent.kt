package com.tangem.features.onramp.buy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.onramp.component.BuyCryptoComponent
import com.tangem.features.onramp.selecttoken.OnrampOperationComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultBuyCryptoComponent @AssistedInject constructor(
    onrampOperationComponentFactory: OnrampOperationComponent.Factory,
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: BuyCryptoComponent.Params,
) : BuyCryptoComponent {

    private val selectTokenComponent: OnrampOperationComponent = onrampOperationComponentFactory.create(
        context = appComponentContext,
        params = OnrampOperationComponent.Params.Buy(userWalletId = params.userWalletId),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        selectTokenComponent.Content(modifier = modifier)
    }

    @AssistedFactory
    interface Factory : BuyCryptoComponent.Factory {

        override fun create(context: AppComponentContext, params: BuyCryptoComponent.Params): DefaultBuyCryptoComponent
    }
}