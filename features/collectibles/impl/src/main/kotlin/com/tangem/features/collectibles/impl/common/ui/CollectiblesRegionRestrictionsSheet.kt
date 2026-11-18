package com.tangem.features.collectibles.impl.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.message.modal.MessageModal
import com.tangem.core.ui.message.modal.MessageModalUM
import com.tangem.core.ui.message.modal.messageModalUM
import com.tangem.domain.collectibles.models.CollectiblesRegionStatus

/**
 * Whether an action closed by the region may run; on `false` the user has already been told why. A restricted region
 * calls [onRestricted] to show the sheet; a region that could not be checked closes the action with a generic error,
 * since the request may have failed for want of a network.
 */
internal suspend fun handleRegionStatus(
    status: CollectiblesRegionStatus,
    messageSender: UiMessageSender,
    onRestricted: suspend () -> Unit,
): Boolean {
    return when (status) {
        CollectiblesRegionStatus.ALLOWED -> true
        CollectiblesRegionStatus.RESTRICTED -> {
            onRestricted()
            false
        }
        CollectiblesRegionStatus.UNKNOWN -> {
            messageSender.send(SnackbarMessage(message = resourceReference(R.string.common_something_went_wrong)))
            false
        }
    }
}

// TODO: drawn by each screen's own sheet slot only because a ModalMessage sent through UiMessageSender is rendered
//  outside the modal host, as an opaque dialog window without glass. Once that is fixed, send it as a ModalMessage
//  instead, and revisit every Packs dialog shown the same way.
@Composable
internal fun CollectiblesRegionRestrictionsSheet(onDismiss: () -> Unit) {
    val state = remember { regionRestrictionsModalUM() }

    MessageModal(state = state, onDismissRequest = onDismiss)
}

internal class CollectiblesRegionRestrictionsSheetComponent(
    appComponentContext: AppComponentContext,
    private val onDismiss: () -> Unit,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    override fun dismiss() {
        onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        CollectiblesRegionRestrictionsSheet(onDismiss = onDismiss)
    }
}

private fun regionRestrictionsModalUM(): MessageModalUM = messageModalUM {
    status {
        variant = TangemModalStatus.Variant.Warning
        title = resourceReference(R.string.collectibles_region_restrictions_title)
        description = resourceReference(R.string.collectibles_region_restrictions_subtitle)
    }
    primaryButton {
        text = resourceReference(R.string.common_close)
    }
}