package com.tangem.core.ui.extensions

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource

sealed interface TextReference {
    class Res(@StringRes val id: Int, val formatArgs: List<Any> = emptyList()) : TextReference {
        constructor(@StringRes id: Int, vararg formatArgs: Any) : this(id, formatArgs.toList())
    }

    class Str(val value: String) : TextReference
}

@Composable
@ReadOnlyComposable
fun TextReference.resolveReference(): String {
    return when (this) {
        is TextReference.Res -> stringResource(this.id, *this.formatArgs.toTypedArray())
        is TextReference.Str -> this.value
    }
}