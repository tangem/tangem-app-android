package com.tangem.domain.common.form

import com.tangem.common.json.MoshiJsonConverter

/**
 * Created by Anton Zhilenkov on 12/04/2022.
 */
interface DataConverterVisitor<Data, Result> {
    fun visit(data: Data?)
    fun getConvertedData(): Result
}

interface FieldDataConverter<Result> : DataConverterVisitor<FieldData, Result>

abstract class BaseFieldDataConverter<Result> : FieldDataConverter<Result> {
    protected val collectIds: List<FieldId>
        get() = getIdToCollect()

    protected val collectedData: MutableMap<FieldId, Any?> = mutableMapOf()

    override fun visit(data: Pair<FieldId, Field.Data<*>>?) {
        val id = data?.first ?: return

        if (collectIds.contains(id)) {
            collectedData[id] = data.second.value
        }
    }

    abstract fun getIdToCollect(): List<FieldId>
}

class FieldToJsonConverter(
    private val fieldsToConvert: List<FieldId> = listOf(),
    protected val jsonConverter: MoshiJsonConverter,
) : BaseFieldDataConverter<String>() {

    override fun getConvertedData(): String = jsonConverter.toJson(collectedData, "  ")

    override fun getIdToCollect(): List<FieldId> = fieldsToConvert
}
