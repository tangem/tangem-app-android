package com.tangem.features.jointaccount.main.component

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.ui.MemberCardModal

/** [Params.address] is rendered in full and never shortened: the card is how members verify each other in MVP1 */
internal class MemberCardComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {
    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        MemberCardModal(
            avatar = params.avatar,
            name = params.name,
            address = params.address,
            onCopyClick = params.onCopyClick,
            onDismiss = ::dismiss,
        )
    }

    data class Params(
        val avatar: JointAccountMembersUM.MemberAvatarUM,
        val name: TextReference,
        val address: TextReference,
        val onCopyClick: () -> Unit,
        val onDismiss: () -> Unit,
    )
}