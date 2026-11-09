package com.tangem.core.local.moshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.utils.logging.TangemLogger
import java.lang.reflect.Type

/**
 * Reports classes a persistence [Moshi] would read by reflection, nested ones included: R8 renames their members
 * between releases. Add it after the custom adapters, right before KotlinJsonAdapterFactory.
 */
class PersistedModelGuard(private val isStrict: Boolean) : JsonAdapter.Factory {

    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        val rawType = Types.getRawType(type)
        if (annotations.isEmpty() && rawType.isReadByReflection() && !rawType.isKeptCardSdkModel()) {
            val message = "${rawType.name} is persisted through KotlinJsonAdapterFactory, as it has no " +
                "generated adapter. Annotate it with @JsonClass(generateAdapter = true) in a module with Moshi " +
                "codegen, or persist a DTO that has one"
            TangemLogger.e(message)
            // Moshi adds the path from the persisted type only to an IllegalArgumentException
            require(!isStrict) { message }
        }
        return null
    }

    private fun Class<*>.isReadByReflection(): Boolean {
        return !isInterface && !isEnum && isAnnotationPresent(Metadata::class.java) && !isPlatformType() &&
            !hasGeneratedAdapter()
    }

    private fun Class<*>.isPlatformType(): Boolean = PLATFORM_PACKAGES.any(name::startsWith)

    private fun Class<*>.isKeptCardSdkModel(): Boolean = CARD_SDK_KEPT_PACKAGES.any(name::startsWith)

    private fun Class<*>.hasGeneratedAdapter(): Boolean {
        val adapterName = Types.generatedJsonAdapterName(name)
        return getAnnotation(JsonClass::class.java)?.generateAdapter == true &&
            runCatching { Class.forName(adapterName, false, classLoader) }.isSuccess
    }

    private companion object {
        val PLATFORM_PACKAGES = listOf("android.", "androidx.", "java.", "javax.", "kotlin.", "kotlinx.", "scala.")

        /** Card SDK models have no generated adapters, so proguard-rules.pro keeps these packages whole */
        val CARD_SDK_KEPT_PACKAGES = listOf(
            "com.tangem.operations.",
            "com.tangem.crypto.",
            "com.tangem.common.card.",
            "com.tangem.common.json.",
        )
    }
}