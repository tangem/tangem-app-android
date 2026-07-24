package com.tangem.domain

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

/**
 * Guards the `contract_methods.json` asset consumed by `SdkTransactionTypeConverter` (via
 * `DefaultWalletManagersFacade.readSmartContractMethods`). History marking of gasless fee transfers
 * relies on every gasless entry-point selector being mapped to the `gaslessTransaction` method name.
 */
internal class ContractMethodsAssetTest {

    private val methods: Map<String, Map<String, String>> by lazy {
        val json = File("src/main/assets/contract_methods.json").readText()
        val type = Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Types.newParameterizedType(Map::class.java, String::class.java, String::class.java),
        )
        requireNotNull(Moshi.Builder().build().adapter<Map<String, Map<String, String>>>(type).fromJson(json))
    }

    
    @ParameterizedTest
    @ValueSource(strings = ["0x6234d42b", "0x4b072692", "0xf9b181bf"])
    fun `GIVEN gasless selector WHEN asset parsed THEN maps to gaslessTransaction`(selector: String) {
        val entry = methods[selector]

        assertThat(entry).isNotNull()
        assertThat(entry?.get("name")).isEqualTo("gaslessTransaction")
    }
}