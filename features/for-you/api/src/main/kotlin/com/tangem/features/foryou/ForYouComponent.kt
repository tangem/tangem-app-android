package com.tangem.features.foryou

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

@Stable
interface ForYouComponent {

    @Composable
    fun Content(bottomSheetState: State<BottomSheetState>, contentPadding: PaddingValues, modifier: Modifier)

    data class Params(
        val callbacks: ForYouModelCallbacks,
    )

    interface ForYouModelCallbacks {
        fun onTokenClick(userWalletId: UserWalletId, currency: CryptoCurrency)
        fun onAllEarnTokensClick()
    }

    interface Factory : ComponentFactory<Params, ForYouComponent>
}