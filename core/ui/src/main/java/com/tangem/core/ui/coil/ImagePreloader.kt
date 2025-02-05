package com.tangem.core.ui.coil

/**
 * Preload image and store in runtime memory
 */
interface ImagePreloader {
    fun preload(url: String)
}