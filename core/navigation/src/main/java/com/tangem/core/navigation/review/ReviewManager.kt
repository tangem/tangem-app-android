package com.tangem.core.navigation.review

/**
 * Interface to manage in-app review requests.
 */
interface ReviewManager {

    /**
     * Requests an in-app review flow.
     */
    fun request(onDismissClick: () -> Unit)
}