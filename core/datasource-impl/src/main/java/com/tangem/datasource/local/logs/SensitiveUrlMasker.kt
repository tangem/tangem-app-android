package com.tangem.datasource.local.logs

class SensitiveUrlMasker(sensitiveValues: Collection<String>) {

    // Sorted by descending length so a value that is a prefix of another (e.g. "my-node" vs
    // "my-node-prod") cannot mask the shorter one first and leave the suffix in the log.
    private val sensitiveValues: List<String> = sensitiveValues
        .distinct()
        .sortedByDescending(String::length)

    fun mask(url: String): String {
        var result = url
        for (value in sensitiveValues) {
            if (result.contains(value, ignoreCase = true)) {
                result = result.replace(value, MASKED_VALUE, ignoreCase = true)
            }
        }
        return result
    }

    companion object {
        const val MASKED_VALUE = "******"
    }
}