package com.io.alquilame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.io.alquilame.R
import com.io.alquilame.ui.BottomBar
import com.io.alquilame.ui.Nav
import com.io.alquilame.ui.common.AppScreen
import com.io.alquilame.ui.common.Format
import com.io.alquilame.ui.common.Load
import com.io.alquilame.ui.common.MessageView
import com.io.alquilame.ui.common.SectionTitle
import com.io.alquilame.ui.common.rememberLoader
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** Month grid with the number of reservations per day and the agenda of the selected day. */
@Composable
fun CalendarScreen(nav: Nav) {
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selectedText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val month = YearMonth.parse(monthText)
    val selected = LocalDate.parse(selectedText)

    val calendar = rememberLoader("calendar-$monthText") { call { calendar(monthText) } }
    val day = rememberLoader("agenda-$selectedText") { call { agenda(selectedText) } }
    val counts = remember(calendar.data) { calendar.data?.days.orEmpty().associate { it.date to it.reservations } }
    val today = Format.date(calendar.data?.today) ?: LocalDate.now()

    fun show(newMonth: YearMonth) {
        monthText = newMonth.toString()
        // Keep a day selected inside the visible month.
        if (YearMonth.from(selected) != newMonth) {
            selectedText = (if (YearMonth.from(today) == newMonth) today else newMonth.atDay(1)).toString()
        }
    }

    AppScreen(
        title = stringResource(R.string.tab_calendar),
        actions = { TextButton(onClick = { show(YearMonth.from(today)); selectedText = today.toString() }) {
            Text(stringResource(R.string.today))
        } },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.newReservation(date = selectedText) }) {
                Icon(Icons.Default.Add, stringResource(R.string.new_reservation))
            }
        },
        bottomBar = { BottomBar(nav, "calendar") },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { show(month.minusMonths(1)) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous_month))
                    }
                    Text(
                        Format.month(month),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    IconButton(onClick = { show(month.plusMonths(1)) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_month))
                    }
                }
                MonthGrid(month, selected, today, counts) { selectedText = it.toString() }
            }
            item { SectionTitle(Format.longDate(selected)) }
            when (val state = day.state) {
                Load.Loading -> item {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is Load.Failed -> item {
                    MessageView(
                        Icons.Default.EventAvailable,
                        state.message,
                        Modifier.height(200.dp),
                        stringResource(R.string.retry),
                    ) { day.reload() }
                }
                is Load.Ready -> if (state.data.items.isEmpty()) {
                    item {
                        MessageView(
                            Icons.Default.EventAvailable,
                            stringResource(R.string.calendar_empty_day),
                            Modifier.height(200.dp),
                        )
                    }
                } else {
                    items(state.data.items, key = { it.key }) { item ->
                        AgendaCard(item, showDate = false, onClick = { nav.agendaItem(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    today: LocalDate,
    counts: Map<String, Int>,
    onSelect: (LocalDate) -> Unit,
) {
    val firstDayOfWeek = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    val weekDays = remember(firstDayOfWeek) { (0L..6L).map { firstDayOfWeek.plus(it) } }
    val offset = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val cells: List<LocalDate?> = List(offset) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }

    Column {
        Row(Modifier.fillMaxWidth()) {
            weekDays.forEach { weekDay ->
                Text(
                    weekDay.getDisplayName(TextStyle.NARROW, Locale.getDefault()).uppercase(),
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (date != null) {
                            DayCell(date, date == selected, date == today, counts[date.toString()] ?: 0) {
                                onSelect(date)
                            }
                        }
                    }
                }
                repeat(7 - week.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, selected: Boolean, today: Boolean, count: Int, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().clip(CircleShape)
            .background(if (selected) colors.primary else Color.Transparent)
            .then(if (today && !selected) Modifier.border(1.5.dp, colors.primary, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (count > 0 || today) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) colors.onPrimary else colors.onSurface,
        )
        if (count > 0) {
            Box(
                Modifier.size(6.dp).clip(CircleShape).background(if (selected) colors.onPrimary else colors.tertiary)
            )
        } else {
            Box(Modifier.size(6.dp))
        }
    }
}
