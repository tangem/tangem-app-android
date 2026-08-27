package com.tangem.features.walletconnect.transaction.converter

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestBlockUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoItemUM
import com.tangem.utils.converter.Converter
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.toImmutableList
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/**
 * Parses various transaction parameters from various JSON schemas to UI format for transaction request BS
 */
internal class TransactionParamsConverter @Inject constructor() : Converter<String, List<WcTransactionRequestBlockUM>> {

    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth")
    override fun convert(value: String): List<WcTransactionRequestBlockUM> {
        val result = mutableListOf<WcTransactionRequestBlockUM>()

        fun loop(node: Any?) {
            when (node) {
                is JSONObject -> {
                    for (key in node.keys()) {
                        val objectValue = node.get(key)

                        when (key) {
                            PRIMARY_TYPE, DATA, FROM, TO, VALUE -> if (objectValue is String) {
                                result.addSingleStringValueBlock(key, objectValue)
                            }

                            DOMAIN -> if (objectValue is JSONObject) {
                                result.addMultipleObjectsBlock(key, objectValue)
                            }

                            // Render the whole EIP-712 `message` object, not just contents/from/to.
                            // Otherwise dangerous approval payloads (EIP-2612 Permit / Permit2), whose
                            // fields are owner/spender/value/amount/deadline, would show nothing —
                            // the user must be able to see the spender and the approved value/amount.
                            MESSAGE -> if (objectValue is JSONObject) {
                                result.addObjectBlocks(key, objectValue)
                            }

                            else -> loop(objectValue)
                        }
                    }
                }
                is JSONArray -> {
                    for (i in 0 until node.length()) {
                        loop(node.get(i))
                    }
                }
            }
        }
        try {
            when (value.trimStart().firstOrNull()) {
                '[' -> loop(JSONArray(value))
                '{' -> loop(JSONObject(value))
                else -> loop(JSONArray(value)) // default to array for backward compatibility
            }
        } catch (e: Exception) {
            TangemLogger.withTag("Wallet Connect").e("Failed to parse transaction params: ${e.message.orEmpty()}")
        }

        return result
    }

    private fun MutableList<WcTransactionRequestBlockUM>.addSingleStringValueBlock(
        key: String,
        stringValue: String,
    ): MutableList<WcTransactionRequestBlockUM> {
        add(
            WcTransactionRequestBlockUM(
                info = listOf(
                    WcTransactionRequestInfoItemUM(
                        title = TextReference.Str(key.capitalize()),
                        description = stringValue,
                    ),
                ).toImmutableList(),
            ),
        )
        return this
    }

    /**
     * Renders an EIP-712 `message`-like object **fully and recursively**, so no security-critical field
     * can hide:
     *  - all scalar fields of [obj] (e.g. owner/spender/value/nonce/deadline of an EIP-2612 Permit) as
     *    one block titled [key],
     *  - every nested object at any depth (e.g. Permit2 `details`/`permitted`) as its own block,
     *  - arrays of scalars and arrays of objects (e.g. Permit2 `PermitBatch`).
     *
     * Replaces the old behaviour that surfaced only `contents`/`from`/`to`.
     */
    private fun MutableList<WcTransactionRequestBlockUM>.addObjectBlocks(key: String, obj: JSONObject) {
        val scalars = extractObjects(obj)
        if (scalars.isNotEmpty()) {
            add(
                WcTransactionRequestBlockUM(
                    info = buildList {
                        add(WcTransactionRequestInfoItemUM(TextReference.Str(key.capitalize())))
                        addAll(scalars)
                    }.toImmutableList(),
                ),
            )
        }
        for (nestedKey in obj.keys()) {
            when (val nested = obj.get(nestedKey)) {
                is JSONObject -> addObjectBlocks(nestedKey, nested)
                is JSONArray -> addArrayBlocks(nestedKey, nested)
            }
        }
    }

    private fun MutableList<WcTransactionRequestBlockUM>.addArrayBlocks(key: String, array: JSONArray) {
        // Scalar elements: one block, one row per element.
        val scalarItems = buildList {
            for (i in 0 until array.length()) {
                val element = array.get(i)
                if (element is String || element is Number || element is Boolean) {
                    add(WcTransactionRequestInfoItemUM(TextReference.Str("$key[$i]"), element.toString()))
                }
            }
        }
        if (scalarItems.isNotEmpty()) {
            add(
                WcTransactionRequestBlockUM(
                    info = buildList {
                        add(WcTransactionRequestInfoItemUM(TextReference.Str(key.capitalize())))
                        addAll(scalarItems)
                    }.toImmutableList(),
                ),
            )
        }
        // Object / nested-array elements: recurse so their fields stay visible at any depth.
        for (i in 0 until array.length()) {
            when (val element = array.get(i)) {
                is JSONObject -> addObjectBlocks("$key[$i]", element)
                is JSONArray -> addArrayBlocks("$key[$i]", element)
            }
        }
    }

    private fun MutableList<WcTransactionRequestBlockUM>.addMultipleObjectsBlock(
        key: String,
        objectValue: JSONObject,
    ): MutableList<WcTransactionRequestBlockUM> {
        add(
            WcTransactionRequestBlockUM(
                info = buildList {
                    add(WcTransactionRequestInfoItemUM(TextReference.Str(key.capitalize())))
                    addAll(extractObjects(objectValue))
                }.toImmutableList(),
            ),
        )
        return this
    }

    private fun String.capitalize(): String = replaceFirstChar { it.uppercaseChar() }

    private fun extractObjects(obj: JSONObject): List<WcTransactionRequestInfoItemUM> {
        val items = mutableListOf<WcTransactionRequestInfoItemUM>()
        for (key in obj.keys()) {
            val objectValue = obj.get(key)
            if (objectValue is String || objectValue is Number || objectValue is Boolean) {
                items.add(
                    WcTransactionRequestInfoItemUM(
                        title = TextReference.Str(key),
                        description = objectValue.toString(),
                    ),
                )
            }
        }
        return items
    }

    private companion object {
        const val PRIMARY_TYPE = "primaryType"
        const val DATA = "data"
        const val FROM = "from"
        const val TO = "to"
        const val VALUE = "value"
        const val DOMAIN = "domain"
        const val MESSAGE = "message"
    }
}