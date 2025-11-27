package com.tangem.features.onramp.hottokens.portfolio

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.addtoken.AddTokenContent
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.onramp.hottokens.portfolio.model.OnrampAddTokenModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow

internal class OnrampAddTokenComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
) : AppComponentContext by context, ComposableContentComponent {

    private val model: OnrampAddTokenModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state = model.uiState.collectAsStateWithLifecycle()
        val um = state.value ?: return
        AddTokenContent(
            modifier = modifier,
            state = um,
        )
    }

    data class Params(
        val tokenToAdd: Flow<AddHotCryptoData>,
        val callbacks: Callbacks,
    )

    data class AddHotCryptoData(
        val cryptoCurrency: CryptoCurrency,
        val userWallet: UserWallet,
        val account: AccountStatus,
        val isMorePortfolioAvailable: Boolean,
    )

    interface Callbacks {
        fun onChangePortfolioClick()
        fun onTokenAdded(status: CryptoCurrencyStatus)
    }

    @AssistedFactory
    interface Factory : ComponentFactory<Params, OnrampAddTokenComponent> {
        override fun create(context: AppComponentContext, params: Params): OnrampAddTokenComponent
    }
}