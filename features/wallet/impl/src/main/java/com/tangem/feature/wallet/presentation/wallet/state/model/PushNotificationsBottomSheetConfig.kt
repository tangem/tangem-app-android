package com.tangem.feature.wallet.presentation.wallet.state.model

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

data class PushNotificationsBottomSheetConfig(
    val onRequest: () -> Unit,
    val onNeverRequest: () -> Unit,
    val onAllow: () -> Unit,
    val onDeny: () -> Unit,
) : TangemBottomSheetConfigContent