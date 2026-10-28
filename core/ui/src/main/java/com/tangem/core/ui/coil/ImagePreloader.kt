package com.tangem.core.ui.coil

import coil.request.Disposable

/**
 * Preload image and store in runtime memory
 */
interface ImagePreloader {

    /** Starts loading [url] into the image cache; await the returned [Disposable.job] to know when it is there. */
    fun preload(url: String): Disposable
}