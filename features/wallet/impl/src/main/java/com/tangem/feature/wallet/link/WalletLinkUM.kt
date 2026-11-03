package com.tangem.feature.wallet.link

import com.tangem.core.ui.message.modal.MessageModalUM

/** [requestId] stays the same while one request updates its [modal], and changes for the next request. */
internal data class WalletLinkUM(
    val requestId: Long,
    val modal: MessageModalUM,
)