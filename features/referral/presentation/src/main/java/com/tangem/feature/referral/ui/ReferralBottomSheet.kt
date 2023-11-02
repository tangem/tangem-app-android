package com.tangem.feature.referral.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.referral.models.ReferralStateHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReferralBottomSheet(
    sheetState: SheetState,
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    config: ReferralStateHolder.ReferralInfoState,
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            shape = RoundedCornerShape(
                topStart = TangemTheme.dimens.radius16,
                topEnd = TangemTheme.dimens.radius16,
            ),
            containerColor = TangemTheme.colors.background.primary,
        ) {
            AgreementBottomSheetContent(
                url = when (config) {
                    is ReferralStateHolder.ReferralInfoState.NonParticipantContent -> config.url
                    is ReferralStateHolder.ReferralInfoState.ParticipantContent -> config.url
                    is ReferralStateHolder.ReferralInfoState.Loading -> ""
                },
            )
        }
    }
}