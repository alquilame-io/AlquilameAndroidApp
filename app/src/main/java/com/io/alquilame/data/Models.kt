package com.io.alquilame.data

import kotlinx.serialization.Serializable

/**
 * Wire models of the `/api/mobile/v2` API. Field names are mapped from the
 * server's snake_case by the Json naming strategy. Money travels as a
 * two-decimal string and dates as ISO strings.
 */

@Serializable
data class Envelope<T>(
    val success: Boolean = true,
    val data: T? = null,
    val message: String? = null,
)

@Serializable
data class Session(val apiKey: String, val tenant: Tenant)

@Serializable
data class Tenant(
    val id: Int,
    val name: String,
    val slug: String,
    val email: String? = null,
    val plan: String? = null,
    val paidPlan: Boolean = false,
    val currency: String = "MXN",
    val timezone: String? = null,
    val today: String? = null,
    val logoUrl: String? = null,
    val primaryColor: String? = null,
    val defaultReservationMode: String? = null,
    val stripeConnected: Boolean = false,
    val storefrontUrl: String? = null,
)

@Serializable
data class MerchBrief(
    val id: Int,
    val name: String,
    val category: String? = null,
    val price: String? = null,
    val thumbUrl: String? = null,
)

@Serializable
data class Merchandise(
    val id: Int,
    val name: String,
    val slug: String? = null,
    val quantity: Int = 1,
    val category: String? = null,
    val price: String? = null,
    val code: String? = null,
    val location: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null,
    val thumbUrl: String? = null,
    val archived: Boolean = false,
    val hasReservations: Boolean = false,
    /** Only present in stock responses: units free on the requested date. */
    val available: Int? = null,
)

@Serializable
data class MerchandiseList(
    val items: List<Merchandise> = emptyList(),
    val categories: List<String> = emptyList(),
)

@Serializable
data class Stock(val date: String, val items: List<Merchandise> = emptyList())

@Serializable
data class ClientBrief(
    val id: Int,
    val fullName: String,
    val phone: String? = null,
    val email: String? = null,
)

@Serializable
data class Client(
    val id: Int,
    val firstName: String = "",
    val lastName: String = "",
    val fullName: String = "",
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val identificationNumber: String? = null,
    val identificationType: String? = null,
    val dateOfBirth: String? = null,
    val notes: String? = null,
) {
    fun brief() = ClientBrief(id, fullName, phone, email)
}

@Serializable
data class ClientDetail(
    val client: Client,
    val contracts: List<ContractSummary> = emptyList(),
    val agenda: List<AgendaItem> = emptyList(),
)

@Serializable
data class ContractBrief(val id: Int, val contractNumber: String? = null, val status: String = "draft")

@Serializable
data class AgendaLine(val merchandise: MerchBrief? = null, val quantity: Int = 1)

@Serializable
data class AgendaItem(
    val type: String,
    val key: String,
    val reservationId: Int? = null,
    val groupId: Int? = null,
    val title: String = "",
    val date: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val client: ClientBrief? = null,
    val contract: ContractBrief? = null,
    val items: List<AgendaLine> = emptyList(),
) {
    val isGroup get() = type == "group"
}

@Serializable
data class Dashboard(
    val today: String,
    val tomorrow: String,
    val reservationsCount: Int = 0,
    val quotesCount: Int = 0,
    val items: List<AgendaItem> = emptyList(),
)

@Serializable
data class Agenda(val from: String, val to: String, val items: List<AgendaItem> = emptyList())

@Serializable
data class CalendarDay(val date: String, val reservations: Int = 0, val quantity: Int = 0)

@Serializable
data class CalendarMonth(
    val month: String,
    val today: String,
    val days: List<CalendarDay> = emptyList(),
)

@Serializable
data class Reservation(
    val id: Int,
    val date: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val quantity: Int = 1,
    val groupId: Int? = null,
    val merchandise: MerchBrief? = null,
    val client: ClientBrief? = null,
    val contract: ContractBrief? = null,
)

@Serializable
data class GroupItem(
    val merchandise: MerchBrief? = null,
    val quantity: Int = 1,
    val startTime: String? = null,
    val endTime: String? = null,
    val days: Int = 1,
)

@Serializable
data class ReservationGroup(
    val id: Int,
    val name: String = "",
    val notes: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val client: ClientBrief? = null,
    val contract: ContractBrief? = null,
    val items: List<GroupItem> = emptyList(),
    val reservations: List<Reservation> = emptyList(),
)

@Serializable
data class ContractSummary(
    val id: Int,
    val contractNumber: String? = null,
    val status: String = "draft",
    val startDate: String? = null,
    val endDate: String? = null,
    val client: ClientBrief? = null,
    val grandTotal: String = "0.00",
    val totalPaid: String = "0.00",
    val remainingBalance: String = "0.00",
    val insertedAt: String? = null,
)

@Serializable
data class ExtraService(val name: String = "", val price: String? = null, val description: String? = null)

@Serializable
data class ProductLine(
    val name: String = "",
    val quantity: Int = 1,
    val price: String? = null,
    val subtotal: String? = null,
    /** Reserved days billed for this product. */
    val days: Int = 1,
)

@Serializable
data class Payment(
    val id: Int,
    val contractId: Int? = null,
    val amount: String = "0.00",
    val paymentMethod: String = "cash",
    val paymentDate: String? = null,
    val referenceNumber: String? = null,
    val notes: String? = null,
)

@Serializable
data class PaymentLink(
    val id: Int,
    val url: String = "",
    val amount: String = "0.00",
    val currency: String? = null,
    val description: String? = null,
    val status: String = "active",
    val paymentStatus: String = "unpaid",
    val paidAt: String? = null,
    val contractId: Int? = null,
    val contractNumber: String? = null,
    val insertedAt: String? = null,
)

@Serializable
data class PaymentLinks(val stripeConnected: Boolean = false, val items: List<PaymentLink> = emptyList())

@Serializable
data class Contract(
    val id: Int,
    val contractNumber: String? = null,
    val status: String = "draft",
    val startDate: String? = null,
    val endDate: String? = null,
    val client: ClientBrief? = null,
    val grandTotal: String = "0.00",
    val totalPaid: String = "0.00",
    val remainingBalance: String = "0.00",
    val reservationId: Int? = null,
    val reservationGroupId: Int? = null,
    val depositAmount: String? = null,
    val discount: String? = null,
    val includesTax: Boolean = false,
    val taxValue: String? = null,
    val notes: String? = null,
    val extraServices: List<ExtraService> = emptyList(),
    val products: List<ProductLine> = emptyList(),
    val productsTotal: String = "0.00",
    val servicesTotal: String = "0.00",
    val subtotal: String = "0.00",
    val taxAmount: String = "0.00",
    val payments: List<Payment> = emptyList(),
    val paymentLinks: List<PaymentLink> = emptyList(),
)

@Serializable
data class QuoteSummary(
    val id: Int,
    val quoteNumber: String? = null,
    val status: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val client: ClientBrief? = null,
    val itemsCount: Int = 0,
    val total: String = "0.00",
    val insertedAt: String? = null,
)

@Serializable
data class QuoteItem(
    val id: Int? = null,
    val merchandise: MerchBrief? = null,
    val quantity: Int = 1,
    val price: String? = null,
    val total: String? = null,
)

@Serializable
data class Quote(
    val id: Int,
    val quoteNumber: String? = null,
    val status: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val client: ClientBrief? = null,
    val total: String = "0.00",
    val items: List<QuoteItem> = emptyList(),
    val extraServices: List<ExtraService> = emptyList(),
    val discount: String? = null,
    val includesTax: Boolean = false,
    val taxValue: String? = null,
    val subtotal: String = "0.00",
    val taxAmount: String = "0.00",
)

@Serializable
data class DateRevenue(val date: String, val revenue: String = "0.00")

@Serializable
data class NamedRevenue(
    val name: String = "",
    val revenue: String = "0.00",
    val quantity: Int? = null,
    val contracts: Int? = null,
)

@Serializable
data class Report(
    val startDate: String,
    val endDate: String,
    val locked: Boolean = false,
    val totalContracts: Int = 0,
    val totalRevenue: String = "0.00",
    val averageContractValue: String = "0.00",
    val avgRevenuePerDay: String = "0.00",
    val totalMerchandises: Int = 0,
    val revenueByDate: List<DateRevenue> = emptyList(),
    val topMerchandises: List<NamedRevenue> = emptyList(),
    val revenueByCategory: List<NamedRevenue> = emptyList(),
    val topClients: List<NamedRevenue> = emptyList(),
    val unpaidContracts: List<ContractSummary> = emptyList(),
    val nps: Nps = Nps(),
    val traffic: Traffic = Traffic(),
)

/** Net Promoter Score of the period: -100..100. */
@Serializable
data class Nps(
    val score: Int = 0,
    val total: Int = 0,
    val promoters: Int = 0,
    val passives: Int = 0,
    val detractors: Int = 0,
    val promoterPct: Double = 0.0,
    val passivePct: Double = 0.0,
    val detractorPct: Double = 0.0,
)

@Serializable
data class SessionCount(val name: String = "", val sessions: Int = 0)

/** Storefront traffic from Google Analytics. [status] is "ok", "not_connected" or "error". */
@Serializable
data class Traffic(
    val status: String = "not_connected",
    val sessions: Int = 0,
    val users: Int = 0,
    val newUsers: Int = 0,
    val productViews: Int = 0,
    val whatsappClicks: Int = 0,
    val bookings: Int = 0,
    val sources: List<SessionCount> = emptyList(),
    val devices: List<SessionCount> = emptyList(),
)

@Serializable
data class WebLogin(val nonce: String, val authUrl: String, val expiresAt: String? = null)

@Serializable
data class Deleted(val deleted: Boolean = true)
