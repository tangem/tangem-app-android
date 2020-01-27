package com.tangem.tangem_card.data

enum class ProductMask(val code: Byte) {
    Note(0x01),
    Tag(0x02),
    Id(0x04);

    companion object {
        private val values = values()
        fun byCode(code: Byte): ProductMask? = values.find { it.code == code }
    }
}