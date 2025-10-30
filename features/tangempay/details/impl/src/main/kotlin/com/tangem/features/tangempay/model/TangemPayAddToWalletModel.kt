package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayAddToWalletStepItemUM
import com.tangem.features.tangempay.entity.TangemPayAddToWalletUM
import com.tangem.features.tangempay.utils.GoogleWalletUtil
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayAddToWalletModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val googleWalletUtil: GoogleWalletUtil,
) : Model() {

    val uiState: StateFlow<TangemPayAddToWalletUM>
        field = MutableStateFlow(getInitialState())

    @Suppress("MagicNumber")
    private fun getInitialState(): TangemPayAddToWalletUM {
        return TangemPayAddToWalletUM(
            steps = persistentListOf(
                TangemPayAddToWalletStepItemUM(
                    count = 1,
                    text = resourceReference(R.string.tangempay_card_details_open_wallet_step_1),
                ),
                TangemPayAddToWalletStepItemUM(
                    count = 2,
                    text = resourceReference(R.string.tangempay_card_details_open_wallet_step_2),
                ),
                TangemPayAddToWalletStepItemUM(
                    count = 3,
                    text = resourceReference(R.string.tangempay_card_details_open_wallet_step_3),
                ),
                TangemPayAddToWalletStepItemUM(
                    count = 4,
                    text = resourceReference(R.string.tangempay_card_details_open_wallet_step_4),
                ),
                TangemPayAddToWalletStepItemUM(
                    count = 5,
                    text = resourceReference(R.string.tangempay_card_details_open_wallet_step_5),
                ),
            ),
            showAddToWalletButton = googleWalletUtil.isWalletAvailable(),
            onBackClick = router::pop,
            onClickOpenWallet = googleWalletUtil::openWallet,
        )
    }
}