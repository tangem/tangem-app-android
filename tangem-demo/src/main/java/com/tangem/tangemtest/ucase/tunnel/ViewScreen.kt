package com.tangem.tangemtest.ucase.tunnel

import com.tangem.tangemtest._arch.structure.Id

interface ViewScreen

interface SnackbarHolder {
    fun showSnackbar(message: String)
    fun showSnackbar(id: Int)
    fun showSnackbar(id: Id)
}

interface ActionView : ViewScreen, SnackbarHolder {
    fun showActionFab(show: Boolean)
}