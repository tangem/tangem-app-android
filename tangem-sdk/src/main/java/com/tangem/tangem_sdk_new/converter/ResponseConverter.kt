package com.tangem.tangem_sdk_new.converter

import com.google.gson.*
import com.tangem.commands.*
import com.tangem.common.extensions.toHexString
import com.tangem.tangem_sdk_new.extensions.print
import java.lang.reflect.Type
import java.text.DateFormat
import java.util.*

/**
[REDACTED_AUTHOR]
 */
class ResponseConverter {

    val gson: Gson by lazy { init() }

    private val fieldConverter = ResponseFieldConverter()

    private fun init(): Gson {
        val builder = GsonBuilder().apply {
            registerTypeAdapter(ByteArray::class.java, ByteTypeAdapter(fieldConverter))
            registerTypeAdapter(SigningMethodMask::class.java, SigningMethodTypeAdapter(fieldConverter))
            registerTypeAdapter(SettingsMask::class.java, SettingsMaskTypeAdapter(fieldConverter))
            registerTypeAdapter(ProductMask::class.java, ProductMaskTypeAdapter(fieldConverter))
            registerTypeAdapter(Date::class.java, DateTypeAdapter())
        }
        builder.setPrettyPrinting()
        return builder.create()
    }

    fun convertResponse(response: CommandResponse?): String = gson.toJson(response)
}

class ByteTypeAdapter(
        private val fieldConverter: ResponseFieldConverter
) : JsonSerializer<ByteArray> {
    override fun serialize(src: ByteArray, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(fieldConverter.byteArrayToHex(src))
    }
}

class SettingsMaskTypeAdapter(
        private val fieldConverter: ResponseFieldConverter
) : JsonSerializer<SettingsMask> {
    override fun serialize(src: SettingsMask, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonArray().apply {
            fieldConverter.settingsMaskList(src).forEach { add(it) }
        }
    }
}

class ProductMaskTypeAdapter(
        private val fieldConverter: ResponseFieldConverter
) : JsonSerializer<ProductMask> {
    override fun serialize(src: ProductMask, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonArray().apply {
            fieldConverter.productMaskList(src).forEach { add(it) }
        }
    }
}

class SigningMethodTypeAdapter(
        private val fieldConverter: ResponseFieldConverter
) : JsonSerializer<SigningMethodMask> {
    override fun serialize(src: SigningMethodMask, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonArray().apply {
            fieldConverter.signingMethodList(src).forEach { add(it) }
        }
    }
}

class DateTypeAdapter : JsonSerializer<Date> {
    override fun serialize(src: Date, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val formatter = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale("en_US"))
        return JsonPrimitive(formatter.format(src).toString())
    }
}

class ResponseFieldConverter {

    fun productMask(productMask: ProductMask?): String {
        return productMaskList(productMask).print(wrap = false)
    }

    fun productMaskList(productMask: ProductMask?): List<String> {
        val mask = productMask ?: return emptyList()

        return Product.values().filter { mask.contains(it) }.map { it.name }
    }

    fun signingMethod(signingMask: SigningMethodMask?): String {
        return signingMethodList(signingMask).print(wrap = false)
    }

    fun signingMethodList(signingMask: SigningMethodMask?): List<String> {
        val mask = signingMask ?: return emptyList()

        return SigningMethod.values().filter { mask.contains(it) }.map { it.name }
    }

    fun settingsMask(settingsMask: SettingsMask?): String {
        return settingsMaskList(settingsMask).print(wrap = false)
    }

    fun settingsMaskList(settingsMask: SettingsMask?): List<String> {
        val masks = settingsMask ?: return emptyList()

        return Settings.values().filter { masks.contains(it) }.map { it.name }
    }

    fun byteArrayToHex(byteArray: ByteArray?): String? {
        return byteArray?.toHexString()
    }

    fun byteArrayToString(byteArray: ByteArray?): String? {
        return if (byteArray == null) null else String(byteArray)
    }
}