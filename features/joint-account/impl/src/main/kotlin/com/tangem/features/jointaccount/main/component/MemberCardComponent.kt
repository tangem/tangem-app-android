package com.tangem.features.jointaccount.main.component

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.ui.MemberCardModal

/**
 * Member card modal: a single joint-account member shown for out-of-band verification.
 *
 * Presented as a bottom-sheet child slot over the members screen. The [Params.address] is rendered
 * **in full** and must never be shortened in the middle — this card is the only member-verification
 * mechanism in MVP1.
 *
 * Internal to the feature: hosted only by [DefaultJointAccountMembersComponent], so it is constructed
 * directly rather than through an api contract / Dagger factory.
 */
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

    /**
     * @property avatar      colored monogram avatar.
     * @property name        member's display name.
     * @property address     the full, unshortened owner address.
     * @property onCopyClick copy the full address to the clipboard.
     * @property onDismiss   dismiss the card.
     */
    data class Params(
        val avatar: JointAccountMembersUM.MemberAvatarUM,
        val name: TextReference,
        val address: TextReference,
        val onCopyClick: () -> Unit,
        val onDismiss: () -> Unit,
    )
}