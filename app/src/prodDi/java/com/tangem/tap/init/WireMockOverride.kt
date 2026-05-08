package com.tangem.tap.init

/**
 * Production variant (every buildType except `mocked`): no-op. The real
 * implementation lives in `app/src/mocked/java/com/tangem/tap/init/WireMockOverride.kt`
 * and is only present on the classpath for the `mocked` buildType.
 */
object WireMockOverride {
    fun apply() = Unit
}