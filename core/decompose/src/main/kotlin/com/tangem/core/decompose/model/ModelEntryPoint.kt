package com.tangem.core.decompose.model

import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.instancekeeper.getOrCreateSimple
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.di.DecomposeComponent
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import javax.inject.Provider

/**
 * Entry point for the models in the application.
 *
 * It provides a map of model providers.
 */
@EntryPoint
@InstallIn(DecomposeComponent::class)
interface ModelsEntryPoint {

    fun models(): Map<Class<*>, Provider<Model>>
}

/**
 * Gets or creates a component's [Model].
 */
inline fun <reified M : Model> AppComponentContext.getOrCreateModel(): M {
    val modelKey = "model_${M::class.simpleName}"

    val entryPoint = instanceKeeper.getOrCreateSimple(key = "modelsEntryPoint") {
        val hiltComponent = hiltComponentBuilder
            .router(router)
            .uiMessageSender(messageSender)
            .build()

        EntryPoints.get(hiltComponent, ModelsEntryPoint::class.java)
    }

    val model = instanceKeeper.getOrCreate(modelKey) {
        requireNotNull(entryPoint.models()[M::class.java]?.get()) {
            "Model ${M::class.simpleName} is not provided"
        }
    }

    val isModelExist = tags.getOrElse(modelKey) { false } as Boolean
    if (!isModelExist) {
        tags[modelKey] = true
    }

    return model as M
}