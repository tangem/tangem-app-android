package com.tangem.tap.features.onboarding.products.wallet.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.features.onboarding.products.wallet.redux.AccessCodeError
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupAction
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.layout_backup_access_code.*
import kotlinx.android.synthetic.main.layout_backup_access_code_info.*
import kotlinx.android.synthetic.main.layout_backup_access_code_submit.*

class AccessCodeDialog(context: Context) : BottomSheetDialog(context) {


    init {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        val dialogView = LayoutInflater.from(context).inflate(R.layout.layout_backup_access_code, null)
        setContentView(dialogView)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun setContentView(view: View) {
        super.setContentView(view)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }


    fun showInfoScreen() {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        access_code_title.text = context.getText(R.string.onboarding_access_code_intro_title)
        layout_backup_access_code_info.show()
        layout_backup_access_code_submit.hide()
        btn_access_code_create.setOnClickListener {
            store.dispatch(BackupAction.ShowEnterAccessCodeScreen)
        }
    }

    fun showEnterAccessCode() {
        layout_backup_access_code_info.hide()
        layout_backup_access_code_submit.show()
        access_code_title.text = context.getText(R.string.onboarding_access_code_intro_title)
        btn_access_code_submit.text = context.getText(R.string.common_continue)
        btn_access_code_submit.setOnClickListener {
            store.dispatch(BackupAction.CheckAccessCode(et_access_code.text.toString()))
        }
    }

    fun showReenterAccessCode() {
        layout_backup_access_code_info.hide()
        layout_backup_access_code_submit.show()
        access_code_title.text = context.getText(R.string.onboarding_access_code_repeat_code_title)
        et_access_code.setText("")
        btn_access_code_submit.text = context.getText(R.string.common_submit)
        btn_access_code_submit.setOnClickListener {
            store.dispatch(BackupAction.SaveAccessCodeConfirmation(et_access_code.text.toString()))
        }
    }

    fun showError(error: AccessCodeError?) {
        when (error) {
            AccessCodeError.CodeTooShort ->
                til_access_code.error = context.getString(R.string.onboarding_access_code_too_short)
            AccessCodeError.CodesDoNotMatch ->
                til_access_code.error = context.getString(R.string.onboarding_access_codes_doesnt_match)
            null -> til_access_code.error = null
        }
    }
}