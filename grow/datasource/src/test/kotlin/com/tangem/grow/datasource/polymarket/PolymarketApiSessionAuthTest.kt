package com.tangem.grow.datasource.polymarket

import com.google.common.truth.Truth.assertThat
import com.tangem.lib.auth.api.RequiresSessionAuth
import org.junit.jupiter.api.Test

internal class PolymarketApiSessionAuthTest {

    @Test
    fun `GIVEN polymarket api WHEN read endpoints THEN every endpoint requires session auth`() {
        // Act
        val unauthorized = PolymarketApi::class.java.declaredMethods
            .filterNot { it.isSynthetic || it.isAnnotationPresent(RequiresSessionAuth::class.java) }
            .map { it.name }

        // Assert
        assertThat(unauthorized).isEmpty()
    }
}