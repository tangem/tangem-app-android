package com.tangem.features.jointaccount.main.component

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.jointaccount.main.ui.AboutSignaturesBS

/**
 * "About signatures" sheet: explains the account's approval rule.
 *
 * Internal to the feature: hosted only by [DefaultJointAccountMembersComponent], so it is constructed
 * directly rather than through an api contract / Dagger factory.
 */
internal class AboutSignaturesComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        AboutSignaturesBS(threshold = params.threshold, total = params.total, onDismiss = ::dismiss)
    }

    /**
     * @property threshold signatures required to execute an operation.
     * @property total     members of the account.
     * @property onDismiss close the sheet.
     */
    data class Params(
        val threshold: Int,
        val total: Int,
        val onDismiss: () -> Unit,
    )
}