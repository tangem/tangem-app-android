package com.tangem.feature.wallet.presentation.wallet.state.model

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

data class PushNotificationsBottomSheetConfig(
    val isFirstTimeAsking: Boolean,
    val onRequest: () -> Unit,
    val onAllow: () -> Unit,
    val onDeny: () -> Unit,
    val openSettings: () -> Unit,
) : TangemBottomSheetConfigContent