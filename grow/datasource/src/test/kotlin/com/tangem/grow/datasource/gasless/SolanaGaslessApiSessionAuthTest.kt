package com.tangem.grow.datasource.gasless

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.RetrofitApiSpec
import com.tangem.grow.datasource.config.GaslessTxService
import com.tangem.grow.datasource.di.GrowDatasourceModule
import com.tangem.lib.auth.api.RequiresSessionAuth
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class SolanaGaslessApiSessionAuthTest {

    @Test
    fun `GIVEN solana gasless api WHEN read endpoints THEN every endpoint requires session auth`() {
        // Act
        val unauthorized = SolanaGaslessApi::class.java.declaredMethods
            .filterNot { it.isSynthetic || it.isAnnotationPresent(RequiresSessionAuth::class.java) }
            .map { it.name }

        // Assert
        assertThat(unauthorized).isEmpty()
    }

    @Test
    fun `GIVEN solana gasless module WHEN building spec THEN session auth is enabled`() {
        // Act
        val spec = GrowDatasourceModule.solanaGaslessApiSpec()

        // Assert
        assertThat(spec).isEqualTo(
            RetrofitApiSpec(
                apiConfigId = GaslessTxService.ID,
                shouldApplyTimeoutAnnotations = true,
                shouldUseSessionAuth = true,
            ),
        )
    }

    @ParameterizedTest
    @ValueSource(classes = [TronGaslessApi::class, GaslessTxServiceApi::class, GaslessTxServiceApiV2::class])
    fun `GIVEN other gasless apis WHEN read endpoints THEN none require session auth`(api: Class<*>) {
        // Act
        val authorized = api.declaredMethods
            .filterNot { it.isSynthetic }
            .filter { it.isAnnotationPresent(RequiresSessionAuth::class.java) }
            .map { it.name }

        // Assert
        assertThat(authorized).isEmpty()
    }
}