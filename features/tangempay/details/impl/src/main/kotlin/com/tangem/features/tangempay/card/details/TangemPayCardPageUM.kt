package com.tangem.features.tangempay.card.details

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.features.tangempay.card.gpay.AddToWalletBlockState
import com.tangem.features.tangempay.common.TangemPayDropDownItemUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class TangemPayCardPageUM(
    val settings: ImmutableList<TangemPayCardPageSetting>,
    val onBackClick: () -> Unit,
    val dailyLimitState: TangemPayDailyLimitBlockState,
    val addToWalletBlockState: AddToWalletBlockState? = null,
    val cardState: TangemPayCardState = TangemPayCardState.Active,
    val menuItems: ImmutableList<TangemPayDropDownItemUM>,
    val delivery: TangemPayCardDeliveryUM? = null,
)

@Immutable
internal data class TangemPayCardDeliveryUM(
    val email: String,
    val onContactSupportClick: () -> Unit,
    val onActivateCardClick: () -> Unit,
)

@Immutable
internal data class TangemPayCardPageSetting(
    val id: Id,
    val title: TextReference,
    val isLoading: Boolean = false,
    val isEnabled: Boolean = true,
    val testTag: String? = null,
    val onClick: () -> Unit,
    @param:DrawableRes val iconRes: Int,
) {

    enum class Id {
        Details, Freeze, ChangePin
    }
}