package com.tangem.features.jointaccount.main.component

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.jointaccount.main.ui.JointAccountShareSafelyBS

/**
 * "Sharing safely" info modal: static explainer shown from the members screen's share-safely banner.
 *
 * Presented as a bottom-sheet child slot over the members screen. Internal to the feature: hosted only
 * by [DefaultJointAccountMembersComponent], so it is constructed directly rather than through an api
 * contract / Dagger factory.
 */
internal class ShareSafelyComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        JointAccountShareSafelyBS(onDismiss = ::dismiss)
    }

    data class Params(val onDismiss: () -> Unit)
}