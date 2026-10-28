package com.tangem.features.jointaccount.main.component

import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.jointaccount.main.entity.JointAccountMembersSheetConfig
import com.tangem.features.jointaccount.main.model.JointAccountMembersModel
import javax.inject.Inject

internal class JointAccountMembersChildFactory @Inject constructor() {

    fun createChild(
        config: JointAccountMembersSheetConfig,
        appComponentContext: AppComponentContext,
        model: JointAccountMembersModel,
    ): ComposableBottomSheetComponent {
        val onDismiss = { model.bottomSheetNavigation.dismiss() }

        return when (config) {
            is JointAccountMembersSheetConfig.MemberCard -> MemberCardComponent(
                appComponentContext = appComponentContext,
                params = MemberCardComponent.Params(
                    avatar = config.card.avatar,
                    name = config.card.name,
                    address = stringReference(config.card.address),
                    onCopyClick = { model.onCopyAddressClick(config.card.address) },
                    onDismiss = onDismiss,
                ),
            )
            is JointAccountMembersSheetConfig.AboutSignatures -> AboutSignaturesComponent(
                appComponentContext = appComponentContext,
                params = AboutSignaturesComponent.Params(
                    threshold = config.threshold,
                    total = config.total,
                    onDismiss = onDismiss,
                ),
            )
            is JointAccountMembersSheetConfig.InviteMember -> InviteMemberComponent(
                appComponentContext = appComponentContext,
                params = InviteMemberComponent.Params(
                    message = config.message,
                    onShareClick = model::onShareInviteClick,
                    onDismiss = onDismiss,
                ),
            )
        }
    }
}