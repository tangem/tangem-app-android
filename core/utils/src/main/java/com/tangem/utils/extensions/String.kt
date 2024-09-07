package com.tangem.utils.extensions

import com.tangem.utils.StringsSigns.STARS

/**
 * Returns the string itself if hide is false, otherwise returns a string of stars.
 *
 * @param hide A boolean flag that determines whether to hide the string.
 * If true, the string will be hidden (replaced by `STARS`).
 * If false, the original string will be returned.
 *
 * @return The original string if hide is false, or STARS if hide is true.
 */
fun String.orHide(hide: Boolean): String {
    return if (hide) STARS else this
}