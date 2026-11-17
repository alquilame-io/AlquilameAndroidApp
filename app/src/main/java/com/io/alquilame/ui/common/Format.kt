package com.io.alquilame.ui.common

import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

/** Parsing and display helpers for the API's string dates, times and money. */
object Format {
    fun date(value: String?): LocalDate? = value?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }

    fun time(value: String?): LocalTime? = value?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

    fun decimal(value: String?): BigDecimal =
        value?.trim()?.replace(',', '.')?.toBigDecimalOrNull() ?: BigDecimal.ZERO

    /** "$1,250.00" in the tenant's currency. */
    fun money(amount: String?, currencyCode: String): String = money(decimal(amount), currencyCode)

    fun money(amount: BigDecimal, currencyCode: String): String {
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        runCatching { format.currency = Currency.getInstance(currencyCode) }
        return format.format(amount)
    }

    /** "sáb, 21 nov" — compact date for lists. */
    fun shortDate(date: LocalDate?): String =
        date?.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())) ?: ""

    fun shortDate(value: String?): String = shortDate(date(value))

    /** "21 nov 2026" */
    fun mediumDate(date: LocalDate?): String =
        date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())) ?: ""

    fun mediumDate(value: String?): String = mediumDate(date(value))

    /** "sábado, 21 de noviembre" */
    fun longDate(date: LocalDate?): String =
        date?.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()))
            ?.replaceFirstChar { it.titlecase(Locale.getDefault()) } ?: ""

    fun month(month: YearMonth): String =
        month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }

    fun hour(value: String?): String = time(value)?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: ""

    fun hour(time: LocalTime?): String = time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: ""

    /** "10:00 – 14:00", "10:00" or "" when the reservation is for the whole day. */
    fun timeRange(start: String?, end: String?): String {
        val from = hour(start)
        val to = hour(end)
        return when {
            from.isNotEmpty() && to.isNotEmpty() -> "$from – $to"
            from.isNotEmpty() -> from
            else -> ""
        }
    }

    /** "21 nov 2026" or "21 nov 2026 – 23 nov 2026". */
    fun dateRange(start: String?, end: String?): String {
        val from = date(start)
        val to = date(end)
        return when {
            from == null -> ""
            to == null || to == from -> mediumDate(from)
            else -> "${mediumDate(from)} – ${mediumDate(to)}"
        }
    }

    /** Accepts what a user types as an amount ("1.250,5", "1250.50") and normalises it for the API. */
    fun parseAmount(input: String, locale: Locale = Locale.getDefault()): BigDecimal? {
        val cleaned = input.trim().replace(" ", "").replace("$", "")
        if (cleaned.isEmpty()) return null
        val grouping = DecimalFormatSymbols.getInstance(locale).groupingSeparator
        val commas = cleaned.count { it == ',' }
        val dots = cleaned.count { it == '.' }
        val normalised = when {
            // Both present: whichever comes last is the decimal separator.
            commas > 0 && dots > 0 ->
                if (cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.')) {
                    cleaned.replace(".", "").replace(',', '.')
                } else {
                    cleaned.replace(",", "")
                }
            commas + dots == 0 -> cleaned
            // Repeated separator ("1,250,000") can only be grouping.
            commas + dots > 1 -> cleaned.replace(",", "").replace(".", "")
            else -> {
                val separator = if (commas == 1) ',' else '.'
                val decimals = cleaned.length - cleaned.indexOf(separator) - 1
                // "1,250" in a locale that groups with commas is one thousand two hundred fifty.
                if (separator == grouping && decimals == 3) {
                    cleaned.replace(separator.toString(), "")
                } else {
                    cleaned.replace(separator, '.')
                }
            }
        }
        return normalised.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 }
    }

    /** Amount string for an editable field: "150" instead of "150.00". */
    fun editable(amount: String?): String {
        val value = amount?.toBigDecimalOrNull() ?: return ""
        if (value.signum() == 0) return ""
        return value.stripTrailingZeros().toPlainString()
    }
}

/** A sign-in QR encodes `https://<tenant>/auth/nonce/<slug>/<nonce>`. */
fun parseLoginQr(content: String): Pair<String, String>? {
    val match = Regex("""/auth/nonce/([^/?#\s]+)/([^/?#\s]+)""").find(content) ?: return null
    return match.groupValues[1] to match.groupValues[2]
}
