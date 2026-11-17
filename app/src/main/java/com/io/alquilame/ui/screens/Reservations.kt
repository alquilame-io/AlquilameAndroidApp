package com.io.alquilame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.io.alquilame.R
import com.io.alquilame.data.ClientBrief
import com.io.alquilame.data.ContractBrief
import com.io.alquilame.data.Reservation
import com.io.alquilame.data.ReservationGroup
import com.io.alquilame.ui.Nav
import com.io.alquilame.ui.common.ActionError
import com.io.alquilame.ui.common.AppScreen
import com.io.alquilame.ui.common.ClientField
import com.io.alquilame.ui.common.ConfirmDialog
import com.io.alquilame.ui.common.ContractStatusChip
import com.io.alquilame.ui.common.DateField
import com.io.alquilame.ui.common.FormBody
import com.io.alquilame.ui.common.Format
import com.io.alquilame.ui.common.IconLine
import com.io.alquilame.ui.common.LoadBox
import com.io.alquilame.ui.common.PickedProduct
import com.io.alquilame.ui.common.ProductImage
import com.io.alquilame.ui.common.ProductPickerDialog
import com.io.alquilame.ui.common.QuantityStepper
import com.io.alquilame.ui.common.SectionTitle
import com.io.alquilame.ui.common.TextInput
import com.io.alquilame.ui.common.TimeField
import com.io.alquilame.ui.common.money
import com.io.alquilame.ui.common.rememberActions
import com.io.alquilame.ui.common.rememberLoader
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** Form state of a new reservation; a ViewModel so it survives rotation. */
class ReservationDraft : ViewModel() {
    var initialised = false
    var name by mutableStateOf("")
    var notes by mutableStateOf("")
    var startDate: LocalDate by mutableStateOf(LocalDate.now())
    var endDate: LocalDate? by mutableStateOf(null)
    var startTime: LocalTime? by mutableStateOf(null)
    var endTime: LocalTime? by mutableStateOf(null)
    var client: ClientBrief? by mutableStateOf(null)
    val products = mutableStateListOf<PickedProduct>()

    val isMultiDay get() = endDate?.let { it != startDate } ?: false

    /** One product for one day is a plain reservation; anything else is an event (group). */
    val isEvent get() = products.size > 1 || isMultiDay || name.isNotBlank() || notes.isNotBlank()
}

/**
 * Creates a reservation. With one product on one day it books a single
 * reservation; with several products, several days or a name it books an
 * event that groups them.
 */
@Composable
fun ReservationEditorScreen(nav: Nav, initialDate: String, clientId: Int?, clientName: String) {
    val draft: ReservationDraft = viewModel()
    if (!draft.initialised) {
        draft.initialised = true
        Format.date(initialDate)?.let { draft.startDate = it }
        if (clientId != null) draft.client = ClientBrief(clientId, clientName)
    }
    val actions = rememberActions()
    var picking by rememberSaveable { mutableStateOf(false) }

    fun save() = actions.run {
        val end = draft.endDate?.takeIf { !it.isBefore(draft.startDate) }
        if (draft.isEvent) {
            val group = call {
                createGroup(
                    buildJsonObject {
                        put("name", draft.name.trim())
                        put("notes", draft.notes.trim())
                        put("start_date", draft.startDate.toString())
                        put("end_date", (end ?: draft.startDate).toString())
                        put("client_id", draft.client?.id)
                        putJsonArray("items") {
                            draft.products.forEach { picked ->
                                addJsonObject {
                                    put("merchandise_id", picked.product.id)
                                    put("quantity", picked.quantity)
                                    put("start_time", draft.startTime?.let(Format::hour))
                                    put("end_time", draft.endTime?.let(Format::hour))
                                }
                            }
                        }
                    }
                )
            }
            nav.group(group.id, replace = true)
        } else {
            val picked = draft.products.first()
            val reservation = call {
                createReservation(
                    buildJsonObject {
                        put("merchandise_id", picked.product.id)
                        put("quantity", picked.quantity)
                        put("date", draft.startDate.toString())
                        put("start_time", draft.startTime?.let(Format::hour))
                        put("end_time", draft.endTime?.let(Format::hour))
                        put("client_id", draft.client?.id)
                    }
                )
            }
            nav.reservation(reservation.id, replace = true)
        }
    }

    AppScreen(title = stringResource(R.string.new_reservation), onBack = nav::back) { padding ->
        FormBody(
            padding = padding,
            saveLabel = stringResource(if (draft.isEvent) R.string.save_event else R.string.save_reservation),
            onSave = ::save,
            saving = actions.busy,
            enabled = draft.products.isNotEmpty(),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField(
                    stringResource(R.string.date_from),
                    draft.startDate,
                    { if (it != null) draft.startDate = it },
                    Modifier.weight(1f),
                )
                DateField(
                    stringResource(R.string.date_to_optional),
                    draft.endDate,
                    { draft.endDate = it },
                    Modifier.weight(1f),
                    clearable = true,
                )
            }
            if (draft.endDate?.isBefore(draft.startDate) == true) {
                Text(
                    stringResource(R.string.error_end_before_start),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeField(stringResource(R.string.time_from), draft.startTime, { draft.startTime = it }, Modifier.weight(1f))
                TimeField(stringResource(R.string.time_to), draft.endTime, { draft.endTime = it }, Modifier.weight(1f))
            }
            ClientField(draft.client, { draft.client = it })

            SectionTitle(stringResource(R.string.products))
            draft.products.forEachIndexed { index, picked ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProductImage(picked.product.thumbUrl, 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(picked.product.name, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    QuantityStepper(
                        value = picked.quantity,
                        onChange = { quantity ->
                            if (quantity <= 0) draft.products.removeAt(index) else draft.products[index] = picked.copy(quantity = quantity)
                        },
                        max = (picked.product.available ?: picked.product.quantity).coerceAtLeast(picked.quantity),
                    )
                }
            }
            OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (draft.products.isEmpty()) R.string.add_products else R.string.edit_products))
            }

            SectionTitle(stringResource(R.string.event_details_optional))
            TextInput(stringResource(R.string.event_name), draft.name, { draft.name = it })
            TextInput(stringResource(R.string.notes), draft.notes, { draft.notes = it }, singleLine = false)
        }
    }

    if (picking) {
        ProductPickerDialog(
            date = draft.startDate,
            startTime = draft.startTime,
            endTime = draft.endTime,
            initial = draft.products.associate { it.product.id to it.quantity },
            onDone = { picked ->
                draft.products.clear()
                draft.products.addAll(picked)
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Single reservation
// ---------------------------------------------------------------------------------------------

@Composable
fun ReservationDetailScreen(nav: Nav, id: Int) {
    val loader = rememberLoader("reservation-$id") { call { reservation(id) } }
    val actions = rememberActions()
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    AppScreen(
        title = stringResource(R.string.reservation),
        onBack = nav::back,
        actions = {
            if (loader.data != null) {
                IconButton(onClick = { editing = true }) { Icon(Icons.Default.Edit, stringResource(R.string.edit)) }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.Delete, stringResource(R.string.delete))
                }
            }
        },
    ) { padding ->
        LoadBox(loader, Modifier.padding(padding)) { reservation ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProductImage(reservation.merchandise?.thumbUrl, 72.dp)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            reservation.merchandise?.name.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            pluralStringResource(R.plurals.units, reservation.quantity, reservation.quantity),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                InfoCard {
                    IconLine(Icons.Default.CalendarMonth, Format.longDate(Format.date(reservation.date)))
                    Format.timeRange(reservation.startTime, reservation.endTime).takeIf { it.isNotEmpty() }?.let {
                        IconLine(Icons.Default.Schedule, it)
                    }
                    IconLine(Icons.Default.Person, reservation.client?.fullName ?: stringResource(R.string.no_client))
                }
                reservation.client?.let { client ->
                    TextButton(onClick = { nav.client(client.id) }) { Text(stringResource(R.string.view_client)) }
                }
                if (reservation.groupId != null) {
                    OutlinedButton(onClick = { nav.group(reservation.groupId) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.view_event))
                    }
                } else {
                    ContractAction(
                        contract = reservation.contract,
                        busy = actions.busy,
                        onOpen = { nav.contract(it) },
                        onCreate = { actions.run { nav.contract(call { contractFromReservation(id) }.id) } },
                    )
                }
            }
            if (editing) {
                ReservationEditDialog(
                    reservation = reservation,
                    onDismiss = { editing = false },
                    onSaved = { loader.set(it); editing = false },
                )
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_reservation_title),
            text = stringResource(R.string.delete_reservation_text),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = { actions.run { call { deleteReservation(id) }; nav.back() } },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

/** "Create contract" for bookings without one, or a link to the existing contract. */
@Composable
private fun ContractAction(contract: ContractBrief?, busy: Boolean, onOpen: (Int) -> Unit, onCreate: () -> Unit) {
    if (contract != null) {
        OutlinedButton(onClick = { onOpen(contract.id) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Default.Description, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.contract_n, contract.contractNumber.orEmpty()))
            Spacer(Modifier.width(8.dp))
            ContractStatusChip(contract.status)
        }
    } else {
        Button(onClick = onCreate, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Default.Description, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.create_contract))
        }
    }
}

@Composable
private fun ReservationEditDialog(reservation: Reservation, onDismiss: () -> Unit, onSaved: (Reservation) -> Unit) {
    val actions = rememberActions()
    var date by remember { mutableStateOf(Format.date(reservation.date) ?: LocalDate.now()) }
    var startTime by remember { mutableStateOf(Format.time(reservation.startTime)) }
    var endTime by remember { mutableStateOf(Format.time(reservation.endTime)) }
    var quantity by remember { mutableStateOf(reservation.quantity) }
    var client by remember { mutableStateOf(reservation.client) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_reservation)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField(stringResource(R.string.date), date, { if (it != null) date = it })
                TimeField(stringResource(R.string.time_from), startTime, { startTime = it })
                TimeField(stringResource(R.string.time_to), endTime, { endTime = it })
                if (reservation.contract == null) ClientField(client, { client = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.quantity), Modifier.weight(1f))
                    QuantityStepper(quantity, { quantity = it }, min = 1)
                }
                ActionError(actions)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !actions.busy,
                onClick = {
                    actions.run {
                        onSaved(
                            call {
                                updateReservation(
                                    reservation.id,
                                    buildJsonObject {
                                        put("date", date.toString())
                                        put("start_time", startTime?.let(Format::hour))
                                        put("end_time", endTime?.let(Format::hour))
                                        put("quantity", quantity)
                                        put("client_id", client?.id)
                                    },
                                )
                            }
                        )
                    }
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// ---------------------------------------------------------------------------------------------
// Event (reservation group)
// ---------------------------------------------------------------------------------------------

@Composable
fun GroupDetailScreen(nav: Nav, id: Int) {
    val loader = rememberLoader("group-$id") { call { group(id) } }
    val actions = rememberActions()
    var editing by rememberSaveable { mutableStateOf(false) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var removing by remember { mutableStateOf<Int?>(null) }
    var menu by remember { mutableStateOf(false) }
    var rescheduling by rememberSaveable { mutableStateOf(false) }
    var confirmUngroup by rememberSaveable { mutableStateOf(false) }

    AppScreen(
        title = loader.data?.name?.ifBlank { null } ?: stringResource(R.string.event),
        onBack = nav::back,
        actions = {
            if (loader.data != null) {
                IconButton(onClick = { editing = true }) { Icon(Icons.Default.Edit, stringResource(R.string.edit)) }
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.more_options))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.change_dates)) },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                        onClick = { menu = false; rescheduling = true },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.ungroup)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.CallSplit, null) },
                        onClick = { menu = false; confirmUngroup = true },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { menu = false; confirmDelete = true },
                    )
                }
            }
        },
    ) { padding ->
        LoadBox(loader, Modifier.padding(padding)) { group ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InfoCard {
                    val start = group.startDate ?: group.reservations.minOfOrNull { it.date }
                    val end = group.endDate ?: group.reservations.maxOfOrNull { it.date }
                    IconLine(Icons.Default.CalendarMonth, Format.dateRange(start, end))
                    group.items.firstOrNull()?.let { first ->
                        Format.timeRange(first.startTime, first.endTime).takeIf { it.isNotEmpty() }?.let {
                            IconLine(Icons.Default.Schedule, it)
                        }
                    }
                    IconLine(Icons.Default.Person, group.client?.fullName ?: stringResource(R.string.no_client))
                    if (!group.notes.isNullOrBlank()) IconLine(Icons.AutoMirrored.Filled.Notes, group.notes)
                }
                group.client?.let { client ->
                    TextButton(onClick = { nav.client(client.id) }) { Text(stringResource(R.string.view_client)) }
                }

                SectionTitle(stringResource(R.string.products))
                group.items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProductImage(item.merchandise?.thumbUrl, 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${item.quantity} × ${item.merchandise?.name.orEmpty()}",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            val price = item.merchandise?.price?.let { money(it) }
                            val days = pluralStringResource(R.plurals.days, item.days, item.days)
                            Text(
                                listOfNotNull(price, days).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { removing = item.merchandise?.id }, enabled = !actions.busy) {
                            Icon(Icons.Default.RemoveCircleOutline, stringResource(R.string.remove))
                        }
                    }
                    HorizontalDivider()
                }
                OutlinedButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_products))
                }
                Spacer(Modifier.height(4.dp))
                ContractAction(
                    contract = group.contract,
                    busy = actions.busy,
                    onOpen = { nav.contract(it) },
                    onCreate = { actions.run { nav.contract(call { contractFromGroup(id) }.id) } },
                )
            }

            if (editing) {
                GroupEditDialog(group, onDismiss = { editing = false }, onSaved = { loader.set(it); editing = false })
            }
            if (rescheduling) {
                RescheduleDialog(group, onDismiss = { rescheduling = false }) {
                    loader.set(it)
                    rescheduling = false
                }
            }
            if (adding) {
                val existing = group.items.mapNotNull { it.merchandise?.id }.toSet()
                ProductPickerDialog(
                    date = Format.date(group.startDate ?: group.reservations.firstOrNull()?.date),
                    startTime = Format.time(group.items.firstOrNull()?.startTime),
                    endTime = Format.time(group.items.firstOrNull()?.endTime),
                    initial = emptyMap(),
                    excludeGroupId = id,
                    onDone = { picked ->
                        adding = false
                        val first = group.items.firstOrNull()
                        actions.run {
                            // Products already in the event are skipped; edit them by removing and re-adding.
                            picked.filter { it.product.id !in existing }.forEach { item ->
                                loader.set(
                                    call {
                                        addGroupItem(
                                            id,
                                            buildJsonObject {
                                                put("merchandise_id", item.product.id)
                                                put("quantity", item.quantity)
                                                put("start_time", first?.startTime)
                                                put("end_time", first?.endTime)
                                            },
                                        )
                                    }
                                )
                            }
                        }
                    },
                    onDismiss = { adding = false },
                )
            }
        }
    }

    removing?.let { merchandiseId ->
        ConfirmDialog(
            title = stringResource(R.string.remove_product_title),
            text = stringResource(R.string.remove_product_text),
            confirmLabel = stringResource(R.string.remove),
            onConfirm = { actions.run { loader.set(call { removeGroupItem(id, merchandiseId) }) } },
            onDismiss = { removing = null },
        )
    }
    if (confirmUngroup) {
        ConfirmDialog(
            title = stringResource(R.string.ungroup_title),
            text = stringResource(R.string.ungroup_text),
            confirmLabel = stringResource(R.string.ungroup),
            destructive = false,
            onConfirm = { actions.run { call { ungroup(id) }; nav.back() } },
            onDismiss = { confirmUngroup = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_event_title),
            text = stringResource(R.string.delete_event_text),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = { actions.run { call { deleteGroup(id) }; nav.back() } },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun GroupEditDialog(group: ReservationGroup, onDismiss: () -> Unit, onSaved: (ReservationGroup) -> Unit) {
    val actions = rememberActions()
    var name by remember { mutableStateOf(group.name) }
    var notes by remember { mutableStateOf(group.notes.orEmpty()) }
    var client by remember { mutableStateOf(group.client) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_event)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextInput(stringResource(R.string.event_name), name, { name = it })
                if (group.contract == null) ClientField(client, { client = it })
                TextInput(stringResource(R.string.notes), notes, { notes = it }, singleLine = false)
                ActionError(actions)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !actions.busy && name.isNotBlank(),
                onClick = {
                    actions.run {
                        onSaved(
                            call {
                                updateGroup(
                                    group.id,
                                    buildJsonObject {
                                        put("name", name.trim())
                                        put("notes", notes.trim())
                                        put("client_id", client?.id)
                                    },
                                )
                            }
                        )
                    }
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** New date range for an event; every product is re-booked on each new day. */
@Composable
private fun RescheduleDialog(group: ReservationGroup, onDismiss: () -> Unit, onSaved: (ReservationGroup) -> Unit) {
    val actions = rememberActions()
    val first = Format.date(group.startDate ?: group.reservations.minOfOrNull { it.date })
    val last = Format.date(group.endDate ?: group.reservations.maxOfOrNull { it.date })
    var start by remember { mutableStateOf(first ?: LocalDate.now()) }
    var end by remember { mutableStateOf(last ?: start) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.change_dates)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.change_dates_text), style = MaterialTheme.typography.bodyMedium)
                DateField(
                    stringResource(R.string.date_from),
                    start,
                    { picked ->
                        if (picked != null) {
                            // Keep the event the same length when the first day moves.
                            end = picked.plusDays(java.time.temporal.ChronoUnit.DAYS.between(start, end).coerceAtLeast(0))
                            start = picked
                        }
                    },
                )
                DateField(stringResource(R.string.date_to), end, { if (it != null) end = it })
                ActionError(actions)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !actions.busy && !end.isBefore(start),
                onClick = {
                    actions.run {
                        onSaved(
                            call {
                                rescheduleGroup(
                                    group.id,
                                    buildJsonObject {
                                        put("start_date", start.toString())
                                        put("end_date", end.toString())
                                    },
                                )
                            }
                        )
                    }
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
