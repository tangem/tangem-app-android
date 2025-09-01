package com.tangem.core.ui.components.appbar.models

import androidx.annotation.DrawableRes
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference

sealed class TopAppBarButtonUM(
    open val onClicked: () -> Unit,
    open val enabled: Boolean = true,
) {

    data class Icon(
        @DrawableRes val iconRes: Int,
        override val onClicked: () -> Unit,
        override val enabled: Boolean = true,
    ) : TopAppBarButtonUM(onClicked, enabled)

    data class Text(
        val text: TextReference,
        override val onClicked: () -> Unit,
        override val enabled: Boolean = true,
    ) : TopAppBarButtonUM(onClicked, enabled)

    @Suppress("FunctionName")
    companion object {

        fun Back(onBackClicked: () -> Unit) = Back(true, onBackClicked)

        fun Back(enabled: Boolean = true, onBackClicked: () -> Unit) = Icon(
            iconRes = R.drawable.ic_back_24,
            onClicked = onBackClicked,
            enabled = enabled,
        )

        fun Close(enabled: Boolean = true, onCloseClick: () -> Unit) = Icon(
            iconRes = R.drawable.ic_close_24,
            onClicked = onCloseClick,
            enabled = enabled,
        )

        fun Text(text: TextReference, onTextClicked: () -> Unit, enabled: Boolean = true) = Text(
            text = text,
            onClicked = onTextClicked,
            enabled = enabled,
        )
    }
}