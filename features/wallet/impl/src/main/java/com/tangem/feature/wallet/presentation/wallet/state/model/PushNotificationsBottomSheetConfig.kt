package com.tangem.feature.wallet.presentation.wallet.state.model

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

data class PushNotificationsBottomSheetConfig(
    val isFirstTimeAsking: Boolean,
    val onAllow: () -> Unit,
    val onAllowed: () -> Unit,
    val onDeny: () -> Unit,
    val openSettings: () -> Unit,
) : TangemBottomSheetConfigContent