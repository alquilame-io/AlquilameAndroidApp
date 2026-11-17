package com.io.alquilame.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.io.alquilame.R
import com.io.alquilame.data.Merchandise
import com.io.alquilame.ui.BottomBar
import com.io.alquilame.ui.Nav
import com.io.alquilame.ui.common.AmountInput
import com.io.alquilame.ui.common.AppScreen
import com.io.alquilame.ui.common.ConfirmDialog
import com.io.alquilame.ui.common.FormBody
import com.io.alquilame.ui.common.Format
import com.io.alquilame.ui.common.LoadBox
import com.io.alquilame.ui.common.MessageView
import com.io.alquilame.ui.common.NavRow
import com.io.alquilame.ui.common.ProductImage
import com.io.alquilame.ui.common.SearchField
import com.io.alquilame.ui.common.TextInput
import com.io.alquilame.ui.common.money
import com.io.alquilame.ui.common.rememberActions
import com.io.alquilame.ui.common.rememberLoader
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun InventoryScreen(nav: Nav) {
    val loader = rememberLoader("merchandises") { call { merchandises() } }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }

    AppScreen(
        title = stringResource(R.string.tab_inventory),
        subtitle = loader.data?.let { stringResource(R.string.n_products, it.items.size) },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.newMerchandise() }) {
                Icon(Icons.Default.Add, stringResource(R.string.new_product))
            }
        },
        bottomBar = { BottomBar(nav, "inventory") },
    ) { padding ->
        LoadBox(loader, Modifier.padding(padding)) { list ->
            val shown = remember(list, query, category) {
                val q = query.trim().lowercase()
                list.items.filter { product ->
                    (category == null || product.category == category) &&
                        (q.isEmpty() || product.name.lowercase().contains(q) ||
                            product.code.orEmpty().lowercase().contains(q) ||
                            product.tags.any { it.lowercase().contains(q) })
                }
            }
            Column(Modifier.fillMaxSize()) {
                SearchField(
                    query,
                    { query = it },
                    stringResource(R.string.search_products),
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                if (list.categories.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            FilterChip(
                                selected = category == null,
                                onClick = { category = null },
                                label = { Text(stringResource(R.string.all)) },
                            )
                        }
                        items(list.categories) { name ->
                            FilterChip(
                                selected = category == name,
                                onClick = { category = if (category == name) null else name },
                                label = { Text(name) },
                            )
                        }
                    }
                }
                if (shown.isEmpty()) {
                    MessageView(
                        Icons.Default.Inventory2,
                        stringResource(if (list.items.isEmpty()) R.string.inventory_empty else R.string.no_products),
                    )
                } else {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                        items(shown, key = { it.id }) { product ->
                            ProductRow(product) { nav.merchandise(product.id) }
                            HorizontalDivider(Modifier.padding(start = 88.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductRow(product: Merchandise, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductImage(product.thumbUrl, 56.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                product.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val detail = listOfNotNull(product.category, product.code).filter { it.isNotBlank() }.joinToString(" · ")
            if (detail.isNotEmpty()) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            product.price?.let { Text(money(it), style = MaterialTheme.typography.bodyLarge) }
            Text(
                stringResource(R.string.n_in_stock, product.quantity),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Create or edit a product, including its photo from the camera or the gallery. */
@Composable
fun MerchandiseEditScreen(nav: Nav, id: Int?) {
    val context = LocalContext.current
    val actions = rememberActions()
    val catalog = rememberLoader("merchandise-categories") { call { merchandises() } }
    val existing = if (id != null) rememberLoader("merchandise-$id") { call { merchandise(id) } } else null

    var loaded by rememberSaveable { mutableStateOf(id == null) }
    var name by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("1") }
    var price by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable { mutableStateOf("") }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    var choosingPhoto by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val product = existing?.data
    LaunchedEffect(product) {
        if (product != null && !loaded) {
            name = product.name
            quantity = product.quantity.toString()
            price = Format.editable(product.price)
            category = product.category.orEmpty()
            code = product.code.orEmpty()
            location = product.location.orEmpty()
            description = product.description.orEmpty()
            tags = product.tags.joinToString(", ")
            loaded = true
        }
    }
    val priceLocked = product?.hasReservations == true

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photo = uri.toString()
    }
    val cameraFile = remember { File(File(context.cacheDir, "shared").apply { mkdirs() }, "product_photo.jpg") }
    val cameraUri = remember { FileProvider.getUriForFile(context, "${context.packageName}.files", cameraFile) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        // The file is reused, so the timestamp makes Coil reload the preview.
        if (taken) photo = "$cameraUri?t=${System.currentTimeMillis()}"
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) camera.launch(cameraUri)
    }

    fun save() = actions.run {
        val fields = buildMap<String, RequestBody> {
            fun text(key: String, value: String) = put(key, value.toRequestBody("text/plain".toMediaType()))
            text("name", name.trim())
            text("quantity", (quantity.toIntOrNull() ?: 1).toString())
            if (!priceLocked) Format.parseAmount(price)?.let { text("price", it.toPlainString()) }
            text("category", category.trim())
            text("code", code.trim())
            text("location", location.trim())
            text("description", description.trim())
            text("tags_string", tags.trim())
        }
        val image = photo?.let { encodePhoto(context, Uri.parse(it)) }?.let { bytes ->
            MultipartBody.Part.createFormData("image", "photo.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
        }
        call { if (id == null) createMerchandise(fields, image) else updateMerchandise(id, fields, image) }
        nav.back()
    }

    AppScreen(
        title = stringResource(if (id == null) R.string.new_product else R.string.edit_product),
        onBack = nav::back,
        actions = {
            if (id != null && loaded) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.Delete, stringResource(R.string.delete))
                }
            }
        },
    ) { padding ->
        val form: @Composable () -> Unit = {
            FormBody(
                padding = padding,
                saveLabel = stringResource(R.string.save),
                onSave = ::save,
                saving = actions.busy,
                enabled = name.isNotBlank() && (quantity.toIntOrNull() ?: 0) > 0,
            ) {
                PhotoPicker(photo ?: product?.imageUrl) { choosingPhoto = true }
                TextInput(stringResource(R.string.name), name, { name = it })
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextInput(
                        stringResource(R.string.quantity),
                        quantity,
                        { new -> quantity = new.filter(Char::isDigit).take(6) },
                        Modifier.weight(1f),
                        keyboard = KeyboardType.Number,
                    )
                    Box(Modifier.weight(1f)) {
                        if (priceLocked) {
                            TextInput(stringResource(R.string.price_per_day), price, {}, enabled = false, prefix = "$")
                        } else {
                            AmountInput(stringResource(R.string.price_per_day), price, { price = it })
                        }
                    }
                }
                if (priceLocked) {
                    Text(
                        stringResource(R.string.price_locked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextInput(stringResource(R.string.category), category, { category = it })
                val suggestions = catalog.data?.categories.orEmpty().filter { it != category }
                if (suggestions.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(suggestions) { name ->
                            SuggestionChip(onClick = { category = name }, label = { Text(name) })
                        }
                    }
                }
                TextInput(stringResource(R.string.description), description, { description = it }, singleLine = false)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextInput(stringResource(R.string.code), code, { code = it }, Modifier.weight(1f))
                    TextInput(stringResource(R.string.location), location, { location = it }, Modifier.weight(1f))
                }
                TextInput(
                    stringResource(R.string.tags),
                    tags,
                    { tags = it },
                    supporting = stringResource(R.string.tags_hint),
                )
            }
        }
        if (existing != null && !loaded) LoadBox(existing, Modifier.padding(padding)) { } else form()
    }

    if (choosingPhoto) {
        AlertDialog(
            onDismissRequest = { choosingPhoto = false },
            title = { Text(stringResource(R.string.product_photo)) },
            text = {
                Column {
                    NavRow(Icons.Default.PhotoCamera, stringResource(R.string.take_photo), onClick = {
                        choosingPhoto = false
                        cameraPermission.launch(Manifest.permission.CAMERA)
                    })
                    NavRow(Icons.Default.PhotoLibrary, stringResource(R.string.choose_from_gallery), onClick = {
                        choosingPhoto = false
                        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    })
                }
            },
            confirmButton = {
                TextButton(onClick = { choosingPhoto = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    if (confirmDelete && id != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete_product_title),
            text = stringResource(R.string.delete_product_text),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = { actions.run { call { deleteMerchandise(id) }; nav.back() } },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun PhotoPicker(model: String?, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (model.isNullOrBlank()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddAPhoto, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.add_photo), color = MaterialTheme.colorScheme.primary)
            }
        } else {
            AsyncImage(model, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

private const val MAX_PHOTO_SIDE = 1600

/** Decodes a picked photo upright, scales it down and re-encodes it as JPEG for upload. */
private suspend fun encodePhoto(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    runCatching {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val largest = maxOf(info.size.width, info.size.height)
                if (largest > MAX_PHOTO_SIDE) {
                    val scale = MAX_PHOTO_SIDE.toFloat() / largest
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_PHOTO_SIDE) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        } ?: return@runCatching null
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        }
    }.getOrNull()
}
