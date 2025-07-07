package com.tangem.feature.qrscanning.presentation.transformers

import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.feature.qrscanning.impl.R
import com.tangem.feature.qrscanning.model.QrScanningClickIntents
import com.tangem.feature.qrscanning.presentation.PasteAction
import com.tangem.feature.qrscanning.presentation.QrScanningState
import com.tangem.feature.qrscanning.presentation.TopBarConfig

private const val WC_SCHEME = "wc"

internal class InitializeQrScanningStateTransformer(
    private val clickIntents: QrScanningClickIntents,
    private val clipboardManager: ClipboardManager,
    private val source: SourceType,
    private val network: String?,
) : QrScanningTransformer {

    override fun transform(prevState: QrScanningState): QrScanningState {
        val message = when (source) {
            SourceType.SEND -> network?.let { resourceReference(R.string.send_qrcode_scan_info, wrappedList(it)) }
            SourceType.WALLET_CONNECT -> stringReference("Open Web3 app and chose WalletConnect option")
        }

        return QrScanningState(
            topBarConfig = constructTopBarConfig(),
            message = message,
            onBackClick = clickIntents::onBackClick,
            onQrScanned = clickIntents::onQrScanned,
            onGalleryClick = clickIntents::onGalleryClicked,
            pasteAction = constructPasteAction(),
        )
    }

    private fun constructTopBarConfig(): TopBarConfig {
        return when (source) {
            SourceType.SEND -> TopBarConfig(title = null, startIcon = R.drawable.ic_back_24)
            SourceType.WALLET_CONNECT -> TopBarConfig(
                title = resourceReference(R.string.wc_new_connection),
                startIcon = R.drawable.ic_close_24,
            )
        }
    }

    private fun constructPasteAction(): PasteAction {
        val uri = clipboardManager.getText()
        return if (source == SourceType.WALLET_CONNECT && uri != null && isWalletConnectUri(uri)) {
            PasteAction.Perform { clickIntents.onQrScanned(uri) }
        } else {
            PasteAction.None
        }
    }

    private fun isWalletConnectUri(uri: String): Boolean {
        return uri.lowercase().startsWith(WC_SCHEME)
    }
}