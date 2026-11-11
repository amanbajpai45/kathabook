package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InventoryItem
import com.example.data.repository.KiranaRepository
import com.example.ui.theme.JamaGreen
import com.example.ui.theme.KiranaPrimary
import com.example.ui.theme.KiranaSecondary
import com.example.ui.theme.UdharRed
import kotlinx.coroutines.launch

private val CATEGORIES = listOf(
    "सभी",
    "अनाज व आटा",
    "दालें",
    "तेल व घी",
    "मसाले",
    "नमकीन व बिस्कुट",
    "डेयरी व चाय",
    "अन्य सामान"
)

private val UNITS = listOf("kg", "gm", "ltr", "pkt", "pc")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    currentUserId: String,
    repository: KiranaRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var inventoryList by remember { mutableStateOf<List<InventoryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("सभी") }
    var showLowStockOnly by remember { mutableStateOf(false) }

    var itemToEdit by remember { mutableStateOf<InventoryItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<InventoryItem?>(null) }

    LaunchedEffect(currentUserId) {
        repository.observeInventory(currentUserId).collect { list ->
            inventoryList = list
            isLoading = false
        }
    }

    val lowStockCount = inventoryList.count { it.quantity <= (it.lowStockAlert ?: 5.0) }

    val filteredList = inventoryList.filter { item ->
        val matchesSearch = item.name.contains(searchQuery, ignoreCase = true) ||
                (item.hindiName?.contains(searchQuery, ignoreCase = true) == true) ||
                item.category.contains(searchQuery, ignoreCase = true)

        val matchesCategory = selectedCategory == "सभी" || item.category == selectedCategory
        val matchesLowStock = !showLowStockOnly || (item.quantity <= (item.lowStockAlert ?: 5.0))

        matchesSearch && matchesCategory && matchesLowStock
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = KiranaPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_inventory_item_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("नया सामान जोड़ें", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Stats Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("कुल सामान स्टॉक", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "${inventoryList.size} आइटम्स",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = KiranaPrimary
                            )
                        )
                    }

                    if (lowStockCount > 0) {
                        Surface(
                            onClick = { showLowStockOnly = !showLowStockOnly },
                            shape = RoundedCornerShape(12.dp),
                            color = if (showLowStockOnly) UdharRed else Color(0xFFFEF2F2),
                            border = androidx.compose.foundation.BorderStroke(1.dp, UdharRed)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (showLowStockOnly) Color.White else UdharRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$lowStockCount कम स्टॉक!",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (showLowStockOnly) Color.White else UdharRed
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("search_inventory_input"),
                placeholder = { Text("सामान का नाम या ब्रांड खोजें...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Category Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CATEGORIES) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KiranaPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = KiranaPrimary
                        )
                    )
                }
            }

            // List of Inventory Items
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KiranaPrimary)
                }
            } else if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "कोई सामान नहीं मिला" else "स्टॉक में कोई सामान नहीं है। नीचे 'नया सामान जोड़ें' बटन दबाएं।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        InventoryItemCard(
                            item = item,
                            onIncrement = {
                                scope.launch {
                                    repository.adjustStock(currentUserId, item.id, 1.0)
                                }
                            },
                            onDecrement = {
                                if (item.quantity > 0) {
                                    scope.launch {
                                        repository.adjustStock(currentUserId, item.id, -1.0)
                                    }
                                }
                            },
                            onEdit = { itemToEdit = item },
                            onDelete = { itemToDelete = item }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog || itemToEdit != null) {
        AddOrEditItemDialog(
            currentUserId = currentUserId,
            existingItem = itemToEdit,
            onDismiss = {
                showAddDialog = false
                itemToEdit = null
            },
            onSave = { savedItem ->
                scope.launch {
                    val result = if (itemToEdit != null) {
                        repository.updateInventoryItem(savedItem)
                    } else {
                        repository.addInventoryItem(savedItem)
                    }
                    if (result.isSuccess) {
                        Toast.makeText(context, "${savedItem.name} सेव हो गया!", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                        itemToEdit = null
                    } else {
                        Toast.makeText(context, "सामान सेव करने में विफल", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Delete Confirmation
    if (itemToDelete != null) {
        val item = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("सामान हटाएं?") },
            text = { Text("क्या आप सचमुच '${item.name}' को इन्वेंटरी से हटाना चाहते हैं?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.deleteInventoryItem(currentUserId, item.id)
                            itemToDelete = null
                        }
                    }
                ) {
                    Text("हटाएं", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
private fun InventoryItemCard(
    item: InventoryItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isLowStock = item.quantity <= (item.lowStockAlert ?: 5.0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_item_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!item.hindiName.isNullOrBlank()) {
                        Text(
                            text = item.hindiName,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = KiranaPrimary.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = KiranaPrimary,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (item.isAvailableForOnlineOrder) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = JamaGreen.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = JamaGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "ऑनलाइन आर्डर",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = JamaGreen,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Edit / Delete icons
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Price & Stock Stepper Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Price info
                Column {
                    Text(
                        text = "₹${String.format("%,.0f", item.sellingPrice)} / ${item.unit}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = KiranaPrimary
                        )
                    )
                    if (item.costPrice != null && item.costPrice > 0) {
                        val margin = item.sellingPrice - item.costPrice
                        Text(
                            text = "लागत: ₹${String.format("%.0f", item.costPrice)} (मुनाफा: ₹${String.format("%.0f", margin)})",
                            style = MaterialTheme.typography.labelSmall.copy(color = JamaGreen)
                        )
                    }
                }

                // Quick Stock Adjustment (+ / -)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = onDecrement,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${if (item.quantity % 1.0 == 0.0) item.quantity.toLong().toString() else item.quantity} ${item.unit}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isLowStock) UdharRed else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        if (isLowStock) {
                            Text("स्टॉक कम!", fontSize = 11.sp, color = UdharRed, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        onClick = onIncrement,
                        shape = CircleShape,
                        color = KiranaPrimary,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddOrEditItemDialog(
    currentUserId: String,
    existingItem: InventoryItem?,
    onDismiss: () -> Unit,
    onSave: (InventoryItem) -> Unit
) {
    var name by remember { mutableStateOf(existingItem?.name ?: "") }
    var hindiName by remember { mutableStateOf(existingItem?.hindiName ?: "") }
    var category by remember { mutableStateOf(existingItem?.category ?: "अनाज व आटा") }
    var quantityText by remember { mutableStateOf(existingItem?.quantity?.toString() ?: "10") }
    var unit by remember { mutableStateOf(existingItem?.unit ?: "kg") }
    var costPriceText by remember { mutableStateOf(existingItem?.costPrice?.toString() ?: "") }
    var sellingPriceText by remember { mutableStateOf(existingItem?.sellingPrice?.toString() ?: "") }
    var lowStockAlertText by remember { mutableStateOf(existingItem?.lowStockAlert?.toString() ?: "5") }
    var isAvailableForOnlineOrder by remember { mutableStateOf(existingItem?.isAvailableForOnlineOrder ?: true) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingItem != null) "सामान एडिट करें" else "नया सामान जोड़ें", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("सामान का नाम (English) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("item_name_input")
                )

                OutlinedTextField(
                    value = hindiName,
                    onValueChange = { hindiName = it },
                    label = { Text("हिंदी नाम (उदा. आशीर्वाद आटा)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("कैटेगरी *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        CATEGORIES.filter { it != "सभी" }.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Quantity and Unit Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("स्टॉक मात्रा *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.3f)
                    )

                    ExposedDropdownMenuBox(
                        expanded = unitDropdownExpanded,
                        onExpandedChange = { unitDropdownExpanded = !unitDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("इकाई") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = unitDropdownExpanded,
                            onDismissRequest = { unitDropdownExpanded = false }
                        ) {
                            UNITS.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unit = u
                                        unitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Prices Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sellingPriceText,
                        onValueChange = { sellingPriceText = it },
                        label = { Text("बिक्री मूल्य (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("item_selling_price_input")
                    )
                    OutlinedTextField(
                        value = costPriceText,
                        onValueChange = { costPriceText = it },
                        label = { Text("खरीद लागत (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = lowStockAlertText,
                    onValueChange = { lowStockAlertText = it },
                    label = { Text("कम स्टॉक चेतावनी सीमा (थ्रेशोल्ड)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Online Order Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAvailableForOnlineOrder = !isAvailableForOnlineOrder }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isAvailableForOnlineOrder,
                        onCheckedChange = { isAvailableForOnlineOrder = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ग्राहक घर बैठे आर्डर कर सकते हैं", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityText.toDoubleOrNull() ?: 0.0
                    val sellingPrice = sellingPriceText.toDoubleOrNull() ?: 0.0
                    val costPrice = costPriceText.toDoubleOrNull()
                    val alert = lowStockAlertText.toDoubleOrNull() ?: 5.0

                    if (name.isNotBlank() && sellingPrice > 0) {
                        onSave(
                            InventoryItem(
                                id = existingItem?.id ?: "",
                                userId = currentUserId,
                                name = name.trim(),
                                hindiName = hindiName.trim().takeIf { it.isNotBlank() },
                                category = category,
                                quantity = qty,
                                unit = unit,
                                costPrice = costPrice,
                                sellingPrice = sellingPrice,
                                lowStockAlert = alert,
                                isAvailableForOnlineOrder = isAvailableForOnlineOrder
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && (sellingPriceText.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = KiranaPrimary)
            ) {
                Text("सुरक्षित करें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}
