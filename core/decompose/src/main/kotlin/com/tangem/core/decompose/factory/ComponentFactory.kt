package com.tangem.core.decompose.factory

import com.tangem.core.decompose.context.AppComponentContext

interface ComponentFactory<Component : Any, Params : Any> {

    fun create(context: AppComponentContext, params: Params): Component
}