package com.tangem.tangemtest.ucase.domain.responses

import com.google.gson.*
import com.tangem.commands.*
import com.tangem.common.extensions.toHexString
import com.tangem.tangemtest.extensions.print
import java.lang.reflect.Type
import java.text.DateFormat
import java.util.*

/**
[REDACTED_AUTHOR]
 */
class ResponseJsonConverter {

    val gson: Gson by lazy { init() }

    private val fieldConverter = ResponseFieldConverter()

    private fun init(): Gson {
        val builder = GsonBuilder().apply {
            registerTypeAdapter(ByteArray::class.java, ByteTypeAdapter(fieldConverter))
            registerTypeAdapter(SigningMethod::class.java, SigningMethodTypeAdapter(fieldConverter))
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
        return JsonPrimitive(fieldConverter.byteArray(src))
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
) : JsonSerializer<SigningMethod> {
    override fun serialize(src: SigningMethod, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
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

        val signingMap = mutableMapOf(
                ProductMask.note to "Note",
                ProductMask.tag to "Tag",
                ProductMask.idCard to "IdCard",
                ProductMask.idIssuer to "IdIssuer"
        )
        return signingMap.filter { mask.contains(it.key) }.map { it.value }
    }

    fun signingMethod(signingMethod: SigningMethod?): String {
        return signingMethodList(signingMethod).print(wrap = false)
    }

    fun signingMethodList(signingMethod: SigningMethod?): List<String> {
        val methods = signingMethod ?: return emptyList()

        val signingMap = mutableMapOf(
                SigningMethod.signHash to "Hash",
                SigningMethod.signRaw to "Raw",
                SigningMethod.signHashValidatedByIssuer to "HashValidatedByIssuer",
                SigningMethod.signRawValidatedByIssuer to "RawValidatedByIssuer",
                SigningMethod.signHashValidatedByIssuerAndWriteIssuerData to "HashValidatedByIssuerAndWriteIssuerData",
                SigningMethod.signRawValidatedByIssuerAndWriteIssuerData to "RawValidatedByIssuerAndWriteIssuerData",
                SigningMethod.signPos to "Pos"
        )
        return signingMap.filter { methods.contains(it.key) }.map { it.value }
    }

    fun settingsMask(settingsMask: SettingsMask?): String {
        return settingsMaskList(settingsMask).print(wrap = false)
    }

    fun settingsMaskList(settingsMask: SettingsMask?): List<String> {
        val masks = settingsMask ?: return emptyList()

        return Settings.values().filter { masks.contains(it) }.map { it.name }
    }

    fun byteArray(byteArray: ByteArray?): String {
        return byteArray?.toHexString() ?: ""
    }
}