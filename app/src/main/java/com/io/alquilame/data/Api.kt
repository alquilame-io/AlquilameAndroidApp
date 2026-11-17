package com.io.alquilame.data

import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query

private const val V2 = "api/mobile/v2"

/** The Alquilame mobile API. Bodies are free-form JSON objects built by the screens. */
interface Api {
    // ---- Session
    @POST("$V2/login")
    suspend fun login(@Body body: JsonObject): Envelope<Session>

    @POST("$V2/login/nonce")
    suspend fun loginWithNonce(@Body body: JsonObject): Envelope<Session>

    @GET("$V2/me")
    suspend fun me(): Envelope<Tenant>

    /** One-time URL that signs the embedded browser into the web admin panel. */
    @POST("api/mobile/auth/nonce")
    suspend fun webLogin(@Body body: JsonObject): Envelope<WebLogin>

    // ---- Overview
    @GET("$V2/dashboard")
    suspend fun dashboard(): Envelope<Dashboard>

    @GET("$V2/agenda")
    suspend fun agenda(@Query("date") date: String): Envelope<Agenda>

    @GET("$V2/calendar")
    suspend fun calendar(@Query("month") month: String): Envelope<CalendarMonth>

    @GET("$V2/stock")
    suspend fun stock(
        @Query("date") date: String?,
        @Query("start_time") startTime: String? = null,
        @Query("end_time") endTime: String? = null,
        @Query("exclude_reservation_id") excludeReservationId: Int? = null,
        @Query("exclude_group_id") excludeGroupId: Int? = null,
    ): Envelope<Stock>

    @GET("$V2/reports")
    suspend fun reports(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
    ): Envelope<Report>

    // ---- Reservations
    @GET("$V2/reservations/{id}")
    suspend fun reservation(@Path("id") id: Int): Envelope<Reservation>

    @POST("$V2/reservations")
    suspend fun createReservation(@Body body: JsonObject): Envelope<Reservation>

    @PUT("$V2/reservations/{id}")
    suspend fun updateReservation(@Path("id") id: Int, @Body body: JsonObject): Envelope<Reservation>

    @DELETE("$V2/reservations/{id}")
    suspend fun deleteReservation(@Path("id") id: Int): Envelope<Deleted>

    @POST("$V2/reservations/{id}/contract")
    suspend fun contractFromReservation(@Path("id") id: Int): Envelope<Contract>

    // ---- Reservation groups (events)
    @GET("$V2/reservation-groups/{id}")
    suspend fun group(@Path("id") id: Int): Envelope<ReservationGroup>

    @POST("$V2/reservation-groups")
    suspend fun createGroup(@Body body: JsonObject): Envelope<ReservationGroup>

    @PUT("$V2/reservation-groups/{id}")
    suspend fun updateGroup(@Path("id") id: Int, @Body body: JsonObject): Envelope<ReservationGroup>

    @DELETE("$V2/reservation-groups/{id}")
    suspend fun deleteGroup(@Path("id") id: Int): Envelope<Deleted>

    @POST("$V2/reservation-groups/{id}/items")
    suspend fun addGroupItem(@Path("id") id: Int, @Body body: JsonObject): Envelope<ReservationGroup>

    @DELETE("$V2/reservation-groups/{id}/items/{merchandiseId}")
    suspend fun removeGroupItem(
        @Path("id") id: Int,
        @Path("merchandiseId") merchandiseId: Int,
    ): Envelope<ReservationGroup>

    /** Moves the event to a new date range, re-booking every product. */
    @PUT("$V2/reservation-groups/{id}/dates")
    suspend fun rescheduleGroup(@Path("id") id: Int, @Body body: JsonObject): Envelope<ReservationGroup>

    /** Groups standalone reservations into a new event. */
    @POST("$V2/reservation-groups/merge")
    suspend fun mergeReservations(@Body body: JsonObject): Envelope<ReservationGroup>

    /** Dissolves the event, keeping its reservations. */
    @POST("$V2/reservation-groups/{id}/ungroup")
    suspend fun ungroup(@Path("id") id: Int): Envelope<Deleted>

    @POST("$V2/reservation-groups/{id}/contract")
    suspend fun contractFromGroup(@Path("id") id: Int): Envelope<Contract>

    // ---- Inventory
    @GET("$V2/merchandises")
    suspend fun merchandises(): Envelope<MerchandiseList>

    @GET("$V2/merchandises/{id}")
    suspend fun merchandise(@Path("id") id: Int): Envelope<Merchandise>

    @Multipart
    @POST("$V2/merchandises")
    suspend fun createMerchandise(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part image: MultipartBody.Part?,
    ): Envelope<Merchandise>

    @Multipart
    @PUT("$V2/merchandises/{id}")
    suspend fun updateMerchandise(
        @Path("id") id: Int,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part image: MultipartBody.Part?,
    ): Envelope<Merchandise>

    @DELETE("$V2/merchandises/{id}")
    suspend fun deleteMerchandise(@Path("id") id: Int): Envelope<Deleted>

    // ---- Clients
    @GET("$V2/clients")
    suspend fun clients(@Query("q") query: String? = null): Envelope<List<Client>>

    @GET("$V2/clients/{id}")
    suspend fun client(@Path("id") id: Int): Envelope<ClientDetail>

    @POST("$V2/clients")
    suspend fun createClient(@Body body: JsonObject): Envelope<Client>

    @PUT("$V2/clients/{id}")
    suspend fun updateClient(@Path("id") id: Int, @Body body: JsonObject): Envelope<Client>

    @DELETE("$V2/clients/{id}")
    suspend fun deleteClient(@Path("id") id: Int): Envelope<Deleted>

    // ---- Contracts
    @GET("$V2/contracts")
    suspend fun contracts(): Envelope<List<ContractSummary>>

    @GET("$V2/contracts/{id}")
    suspend fun contract(@Path("id") id: Int): Envelope<Contract>

    @PUT("$V2/contracts/{id}")
    suspend fun updateContract(@Path("id") id: Int, @Body body: JsonObject): Envelope<Contract>

    @PUT("$V2/contracts/{id}/status")
    suspend fun updateContractStatus(@Path("id") id: Int, @Body body: JsonObject): Envelope<Contract>

    @DELETE("$V2/contracts/{id}")
    suspend fun deleteContract(@Path("id") id: Int): Envelope<Deleted>

    @POST("$V2/contracts/{id}/payments")
    suspend fun createPayment(@Path("id") id: Int, @Body body: JsonObject): Envelope<Contract>

    @PUT("$V2/payments/{id}")
    suspend fun updatePayment(@Path("id") id: Int, @Body body: JsonObject): Envelope<Contract>

    @DELETE("$V2/payments/{id}")
    suspend fun deletePayment(@Path("id") id: Int): Envelope<Contract>

    @POST("$V2/contracts/{id}/payment-links")
    suspend fun createContractPaymentLink(@Path("id") id: Int, @Body body: JsonObject): Envelope<Contract>

    // ---- Quotes
    @GET("$V2/quotes")
    suspend fun quotes(): Envelope<List<QuoteSummary>>

    @GET("$V2/quotes/{id}")
    suspend fun quote(@Path("id") id: Int): Envelope<Quote>

    @POST("$V2/quotes")
    suspend fun createQuote(@Body body: JsonObject): Envelope<Quote>

    @PUT("$V2/quotes/{id}")
    suspend fun updateQuote(@Path("id") id: Int, @Body body: JsonObject): Envelope<Quote>

    @DELETE("$V2/quotes/{id}")
    suspend fun deleteQuote(@Path("id") id: Int): Envelope<Deleted>

    @POST("$V2/quotes/{id}/convert")
    suspend fun convertQuote(@Path("id") id: Int, @Body body: JsonObject): Envelope<Contract>

    // ---- Payment links
    @GET("$V2/payment-links")
    suspend fun paymentLinks(): Envelope<PaymentLinks>

    @POST("$V2/payment-links")
    suspend fun createPaymentLink(@Body body: JsonObject): Envelope<PaymentLink>

    @POST("$V2/payment-links/{id}/deactivate")
    suspend fun deactivatePaymentLink(@Path("id") id: Int): Envelope<PaymentLink>
}
