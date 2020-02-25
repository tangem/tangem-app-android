package com.tangem.common.tlv

import com.tangem.Log
import com.tangem.commands.*
import com.tangem.commands.common.IssuerDataMode
import com.tangem.common.extensions.toDate
import com.tangem.common.extensions.toHexString
import com.tangem.common.extensions.toInt
import com.tangem.common.extensions.toUtf8
import com.tangem.tasks.TaskError
import java.util.*

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
            } catch (exception: TaskError.MissingTag) {
                null
            }

    /**
     * Finds [Tlv] by its [TlvTag].
     * Throws [TaskError.MissingTag] if [Tlv] is not found,
     * otherwise converts [Tlv] value to [T].
     *
     * @param tag [TlvTag] of a [Tlv] which value is to be returned.
     *
     * @return [Tlv] value converted to a nullable type [T].
     *
     * @throws [TaskError.MissingTag] exception if no [Tlv] is found by the Tag.
     */
    inline fun <reified T> map(tag: TlvTag): T {
        val tlvValue: ByteArray = tlvList.find { it.tag == tag }?.value
                ?: if (tag.valueType() == TlvValueType.BoolValue && T::class == Boolean::class) {
                    return false as T
                } else {
                    Log.e(this::class.simpleName!!, "Tag $tag not found")
                    throw TaskError.MissingTag()
                }

        return when (tag.valueType()) {
            TlvValueType.HexString -> {
                typeCheck<T, String>(tag)
                tlvValue.toHexString() as T
            }
            TlvValueType.Utf8String -> {
                typeCheck<T, String>(tag)
                tlvValue.toUtf8() as T
            }
            TlvValueType.Uint16, TlvValueType.Uint32 -> {
                typeCheck<T, Int>(tag)
                try {
                    tlvValue.toInt() as T
                } catch (exception: IllegalArgumentException) {
                    Log.e(this::class.simpleName!!, exception.message ?: "")
                    throw TaskError.ConvertError()
                }
            }
            TlvValueType.BoolValue -> {
                typeCheck<T, Boolean>(tag)
                true as T
            }
            TlvValueType.ByteArray -> {
                typeCheck<T, ByteArray>(tag)
                tlvValue as T
            }
            TlvValueType.EllipticCurve -> {
                typeCheck<T, EllipticCurve>(tag)
                try {
                    EllipticCurve.byName(tlvValue.toUtf8()) as T
                } catch (exception: Exception) {
                    logException(tag, tlvValue.toUtf8(), exception)
                    throw TaskError.ConvertError()
                }


            }
            TlvValueType.DateTime -> {
                typeCheck<T, Date>(tag)
                try {
                    tlvValue.toDate() as T
                } catch (exception: Exception) {
                    logException(tag, tlvValue.toHexString(), exception)
                    throw TaskError.ConvertError()
                }
            }
            TlvValueType.ProductMask -> {
                typeCheck<T, ProductMask>(tag)
                try {
                    ProductMask.byCode(tlvValue.first()) as T
                } catch (exception: Exception) {
                    logException(tag, tlvValue.first().toString(), exception)
                    throw TaskError.ConvertError()
                }
            }
            TlvValueType.SettingsMask -> {
                typeCheck<T, SettingsMask>(tag)
                SettingsMask(tlvValue.toInt()) as T
            }
            TlvValueType.CardStatus -> {
                typeCheck<T, CardStatus>(tag)
                try {
                    CardStatus.byCode(tlvValue.toInt()) as T
                } catch (exception: Exception) {
                    logException(tag, tlvValue.toInt().toString(), exception)
                    throw TaskError.ConvertError()
                }
            }
            TlvValueType.SigningMethod -> {
                typeCheck<T, SigningMethod>(tag)
                try {
                    SigningMethod(tlvValue.toInt()) as T
                } catch (exception: Exception) {
                    logException(tag, tlvValue.toInt().toString(), exception)
                    throw TaskError.ConvertError()
                }
            }
            TlvValueType.IssuerDataMode -> {
                typeCheck<T, IssuerDataMode>(tag)
                try {
                    IssuerDataMode.byCode(tlvValue.toInt().toByte()) as T
                } catch (exception: Exception) {
                    logException(tag, tlvValue.toInt().toString(), exception)
                    throw TaskError.ConvertError()
                }
            }
        }
    }

    fun logException(tag: TlvTag, value: String, exception: Exception) {
        Log.e(this::class.simpleName!!,
                "Unknown ${tag.name} with value of: value, \n${exception.message}")
    }

    inline fun <reified T, reified ExpectedT> typeCheck(tag: TlvTag) {
        if (T::class != ExpectedT::class) {
            Log.e(this::class.simpleName!!,
                    "Mapping error. Type for tag: $tag must be ${tag.valueType()}. It is ${T::class}")
            throw TaskError.WrongType()
        }
    }

}