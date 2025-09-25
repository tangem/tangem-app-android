package com.tangem.core.configtoggle.utils

import java.util.Locale

internal fun Map<String, Boolean>.toTableString(tableName: String): String {
    return buildString {
        append("$tableName:\n")
        append("|------------------------------------------|-----------|\n")
        append(String.format(Locale.getDefault(), "| %-40s | %-9s |\n", "name", "isEnabled"))
        append("|------------------------------------------|-----------|\n")
        entries.forEachIndexed { index, (name, isEnabled) ->
            append(String.format(Locale.getDefault(), "| %-40s | %-9s |\n", name, isEnabled))
        }
        append("|------------------------------------------|-----------|")
    }
}