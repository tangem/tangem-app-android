package com.tangem.domain.wallet

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import java.io.File
import kotlin.collections.HashMap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.tangem.data.network.model.CardVerifyAndGetInfo
import com.tangem.domain.cardReader.CardCrypto
import com.tangem.util.Util
import com.tangem.wallet.R
import java.io.InputStream
import java.lang.Exception
import java.nio.charset.StandardCharsets
import java.time.Instant


data class LocalStorage(
        val context: Context
) {
    private lateinit var artworks: HashMap<String, ArtworkInfo>
    private lateinit var batches: HashMap<String, BatchInfo>
    private val artworksFile: File = File(context.filesDir, "artworks.json")
    private val batchesFile: File = File(context.filesDir, "batches.json")

    private var cacheDir: File? = null

    init {
        cacheDir = File(context.filesDir, "artworks")
        if (!cacheDir!!.exists())
            cacheDir!!.mkdirs()

        if (artworksFile.exists()) {
            artworksFile.bufferedReader().use { artworks = Gson().fromJson(it, object : TypeToken<HashMap<String, ArtworkInfo>>() {}.type) }
        } else {
            artworks = HashMap()
            putResourceArtworkToCatalog(R.drawable.card_default, false)
            putResourceArtworkToCatalog(R.drawable.card_ru006, false)
            putResourceArtworkToCatalog(R.drawable.card_ru007, false)
            putResourceArtworkToCatalog(R.drawable.card_ru011, false)
            putResourceArtworkToCatalog(R.drawable.card_ru012, false)
            putResourceArtworkToCatalog(R.drawable.card_ru013, false)
            putResourceArtworkToCatalog(R.drawable.card_ru014, false)
            putResourceArtworkToCatalog(R.drawable.card_ru015, false)
            putResourceArtworkToCatalog(R.drawable.card_ru016, false)
            putResourceArtworkToCatalog(R.drawable.card_ru020, false)
            putResourceArtworkToCatalog(R.drawable.card_ru021, false)
            putResourceArtworkToCatalog(R.drawable.card_ru022, false)
            putResourceArtworkToCatalog(R.drawable.card_ru023, true)
        }
        if (batchesFile.exists()) {
            batchesFile.bufferedReader().use { batches = Gson().fromJson(it, object : TypeToken<HashMap<String, BatchInfo>>() {}.type) }
        } else {
            batches = HashMap()

            putBatchToCatalog("0004", R.drawable.card_ru006, false)
            putBatchToCatalog("0006", R.drawable.card_ru006, false)
            putBatchToCatalog("0010", R.drawable.card_ru006, false)

            putBatchToCatalog("0005", R.drawable.card_ru007, false)
            putBatchToCatalog("0007", R.drawable.card_ru007, false)
            putBatchToCatalog("0011", R.drawable.card_ru007, false)

            putBatchToCatalog("0012", R.drawable.card_ru011, false)
            putBatchToCatalog("0013", R.drawable.card_ru012, false)
            putBatchToCatalog("0014", R.drawable.card_ru006, false)
            putBatchToCatalog("0015", R.drawable.card_ru020, false)
            putBatchToCatalog("0016", R.drawable.card_ru021, false)
            putBatchToCatalog("0017", R.drawable.card_ru013, false)
            putBatchToCatalog("0019", R.drawable.card_ru016, false)
            putBatchToCatalog("001A", R.drawable.card_ru014, false)
            putBatchToCatalog("001B", R.drawable.card_ru015, false)
            putBatchToCatalog("001C", R.drawable.card_ru023, false)
            putBatchToCatalog("001D", R.drawable.card_ru022, true)
        }
    }

    private fun getArtworkFile(artworkId: String): File {
        return File(cacheDir, "$artworkId.png")
    }

    private fun loadArtworkBitmapFromFile(artworkId: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(getArtworkFile(artworkId).absolutePath)
        } catch (E: Exception) {
            E.printStackTrace()
            null
        }
    }

    private fun saveArtworkBitmapToFile(artworkId: String, data: ByteArray) {
        val file = getArtworkFile(artworkId)
        file.writeBytes(data)
    }

    fun checkNeedUpdateArtwork(artwork: CardVerifyAndGetInfo.Response.Item.ArtworkInfo?): Boolean {
        if (artwork == null || artwork.id.isBlank()) return false
        val localArtwork = artworks[artwork.id] ?: return true

        if (localArtwork.hash == artwork.hash) return false


        return localArtwork.updateDate == null || localArtwork.updateDate < artwork.getUpdateDate()
    }

    fun checkBatchInfoChanged(card: TangemCard, result: CardVerifyAndGetInfo.Response.Item): Boolean {
        var sData: String? = result.substitution?.data
        var sSignature: String? = result.substitution?.signature
        if( card.batch!=result.batch ) {
            Log.e("LocalStorage", "Invalid batch received!")
            return false
        }
        if (!BatchInfo.CardDataSubstitution.verifySignature(card, sData, sSignature)) {
            sData = null
            sSignature = null
        }
        val batchInfo = batches[result.batch]
        if (batchInfo == null || batchInfo.artworkId?.toLowerCase() != result.artwork?.id?.toLowerCase() || batchInfo.dataSubstitution != sData) {
            putBatchToCatalog(result.batch, result.artwork?.id?.toLowerCase(), sData, sSignature)
            return true
        }
        return false
    }

    fun updateArtwork(artworkId: String, inputStream: InputStream, updateDate: Instant) {
        val data = inputStream.readBytes()
        saveArtworkBitmapToFile(artworkId.toLowerCase(), data)
        putArtworkToCatalog(artworkId, false, data, updateDate, true)
    }

    private fun putBatchToCatalog(batch: String, resourceId: Int, forceSave: Boolean = true) {
        putBatchToCatalog(batch, context.resources.getResourceEntryName(resourceId), null, null, forceSave)
    }

    private fun putBatchToCatalog(batch: String, artworkId: String?, substitution: String?, substitutionSignature: String?, forceSave: Boolean = true) {
        batches[batch] = BatchInfo(artworkId, substitution, substitutionSignature)
        if (forceSave) {
            val sBatches = Gson().toJson(batches)
            batchesFile.bufferedWriter().use { it.write(sBatches) }
        }
    }

    @SuppressLint("ResourceType")
    private fun putResourceArtworkToCatalog(resourceId: Int, forceSave: Boolean) {
        context.resources.openRawResource(R.drawable.card_default).use { putArtworkToCatalog(context.resources.getResourceEntryName(resourceId), true, it.readBytes(), null, forceSave) }
    }

    private fun putArtworkToCatalog(artworkId: String, isResource: Boolean, data: ByteArray, instant: Instant?, forceSave: Boolean = true) {
        val artworkInfo = ArtworkInfo(
                isResource, Util.bytesToHex(Util.calculateSHA256(data)), instant
        )
        artworks[artworkId.toLowerCase()] = artworkInfo
        if (forceSave) {
            val sArtworks = Gson().toJson(artworks)
            artworksFile.bufferedWriter().use { it.write(sArtworks) }
        }
    }

    private fun getArtworkBitmap(artworkId: String): Bitmap? {
        if( artworkId.isBlank() ) return null
        val info = artworks[artworkId.toLowerCase()] ?: return null
        return if (info.isResource) {
            val resID = context.resources.getIdentifier(artworkId, "drawable", context.packageName)
            if (resID == 0) return null
            BitmapFactory.decodeResource(context.resources, resID)
        } else {
            loadArtworkBitmapFromFile(artworkId)
        }
    }

    private fun getDefaultArtworkBitmap(): Bitmap {
        val defaultArtworkId = context.resources.getResourceEntryName(R.drawable.card_default)
        val info = artworks[defaultArtworkId]
        var bitmap: Bitmap? = null
        if (info != null && !info.isResource) {
            bitmap = loadArtworkBitmapFromFile(defaultArtworkId)
        }
        if (bitmap == null) return BitmapFactory.decodeResource(context.resources, R.drawable.card_default)
        return bitmap
    }

    fun getCardArtworkBitmap(card: TangemCard): Bitmap {
        // special cases (first series of cards, hardcode CID->artwork), on new series batch<->artwork
        val artworkId =
                when (card.cidDescription) {
                    in "AA01 0000 0000 0000".."AA01 0000 0000 4999" -> {
                        context.resources.getResourceEntryName(R.drawable.card_ru006)
                    }
                    in "AA01 0000 0000 5000".."AA01 0000 0000 9999" -> {
                        context.resources.getResourceEntryName(R.drawable.card_ru007)
                    }

                    in "AE01 0000 0000 0000".."AE01 0000 0000 4999" -> {
                        context.resources.getResourceEntryName(R.drawable.card_ru006)
                    }
                    in "AE01 0000 0000 5000".."AE01 0000 0000 9999" -> {
                        context.resources.getResourceEntryName(R.drawable.card_ru007)
                    }

                    in "CB01 0000 0000 0000".."CB01 0000 0000 9999" -> {
                        context.resources.getResourceEntryName(R.drawable.card_ru006)
                    }
                    in "CB01 0000 0001 0000".."CB01 0000 0001 9999" -> {
                        context.resources.getResourceEntryName(R.drawable.card_ru007)
                    }

                    in "CB01 0000 0002 0000".."CB01 0000 0003 9999" -> {
                        context.resources.getResourceEntryName(R.drawable.card_ru006)
                    }
                    in "CB01 0000 0004 0000".."CB01 0000 0005 9999" -> {
                        context.resources.getResourceEntryName(R.drawable.card_ru007)
                    }

                    in "CB02 0000 0000 0000".."CB02 0000 0002 4999" -> {
                        context.resources.getResourceEntryName(R.drawable.card_ru006)
                    }
                    in "CB02 0000 0002 5000".."CB02 0000 0004 9999" -> {
                        context.resources.getResourceEntryName(R.drawable.card_ru007)
                    }

                    in "CB05 0000 1000 0000".."CB05 0000 1000 9999" -> {
                        context.resources.getResourceEntryName(R.drawable.card_ru006)
                    }

                    else -> {
                        val batchInfo = batches[card.batch] ?: return getDefaultArtworkBitmap()
                        batchInfo.artworkId
                    }
                } ?: return getDefaultArtworkBitmap()
//        if (artworkId.contains('/'))
//            artworkId = artworkId.split('/').last()
        return getArtworkBitmap(artworkId) ?: return getDefaultArtworkBitmap()
    }

    fun applySubstitution(card: TangemCard) {
        val batchInfo = batches[card.batch] ?: return
        val substitution = batchInfo.getDataSubstitution(card) ?: return
        substitution.applyToCard(card)
    }

    data class ArtworkInfo(
            val isResource: Boolean,
            val hash: String,
            val updateDate: Instant?
    )

    data class BatchInfo(
            val artworkId: String?,
            val dataSubstitution: String?,
            val dataSubstitutionSignature: String?
    ) {

        fun getDataSubstitution(card: TangemCard): CardDataSubstitution? {
            return if (CardDataSubstitution.verifySignature(card, dataSubstitution, dataSubstitutionSignature)) {
                Gson().fromJson(dataSubstitution, CardDataSubstitution::class.java)
            } else {
                null
            }
        }

        class CardDataSubstitution(
                @SerializedName("token_symbol")
                private val tokenSymbol: String?,
                @SerializedName("token_decimal")
                private val tokenDecimal: Int?,
                @SerializedName("token_contract_address")
                private val contractAddress: String?
        ) {
            fun applyToCard(card: TangemCard) {
                if (card.tokenSymbol.isNullOrBlank()) card.tokenSymbol = tokenSymbol
                if (tokenDecimal != null) card.tokensDecimal = tokenDecimal
                if (card.contractAddress.isNullOrBlank()) card.contractAddress = contractAddress
            }

            companion object {
                fun verifySignature(card: TangemCard, substitutionData: String?, substitutionSignature: String?): Boolean {
                    if (substitutionData == null && substitutionSignature == null) return true
                    if (substitutionData == null || substitutionSignature == null) return false
                    val dataToSign = card.batch.toByteArray(StandardCharsets.UTF_8) + substitutionData.toByteArray(StandardCharsets.UTF_8)
                    return CardCrypto.VerifySignature(card.issuer.publicDataKey, dataToSign, Util.hexToBytes(substitutionSignature))
                }
            }

        }
    }
}