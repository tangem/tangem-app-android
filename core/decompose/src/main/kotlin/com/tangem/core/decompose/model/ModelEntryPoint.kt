package com.tangem.core.decompose.model

import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.instancekeeper.getOrCreateSimple
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.di.ModelComponent
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
@InstallIn(ModelComponent::class)
interface ModelsEntryPoint {

    fun models(): Map<Class<*>, Provider<Model>>
}

/**
 * Gets or creates a component's [Model] with no parameters.
 */
inline fun <reified M : Model> AppComponentContext.getOrCreateModel(): M = getOrCreateModel(params = null)

/**
 * Gets or creates a component's [Model].
 *
 * Be careful with objects you pass in parameters as they will be used in [Model] lifecycle.
 * If you pass a object with different lifecycle than the [Model] then you can face memory leaks.
 *
 * @param params The parameters to store in the [ParamsContainer],

 */
inline fun <reified M : Model, reified P : Any> AppComponentContext.getOrCreateModel(params: P?): M {
    val entryPoint = instanceKeeper.getOrCreateSimple(key = "modelsEntryPoint") {
        val hiltComponent = hiltComponentBuilder
            .router(router)
            .uiMessageSender(messageSender)
            .paramsContainer(MutableParamsContainer(params ?: Unit))
            .build()

        EntryPoints.get(hiltComponent, ModelsEntryPoint::class.java)
    }

    val modelKey = "model_${M::class.simpleName}"
    val model = instanceKeeper.getOrCreate(modelKey) {
        requireNotNull(entryPoint.models()[M::class.java]?.get()) {
            "Model ${M::class.simpleName} is not provided"
        } as M
    }

    val isModelExist = tags.getOrElse(modelKey) { false } as Boolean
    if (!isModelExist) {
        tags[modelKey] = true
    }

    return model
}