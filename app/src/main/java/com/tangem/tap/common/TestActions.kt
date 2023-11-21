package com.tangem.tap.common

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.widget.LinearLayoutCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.store

/**
[REDACTED_AUTHOR]
 */

object TestActions {

    // It used only for the test actions in debug or debug_beta builds
    var testAmountInjectionForWalletManagerEnabled = false
}

typealias TestAction = Pair<String, () -> Unit>

class TestActionsBottomSheetDialog(
    private val appDialog: AppDialog.TestActionsDialog,
    context: Context,
) : BottomSheetDialog(context) {

    @Suppress("MagicNumber")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(createContentView())
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.peekHeight = 400
        setOnCancelListener {
            store.dispatchDialogHide()
        }
    }

    private fun createContentView(): View {
        val actionButtons = mutableListOf<View>()
        appDialog.actionsList.forEach { (actionName, action) ->
            val view = Button(context).apply {
                text = actionName
                setOnClickListener {
                    action()
                    cancel()
                }
            }
            actionButtons.add(view)
        }

        return LinearLayoutCompat(context).apply {
            orientation = LinearLayoutCompat.VERTICAL
            actionButtons.forEach { addView(it) }
        }
    }
}