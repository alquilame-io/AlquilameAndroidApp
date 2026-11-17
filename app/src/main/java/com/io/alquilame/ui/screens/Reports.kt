package com.io.alquilame.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.io.alquilame.R
import com.io.alquilame.data.DateRevenue
import com.io.alquilame.data.NamedRevenue
import com.io.alquilame.data.Nps
import com.io.alquilame.data.SessionCount
import com.io.alquilame.data.Traffic
import com.io.alquilame.ui.Nav
import com.io.alquilame.ui.common.AppScreen
import com.io.alquilame.ui.common.Format
import com.io.alquilame.ui.common.KeyValueRow
import com.io.alquilame.ui.common.LoadBox
import com.io.alquilame.ui.common.SectionTitle
import com.io.alquilame.ui.common.money
import com.io.alquilame.ui.common.rememberLoader
import com.io.alquilame.ui.theme.StatusColors
import java.time.LocalDate
import java.util.Locale

private enum class Period(val label: Int) {
    ThisMonth(R.string.period_this_month),
    LastMonth(R.string.period_last_month),
    Last30(R.string.period_last_30),
    ThisYear(R.string.period_this_year);

    fun range(today: LocalDate): Pair<LocalDate, LocalDate> = when (this) {
        ThisMonth -> today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())
        LastMonth -> today.minusMonths(1).let { it.withDayOfMonth(1) to it.withDayOfMonth(it.lengthOfMonth()) }
        Last30 -> today.minusDays(29) to today
        ThisYear -> today.withDayOfYear(1) to today.withDayOfYear(today.lengthOfYear())
    }
}

/** Revenue summary for a period: KPIs, revenue per day, best products, categories, clients and debts. */
@Composable
fun ReportsScreen(nav: Nav) {
    var period by rememberSaveable { mutableStateOf(Period.ThisMonth) }
    val (start, end) = period.range(LocalDate.now())
    val loader = rememberLoader("reports-$start-$end") { call { reports(start.toString(), end.toString()) } }

    AppScreen(
        title = stringResource(R.string.reports),
        subtitle = "${Format.mediumDate(start)} – ${Format.mediumDate(end)}",
        onBack = nav::back,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(Period.entries) { option ->
                    FilterChip(
                        selected = option == period,
                        onClick = { period = option },
                        label = { Text(stringResource(option.label)) },
                    )
                }
            }
            LoadBox(loader) { report ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Kpi(stringResource(R.string.kpi_revenue), money(report.totalRevenue), Modifier.weight(1f))
                        Kpi(stringResource(R.string.kpi_contracts), report.totalContracts.toString(), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Kpi(stringResource(R.string.kpi_average), money(report.averageContractValue), Modifier.weight(1f))
                        Kpi(stringResource(R.string.kpi_per_day), money(report.avgRevenuePerDay), Modifier.weight(1f))
                    }

                    if (report.revenueByDate.isNotEmpty()) {
                        SectionTitle(stringResource(R.string.revenue_by_day))
                        RevenueChart(report.revenueByDate)
                    }

                    if (report.locked) {
                        Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, null)
                                Text(stringResource(R.string.reports_locked), Modifier.padding(start = 12.dp))
                            }
                        }
                    } else {
                        Ranking(stringResource(R.string.top_products), report.topMerchandises) {
                            it.quantity?.let { quantity -> stringResource(R.string.n_rented, quantity) }
                        }
                        Ranking(stringResource(R.string.revenue_by_category), report.revenueByCategory) { null }
                        Ranking(stringResource(R.string.top_clients), report.topClients) {
                            it.contracts?.let { count -> stringResource(R.string.n_contracts, count) }
                        }
                        NpsSection(report.nps)
                        TrafficSection(report.traffic)
                        if (report.unpaidContracts.isNotEmpty()) {
                            SectionTitle(stringResource(R.string.unpaid_contracts))
                            report.unpaidContracts.forEach { contract ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { nav.contract(contract.id) }.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(contract.contractNumber.orEmpty(), fontWeight = FontWeight.Medium)
                                        Text(
                                            contract.client?.fullName ?: stringResource(R.string.no_client),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        money(contract.remainingBalance),
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Kpi(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Bar per day with revenue; labels show the first and last day. */
@Composable
private fun RevenueChart(points: List<DateRevenue>) {
    val values = points.map { Format.decimal(it.revenue).toFloat() }
    val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    val color = MaterialTheme.colorScheme.primary
    Column {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val slot = size.width / values.size
            val barWidth = (slot * 0.7f).coerceAtMost(48.dp.toPx())
            values.forEachIndexed { index, value ->
                val height = (value / max) * size.height
                drawRoundRect(
                    color = color,
                    topLeft = Offset(index * slot + (slot - barWidth) / 2, size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(
                Format.shortDate(points.first().date),
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (points.size > 1) {
                Text(
                    Format.shortDate(points.last().date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Ranking(title: String, rows: List<NamedRevenue>, detail: @Composable (NamedRevenue) -> String?) {
    if (rows.isEmpty()) return
    SectionTitle(title)
    val max = rows.maxOf { Format.decimal(it.revenue).toFloat() }.coerceAtLeast(1f)
    rows.forEach { row ->
        Column(Modifier.padding(vertical = 4.dp)) {
            Row {
                Text(row.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(money(row.revenue), fontWeight = FontWeight.Medium)
            }
            detail(row)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(
                progress = { Format.decimal(row.revenue).toFloat() / max },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                drawStopIndicator = {},
            )
        }
    }
}

/** Net Promoter Score with the promoter / passive / detractor split. */
@Composable
private fun NpsSection(nps: Nps) {
    SectionTitle(stringResource(R.string.nps_title))
    if (nps.total == 0) {
        Text(
            stringResource(R.string.nps_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val scoreColor = when {
        nps.score >= 50 -> StatusColors.success
        nps.score >= 0 -> StatusColors.warning
        else -> MaterialTheme.colorScheme.error
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            nps.score.toString(),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = scoreColor,
        )
        Text(
            pluralStringResource(R.plurals.responses, nps.total, nps.total),
            Modifier.padding(start = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    val detractorColor = MaterialTheme.colorScheme.error
    Canvas(Modifier.fillMaxWidth().height(10.dp)) {
        var x = 0f
        listOf(
            nps.promoterPct to StatusColors.success,
            nps.passivePct to StatusColors.warning,
            nps.detractorPct to detractorColor,
        ).forEach { (percent, color) ->
            val width = size.width * (percent.toFloat() / 100f)
            drawRect(color, topLeft = Offset(x, 0f), size = Size(width, size.height))
            x += width
        }
    }
    KeyValueRow(stringResource(R.string.nps_promoters), "${nps.promoters} (${nps.promoterPct}%)")
    KeyValueRow(stringResource(R.string.nps_passives), "${nps.passives} (${nps.passivePct}%)")
    KeyValueRow(stringResource(R.string.nps_detractors), "${nps.detractors} (${nps.detractorPct}%)")
}

/** Storefront traffic from Google Analytics and the visit → WhatsApp → booking funnel. */
@Composable
private fun TrafficSection(traffic: Traffic) {
    SectionTitle(stringResource(R.string.traffic_title))
    when (traffic.status) {
        "ok" -> {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Kpi(stringResource(R.string.traffic_sessions), traffic.sessions.toString(), Modifier.weight(1f))
                Kpi(stringResource(R.string.traffic_users), traffic.users.toString(), Modifier.weight(1f))
                Kpi(stringResource(R.string.traffic_new_users), traffic.newUsers.toString(), Modifier.weight(1f))
            }
            KeyValueRow(stringResource(R.string.traffic_product_views), traffic.productViews.toString())
            KeyValueRow(
                stringResource(R.string.traffic_whatsapp),
                "${traffic.whatsappClicks} · ${rate(traffic.productViews, traffic.whatsappClicks)}",
            )
            KeyValueRow(
                stringResource(R.string.traffic_bookings),
                "${traffic.bookings} · ${rate(traffic.whatsappClicks, traffic.bookings)}",
            )
            SessionBars(stringResource(R.string.traffic_sources), traffic.sources)
            SessionBars(stringResource(R.string.traffic_devices), traffic.devices)
        }
        "error" -> Text(
            stringResource(R.string.traffic_error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        else -> Text(
            stringResource(R.string.traffic_not_connected),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Step-to-step conversion, "—" when there is nothing to convert from. */
private fun rate(from: Int, to: Int): String =
    if (from <= 0) "—" else String.format(Locale.getDefault(), "%.1f%%", to * 100.0 / from)

@Composable
private fun SessionBars(title: String, rows: List<SessionCount>) {
    if (rows.isEmpty()) return
    Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
    val max = rows.maxOf { it.sessions }.coerceAtLeast(1)
    rows.take(6).forEach { row ->
        Column(Modifier.padding(vertical = 3.dp)) {
            Row {
                Text(row.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(row.sessions.toString(), fontWeight = FontWeight.Medium)
            }
            LinearProgressIndicator(
                progress = { row.sessions.toFloat() / max },
                modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                drawStopIndicator = {},
            )
        }
    }
}
