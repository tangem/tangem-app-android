package com.tangem.grow.datasource.gasless

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.RetrofitApiSpec
import com.tangem.grow.datasource.config.GaslessTxService
import com.tangem.grow.datasource.di.GrowDatasourceModule
import com.tangem.lib.auth.api.RequiresSessionAuth
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class GaslessApiSessionAuthTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Spec {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN gasless client spec WHEN built THEN session auth is enabled on the gasless config`(
            model: SpecModel,
        ) {
            // Act
            val spec = model.build()

            // Assert
            assertThat(spec.apiConfigId).isEqualTo(GaslessTxService.ID)
            assertThat(spec.shouldUseSessionAuth).isTrue()
        }

        private fun provideTestModels() = listOf(
            SpecModel(name = "evm", build = GrowDatasourceModule::evmGaslessApiSpec),
            SpecModel(name = "tron", build = GrowDatasourceModule::tronGaslessApiSpec),
            SpecModel(name = "solana", build = GrowDatasourceModule::solanaGaslessApiSpec),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Endpoints {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN gasless api WHEN read endpoints THEN session auth matches the network rollout`(
            model: EndpointsModel,
        ) {
            // Arrange
            val endpoints = model.api.declaredMethods.filterNot { it.isSynthetic }

            // Act
            val withSessionAuth = endpoints.filter { it.isAnnotationPresent(RequiresSessionAuth::class.java) }

            // Assert
            val expected = if (model.isSessionAuthRequired) endpoints else emptyList()
            assertThat(withSessionAuth.map { it.name }).containsExactlyElementsIn(expected.map { it.name })
        }

        private fun provideTestModels() = listOf(
            EndpointsModel(api = SolanaGaslessApi::class.java, isSessionAuthRequired = true),
            EndpointsModel(api = TronGaslessApi::class.java, isSessionAuthRequired = false),
            EndpointsModel(api = GaslessTxServiceApi::class.java, isSessionAuthRequired = false),
            EndpointsModel(api = GaslessTxServiceApiV2::class.java, isSessionAuthRequired = false),
        )
    }

    data class SpecModel(val name: String, val build: () -> RetrofitApiSpec) {
        override fun toString(): String = name
    }

    data class EndpointsModel(val api: Class<*>, val isSessionAuthRequired: Boolean) {
        override fun toString(): String = api.simpleName
    }
}