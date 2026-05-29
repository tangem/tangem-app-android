package com.tangem.features.tangempay.entity

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.details.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.tangem.core.ui.R as CoreUiR

@Immutable
internal data class TangemPayCardPageUM(
    val settings: ImmutableList<TangemPayCardPageSetting>,
    val settingsV2: ImmutableList<TangemPayCardPageSettingV2>,
    val onBackClick: () -> Unit,
    val dailyLimitState: TangemPayDailyLimitBlockState,
    val addToWalletBlockState: AddToWalletBlockState? = null,
    val isReissueInProgress: Boolean = false,
) {
    companion object {
        fun stub(
            addToWalletBlockState: AddToWalletBlockState? = AddToWalletBlockState(
                onClick = {},
                onClickClose = {},
                shouldUseMagicEffect = false,
            ),
            settings: ImmutableList<TangemPayCardPageSetting> = persistentListOf(
                TangemPayCardPageSetting(TextReference.Str("Pin Code")) {},
                TangemPayCardPageSetting(TextReference.Str("Freeze Card")) {},
                TangemPayCardPageSetting(TextReference.Str("Reissue Card")) {},
            ),
            isReissueInProgress: Boolean = false,
            dailyLimitState: TangemPayDailyLimitBlockState = TangemPayDailyLimitBlockState.Content.stub(),
            settingsV2: ImmutableList<TangemPayCardPageSettingV2> = TangemPayCardPageSettingV2.stubList(),
        ) = TangemPayCardPageUM(
            addToWalletBlockState = addToWalletBlockState,
            settings = settings,
            settingsV2 = settingsV2,
            onBackClick = {},
            isReissueInProgress = isReissueInProgress,
            dailyLimitState = dailyLimitState,
        )
    }
}

@Immutable
internal data class TangemPayCardPageSetting(
    val title: TextReference,
    val testTag: String? = null,
    val onSettingClick: () -> Unit,
)

@Immutable
internal data class TangemPayCardPageSettingV2(
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
        fun stubList(isFrozen: Boolean = false): ImmutableList<TangemPayCardPageSettingV2> = persistentListOf(
            TangemPayCardPageSettingV2(
                id = Id.Details,
                title = resourceReference(R.string.details_title),
                onClick = {},
                iconRes = CoreUiR.drawable.ic_visa_card_details_24,
            ),
            TangemPayCardPageSettingV2(
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
            ),
            TangemPayCardPageSettingV2(
                id = Id.ChangePin,
                title = resourceReference(R.string.tangem_pay_pin_code_title),
                onClick = {},
                iconRes = CoreUiR.drawable.ic_card_pin_24,
            ),
        )
    }
}