package com.tangem.common.utils

import com.tangem.utils.logging.TangemLogger
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue

/**
 * Compares addresses from the app (clipboard JSON) with reference addresses from the QA tools API.
 *
 * Both JSON arrays contain objects with fields: blockchain, derivationPath, token (nullable), addresses (array).
 * Comparison normalizes and sorts both arrays before diffing.
 */
object AddressComparisonHelper {

    fun compareAddresses(appJson: String, apiJson: String) {
        val appEntries = parseAndNormalize(appJson)
        val apiEntries = parseAndNormalize(apiJson)

        val missingInApp = apiEntries - appEntries.toSet()
        val extraInApp = appEntries - apiEntries.toSet()

        if (missingInApp.isEmpty() && extraInApp.isEmpty()) {
            TangemLogger.i("Address comparison passed: ${appEntries.size} entries match")
            return
        }

        val report = buildString {
            appendLine("Address comparison FAILED")
            if (missingInApp.isNotEmpty()) {
                appendLine("\nMissing in app (expected from API but not found):")
                missingInApp.forEach { appendLine("  - $it") }
            }
            if (extraInApp.isNotEmpty()) {
                appendLine("\nExtra in app (found in app but not in API):")
                extraInApp.forEach { appendLine("  - $it") }
            }
            appendLine("\nApp entries: ${appEntries.size}, API entries: ${apiEntries.size}")
        }

        TangemLogger.e(report)
        assertTrue(report, false)
    }

    private fun parseAndNormalize(json: String): List<AddressEntry> {
        val array = JSONArray(json)
        val entries = mutableListOf<AddressEntry>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            entries.add(
                AddressEntry(
                    blockchain = normalizeBlockchainName(obj.getString("blockchain")),
                    derivationPath = obj.getString("derivationPath").trim(),
                    token = obj.optString("token", null)?.trim()?.lowercase(),
                    addresses = parseAddresses(obj).sorted(),
                ),
            )
        }

        return entries.sortedWith(
            compareBy<AddressEntry> { it.blockchain }
                .thenBy { it.derivationPath }
                .thenBy { it.token },
        )
    }

    private val blockchainNameOverrides = mapOf(
        "chia network" to "chia",
    )

    private fun normalizeBlockchainName(name: String): String {
        val normalized = name.trim().lowercase()
        return blockchainNameOverrides[normalized] ?: normalized
    }

    private fun parseAddresses(obj: JSONObject): List<String> {
        val addressesArray = obj.getJSONArray("addresses")
        return (0 until addressesArray.length()).map { addressesArray.getString(it) }
    }

    private data class AddressEntry(
        val blockchain: String,
        val derivationPath: String,
        val token: String?,
        val addresses: List<String>,
    )
}