package com.tangem.data.cloudbackup.datasource

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** [Google Drive API v3](https://developers.google.com/workspace/drive/api/reference/rest/v3) */
internal interface GoogleDriveApi {

    @GET("drive/v3/files")
    suspend fun listFiles(
        @Header("Authorization") authorization: String,
        @Query("q") query: String,
        @Query("fields") fields: String = FILE_LIST_FIELDS,
    ): Response<DriveFileListResponse>

    @POST("drive/v3/files")
    suspend fun createFile(
        @Header("Authorization") authorization: String,
        @Body metadata: DriveFileMetadata,
        @Query("fields") fields: String = FILE_FIELDS,
    ): Response<DriveFile>

    @PATCH("drive/v3/files/{fileId}")
    suspend fun updateFileMetadata(
        @Header("Authorization") authorization: String,
        @Path("fileId") fileId: String,
        @Body metadata: DriveFileMetadata,
        @Query("fields") fields: String = FILE_FIELDS,
    ): Response<DriveFile>

    @PATCH("upload/drive/v3/files/{fileId}")
    suspend fun uploadFileContent(
        @Header("Authorization") authorization: String,
        @Path("fileId") fileId: String,
        @Body content: RequestBody,
        @Query("uploadType") uploadType: String = "media",
        @Query("fields") fields: String = FILE_FIELDS,
    ): Response<DriveFile>

    @GET("drive/v3/files/{fileId}")
    suspend fun downloadFileContent(
        @Header("Authorization") authorization: String,
        @Path("fileId") fileId: String,
        @Query("alt") alt: String = "media",
    ): Response<ResponseBody>

    @DELETE("drive/v3/files/{fileId}")
    suspend fun deleteFile(
        @Header("Authorization") authorization: String,
        @Path("fileId") fileId: String,
    ): Response<Unit>

    @GET("drive/v3/about")
    suspend fun getAbout(
        @Header("Authorization") authorization: String,
        @Query("fields") fields: String = ABOUT_FIELDS,
    ): Response<DriveAbout>

    @FormUrlEncoded
    @POST("https://oauth2.googleapis.com/revoke")
    suspend fun revokeToken(@Field("token") token: String): Response<Unit>

    companion object {
        const val BASE_URL = "https://www.googleapis.com/"

        private const val FILE_FIELDS = "id,name,createdTime,appProperties"
        private const val FILE_LIST_FIELDS = "files($FILE_FIELDS)"
        private const val ABOUT_FIELDS = "user(displayName,emailAddress,photoLink)"
    }
}

@Serializable
internal data class DriveFileListResponse(
    @SerialName("files") val files: List<DriveFile>? = null,
)

@Serializable
internal data class DriveFile(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String? = null,
    @SerialName("createdTime") val createdTime: String? = null,
    @SerialName("appProperties") val appProperties: Map<String, String>? = null,
)

@Serializable
internal data class DriveFileMetadata(
    @SerialName("name") val name: String,
    @SerialName("mimeType") val mimeType: String? = null,
    @SerialName("parents") val parents: List<String>? = null,
    @SerialName("appProperties") val appProperties: Map<String, String>? = null,
)

@Serializable
internal data class DriveAbout(
    @SerialName("user") val user: DriveUser? = null,
)

@Serializable
internal data class DriveUser(
    @SerialName("displayName") val displayName: String? = null,
    @SerialName("emailAddress") val emailAddress: String? = null,
    @SerialName("photoLink") val photoLink: String? = null,
)