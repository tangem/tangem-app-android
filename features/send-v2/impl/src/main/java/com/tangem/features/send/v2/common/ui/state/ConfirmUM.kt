package com.tangem.features.send.v2.common.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class ConfirmUM {

    abstract val isPrimaryButtonEnabled: Boolean

    data class Content(
        override val isPrimaryButtonEnabled: Boolean = false,
        val walletName: TextReference,
        val isSending: Boolean,
        val showTapHelp: Boolean,
        val sendingFooter: TextReference,
        val notifications: ImmutableList<NotificationUM>,
    ) : ConfirmUM()

    data class Success(
        val transactionDate: Long,
        val txUrl: String,
    ) : ConfirmUM() {
        override val isPrimaryButtonEnabled: Boolean = true
    }

    data object Empty : ConfirmUM() {
        override val isPrimaryButtonEnabled: Boolean = false
    }
}