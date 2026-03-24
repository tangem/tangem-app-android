package com.tangem.tap.common

import android.app.Dialog
import android.content.Context
import com.tangem.domain.redux.StateDialog
import com.tangem.tap.common.redux.global.GlobalState
import com.tangem.tap.common.ui.ScanFailsDialog
import com.tangem.tap.features.onboarding.OnboardingDialog
import com.tangem.tap.features.onboarding.products.wallet.ui.dialogs.WalletActivationErrorDialog
import com.tangem.tap.store
import org.rekotlin.StoreSubscriber

class DialogManager : StoreSubscriber<GlobalState> {
    var context: Context? = null
    private var dialog: Dialog? = null

    fun onStart(context: Context) {
        this.context = context
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.globalState == newState.globalState
            }.select { it.globalState }
        }
    }

    fun onStop() {
        this.context = null
        store.unsubscribe(this)
    }

    @Suppress("LongMethod", "ComplexMethod")
    override fun newState(state: GlobalState) {
        if (state.dialog == null) {
            dialog?.dismiss()
            dialog = null
            return
        }
        val context = context ?: return
        if (dialog != null) return

        dialog = when (state.dialog) {
            is StateDialog.ScanFailsDialog -> ScanFailsDialog.create(
                context = context,
                source = state.dialog.source,
                onTryAgain = state.dialog.onTryAgain,
            )
            is OnboardingDialog.WalletActivationError -> WalletActivationErrorDialog.create(context, state.dialog)
            else -> null
        }
        dialog?.show()
    }
}