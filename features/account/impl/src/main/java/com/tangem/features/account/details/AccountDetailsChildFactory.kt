package com.tangem.features.account.details

import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.account.details.entity.AccountDetailsSheetConfig
import com.tangem.features.jointaccount.main.JointAboutSignaturesComponent
import com.tangem.features.jointaccount.supportednetworks.JointSupportedNetworksComponent
import javax.inject.Inject

internal class AccountDetailsChildFactory @Inject constructor(
    private val supportedNetworksComponentFactory: JointSupportedNetworksComponent.Factory,
    private val aboutSignaturesComponentFactory: JointAboutSignaturesComponent.Factory,
) {

    fun createChild(
        config: AccountDetailsSheetConfig,
        appComponentContext: AppComponentContext,
        model: AccountDetailsModel,
    ): ComposableBottomSheetComponent {
        val onDismiss = { model.bottomSheetNavigation.dismiss() }

        return when (config) {
            is AccountDetailsSheetConfig.SupportedNetworks -> supportedNetworksComponentFactory.create(
                context = appComponentContext,
                params = JointSupportedNetworksComponent.Params(onDismiss = onDismiss),
            )
            is AccountDetailsSheetConfig.AboutSignatures -> aboutSignaturesComponentFactory.create(
                context = appComponentContext,
                params = JointAboutSignaturesComponent.Params(
                    threshold = config.threshold,
                    total = config.total,
                    onDismiss = onDismiss,
                ),
            )
        }
    }
}