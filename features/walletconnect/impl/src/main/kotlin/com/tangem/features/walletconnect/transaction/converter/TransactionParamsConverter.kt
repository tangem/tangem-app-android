package com.tangem.features.walletconnect.transaction.converter

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestBlockUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoItemUM
import com.tangem.utils.converter.Converter
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

                            MESSAGE -> if (objectValue is JSONObject) {
                                objectValue.optString(CONTENTS).let { contents ->
                                    result.addNestedStringValueBlock(key, nestedKey = CONTENTS, contents)
                                }

                                objectValue.optJSONObject(FROM)?.let { from ->
                                    result.addMultipleObjectsBlock(FROM, from)
                                }

                                objectValue.optJSONObject(TO)?.let { to ->
                                    result.addMultipleObjectsBlock(TO, to)
                                }
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
        loop(JSONArray(value))
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

    private fun MutableList<WcTransactionRequestBlockUM>.addNestedStringValueBlock(
        key: String,
        nestedKey: String,
        stringValue: String,
    ): MutableList<WcTransactionRequestBlockUM> {
        add(
            WcTransactionRequestBlockUM(
                info = listOf(
                    WcTransactionRequestInfoItemUM(TextReference.Str(key.capitalize())),
                    WcTransactionRequestInfoItemUM(
                        title = TextReference.Str(nestedKey.capitalize()),
                        description = stringValue,
                    ),
                ).toImmutableList(),
            ),
        )
        return this
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
        const val CONTENTS = "contents"
    }
}