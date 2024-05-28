package com.tangem.core.decompose.di

/**
 * Annotation for marking a dependency as a component scoped.
 *
 * This means that the lifecycle of the dependency is limited to the lifecycle of the component it is attached to.
 */
@Retention(AnnotationRetention.SOURCE)
annotation class ComponentScoped