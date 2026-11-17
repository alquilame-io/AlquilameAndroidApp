package com.io.alquilame.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.io.alquilame.ui.common.ActionError
import com.io.alquilame.ui.common.TextInput
import com.io.alquilame.ui.common.rememberActions
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.io.alquilame.R
import com.io.alquilame.data.AgendaItem
import com.io.alquilame.ui.BottomBar
import com.io.alquilame.ui.Nav
import com.io.alquilame.ui.common.AppScreen
import com.io.alquilame.ui.common.ContractStatusChip
import com.io.alquilame.ui.common.Format
import com.io.alquilame.ui.common.IconLine
import com.io.alquilame.ui.common.LoadBox
import com.io.alquilame.ui.common.LocalBackend
import com.io.alquilame.ui.common.MessageView
import com.io.alquilame.ui.common.SectionTitle
import com.io.alquilame.ui.common.rememberLoader
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Today, tomorrow and upcoming reservations, with shortcuts to the common actions. */
@Composable
fun HomeScreen(nav: Nav) {
    val tenant = LocalBackend.current.session.tenant
    val loader = rememberLoader("dashboard") { call { dashboard() } }
    val actions = rememberActions()
    // Reservations picked (by long press) to be grouped into one event.
    val selection = remember { mutableStateListOf<Int>() }
    var naming by rememberSaveable { mutableStateOf(false) }
    val selecting = selection.isNotEmpty()
    BackHandler(enabled = selecting) { selection.clear() }

    AppScreen(
        title = if (selecting) {
            pluralStringResource(R.plurals.selected, selection.size, selection.size)
        } else {
            tenant?.name ?: stringResource(R.string.app_name)
        },
        subtitle = if (selecting) {
            stringResource(R.string.group_hint)
        } else {
            Format.longDate(Format.date(loader.data?.today))
        },
        onBack = if (selecting) ({ selection.clear() }) else null,
        actions = {
            if (selecting) {
                TextButton(onClick = { naming = true }, enabled = selection.size >= 2) {
                    Text(stringResource(R.string.group_action))
                }
            }
        },
        floatingActionButton = {
            if (!selecting) {
                ExtendedFloatingActionButton(
                    onClick = { nav.newReservation() },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text(stringResource(R.string.new_reservation)) },
                )
            }
        },
        bottomBar = { BottomBar(nav, "home") },
    ) { padding ->
        LoadBox(loader, Modifier.padding(padding)) { dashboard ->
            val today = dashboard.items.filter { it.date == dashboard.today }
            val tomorrow = dashboard.items.filter { it.date == dashboard.tomorrow }
            val later = dashboard.items.filter { it.date != dashboard.today && it.date != dashboard.tomorrow }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Shortcut(Icons.Default.RequestQuote, stringResource(R.string.quotes_n, dashboard.quotesCount)) {
                            nav.quotes()
                        }
                        Shortcut(Icons.Default.Link, stringResource(R.string.payment_links)) { nav.paymentLinks() }
                        Shortcut(Icons.Default.People, stringResource(R.string.clients)) { nav.clients() }
                        Shortcut(Icons.Default.BarChart, stringResource(R.string.reports)) { nav.reports() }
                    }
                }
                if (dashboard.items.isEmpty()) {
                    item {
                        MessageView(
                            Icons.Default.EventAvailable,
                            stringResource(R.string.home_empty),
                            Modifier.height(320.dp),
                        )
                    }
                }
                agendaSection(R.string.today, today, nav, false, selection, alwaysShow = dashboard.items.isNotEmpty())
                agendaSection(R.string.tomorrow, tomorrow, nav, false, selection)
                agendaSection(R.string.upcoming, later, nav, true, selection)
                if (dashboard.items.count { !it.isGroup && it.contract == null } >= 2) {
                    item {
                        Text(
                            stringResource(R.string.group_tip),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (naming) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { naming = false },
            title = { Text(stringResource(R.string.group_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextInput(stringResource(R.string.event_name), name, { name = it })
                    ActionError(actions)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !actions.busy,
                    onClick = {
                        actions.run {
                            val group = call {
                                mergeReservations(
                                    buildJsonObject {
                                        put("name", name.trim())
                                        putJsonArray("reservation_ids") { selection.forEach { add(it) } }
                                    }
                                )
                            }
                            naming = false
                            selection.clear()
                            nav.group(group.id)
                        }
                    },
                ) { Text(stringResource(R.string.group_action)) }
            },
            dismissButton = { TextButton(onClick = { naming = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Shortcut(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label) }, leadingIcon = { Icon(icon, null) })
}

private fun LazyListScope.agendaSection(
    title: Int,
    items: List<AgendaItem>,
    nav: Nav,
    showDate: Boolean,
    selection: SnapshotStateList<Int>,
    alwaysShow: Boolean = false,
) {
    if (items.isEmpty() && !alwaysShow) return
    item(key = "title-$title") { SectionTitle("${stringResource(title)} · ${items.size}") }
    if (items.isEmpty()) {
        item(key = "empty-$title") {
            Text(
                stringResource(R.string.nothing_today),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    items(items, key = { "$title-${it.key}" }) { item ->
        // Only standalone reservations without a contract can be grouped into an event.
        val id = item.reservationId?.takeIf { !item.isGroup && item.contract == null }
        fun toggle(id: Int) {
            if (!selection.remove(id)) selection.add(id)
        }
        AgendaCard(
            item,
            showDate = showDate,
            selected = id != null && id in selection,
            onClick = { if (selection.isNotEmpty()) id?.let(::toggle) else nav.agendaItem(item) },
            onLongClick = id?.let { { toggle(it) } },
        )
    }
}

/** One reservation or event in a list. */
@Composable
fun AgendaCard(
    item: AgendaItem,
    showDate: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth().clip(CardDefaults.shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            if (showDate) {
                DateBadge(item.date)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title.ifBlank { stringResource(R.string.reservation) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    item.contract?.let { ContractStatusChip(it.status, Modifier.padding(start = 8.dp)) }
                }
                val time = Format.timeRange(item.startTime, item.endTime)
                val range = if (item.isGroup && item.endDate != null && item.endDate != item.startDate) {
                    Format.dateRange(item.startDate, item.endDate)
                } else {
                    ""
                }
                val whenText = listOf(range, time).filter { it.isNotEmpty() }.joinToString(" · ")
                if (whenText.isNotEmpty()) IconLine(Icons.Default.Schedule, whenText)
                item.client?.let { IconLine(Icons.Default.Person, it.fullName) }
                val products = if (item.isGroup) {
                    item.items.joinToString(" · ") { "${it.quantity} × ${it.merchandise?.name.orEmpty()}" }
                } else {
                    pluralStringResource(R.plurals.units, item.items.sumOf { it.quantity }, item.items.sumOf { it.quantity })
                }
                Text(
                    products,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DateBadge(date: String) {
    val parsed = Format.date(date)
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            parsed?.dayOfMonth?.toString() ?: "",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            parsed?.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))?.uppercase() ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
