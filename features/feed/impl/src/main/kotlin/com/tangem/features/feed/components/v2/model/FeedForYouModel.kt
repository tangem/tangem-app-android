package com.tangem.features.feed.components.v2.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.v2.FeedEntryController
import com.tangem.features.feed.v2.FeedEntryRequest
import com.tangem.features.feed.v2.FeedSection
import com.tangem.features.foryou.ForYouComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@Stable
@ModelScoped
internal class FeedForYouModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val feedEntryController: FeedEntryController,
) : Model(), ForYouComponent.ForYouModelCallbacks {

    override fun onTokenClick(userWalletId: UserWalletId, currency: CryptoCurrency) {
        router.push(
            route = FeedRoute.TokenSummary(
                token = FeedRoute.TokenSummary.Token.Portfolio(cryptoCurrency = currency),
            ),
        )
    }

    override fun onAllEarnTokensClick() {
        feedEntryController.request(FeedEntryRequest(section = FeedSection.Earn))
    }
}