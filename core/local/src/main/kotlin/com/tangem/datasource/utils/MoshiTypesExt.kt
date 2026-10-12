package com.tangem.datasource.utils

import com.squareup.moshi.Types
import com.tangem.utils.annotations.RemoveWithToggle
import java.lang.reflect.ParameterizedType

fun mapWithStringKeyTypes(valueTypes: ParameterizedType): ParameterizedType {
    return Types.newParameterizedType(Map::class.java, String::class.java, valueTypes)
}

inline fun <reified T> mapWithStringKeyTypes(): ParameterizedType {
    return Types.newParameterizedType(Map::class.java, String::class.java, T::class.java)
}

inline fun <reified K, reified T> mapWithCustomKeyTypes(): ParameterizedType {
    return Types.newParameterizedType(Map::class.java, K::class.java, T::class.java)
}

inline fun <reified T> listTypes(): ParameterizedType {
    return Types.newParameterizedType(List::class.java, T::class.java)
}

@RemoveWithToggle(
    toggleName = "AND_18288_KOTLINX_DATASTORE_ENABLED",
    description = "Only the toggle-off branches of the DataStore call sites use it",
)
inline fun <reified T> setTypes(): ParameterizedType {
    return Types.newParameterizedType(Set::class.java, T::class.java)
}