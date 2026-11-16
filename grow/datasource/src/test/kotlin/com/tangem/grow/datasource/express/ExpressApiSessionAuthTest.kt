package com.tangem.grow.datasource.express

import com.google.common.truth.Truth.assertThat
import com.tangem.grow.datasource.onramp.OnrampApi
import com.tangem.lib.auth.api.RequiresSessionAuth
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class ExpressApiSessionAuthTest {

    @ParameterizedTest
    @ValueSource(classes = [TangemExpressApi::class, OnrampApi::class])
    fun `GIVEN express api WHEN read endpoints THEN every endpoint requires session auth`(api: Class<*>) {
        // Act
        val unauthorized = api.declaredMethods
            .filterNot { it.isSynthetic || it.isAnnotationPresent(RequiresSessionAuth::class.java) }
            .map { it.name }

        // Assert
        assertThat(unauthorized).isEmpty()
    }
}