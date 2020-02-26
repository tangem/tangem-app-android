package com.tangem.common.tlv

import com.tangem.Log
import com.tangem.commands.*
import com.tangem.commands.common.IssuerDataMode
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toByteArray
import com.tangem.tasks.TaskError
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
            throw TaskError.SerializeCommandError()
        }
    }

    internal inline fun <reified T> encodeValue(tag: TlvTag, value: T): ByteArray {
        return when (tag.valueType()) {
            TlvValueType.HexString -> {
                typeCheck<T, String>(tag)
                return if (tag == TlvTag.Pin || tag == TlvTag.Pin2) {
                    (value as String).calculateSha256()
                } else {
                    (value as String).hexToBytes()
                }
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
                Log.e(this::class.simpleName!!, "Unsupported operation: Boolean to ByteArray for tag $tag")
                throw TaskError.ConvertError()
            }
            TlvValueType.ByteArray -> {
                typeCheck<T, ByteArray>(tag)
                value as ByteArray
            }
            TlvValueType.EllipticCurve -> {
                typeCheck<T, EllipticCurve>(tag)
                (value as EllipticCurve).curve.plus("\\0").toByteArray()
            }
            TlvValueType.DateTime -> {
                typeCheck<T, Date>(tag)
                val calendar = Calendar.getInstance().apply { time = (value as Date) }
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                return year.toByteArray() + month.toByteArray() + day.toByteArray()
            }
            TlvValueType.ProductMask -> {
                typeCheck<T, ProductMask>(tag)
                byteArrayOf(
                        (value as ProductMask).code
                )
            }
            TlvValueType.SettingsMask -> {
                typeCheck<T, SettingsMask>(tag)
                (value as SettingsMask).rawValue.toByteArray()
            }
            TlvValueType.CardStatus -> {
                typeCheck<T, CardStatus>(tag)
                (value as CardStatus).code.toByteArray()
            }
            TlvValueType.SigningMethod -> {
                typeCheck<T, SigningMethod>(tag)
                (value as SigningMethod).rawValue.toByteArray()
            }
            TlvValueType.IssuerDataMode -> {
                typeCheck<T, IssuerDataMode>(tag)
                byteArrayOf((value as IssuerDataMode).code)
            }
        }
    }

    private inline fun <reified T, reified ExpectedT> typeCheck(tag: TlvTag) {
        if (T::class != ExpectedT::class){
            Log.e(this::class.simpleName!!,
                    "Mapping error. Type for tag: $tag must be ${tag.valueType()}. It is ${T::class}")
            throw TaskError.WrongType()
        }
    }
}