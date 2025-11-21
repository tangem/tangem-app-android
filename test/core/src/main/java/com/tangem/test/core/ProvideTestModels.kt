package com.tangem.test.core

import org.junit.jupiter.params.provider.MethodSource

/**
[REDACTED_AUTHOR]
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MethodSource("provideTestModels")
annotation class ProvideTestModels