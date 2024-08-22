package com.tangem.features.markets.token.block.impl.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.markets.token.block.TokenMarketBlockComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@Stable
@ComponentScoped
internal class TokenMarketBlockModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val params = paramsContainer.require<TokenMarketBlockComponent.Params>()

    // token price is available if cryptoCurrency.id.rawCurrencyId != null
}