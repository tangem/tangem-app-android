package com.tangem.tap.common

import android.view.View

interface SnackbarHandler {
    fun showSnackbar(text: Int, buttonTitle: Int? = null, action: View.OnClickListener? = null)
    fun dismissSnackbar()
}