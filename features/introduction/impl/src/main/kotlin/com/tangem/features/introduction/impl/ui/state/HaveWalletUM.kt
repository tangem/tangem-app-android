package com.tangem.features.introduction.impl.ui.state

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.hotwallet.cloudrestore.CloudRestoreErrorMessage

/**
 * The sheet keeps one content type across all of its steps on purpose: [com.tangem.core.ui.ds2.modal.TangemModal]
 * is reified on the content and stops rendering the moment the type stops matching, which would cut the sheet
 * off instead of morphing it.
 */
internal data class HaveWalletUM(
    val step: Step,
    val onCloseClick: () -> Unit,
) : TangemBottomSheetConfigContent {

    sealed interface Step {

        data class Options(
            val cardOrRing: WalletOptionUM,
            val socialLogin: WalletOptionUM?,
            val importWallet: WalletOptionUM,
        ) : Step

        data class Import(
            val title: TextReference,
            val description: TextReference,
            val recoveryPhraseButtonText: TextReference,
            val cloudBackupButtonText: TextReference?,
            val isCloudLoading: Boolean,
            val onRecoveryPhraseClick: () -> Unit,
            val onCloudBackupClick: () -> Unit,
            val onBack: () -> Unit,
        ) : Step

        data class Error(
            val message: CloudRestoreErrorMessage,
            val onGotItClick: () -> Unit,
        ) : Step
    }
}