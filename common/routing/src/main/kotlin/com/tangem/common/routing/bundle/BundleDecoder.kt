package com.tangem.common.routing.bundle

import android.os.Bundle
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule

@ExperimentalSerializationApi
internal class BundleDecoder(
    private val bundle: Bundle,
    private val elementsCount: Int = -1,
    private val isInitializer: Boolean = true,
    override val serializersModule: SerializersModule,
) : AbstractDecoder() {

    private var index = -1
    private var elementKey: String? = null

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (++index >= elementsCount) {
            return CompositeDecoder.DECODE_DONE
        }

        elementKey = descriptor.getElementName(index)
        return index
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val b = if (isInitializer) {
            bundle
        } else {
            requireNotNull(bundle.getBundle(elementKey)) {
                "Bundle is missing for key $elementKey while decoding"
            }
        }

        val count = when (descriptor.kind) {
            StructureKind.MAP,
            StructureKind.LIST,
            -> b.getInt("\$size")
            else -> descriptor.elementsCount
        }

        return BundleDecoder(
            bundle = b,
            elementsCount = count,
            isInitializer = false,
            serializersModule = serializersModule,
        )
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        /* no-op */
    }

    override fun decodeBoolean(): Boolean {
        return bundle.getBoolean(elementKey)
    }

    override fun decodeByte(): Byte {
        return bundle.getByte(elementKey)
    }

    override fun decodeChar(): Char {
        return bundle.getChar(elementKey)
    }

    override fun decodeDouble(): Double {
        return bundle.getDouble(elementKey)
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        return bundle.getInt(elementKey)
    }

    override fun decodeFloat(): Float {
        return bundle.getFloat(elementKey)
    }

    override fun decodeInt(): Int {
        return bundle.getInt(elementKey)
    }

    override fun decodeLong(): Long {
        return bundle.getLong(elementKey)
    }

    override fun decodeNotNullMark(): Boolean {
        return bundle.containsKey(elementKey)
    }

    override fun decodeNull(): Nothing? {
        return null
    }

    override fun decodeShort(): Short {
        return bundle.getShort(elementKey)
    }

    override fun decodeString(): String {
        return requireNotNull(bundle.getString(elementKey)) {
            "String is missing for key $elementKey while decoding"
        }
    }
}