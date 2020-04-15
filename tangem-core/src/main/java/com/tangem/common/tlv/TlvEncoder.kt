package com.tangem.common.tlv

import com.tangem.Log
import com.tangem.SessionError
import com.tangem.commands.*
import com.tangem.commands.common.IssuerDataMode
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toByteArray
import java.util.*

/**
 * Encodes information that is to be written on the card from parsed classes into [ByteArray]
 * (according to the provided [TlvTag] and corresponding [TlvValueType])
 * and then forms [Tlv] with the encoded values.
 */
class TlvEncoder {
    /**

     * @param value information that is to be encoded into [Tlv].
     */
    internal inline fun <reified T> encode(tag: TlvTag, value: T?): Tlv {
        if (value != null) {
            return Tlv(tag, encodeValue(tag, value))
        } else {
            Log.e(this::class.simpleName!!, "Encoding error. Value for tag $tag is null")
            throw SessionError.EncodingFailed()
        }
    }

    internal inline fun <reified T> encodeValue(tag: TlvTag, value: T): ByteArray {
        return when (tag.valueType()) {
            TlvValueType.HexString -> {
                typeCheck<T, String>(tag)
                (value as String).hexToBytes()
            }
            TlvValueType.HexStringToHash -> {
                typeCheck<T, String>(tag)
                (value as String).calculateSha256()
            }
            TlvValueType.Utf8String -> {
                typeCheck<T, String>(tag)
                (value as String).toByteArray()
            }
            TlvValueType.Uint16 -> {
                typeCheck<T, Int>(tag)
                (value as Int).toByteArray(2)
            }
            TlvValueType.Uint32 -> {
                typeCheck<T, Int>(tag)
                (value as Int).toByteArray()
            }
            TlvValueType.BoolValue -> {
                typeCheck<T, Boolean>(tag)
                val booleanValue = value as Boolean
                if (booleanValue) byteArrayOf(1) else byteArrayOf(0)
            }
            TlvValueType.ByteArray -> {
                typeCheck<T, ByteArray>(tag)
                value as ByteArray
            }
            TlvValueType.EllipticCurve -> {
                typeCheck<T, EllipticCurve>(tag)
                (value as EllipticCurve).curve.toByteArray()
            }
            TlvValueType.DateTime -> {
                typeCheck<T, Date>(tag)
                val calendar = Calendar.getInstance().apply { time = (value as Date) }
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                return year.toByteArray(2) + month.toByte() + day.toByte()
            }
            TlvValueType.ProductMask -> {
                typeCheck<T, ProductMask>(tag)
                byteArrayOf(
                        (value as ProductMask).rawValue.toByte()
                )
            }
            TlvValueType.SettingsMask -> {
                typeCheck<T, SettingsMask>(tag)
                val rawValue = (value as SettingsMask).rawValue
                rawValue.toByteArray(determineByteArraySize(rawValue))
            }
            TlvValueType.CardStatus -> {
                typeCheck<T, CardStatus>(tag)
                (value as CardStatus).code.toByteArray()
            }
            TlvValueType.SigningMethod -> {
                typeCheck<T, SigningMethodMask>(tag)
                byteArrayOf((value as SigningMethodMask).rawValue.toByte())
            }
            TlvValueType.IssuerDataMode -> {
                typeCheck<T, IssuerDataMode>(tag)
                byteArrayOf((value as IssuerDataMode).code)
            }
        }
    }

    private fun determineByteArraySize(value: Int): Int {
        val mask = 0xFFFF0000.toInt()
        return if ((value and mask) != 0) 4 else 2
    }

    private inline fun <reified T, reified ExpectedT> typeCheck(tag: TlvTag) {
        if (T::class != ExpectedT::class) {
            Log.e(this::class.simpleName!!,
                    "Mapping error. Type for tag: $tag must be ${tag.valueType()}. It is ${T::class}")
            throw SessionError.EncodingFailedTypeMismatch()
        }
    }
}