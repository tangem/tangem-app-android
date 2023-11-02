package com.tangem.core.ui.extensions

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

/**
 * Utility class for keeping image reference when it can be an image resource or a URL.
 *
 * It necessary to use [Immutable] annotation because all sealed interface has runtime stability.
 * All subclasses are stable.
 */
sealed class ImageReference {

    data class Url(val url: String) : ImageReference()

    data class Res(@DrawableRes val resId: Int) : ImageReference()

    /**
     * Provides reference of any type. It can be used for image loading,
     * when the type of reference is resolved further down the call site in the library.
     *
     * @return image reference either as an image resource ([Int]) or a URL ([String])
     */
    fun getReference(): Any {
        return when (this) {
            is Res -> resId
            is Url -> url
        }
    }
}