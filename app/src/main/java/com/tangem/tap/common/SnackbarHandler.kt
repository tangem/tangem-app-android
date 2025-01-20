package com.tangem.tap.common

import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import com.tangem.core.ui.extensions.TextReference

interface SnackbarHandler {

    fun showSnackbar(
        @StringRes text: Int,
        length: Int = Snackbar.LENGTH_INDEFINITE,
        @StringRes buttonTitle: Int? = null,
        action: (() -> Unit)? = null,
    )

    fun showSnackbar(
        text: TextReference,
        length: Int = Snackbar.LENGTH_INDEFINITE,
        buttonTitle: TextReference? = null,
        action: (() -> Unit)? = null,
    )

    fun dismissSnackbar()
}