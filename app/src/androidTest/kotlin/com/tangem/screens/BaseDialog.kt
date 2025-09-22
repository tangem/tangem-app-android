package com.tangem.screens

import io.github.kakaocup.kakao.common.views.KBaseView

abstract class BaseDialog : KBaseView<BaseDialog>({ isRoot() }) {

    init {
        setDialogRoot()
    }

    private fun setDialogRoot() {
        inRoot { isDialog() }
    }
}