package com.tangem.common.tlv

import com.tangem.commands.*
import com.tangem.common.extensions.toDate
import com.tangem.common.extensions.toHexString
import com.tangem.common.extensions.toInt
import com.tangem.common.extensions.toUtf8
import java.util.*


open class TlvMapperException(message: String?) : Exception(message)

class MissingTagException(message: String? = null) : TlvMapperException(message)
class WrongTypeException(message: String? = null) : TlvMapperException(message)
class ConversionException(message: String? = null) : TlvMapperException(message)

/**
 * Maps value fields in [Tlv] from raw [ByteArray] to concrete classes
 * according to their [TlvTag] and corresponding [TlvValueType].
 *
 * @property tlvList List of TLVs, which values are to be converted to particular classes.
 */
class TlvMapper(val tlvList: List<Tlv>) {

    /**
     * Finds [Tlv] by its [TlvTag].
     * Returns null if [Tlv] is not found, otherwise converts its value to [T].
     *
     * @param tag [TlvTag] of a [Tlv] which value is to be returned.
     *
     * @return Value converted to a nullable type [T].
     */
    inline fun <reified T> mapOptional(tag: TlvTag): T? =
            try {
                map<T>(tag)
            } catch (exception: MissingTagException) {
                null
            }

    /**
     * Finds [Tlv] by its [TlvTag].
     * Throws [MissingTagException] if [Tlv] is not found,
     * otherwise converts [Tlv] value to [T].
     *
     * @param tag [TlvTag] of a [Tlv] which value is to be returned.
     *
     * @return [Tlv] value converted to a nullable type [T].
     *
     * @throws [MissingTagException] if no [Tlv] is found by the Tag.
     */
    inline fun <reified T> map(tag: TlvTag): T {
        val tlvValue: ByteArray = tlvList.find { it.tag == tag }?.value
                ?: if (tag.valueType() == TlvValueType.BoolValue && T::class == Boolean::class) {
                    return false as T
                } else {
                    throw MissingTagException("Tag $tag not found")
                }

        return when (tag.valueType()) {
            TlvValueType.HexString -> {
                if (T::class != String::class)
                    throw WrongTypeException("Mapping error. Type for tag: $tag must be ${tag.valueType()}. It is ${T::class}")
                tlvValue.toHexString() as T
            }
            TlvValueType.Utf8String -> {
                if (T::class != String::class)
                    throw WrongTypeException("Mapping error. Type for tag: $tag must be ${tag.valueType()}. It is ${T::class}")
                tlvValue.toUtf8() as T
            }
            TlvValueType.IntValue -> {
                if (T::class != Integer::class)
                    throw WrongTypeException("Mapping error. Type for tag: $tag must be ${tag.valueType()}. It is ${T::class}")
                try {
                    tlvValue.toInt() as T
                } catch (exception: IllegalArgumentException) {
                    throw ConversionException(exception.message)
                }
            }
            TlvValueType.BoolValue -> {
                if (T::class != Boolean::class)
                    throw WrongTypeException("Mapping error. Type for tag: $tag must be ${tag.valueType()}. It is ${T::class}")
                true as T
            }
            TlvValueType.ByteArray -> {
                if (T::class != ByteArray::class)
                    throw WrongTypeException("Mapping error. Type for tag: $tag must be ${tag.valueType()}. It is ${T::class}")
                tlvValue as T
            }
            TlvValueType.EllipticCurve -> {
                if (T::class != EllipticCurve::class)
                    throw WrongTypeException("Mapping error. Type for tag: $tag must be ${tag.valueType()}. It is ${T::class}")
                EllipticCurve.byName(tlvValue.toUtf8()) as? T
                        ?: throw ConversionException("Unknown Elliptic Curve value: ${tlvValue.toUtf8()}")
            }
            TlvValueType.DateTime -> {
                if (T::class != Date::class)
                    throw WrongTypeException("Mapping error. Type for tag: $tag must be ${tag.valueType()}. It is ${T::class}")
                try {
                    tlvValue.toDate() as T
                } catch (exception: Exception) {
                    throw ConversionException("Converting to date with the following exception: " + exception.message)
                }
            }
            TlvValueType.ProductMask -> {
                if (T::class != ProductMask::class)
                    throw WrongTypeException("Mapping error. Type for tag: $tag must be ${tag.valueType()}. It is ${T::class}")
                ProductMask.byCode(tlvValue.first()) as? T
                        ?: throw ConversionException("Unknown Product Mask Code: ${tlvValue.first()}.")
            }
            TlvValueType.SettingsMask -> {
                if (T::class != SettingsMask::class)
                    throw WrongTypeException("Mapping error. Type for tag: $tag must be ${tag.valueType()}. It is ${T::class}")
                SettingsMask(tlvValue.toInt()) as T
            }
            TlvValueType.CardStatus -> {
                if (T::class != CardStatus::class)
                    throw WrongTypeException("Mapping error. Type for tag: $tag must be ${tag.valueType()}. It is ${T::class}")
                CardStatus.byCode(tlvValue.toInt()) as T
                        ?: throw ConversionException("Unknown Card Status with code of: ${tlvValue.toInt()}")
            }
            TlvValueType.SigningMethod -> {
                if (T::class != SigningMethod::class)
                    throw WrongTypeException("Mapping error. Type for tag: $tag must be ${tag.valueType()}. It is ${T::class}")
                SigningMethod(tlvValue.toInt()) as T
                        ?: throw ConversionException("Unknown Signing Method with code of: ${tlvValue.toInt()}")
            }
        }
    }

}