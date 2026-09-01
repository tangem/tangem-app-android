package com.tangem.features.hotwallet.createmobilewallet.importoptions.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference

internal data class ImportOptionsBottomSheetUM(
    val content: Content,
    val onDismiss: () -> Unit,
) : TangemBottomSheetConfigContent {

    @Immutable
    sealed interface Content {

        data class Options(
            val isCloudLoading: Boolean,
            val onRecoveryPhraseClick: () -> Unit,
            val onCloudBackupClick: () -> Unit,
        ) : Content

        data class Error(
            val title: TextReference,
            val body: TextReference,
            val isWarning: Boolean,
            val onGotItClick: () -> Unit,
        ) : Content
    }
}