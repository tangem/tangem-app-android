package com.tangem.datasource.api.addressbook

import com.tangem.datasource.api.addressbook.models.SyncAddressBooksRequest
import com.tangem.datasource.api.addressbook.models.SyncAddressBooksResponse
import com.tangem.datasource.api.addressbook.models.UpdateAddressBookRequest
import com.tangem.datasource.api.addressbook.models.UpdateAddressBookResponse
import com.tangem.datasource.api.common.response.ApiResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path

interface AddressBookApi {

    @POST("v1/address-books/sync")
    suspend fun syncAddressBooks(@Body body: SyncAddressBooksRequest): ApiResponse<SyncAddressBooksResponse>

    @PUT("v1/address-books/{walletId}")
    suspend fun updateAddressBook(
        @Path("walletId") walletId: String,
        @Header("If-Match") eTag: String?,
        @Body body: UpdateAddressBookRequest,
    ): ApiResponse<UpdateAddressBookResponse>
}