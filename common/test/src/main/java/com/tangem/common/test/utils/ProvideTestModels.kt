package com.tangem.common.test.utils

import org.junit.jupiter.params.provider.MethodSource

/**
[REDACTED_AUTHOR]
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MethodSource("provideTestModels")
annotation class ProvideTestModels