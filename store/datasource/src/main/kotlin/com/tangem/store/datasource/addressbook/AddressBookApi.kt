package com.tangem.store.datasource.addressbook

import com.tangem.core.remote.response.ApiResponse
import com.tangem.store.datasource.addressbook.models.SyncAddressBooksRequest
import com.tangem.store.datasource.addressbook.models.SyncAddressBooksResponse
import com.tangem.store.datasource.addressbook.models.UpdateAddressBookRequest
import com.tangem.store.datasource.addressbook.models.UpdateAddressBookResponse
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