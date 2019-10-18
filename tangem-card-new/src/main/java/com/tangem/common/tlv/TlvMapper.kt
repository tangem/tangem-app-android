package com.tangem.common.tlv

import com.tangem.commands.CardStatus
import com.tangem.commands.EllipticCurve
import com.tangem.commands.ProductMask
import com.tangem.commands.SigningMethod
import com.tangem.common.extentions.toDate
import com.tangem.common.extentions.toHexString
import com.tangem.common.extentions.toInt
import com.tangem.data.SettingsMask
import java.util.*


open class TlvMapperException(message: String?) : Exception(message)

class MissingTagException(message: String? = null) : TlvMapperException(message)
class WrongTypeException(message: String? = null) : TlvMapperException(message)
class ConvertionException(message: String? = null) : TlvMapperException(message)

class TlvMapper(val tlvList: List<Tlv>) {

    inline fun <reified T> mapOptional(tag: TlvTag): T? =
            try {
                map<T>(tag)
            } catch (exception: MissingTagException) {
                null
            }

    inline fun <reified T> map(tag: TlvTag): T {
        val tlvValue: ByteArray = tlvList.find { it.tag == tag }?.value
                ?: throw MissingTagException()

        return when (tag.valueType()) {
            TlvValueType.HexString -> {
                if (T::class != String::class)
                    throw WrongTypeException("Mapping error. Type for tag: $tag must be ${tag.valueType()}. It is ${T::class}")
               tlvValue.toHexString() as T
            }
            TlvValueType.Utf8String -> {
                if (T::class != String::class)
                    throw WrongTypeException("Mapping error. Type for tag: $tag must be ${tag.valueType()}. It is ${T::class}")
                String(tlvValue) as T
            }
            TlvValueType.IntValue -> {
                if (T::class != Integer::class)
                    throw WrongTypeException("Mapping error. Type for tag: $tag must be ${tag.valueType()}. It is ${T::class}")
                tlvValue.toInt() as T
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
                EllipticCurve.byName(String(tlvValue)) as T
            }
            TlvValueType.DateTime -> {
                if (T::class != Date::class)
                    throw WrongTypeException("Mapping error. Type for tag: $tag must be ${tag.valueType()}. It is ${T::class}")
                tlvValue.toDate() as T
            }
            TlvValueType.ProductMask -> {
                if (T::class != ProductMask::class)
                    throw WrongTypeException("Mapping error. Type for tag: $tag must be ${tag.valueType()}. It is ${T::class}")
                ProductMask.byCode(tlvValue.first()) as T

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
            }
            TlvValueType.SigningMethod -> {
                if (T::class != SigningMethod::class)
                    throw WrongTypeException("Mapping error. Type for tag: $tag must be ${tag.valueType()}. It is ${T::class}")
                SigningMethod.byCode(tlvValue.toInt()) as T
            }
        }


    }

}