package com.tangem.tangemserver.android.data

import android.content.Context
import com.tangem.tangemcommon.data.TangemCard

import android.annotation.SuppressLint
import android.graphics.Bitmap
import java.io.File
import kotlin.collections.HashMap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.tangem.tangemserver.android.model.CardVerifyAndGetInfo
import com.tangem.tangemserver.R
import com.tangem.tangemcommon.data.external.CardDataSubstitutionProvider
import com.tangem.tangemcommon.reader.CardCrypto
import com.tangem.tangemcommon.util.Util
import java.io.InputStream
import java.lang.Exception
import java.nio.charset.StandardCharsets
import java.util.*

class LocalStorage
(
        val context: Context
) : CardDataSubstitutionProvider {
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
            try {
                artworksFile.bufferedReader().use { artworks = Gson().fromJson(it, object : TypeToken<HashMap<String, ArtworkInfo>>() {}.type) }
            } catch (e: Exception) {
                e.printStackTrace()
                artworks = HashMap()
            }
        } else {
            artworks = HashMap()
        }

        if (artworks.count() < 21) {
//          forceSave=true only on the last one
            putResourceArtworkToCatalog(R.drawable.card_default, false)
            putResourceArtworkToCatalog(R.drawable.card_default_nft, false)
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
            putResourceArtworkToCatalog(R.drawable.card_ru023, false)
            putResourceArtworkToCatalog(R.drawable.card_ru024, false)
            putResourceArtworkToCatalog(R.drawable.card_ru028, false)
            putResourceArtworkToCatalog(R.drawable.card_ru029, false)
            putResourceArtworkToCatalog(R.drawable.card_ru030, false)
            putResourceArtworkToCatalog(R.drawable.card_ru031, false)
            putResourceArtworkToCatalog(R.drawable.card_ru032, false)
            putResourceArtworkToCatalog(R.drawable.card_ff32, true)
        }
        if (batchesFile.exists()) {
            try {
                batchesFile.bufferedReader().use { batches = Gson().fromJson(it, object : TypeToken<HashMap<String, BatchInfo>>() {}.type) }
            } catch (e: Exception) {
                e.printStackTrace()
                batches = HashMap()
            }
        } else {
            batches = HashMap()
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


        return localArtwork.updateDate == null || localArtwork.updateDate.before(artwork.getUpdateDate())
    }

    fun checkBatchInfoChanged(card: TangemCard, result: CardVerifyAndGetInfo.Response.Item): Boolean {
        var sData: String? = result.substitution?.data
        var sSignature: String? = result.substitution?.signature
        if (card.batch != result.batch) {
            Log.e("CardDataSubstitution", "Invalid batch received!")
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

    fun updateArtwork(artworkId: String, inputStream: InputStream, updateDate: Date) {
        val data = inputStream.readBytes()
        saveArtworkBitmapToFile(artworkId.toLowerCase(), data)
        putArtworkToCatalog(artworkId, false, data, updateDate, true)
    }

//    private fun putBatchToCatalog(batch: String, resourceId: Int, forceSave: Boolean = true) {
//        putBatchToCatalog(batch, context.resources.getResourceEntryName(resourceId), null, null, forceSave)
//    }

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

    private fun putArtworkToCatalog(artworkId: String, isResource: Boolean, data: ByteArray, updateDate: Date?, forceSave: Boolean = true) {
        val artworkInfo = ArtworkInfo(
                isResource, Util.bytesToHex(Util.calculateSHA256(data)), updateDate
        )
        artworks[artworkId.toLowerCase()] = artworkInfo
        if (forceSave) {
            val sArtworks = Gson().toJson(artworks)
            artworksFile.bufferedWriter().use { it.write(sArtworks) }
        }
    }

    private fun getArtworkBitmap(artworkId: String): Bitmap? {
        if (artworkId.isBlank()) return null
        val info = artworks[artworkId.toLowerCase()] ?: return null
        return if (info.isResource) {
            val resID = context.resources.getIdentifier(artworkId, "drawable", context.packageName)
            if (resID == 0) return null
            BitmapFactory.decodeResource(context.resources, resID)
        } else {
            loadArtworkBitmapFromFile(artworkId)
        }
    }

    private fun getDefaultArtworkBitmap(card: TangemCard): Bitmap {
        val defaultArtworkId: String
        if (card.tokenSymbol.startsWith("NFT:")) {
            defaultArtworkId = context.resources.getResourceEntryName(R.drawable.card_default_nft)
        } else {
            defaultArtworkId = context.resources.getResourceEntryName(R.drawable.card_default)
        }
        val info = artworks[defaultArtworkId]
        var bitmap: Bitmap? = null
        if (info != null && !info.isResource) {
            bitmap = loadArtworkBitmapFromFile(defaultArtworkId)
        }
        if (bitmap == null) {
            if (card.tokenSymbol.startsWith("NFT:")) {
                return BitmapFactory.decodeResource(context.resources, R.drawable.card_default_nft)
            } else {
                return BitmapFactory.decodeResource(context.resources, R.drawable.card_default)
            }
        }
        return bitmap
    }

    fun getCardArtworkBitmap(card: TangemCard): Bitmap {
        // special cases (first series of cards, hardcode CID->artwork), on new series batch<->artwork
        val hexCID = Util.bytesToHex(card.cid)
        val artworkResourceId: Int? = when {
            hexCID in "AA01000000000000".."AA01000000004999" -> R.drawable.card_ru006
            hexCID in "AA01000000005000".."AA01000000009999" -> R.drawable.card_ru007

            hexCID in "AE01000000000000".."AE01000000004999" -> R.drawable.card_ru006
            hexCID in "AE01000000005000".."AE01000000009999" -> R.drawable.card_ru007

            hexCID in "CB01000000000000".."CB01000000009999" -> R.drawable.card_ru006
            hexCID in "CB01000000010000".."CB01000000019999" -> R.drawable.card_ru007

            hexCID in "CB01000000020000".."CB01000000039999" -> R.drawable.card_ru006
            hexCID in "CB01000000040000".."CB01000000059999" -> R.drawable.card_ru007

            hexCID in "CB02000000000000".."CB02000000024999" -> R.drawable.card_ru006
            hexCID in "CB02000000025000".."CB02000000049999" -> R.drawable.card_ru007

            hexCID in "CB05000010000000".."CB05000010009999" -> R.drawable.card_ru006

            card.batch == "0004" -> R.drawable.card_ru006
            card.batch == "0006" -> R.drawable.card_ru006
            card.batch == "0010" -> R.drawable.card_ru006
            card.batch == "0005" -> R.drawable.card_ru007
            card.batch == "0007" -> R.drawable.card_ru007
            card.batch == "0011" -> R.drawable.card_ru007
            card.batch == "0012" -> R.drawable.card_ru011
            card.batch == "0013" -> R.drawable.card_ru012
            card.batch == "0014" -> R.drawable.card_ru006
            card.batch == "0015" -> R.drawable.card_ru020
            card.batch == "0016" -> R.drawable.card_ru021
            card.batch == "0017" -> R.drawable.card_ru013
            card.batch == "0019" -> R.drawable.card_ru016
            card.batch == "001A" -> R.drawable.card_ru014
            card.batch == "001B" -> R.drawable.card_ru015
            card.batch == "001C" -> R.drawable.card_ru023
            card.batch == "001D" -> R.drawable.card_ru022
            card.batch == "001E" -> R.drawable.card_ru024
            card.batch == "001F" -> R.drawable.card_ru028
            card.batch == "0018" -> R.drawable.card_ru029
            card.batch == "0020" -> R.drawable.card_ru030
            card.batch == "0021" -> R.drawable.card_ru031
            card.batch == "0022" -> R.drawable.card_ru032
            card.batch == "FF32" -> R.drawable.card_ff32

            else -> null
        }

        if (artworkResourceId != null) {
            val artworkId = context.resources.getResourceEntryName(artworkResourceId)
            return getArtworkBitmap(artworkId) ?: return getDefaultArtworkBitmap(card)
        }
        val batchInfo = batches[card.batch] ?: return getDefaultArtworkBitmap(card)
        val artworkId = batchInfo.artworkId ?: return getDefaultArtworkBitmap(card)
        return getArtworkBitmap(artworkId) ?: return getDefaultArtworkBitmap(card)
    }

    override fun applySubstitution(card: TangemCard) {
        val batchInfo = batches[card.batch] ?: return
        val substitution = batchInfo.getDataSubstitution(card) ?: return
        substitution.applyToCard(card)
    }

    private data class ArtworkInfo(
            val isResource: Boolean,
            val hash: String,
            val updateDate: Date?
    )

    private data class BatchInfo(
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
                if (tokenSymbol != null && (card.tokenSymbol.isNullOrBlank() || card.tokenSymbol.toLowerCase() == "not defined")) card.tokenSymbol = tokenSymbol
                if (tokenDecimal != null && card.tokensDecimal == 0) card.tokensDecimal = tokenDecimal
                if (contractAddress != null && (card.contractAddress.isNullOrBlank() || card.contractAddress.toLowerCase() == "not defined")) card.contractAddress = contractAddress
            }

            companion object {
                fun verifySignature(card: TangemCard, substitutionData: String?, substitutionSignature: String?): Boolean {
                    if (substitutionData == null && substitutionSignature == null) return true
                    if (substitutionData == null || substitutionSignature == null) return false
                    val dataToSign = card.batch.toByteArray(StandardCharsets.UTF_8) + substitutionData.toByteArray(StandardCharsets.UTF_8)
                    return try {
                        CardCrypto.VerifySignature(card.issuerPublicDataKey, dataToSign, Util.hexToBytes(substitutionSignature))
                    } catch (E: Exception) {
                        E.printStackTrace()
                        false
                    }
                }
            }

        }
    }
}