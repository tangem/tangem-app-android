package com.tangem.core.decompose.di

import javax.inject.Scope

/**
 * Annotation for marking a dependency as a model scoped.
 *
 * This means that the lifecycle of the dependency is limited to the lifecycle of the component's model
 */
@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class ModelScoped