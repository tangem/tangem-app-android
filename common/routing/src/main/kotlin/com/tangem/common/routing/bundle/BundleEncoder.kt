package com.tangem.common.routing.bundle

import android.os.Bundle
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule

@ExperimentalSerializationApi
internal class BundleEncoder(
    private val bundle: Bundle,
    private val parentBundle: Bundle? = null,
    private val keyInParent: String? = null,
    private val isInitializer: Boolean = true,
    override val serializersModule: SerializersModule,
) : AbstractEncoder() {

    private var elementKey: String? = null

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        elementKey = descriptor.getElementName(index)
        return super.encodeElement(descriptor, index)
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        return if (isInitializer) {
            BundleEncoder(
                bundle = bundle,
                parentBundle = null,
                keyInParent = elementKey,
                isInitializer = false,
                serializersModule = serializersModule,
            )
        } else {
            BundleEncoder(
                bundle = Bundle(),
                parentBundle = bundle,
                keyInParent = elementKey,
                isInitializer = false,
                serializersModule = serializersModule,
            )
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (descriptor.kind in arrayOf(StructureKind.LIST, StructureKind.MAP)) {
            val size = elementKey?.toIntOrNull()?.let { it + 1 } ?: 0
            bundle.putInt("\$size", size)
        }

        if (keyInParent.isNullOrBlank()) {
            return
        }

        parentBundle?.putBundle(keyInParent, bundle)
    }

    override fun encodeBoolean(value: Boolean) {
        bundle.putBoolean(elementKey, value)
    }

    override fun encodeByte(value: Byte) {
        bundle.putByte(elementKey, value)
    }

    override fun encodeChar(value: Char) {
        bundle.putChar(elementKey, value)
    }

    override fun encodeDouble(value: Double) {
        bundle.putDouble(elementKey, value)
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        bundle.putInt(elementKey, index)
    }

    override fun encodeFloat(value: Float) {
        bundle.putFloat(elementKey, value)
    }

    override fun encodeInt(value: Int) {
        bundle.putInt(elementKey, value)
    }

    override fun encodeLong(value: Long) {
        bundle.putLong(elementKey, value)
    }

    override fun encodeNull() {
        /* no-op */
    }

    override fun encodeShort(value: Short) {
        bundle.putShort(elementKey, value)
    }

    override fun encodeString(value: String) {
        bundle.putString(elementKey, value)
    }

    override fun encodeNotNullMark() {
        /* no-op */
    }
}