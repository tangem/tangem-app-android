package com.tangem.tap.features.onboarding.products.wallet.ui.dialogs

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.features.onboarding.products.wallet.redux.AccessCodeError
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupAction
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.LayoutBackupAccessCodeBinding

class AccessCodeDialog(context: Context) : BottomSheetDialog(context) {

    var binding: LayoutBackupAccessCodeBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutBackupAccessCodeBinding
            .inflate(LayoutInflater.from(context))
        setContentView(binding!!.root)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun setOnDismissListener(listener: DialogInterface.OnDismissListener?) {
        super.setOnDismissListener(listener)
        binding = null
    }

    fun showInfoScreen() = with(binding!!) {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        accessCodeTitle.text = context.getText(R.string.onboarding_access_code_intro_title)
        layoutBackupAccessCodeInfo.root.show()
        layoutBackupAccessCodeSubmit.root.hide()
        layoutBackupAccessCodeInfo.btnAccessCodeCreate.setOnClickListener {
            store.dispatch(BackupAction.ShowEnterAccessCodeScreen)
        }
    }

    fun showEnterAccessCode() = with(binding!!) {
        layoutBackupAccessCodeInfo.root.hide()
        layoutBackupAccessCodeSubmit.root.show()
        accessCodeTitle.text = context.getText(R.string.onboarding_access_code_intro_title)

        with(layoutBackupAccessCodeSubmit) {
            btnAccessCodeSubmit.text = context.getText(R.string.common_continue)
            btnAccessCodeSubmit.setOnClickListener {
                store.dispatch(BackupAction.CheckAccessCode(etAccessCode.text.toString()))
            }
        }
    }

    fun showReenterAccessCode() = with(binding!!) {
        layoutBackupAccessCodeInfo.root.hide()
        layoutBackupAccessCodeSubmit.root.show()
        accessCodeTitle.text = context.getText(R.string.onboarding_access_code_repeat_code_title)
        with(layoutBackupAccessCodeSubmit) {
            etAccessCode.setText("")
            btnAccessCodeSubmit.text = context.getText(R.string.common_submit)
            btnAccessCodeSubmit.setOnClickListener {
                store.dispatch(BackupAction.SaveAccessCodeConfirmation(etAccessCode.text.toString()))
            }
        }
    }

    fun showError(error: AccessCodeError?) = with(binding!!.layoutBackupAccessCodeSubmit) {
        when (error) {
            AccessCodeError.CodeTooShort ->
                tilAccessCode.error = context.getString(R.string.onboarding_access_code_too_short)
            AccessCodeError.CodesDoNotMatch ->
                tilAccessCode.error = context.getString(R.string.onboarding_access_codes_doesnt_match)
            null -> tilAccessCode.error = null
        }
    }
}