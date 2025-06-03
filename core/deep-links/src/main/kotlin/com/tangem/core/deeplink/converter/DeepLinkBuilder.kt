package com.tangem.core.deeplink.converter

import com.tangem.core.deeplink.DeeplinkConst.TANGEM_SCHEME

/**
 * Builder class for constructing deep links with a fluent interface.
 */
internal class DeepLinkBuilder {
    private var scheme: String = TANGEM_SCHEME
    private var action: String = ""
    private val pathParams: MutableList<String> = mutableListOf()
    private val queryParams: MutableMap<String, String> = mutableMapOf()

    /**
     * Sets the scheme for the deep link (e.g., "tangem", "https")
     */
    fun setScheme(scheme: String): DeepLinkBuilder {
        this.scheme = scheme
        return this
    }

    /**
     * Sets the action for the deep link (e.g., "link", "wallet")
     */
    fun setAction(action: String): DeepLinkBuilder {
        this.action = action
        return this
    }

    /**
     * Adds a path parameter to the deep link
     */
    fun addPathParam(param: String): DeepLinkBuilder {
        pathParams.add(param)
        return this
    }

    /**
     * Adds a query parameter to the deep link
     */
    fun addQueryParam(key: String, value: String): DeepLinkBuilder {
        queryParams[key] = value
        return this
    }

    /**
     * Builds the deep link URI string
     */
    fun build(): String {
        val path = if (pathParams.isEmpty()) {
            action
        } else {
            "$action/${pathParams.joinToString("/")}"
        }

        val queryString = if (queryParams.isEmpty()) {
            ""
        } else {
            "?" + queryParams.entries.joinToString("&") { "${it.key}=${it.value}" }
        }

        return "$scheme://$path$queryString"
    }
}