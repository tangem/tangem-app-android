package com.tangem.domain.common.form

/**
 * Created by Anton Zhilenkov on 29/03/2022.
 */
class Form(
    fieldList: List<DataField<*>>,
) {
    private val _fieldList: MutableList<DataField<*>> = fieldList.toMutableList()

    val fieldList: List<DataField<*>>
        get() = _fieldList.toList()

    fun getField(id: FieldId): DataField<*>? = fieldList.firstOrNull { it.id == id }

    fun getData(id: FieldId): Pair<FieldId, *>? = getField(id)?.getData()

    fun setField(field: DataField<*>) {
        val oldField = getField(field.id) ?: return
        val oldIndexOfField = _fieldList.indexOf(oldField)
        if (oldIndexOfField == -1) return

        _fieldList.removeAt(oldIndexOfField)
        _fieldList.add(oldIndexOfField, field)
    }

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
        val isUserInput: Boolean
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
