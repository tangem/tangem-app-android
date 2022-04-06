package com.tangem.domain.common.form

import com.tangem.common.json.MoshiJsonConverter

/**
[REDACTED_AUTHOR]
 */
class Form(
    val fieldList: List<DataField<*>>,
) {
    fun getField(id: FieldId): DataField<*>? = fieldList.firstOrNull { it.id == id }

    fun getData(id: FieldId): Pair<FieldId, *>? = getField(id)?.getData()

    // convert this form data whatever you want
    fun getData(converter: FieldDataConverter<*>) {
        fieldList.forEach { it.visitDataConverter(converter) }
    }
}

interface FieldId

interface Field<Data> {
    val id: FieldId
    var value: Data
    val isEnabled: Boolean
    val isVisible: Boolean
}

abstract class BaseDataField<Data>(
    override val id: FieldId,
    override var value: Data
) : DataField<Data> {

    override fun getData(): Pair<FieldId, Data> = id to value

    override fun visitDataConverter(dataConverter: FieldDataConverter<*>) {
        dataConverter.visit(getData())
    }
}

interface FieldDataConverter<Result> : DataConverterVisitor<Pair<FieldId, Any?>, Result>

abstract class BaseFieldDataConverter<Data>() : FieldDataConverter<Data> {
    protected val collectIds: List<FieldId> = getIdToCollect()

    protected val collectedData: MutableMap<FieldId, Any?> = mutableMapOf()

    override fun visit(data: Pair<FieldId, Any?>?) {
        val id = data?.first ?: return

        if (collectIds.contains(id)) {
            collectedData[id] = data.second
        }
    }

    abstract fun getIdToCollect(): List<FieldId>
}

abstract class FieldToJsonConverter(
    protected val jsonConverter: MoshiJsonConverter
) : BaseFieldDataConverter<String>() {

    override fun getConvertedData(): String = jsonConverter.toJson(collectedData)
}

interface DataConverterVisitor<Visitor, Result> {
    fun visit(data: Visitor?)
    fun getConvertedData(): Result
}

interface DataField<Data> : Field<Data> {
    fun getData(): Pair<FieldId, Data>
    fun visitDataConverter(dataConverter: FieldDataConverter<*>)
}