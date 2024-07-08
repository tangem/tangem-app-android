package com.tangem.core.decompose.factory

import com.tangem.core.decompose.context.AppComponentContext

interface ComponentFactoryNoParams<C : Any> {

    fun create(context: AppComponentContext): C
}
