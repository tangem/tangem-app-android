package com.tangem.features.tangempay.card.details

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.features.tangempay.card.gpay.AddToWalletBlockState
import com.tangem.features.tangempay.common.TangemPayDropDownItemUM
import com.tangem.features.tangempay.details.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.tangem.core.ui.R as CoreUiR

@Immutable
internal data class TangemPayCardPageUM(
    val settings: ImmutableList<TangemPayCardPageSetting>,
    val onBackClick: () -> Unit,
    val dailyLimitState: TangemPayDailyLimitBlockState,
    val addToWalletBlockState: AddToWalletBlockState? = null,
    val cardState: TangemPayCardState = TangemPayCardState.Active,
    val menuItems: ImmutableList<TangemPayDropDownItemUM>,
) {
    companion object {
        fun stub(
            addToWalletBlockState: AddToWalletBlockState? = AddToWalletBlockState(
                onClick = {},
                onClickClose = {},
            ),
            cardState: TangemPayCardState = TangemPayCardState.Active,
            dailyLimitState: TangemPayDailyLimitBlockState = TangemPayDailyLimitBlockState.Content.stub(),
            settings: ImmutableList<TangemPayCardPageSetting> = TangemPayCardPageSetting.stubList(),
        ) = TangemPayCardPageUM(
            addToWalletBlockState = addToWalletBlockState,
            settings = settings,
            onBackClick = {},
            cardState = cardState,
            dailyLimitState = dailyLimitState,
            menuItems = persistentListOf(),
        )
    }
}

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

    companion object {
        fun stubList(isFrozen: Boolean = false): ImmutableList<TangemPayCardPageSetting> = persistentListOf(
            TangemPayCardPageSetting(
                id = Id.Details,
                title = resourceReference(R.string.details_title),
                onClick = {},
                iconRes = CoreUiR.drawable.ic_visa_card_details_24,
            ),
            TangemPayCardPageSetting(
                id = Id.Freeze,
                title = resourceReference(
                    if (isFrozen) {
                        R.string.tangem_pay_freeze_card_unfreeze
                    } else {
                        R.string.tangem_pay_freeze_card_freeze
                    },
                ),
                onClick = {},
                iconRes = CoreUiR.drawable.ic_freeze_24,
                isLoading = true,
            ),
            TangemPayCardPageSetting(
                id = Id.ChangePin,
                title = resourceReference(R.string.tangem_pay_pin_code_title),
                onClick = {},
                iconRes = CoreUiR.drawable.ic_card_pin_24,
                isEnabled = false,
            ),
        )
    }
}