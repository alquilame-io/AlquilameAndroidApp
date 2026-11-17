package com.io.alquilame

import com.io.alquilame.data.AppJson
import com.io.alquilame.data.Contract
import com.io.alquilame.data.Dashboard
import com.io.alquilame.data.Envelope
import com.io.alquilame.ui.common.Format
import com.io.alquilame.ui.common.parseLoginQr
import java.math.BigDecimal
import java.util.Locale
import kotlinx.serialization.builtins.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTest {
    private val mexico = Locale.forLanguageTag("es-MX")
    private val spain = Locale.forLanguageTag("es-ES")

    private fun assertAmount(expected: String, input: String, locale: Locale) =
        assertEquals(0, BigDecimal(expected).compareTo(Format.parseAmount(input, locale)))

    @Test
    fun `amounts are read according to the locale`() {
        assertAmount("1250.5", "1250.50", mexico)
        assertAmount("1250", "1,250", mexico)
        assertAmount("1250.5", "1,250.50", mexico)
        assertAmount("12.5", "12,5", mexico)
        assertAmount("1250", "1.250", spain)
        assertAmount("1250.5", "1.250,50", spain)
        assertAmount("1.25", "1.25", spain)
        assertAmount("1250000", "1,250,000", mexico)
        assertNull(Format.parseAmount("", mexico))
        assertNull(Format.parseAmount("abc", mexico))
    }

    @Test
    fun `editable amounts drop trailing zeros`() {
        assertEquals("150", Format.editable("150.00"))
        assertEquals("12.5", Format.editable("12.50"))
        assertEquals("", Format.editable("0.00"))
        assertEquals("", Format.editable(null))
    }

    @Test
    fun `time ranges`() {
        assertEquals("10:00 – 14:30", Format.timeRange("10:00:00", "14:30:00"))
        assertEquals("10:00", Format.timeRange("10:00:00", null))
        assertEquals("", Format.timeRange(null, null))
    }

    @Test
    fun `sign-in QR codes carry the tenant slug and the nonce`() {
        assertEquals(
            "mi-negocio" to "abc123",
            parseLoginQr("https://mi-negocio.alquilame.io/auth/nonce/mi-negocio/abc123"),
        )
        assertNull(parseLoginQr("https://example.com/whatever"))
    }

    @Test
    fun `API payloads decode from snake_case and tolerate nulls and unknown keys`() {
        val dashboard = AppJson.decodeFromString(
            Envelope.serializer(Dashboard.serializer()),
            """{"success":true,"data":{"today":"2026-11-16","tomorrow":"2026-11-17","reservations_count":1,
               "quotes_count":0,"extra":1,"items":[{"type":"group","key":"group_3","reservation_id":null,
               "group_id":3,"title":"Boda","date":"2026-11-16","start_time":null,"end_time":null,
               "client":{"id":1,"full_name":"Ana López","phone":null,"email":null},
               "contract":{"id":9,"contract_number":"16112026-0001","status":"active"},
               "items":[{"merchandise":{"id":2,"name":"Silla","category":null,"price":"10.00","thumb_url":null},
               "quantity":20}]}]}}""",
        ).data!!
        val item = dashboard.items.single()
        assertTrue(item.isGroup)
        assertEquals("Ana López", item.client?.fullName)
        assertEquals("16112026-0001", item.contract?.contractNumber)
        assertEquals(20, item.items.single().quantity)

        val contract = AppJson.decodeFromString(
            Contract.serializer(),
            """{"id":1,"status":"draft","grand_total":"900.00","includes_tax":false,"tax_value":null,
               "extra_services":[{"name":"Flete","price":"150.00","description":null}],"payments":[],"payment_links":[]}""",
        )
        assertEquals("900.00", contract.grandTotal)
        assertEquals("Flete", contract.extraServices.single().name)
        assertEquals("0.00", contract.remainingBalance)
        assertEquals(String.serializer().descriptor.serialName, "kotlin.String")
    }
}
