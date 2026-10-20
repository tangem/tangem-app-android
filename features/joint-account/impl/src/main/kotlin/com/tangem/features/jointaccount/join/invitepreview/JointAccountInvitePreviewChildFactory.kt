package com.tangem.features.jointaccount.join.invitepreview

import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.jointaccount.join.invitepreview.entity.JointAccountInvitePreviewSheetConfig
import com.tangem.features.jointaccount.join.invitepreview.model.JointAccountInvitePreviewModel
import com.tangem.features.jointaccount.main.component.AboutSignaturesComponent
import com.tangem.features.jointaccount.main.component.MemberCardComponent
import javax.inject.Inject

internal class JointAccountInvitePreviewChildFactory @Inject constructor(
    private val portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
) {

    fun createChild(
        config: JointAccountInvitePreviewSheetConfig,
        appComponentContext: AppComponentContext,
        model: JointAccountInvitePreviewModel,
    ): ComposableBottomSheetComponent {
        val onDismiss = { model.bottomSheetNavigation.dismiss() }

        return when (config) {
            is JointAccountInvitePreviewSheetConfig.WalletSelector -> portfolioSelectorComponentFactory.create(
                context = appComponentContext,
                params = PortfolioSelectorComponent.Params(
                    portfolioFetcher = model.portfolioFetcher,
                    controller = model.portfolioSelectorController,
                    bsCallback = model.portfolioSelectorCallback,
                    settings = PortfolioSelectorComponent.Settings(isWalletSelectionOnly = true),
                ),
            )
            is JointAccountInvitePreviewSheetConfig.CreatorCard -> MemberCardComponent(
                appComponentContext = appComponentContext,
                params = MemberCardComponent.Params(
                    avatar = config.card.avatar,
                    name = config.card.name,
                    address = stringReference(config.card.address),
                    onCopyClick = { model.onCopyAddressClick(config.card.address) },
                    onDismiss = onDismiss,
                ),
            )
            is JointAccountInvitePreviewSheetConfig.AboutSignatures -> AboutSignaturesComponent(
                appComponentContext = appComponentContext,
                params = AboutSignaturesComponent.Params(
                    threshold = config.threshold,
                    total = config.total,
                    onDismiss = onDismiss,
                ),
            )
        }
    }
}