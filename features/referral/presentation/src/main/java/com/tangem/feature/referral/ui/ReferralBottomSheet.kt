package com.tangem.feature.referral.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.feature.referral.models.ReferralStateHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReferralBottomSheet(
    sheetState: SheetState,
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    config: ReferralStateHolder.ReferralInfoState,
) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    if (isVisible) {
        ModalBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            shape = RoundedCornerShape(
                topStart = TangemTheme.dimens.radius16,
                topEnd = TangemTheme.dimens.radius16,
            ),
            contentWindowInsets = { WindowInsetsZero },
            containerColor = TangemTheme.colors.background.primary,
        ) {
            AgreementBottomSheetContent(
                url = when (config) {
                    is ReferralStateHolder.ReferralInfoState.NonParticipantContent -> config.url
                    is ReferralStateHolder.ReferralInfoState.ParticipantContent -> config.url
                    is ReferralStateHolder.ReferralInfoState.Loading -> ""
                },
                bottomBarHeight = bottomBarHeight,
            )
        }
    }
}