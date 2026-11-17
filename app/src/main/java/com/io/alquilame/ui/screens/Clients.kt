package com.io.alquilame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.io.alquilame.R
import com.io.alquilame.data.Client
import com.io.alquilame.ui.Nav
import com.io.alquilame.ui.common.AppScreen
import com.io.alquilame.ui.common.ConfirmDialog
import com.io.alquilame.ui.common.DateField
import com.io.alquilame.ui.common.FormBody
import com.io.alquilame.ui.common.Format
import com.io.alquilame.ui.common.Intents
import com.io.alquilame.ui.common.LoadBox
import com.io.alquilame.ui.common.MessageView
import com.io.alquilame.ui.common.SearchField
import com.io.alquilame.ui.common.SectionTitle
import com.io.alquilame.ui.common.TextInput
import com.io.alquilame.ui.common.rememberActions
import com.io.alquilame.ui.common.rememberLoader
import java.time.LocalDate
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Composable
fun ClientsScreen(nav: Nav) {
    val loader = rememberLoader("clients") { call { clients() } }
    var query by rememberSaveable { mutableStateOf("") }

    AppScreen(
        title = stringResource(R.string.clients),
        onBack = nav::back,
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.newClient() }) {
                Icon(Icons.Default.PersonAdd, stringResource(R.string.new_client))
            }
        },
    ) { padding ->
        LoadBox(loader, Modifier.padding(padding)) { clients ->
            val shown = remember(clients, query) {
                val q = query.trim().lowercase()
                clients.filter {
                    q.isEmpty() || it.fullName.lowercase().contains(q) || it.phone.orEmpty().contains(q) ||
                        it.email.orEmpty().lowercase().contains(q)
                }
            }
            Column(Modifier.fillMaxSize()) {
                SearchField(
                    query,
                    { query = it },
                    stringResource(R.string.search_clients),
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                if (shown.isEmpty()) {
                    MessageView(
                        Icons.Default.People,
                        stringResource(if (clients.isEmpty()) R.string.clients_empty else R.string.no_results),
                    )
                } else {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                        items(shown, key = { it.id }) { client ->
                            ClientRow(client) { nav.client(client.id) }
                            HorizontalDivider(Modifier.padding(start = 72.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientRow(client: Client, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Initials(client.fullName)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(client.fullName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            val detail = listOfNotNull(client.phone, client.email).filter { it.isNotBlank() }.joinToString(" · ")
            if (detail.isNotEmpty()) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun Initials(name: String) {
    val initials = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
    }
}

/** Contact card with one-tap call / WhatsApp / email, plus the client's contracts and reservations. */
@Composable
fun ClientDetailScreen(nav: Nav, id: Int) {
    val context = LocalContext.current
    val loader = rememberLoader("client-$id") { call { client(id) } }
    val actions = rememberActions()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    AppScreen(
        title = loader.data?.client?.fullName ?: stringResource(R.string.client),
        onBack = nav::back,
        actions = {
            if (loader.data != null) {
                IconButton(onClick = { nav.editClient(id) }) { Icon(Icons.Default.Edit, stringResource(R.string.edit)) }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.Delete, stringResource(R.string.delete))
                }
            }
        },
        floatingActionButton = {
            loader.data?.client?.let { client ->
                ExtendedFloatingActionButton(
                    onClick = { nav.newReservation(clientId = client.id, clientName = client.fullName) },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text(stringResource(R.string.new_reservation)) },
                )
            }
        },
    ) { padding ->
        LoadBox(loader, Modifier.padding(padding)) { detail ->
            val client = detail.client
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!client.phone.isNullOrBlank()) {
                            ContactButton(Icons.Default.Call, stringResource(R.string.call)) {
                                Intents.dial(context, client.phone)
                            }
                            ContactButton(Icons.AutoMirrored.Filled.Chat, "WhatsApp") {
                                Intents.whatsApp(context, client.phone)
                            }
                        }
                        if (!client.email.isNullOrBlank()) {
                            ContactButton(Icons.Default.Email, stringResource(R.string.email)) {
                                Intents.email(context, client.email)
                            }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Field(R.string.phone, client.phone)
                        Field(R.string.email, client.email)
                        Field(R.string.address, client.address)
                        Field(
                            R.string.identification,
                            listOfNotNull(client.identificationType?.uppercase(), client.identificationNumber)
                                .joinToString(" ").ifBlank { null },
                        )
                        Field(R.string.date_of_birth, client.dateOfBirth?.let { Format.mediumDate(it) })
                        Field(R.string.notes, client.notes)
                    }
                }
                if (detail.contracts.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.tab_contracts)) }
                    items(detail.contracts, key = { "contract-${it.id}" }) { contract ->
                        ContractCard(contract) { nav.contract(contract.id) }
                    }
                }
                if (detail.agenda.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.reservations)) }
                    items(detail.agenda, key = { it.key }) { item ->
                        AgendaCard(item, showDate = true, onClick = { nav.agendaItem(item) })
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_client_title),
            text = stringResource(R.string.delete_client_text),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = { actions.run { call { deleteClient(id) }; nav.back() } },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun ContactButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 14.dp)) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, maxLines = 1)
    }
}

@Composable
private fun Field(label: Int, value: String?) {
    if (value.isNullOrBlank()) return
    Row {
        Text(
            stringResource(label),
            Modifier.width(120.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private val identificationTypes = listOf("ine", "dni", "passport", "license", "other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientEditScreen(nav: Nav, id: Int?) {
    val actions = rememberActions()
    val existing = if (id != null) rememberLoader("client-edit-$id") { call { client(id) } } else null

    var loaded by rememberSaveable { mutableStateOf(id == null) }
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var idType by rememberSaveable { mutableStateOf("") }
    var idNumber by rememberSaveable { mutableStateOf("") }
    var birth by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    val client = existing?.data?.client
    LaunchedEffect(client) {
        if (client != null && !loaded) {
            firstName = client.firstName
            lastName = client.lastName
            phone = client.phone.orEmpty()
            email = client.email.orEmpty()
            address = client.address.orEmpty()
            idType = client.identificationType.orEmpty()
            idNumber = client.identificationNumber.orEmpty()
            birth = client.dateOfBirth.orEmpty()
            notes = client.notes.orEmpty()
            loaded = true
        }
    }

    fun save() = actions.run {
        val body = buildJsonObject {
            put("first_name", firstName.trim())
            put("last_name", lastName.trim())
            put("phone", phone.trim())
            put("email", email.trim())
            put("address", address.trim())
            put("identification_type", idType)
            put("identification_number", idNumber.trim())
            put("date_of_birth", birth)
            put("notes", notes.trim())
        }
        if (id == null) {
            nav.client(call { createClient(body) }.id, replace = true)
        } else {
            call { updateClient(id, body) }
            nav.back()
        }
    }

    AppScreen(
        title = stringResource(if (id == null) R.string.new_client else R.string.edit_client),
        onBack = nav::back,
    ) { padding ->
        if (existing != null && !loaded) {
            LoadBox(existing, Modifier.padding(padding)) { }
        } else {
            FormBody(
                padding = padding,
                saveLabel = stringResource(R.string.save),
                onSave = ::save,
                saving = actions.busy,
                enabled = firstName.isNotBlank() && lastName.isNotBlank(),
            ) {
                TextInput(
                    stringResource(R.string.first_name),
                    firstName,
                    { firstName = it },
                    capitalization = KeyboardCapitalization.Words,
                )
                TextInput(
                    stringResource(R.string.last_name),
                    lastName,
                    { lastName = it },
                    capitalization = KeyboardCapitalization.Words,
                )
                TextInput(stringResource(R.string.phone), phone, { phone = it.take(20) }, keyboard = KeyboardType.Phone)
                TextInput(stringResource(R.string.email), email, { email = it }, keyboard = KeyboardType.Email)
                TextInput(stringResource(R.string.address), address, { address = it })

                var typeMenu by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = typeMenu, onExpandedChange = { typeMenu = it }) {
                    OutlinedTextField(
                        value = if (idType.isEmpty()) "" else idTypeLabel(idType),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.identification_type)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("—") },
                            onClick = { idType = ""; typeMenu = false },
                        )
                        identificationTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(idTypeLabel(type)) },
                                onClick = { idType = type; typeMenu = false },
                            )
                        }
                    }
                }
                TextInput(
                    stringResource(R.string.identification_number),
                    idNumber,
                    { idNumber = it.take(50) },
                    capitalization = KeyboardCapitalization.Characters,
                )
                DateField(
                    stringResource(R.string.date_of_birth),
                    Format.date(birth),
                    { birth = it?.toString().orEmpty() },
                    clearable = true,
                )
                TextInput(stringResource(R.string.notes), notes, { notes = it }, singleLine = false)
            }
        }
    }
}

@Composable
private fun idTypeLabel(type: String): String = when (type) {
    "ine" -> "INE"
    "dni" -> "DNI"
    "passport" -> stringResource(R.string.id_passport)
    "license" -> stringResource(R.string.id_license)
    else -> stringResource(R.string.id_other)
}
