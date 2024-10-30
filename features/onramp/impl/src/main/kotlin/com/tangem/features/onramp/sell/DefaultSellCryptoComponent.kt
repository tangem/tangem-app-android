package com.tangem.features.onramp.sell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.onramp.component.SellCryptoComponent
import com.tangem.features.onramp.entity.OnrampOperation
import com.tangem.features.onramp.selecttoken.OnrampSelectTokenComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultSellCryptoComponent @AssistedInject constructor(
    onrampSelectTokenComponentFactory: OnrampSelectTokenComponent.Factory,
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: SellCryptoComponent.Params,
) : SellCryptoComponent {

    private val selectTokenComponent: OnrampSelectTokenComponent = onrampSelectTokenComponentFactory.create(
        context = appComponentContext,
        params = OnrampSelectTokenComponent.Params(
            operation = OnrampOperation.SELL,
            hasSearchBar = true,
            userWalletId = params.userWalletId,
        ),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        selectTokenComponent.Content(modifier = modifier)
    }

    @AssistedFactory
    interface Factory : SellCryptoComponent.Factory {

        override fun create(
            context: AppComponentContext,
            params: SellCryptoComponent.Params,
        ): DefaultSellCryptoComponent
    }
}
