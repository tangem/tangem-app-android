package com.tangem.di

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import com.google.android.material.snackbar.Snackbar
import com.tangem.Constant
import com.tangem.wallet.R


class ToastHelper {

    fun showSnackbarUpdateVersion(context: Context, vg: ViewGroup, versionName: String) {
        Snackbar.make(vg, String.format(context.getString(R.string.new_app_version), versionName), Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.update) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse(Constant.URL_TANGEM)
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                    }
                }.show()
    }

    fun showSnackbarSuccess(context: Context, vg: ViewGroup, message: String) {
        val snackbar = Snackbar.make(vg, message, Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction(R.string.ok) {
            snackbar.dismiss()
        }
        snackbar.show()
    }

    fun showSnackbarError(context: Context, vg: ViewGroup, message: String) {
        val snackbar = Snackbar.make(vg, message, Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction(R.string.ok) {
            snackbar.dismiss()
        }
        snackbar.show()
    }

}