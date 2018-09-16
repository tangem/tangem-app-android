package com.tangem.domain.wallet

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import java.io.File
import kotlin.collections.HashMap
import android.graphics.BitmapFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tangem.util.Util
import com.tangem.wallet.R
import java.io.InputStream
import java.lang.Exception
import java.time.Instant


data class ArtworksStorage(
        val context: Context
) {
    private val cache: FileCache = FileCache(context)
    private lateinit var artworks: HashMap<String, ArtworkInfo>
    private lateinit var batches: HashMap<String, BatchInfo>
    private var artworksFile: File = File(context.filesDir, "artworks.json")
    private var batchesFile: File = File(context.filesDir, "batches.json")

    init {
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

            putBatchToCatalog("0004", "card_btc_001", false)
            putBatchToCatalog("0006", "card_btc_001", false)
            putBatchToCatalog("0010", "card_btc_001", false)

            putBatchToCatalog("0005", "card_btc_005", false)
            putBatchToCatalog("0007", "card_btc_005", false)
            putBatchToCatalog("0011", "card_btc_005", true)

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

    fun checkNeedUpdateArtwork(artworkId: String, artworkHash: String, updateDate: Instant?): Boolean {
        val artwork = artworks[artworkId] ?: return true

        if (artwork.hash == artworkHash) return false

        if (updateDate == null) return false

        return artwork.updateDate == null || artwork.updateDate < updateDate
    }

    fun checkBatchArtworkChanged(batch: String, artworkId: String): Boolean {
        val batchInfo = batches[batch]
        if (batchInfo == null) {
            putBatchToCatalog(batch, artworkId)
            return true
        }
        if (batchInfo.artworkId != artworkId) {
            putBatchToCatalog(batch, artworkId)
            return true
        }
        return false
    }

    fun updateArtwork(artworkId: String, inputStream: InputStream, updateDate: Instant) {
        val data = inputStream.readBytes()
        cache.saveBitmap(artworkId, data)
        putArtworkToCatalog(artworkId, false, data, updateDate, true)
    }

    private fun putBatchToCatalog(batch: String, artworkId: String, forceSave: Boolean = true) {
        batches[batch] = BatchInfo(artworkId)
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
            cache.getBitmap(artworkId)
        }
    }

    private fun getDefaultArtworkBitmap(): Bitmap {
        val info = artworks[defaultArtworkId]
        var bitmap: Bitmap? = null
        if (info != null && !info.isResource) {
            bitmap = cache.getBitmap(defaultArtworkId)
        }
        if (bitmap == null) return BitmapFactory.decodeResource(context.resources, R.drawable.card_default)
        return bitmap
    }

    fun getBatchBitmap(batch: String): Bitmap {
        val batchInfo = batches[batch] ?: return getDefaultArtworkBitmap()
        return getArtworkBitmap(batchInfo.artworkId) ?: return getDefaultArtworkBitmap()
    }

    data class ArtworkInfo(
            val isResource: Boolean,
            val hash: String,
            val updateDate: Instant?
    )

    data class BatchInfo(
            val artworkId: String
    )

    inner class FileCache(context: Context) {

        private var cacheDir: File? = null

        init {
            cacheDir = File(context.filesDir, "artworks")
            //Find the dir to save cached images
//            if (android.os.Environment.getExternalStorageState() == android.os.Environment.MEDIA_MOUNTED)
//                cacheDir = File(android.os.Environment.getExternalStorageDirectory(), "artworks_cache")
//            else
//                cacheDir = context.getCacheDir()
            if (!cacheDir!!.exists())
                cacheDir!!.mkdirs()
        }

        private fun getFile(artworkId: String): File {
            return File(cacheDir, "$artworkId.png")
        }

        fun getBitmap(artworkId: String): Bitmap? {
            return try {
                BitmapFactory.decodeFile(getFile(artworkId).absolutePath)
            } catch (E: Exception) {
                E.printStackTrace()
                null
            }
        }

        fun saveBitmap(artworkId: String, data: ByteArray) {
            val file = getFile(artworkId)
            file.writeBytes(data)
        }

        fun clear() {
            val files = cacheDir!!.listFiles() ?: return
            for (f in files)
                f.delete()
        }

    }

    companion object {
        const val defaultArtworkId = ""
    }
}