package com.tangem.feature.wallet.presentation.wallet.state.model

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

data class PushNotificationsBottomSheetConfig(
    val isFirstTimeRequested: Boolean,
    val wasInitiallyAsk: Boolean,
    val onRequest: () -> Unit,
    val onRequestLater: () -> Unit,
    val onAllow: () -> Unit,
    val onDeny: () -> Unit,
    val openSettings: () -> Unit,
) : TangemBottomSheetConfigContent