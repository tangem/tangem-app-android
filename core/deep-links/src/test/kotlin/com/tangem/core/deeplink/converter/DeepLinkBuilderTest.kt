package com.tangem.core.deeplink.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.deeplink.DeeplinkConst
import org.junit.Before
import org.junit.Test

internal class DeepLinkBuilderTest {

    private lateinit var deepLinkBuilder: DeepLinkBuilder

    @Before
    fun setup() {
        deepLinkBuilder = DeepLinkBuilder()
    }

    @Test
    fun `GIVEN default builder WHEN build THEN should return default scheme`() {
        // WHEN
        val result = deepLinkBuilder.build()

        // THEN
        assertThat(result).isEqualTo("${DeeplinkConst.TANGEM_SCHEME}://")
    }

    @Test
    fun `GIVEN custom scheme WHEN setScheme THEN should use custom scheme`() {
        // GIVEN
        val customScheme = "https"

        // WHEN
        val result = deepLinkBuilder
            .setScheme(customScheme)
            .build()

        // THEN
        assertThat(result).isEqualTo("$customScheme://")
    }

    @Test
    fun `GIVEN action WHEN setAction THEN should include action in path`() {
        // GIVEN
        val action = "wallet"

        // WHEN
        val result = deepLinkBuilder
            .setAction(action)
            .build()

        // THEN
        assertThat(result).isEqualTo("${DeeplinkConst.TANGEM_SCHEME}://$action")
    }

    @Test
    fun `GIVEN path params WHEN addPathParam THEN should include params in path`() {
        // GIVEN
        val action = "wallet"
        val param1 = "123"
        val param2 = "456"

        // WHEN
        val result = deepLinkBuilder
            .setAction(action)
            .addPathParam(param1)
            .addPathParam(param2)
            .build()

        // THEN
        assertThat(result).isEqualTo("${DeeplinkConst.TANGEM_SCHEME}://$action/$param1/$param2")
    }

    @Test
    fun `GIVEN query params WHEN addQueryParam THEN should include params in query string`() {
        // GIVEN
        val action = "wallet"
        val key1 = "param1"
        val value1 = "value1"
        val key2 = "param2"
        val value2 = "value2"

        // WHEN
        val result = deepLinkBuilder
            .setAction(action)
            .addQueryParam(key1, value1)
            .addQueryParam(key2, value2)
            .build()

        // THEN
        assertThat(result).isEqualTo("${DeeplinkConst.TANGEM_SCHEME}://$action?$key1=$value1&$key2=$value2")
    }

    @Test
    fun `GIVEN complex deep link WHEN build THEN should construct correct URI`() {
        // GIVEN
        val scheme = "https"
        val action = "wallet"
        val pathParam = "123"
        val queryKey = "token"
        val queryValue = "abc"

        // WHEN
        val result = deepLinkBuilder
            .setScheme(scheme)
            .setAction(action)
            .addPathParam(pathParam)
            .addQueryParam(queryKey, queryValue)
            .build()

        // THEN
        assertThat(result).isEqualTo("$scheme://$action/$pathParam?$queryKey=$queryValue")
    }
}