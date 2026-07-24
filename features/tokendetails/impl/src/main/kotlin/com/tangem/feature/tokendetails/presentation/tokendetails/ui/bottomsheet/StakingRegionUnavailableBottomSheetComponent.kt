package com.tangem.feature.tokendetails.presentation.tokendetails.ui.bottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_error_28
import com.tangem.core.res.R as CoreResR

internal class StakingRegionUnavailableBottomSheetComponent(
    private val onDismiss: () -> Unit,
) : ComposableBottomSheetComponent {

    override fun dismiss() {
        onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state = remember {
            messageBottomSheetUM {
                infoBlock {
                    vector(imageVector = Icons.ic_error_28) {
                        type = MessageBottomSheetUM.Vector.Type.Attention
                        backgroundType = MessageBottomSheetUM.Vector.BackgroundType.Attention
                    }
                    title = resourceReference(CoreResR.string.common_staking)
                    body = resourceReference(CoreResR.string.staking_error_unavailable_region_description)
                }
                primaryButton {
                    text = resourceReference(CoreResR.string.common_close)
                    onClick { closeBs() }
                }
                onDismiss { dismiss() }
            }
        }
        MessageBottomSheet(state = state, onDismissRequest = ::dismiss)
    }
}