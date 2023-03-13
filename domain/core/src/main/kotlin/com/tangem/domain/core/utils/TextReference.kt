package com.tangem.domain.core.utils

sealed interface TextReference {
    class Res(val resId: Int, val formatArgs: List<Any> = emptyList()) : TextReference {
        constructor(resId: Int, vararg formatArgs: Any) : this(resId, formatArgs.toList())
    }

    class Str(val value: String) : TextReference
}
