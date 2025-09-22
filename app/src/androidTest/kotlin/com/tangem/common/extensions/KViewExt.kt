package com.tangem.common.extensions

import io.github.kakaocup.kakao.common.views.KBaseView

fun <T : KBaseView<*>> T.withDialogRoot(): T {
    return this.also { inRoot { isDialog() } }
}