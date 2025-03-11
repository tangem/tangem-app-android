package com.tangem.core.decompose.factory

import com.tangem.core.decompose.context.AppComponentContext

interface ComponentFactory<P : Any, C : Any> {

    fun create(context: AppComponentContext, params: P): C
}
