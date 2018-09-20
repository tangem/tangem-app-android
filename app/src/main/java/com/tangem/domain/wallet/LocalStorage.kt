package com.tangem.domain.wallet

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import java.io.File
import kotlin.collections.HashMap
import android.graphics.BitmapFactory
import com.google.gson.Gson
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
            putResourceArtworkToCatalog("card_default", false)
            putResourceArtworkToCatalog("card_btc_001", false)
            putResourceArtworkToCatalog("card_btc_005", true)
        }
        if (batchesFile.exists()) {
            batchesFile.bufferedReader().use { batches = Gson().fromJson(it, object : TypeToken<HashMap<String, BatchInfo>>() {}.type) }
        } else {
            batches = HashMap()

//            putBatchToCatalog("0004", "card_btc_001", false)
//            putBatchToCatalog("0006", "card_btc_001", false)
//            putBatchToCatalog("0010", "card_btc_001", false)
//
//            putBatchToCatalog("0005", "card_btc_005", false)
//            putBatchToCatalog("0007", "card_btc_005", false)
//            putBatchToCatalog("0011", "card_btc_005", true)

//            "0012": card_seed;
//            "0013": card_btc_hk_s;
//            "0014": card_btc_0014
//            "0015": card_btc_000;
//            "0016": card_eth000;
//            "0017": card_qlear200;
//            "0019": card_cle100;
//            "001A": card_btc_001a;
//            "001B": card_btc_001b;
//            "001C": card_btc_001c;
//            "001D": card_eth_001d;


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
        if (artwork == null) return false
        val localArtwork = artworks[artwork.id] ?: return true

        if (localArtwork.hash == artwork.hash) return false


        return localArtwork.updateDate == null || localArtwork.updateDate < artwork.getUpdateDate()
    }

    fun checkBatchInfoChanged(result: CardVerifyAndGetInfo.Response.Item): Boolean {
        var sData: String? = result.substitution?.data
        var sSignature: String? = result.substitution?.signature
        if (!BatchInfo.CardDataSubstitution.verifySignature(result.batch, sData, sSignature)) {
            sData = null
            sSignature = null
        }
        val batchInfo = batches[result.batch]
        if (batchInfo == null || batchInfo.artworkId != result.artwork?.id || batchInfo.dataSubstitution != sData) {
            putBatchToCatalog(result.batch, result.artwork?.id, sData, sSignature)
            return true
        }
        return false
    }

    fun updateArtwork(artworkId: String, inputStream: InputStream, updateDate: Instant) {
        val data = inputStream.readBytes()
        saveArtworkBitmapToFile(artworkId, data)
        putArtworkToCatalog(artworkId, false, data, updateDate, true)
    }

    private fun putBatchToCatalog(batch: String, artworkId: String?, substitution: String?, substitutionSignature: String?, forceSave: Boolean = true) {
        batches[batch] = BatchInfo(artworkId, substitution, substitutionSignature)
        if (forceSave) {
            val sBatches = Gson().toJson(batches)
            batchesFile.bufferedWriter().use { it.write(sBatches) }
        }
    }

    @SuppressLint("ResourceType")
    private fun putResourceArtworkToCatalog(artworkId: String, forceSave: Boolean) {
        context.resources.openRawResource(R.drawable.card_default).use { putArtworkToCatalog(artworkId, true, it.readBytes(), null, forceSave) }
    }

    private fun putArtworkToCatalog(artworkId: String, isResource: Boolean, data: ByteArray, instant: Instant?, forceSave: Boolean = true) {
        val artworkInfo = ArtworkInfo(
                isResource, Util.bytesToHex(Util.calculateSHA256(data)), instant
        )
        artworks[artworkId] = artworkInfo
        if (forceSave) {
            val sArtworks = Gson().toJson(artworks)
            artworksFile.bufferedWriter().use { it.write(sArtworks) }
        }
    }

    private fun getArtworkBitmap(artworkId: String): Bitmap? {
        val info = artworks[artworkId] ?: return null
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
                        context.resources.getResourceEntryName(R.drawable.card_btc_001)
                    }
                    in "AA01 0000 0000 5000".."AA01 0000 0000 9999" -> {
                        context.resources.getResourceEntryName(R.drawable.card_btc_005)
                    }

                    in "AE01 0000 0000 0000".."AE01 0000 0000 4999" -> {
                        context.resources.getResourceEntryName(R.drawable.card_btc_001)
                    }
                    in "AE01 0000 0000 5000".."AE01 0000 0000 9999" -> {
                        context.resources.getResourceEntryName(R.drawable.card_btc_005)
                    }

                    in "CB01 0000 0000 0000".."CB01 0000 0000 9999" -> {
                        context.resources.getResourceEntryName(R.drawable.card_btc_001)
                    }
                    in "CB01 0000 0001 0000".."CB01 0000 0001 9999" -> {
                        context.resources.getResourceEntryName(R.drawable.card_btc_005)
                    }

                    in "CB01 0000 0002 0000".."CB01 0000 0003 9999" -> {
                        context.resources.getResourceEntryName(R.drawable.card_btc_001)
                    }
                    in "CB01 0000 0004 0000".."CB01 0000 0005 9999" -> {
                        context.resources.getResourceEntryName(R.drawable.card_btc_005)
                    }

                    in "CB02 0000 0000 0000".."CB02 0000 0002 4999" -> {
                        context.resources.getResourceEntryName(R.drawable.card_btc_001)
                    }
                    in "CB02 0000 0002 5000".."CB02 0000 0004 9999" -> {
                        context.resources.getResourceEntryName(R.drawable.card_btc_005)
                    }

                    in "CB05 0000 1000 0000".."CB05 0000 1000 9999" -> {
                        context.resources.getResourceEntryName(R.drawable.card_btc_001)
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
        val substitution = batchInfo.getDataSubstitution(card.batch) ?: return
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

        fun getDataSubstitution(batch: String): CardDataSubstitution? {
            return if (CardDataSubstitution.verifySignature(batch, dataSubstitution, dataSubstitutionSignature)) {
                Gson().fromJson(dataSubstitution, CardDataSubstitution::class.java)
            } else {
                null
            }
        }

        class CardDataSubstitution(
                val tokenSymbol: String?,
                val tokenDecimal: Int?,
                val contractAddress: String?
        ) {
            fun applyToCard(card: TangemCard) {
                if (card.tokenSymbol.isNullOrBlank()) card.tokenSymbol = tokenSymbol
                if (tokenDecimal != null) card.tokensDecimal = tokenDecimal
                if (card.contractAddress.isNullOrBlank()) card.contractAddress = contractAddress
            }

            companion object {
                private val substitutionPublicKey: ByteArray = byteArrayOf(0x00, 0x00)
                fun verifySignature(batch: String, substitutionData: String?, substitutionSignature: String?): Boolean {
                    if (substitutionData == null && substitutionSignature == null) return true
                    if (substitutionData == null || substitutionSignature == null) return false
                    val dataToSign = batch.toByteArray(StandardCharsets.UTF_8) + substitutionData.toByteArray(StandardCharsets.UTF_8)
                    return CardCrypto.VerifySignature(substitutionPublicKey, dataToSign, Util.hexToBytes(substitutionSignature))
                }
            }

        }
    }
}