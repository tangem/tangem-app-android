package com.tangem.devkit._arch.structure

/**
[REDACTED_AUTHOR]
 */
interface Id {

    companion object {
        fun getTag(id: Id): String {
            val className = id.javaClass.simpleName
            return if (id is Enum<*>) "$className.${id.name}" else className
        }
    }
}

class StringId(val value: String) : Id

class StringResId(val value: Int) : Id

enum class Additional : Id {
    UNDEFINED,
    JSON_INCOMING,
    JSON_OUTGOING,
    JSON_TAILS,
}