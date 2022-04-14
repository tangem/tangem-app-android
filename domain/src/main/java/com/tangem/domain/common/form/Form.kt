package com.tangem.domain.common.form

/**
* [REDACTED_AUTHOR]
 */
class Form(
    val fieldList: List<DataField<*>>,
) {
    fun getField(id: FieldId): DataField<*>? = fieldList.firstOrNull { it.id == id }

    fun getData(id: FieldId): Pair<FieldId, *>? = getField(id)?.getData()

    // convert this form data whatever you want
    fun visitDataConverter(converter: FieldDataConverter<*>) {
        fieldList.forEach { it.visitDataConverter(converter) }
    }
}

interface FieldId

interface Field<T> {
    val id: FieldId
    var data: Data<T>

    data class Data<Data>(
        val value: Data,
        val isUserInput: Boolean = true
    )
}

typealias FieldData = Pair<FieldId, Field.Data<*>>

interface DataField<T> : Field<T> {
    fun getData(): Pair<FieldId, Field.Data<T>>
    fun visitDataConverter(dataConverter: FieldDataConverter<*>)
}

abstract class BaseDataField<T>(
    override val id: FieldId,
    override var data: Field.Data<T>,
) : DataField<T> {

    override fun getData(): Pair<FieldId, Field.Data<T>> = id to data

    override fun visitDataConverter(dataConverter: FieldDataConverter<*>) {
        dataConverter.visit(getData())
    }
}

