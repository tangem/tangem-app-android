package com.tangem.tangemtest.ucase.tunnel

import com.google.android.material.snackbar.Snackbar
import com.tangem.tangemtest._arch.structure.Id

interface SnackbarHolder {
    fun showSnackbar(message: String, length: Int = Snackbar.LENGTH_SHORT)
    fun showSnackbar(id: Int, length: Int = Snackbar.LENGTH_SHORT)
}

interface ViewScreen

interface ActionView : ViewScreen, SnackbarHolder {
    fun enableActionFab(enable: Boolean)
    fun showSnackbar(id: Id)
}