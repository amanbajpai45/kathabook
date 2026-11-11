package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.model.InventoryItem
import com.example.data.model.StoreOrder
import com.example.data.model.Transaction
import com.example.data.repository.KiranaRepository
import com.example.ui.theme.JamaGreen
import com.example.ui.theme.KiranaPrimary
import com.example.ui.theme.KiranaSecondary
import com.example.ui.theme.UdharRed
import com.example.util.CommunicationUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    currentUserId: String,
    repository: KiranaRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Store Orders, 1: Ghar Baithe Order (Customer Mode)
    var ordersList by remember { mutableStateOf<List<StoreOrder>>(emptyList()) }
    var inventoryList by remember { mutableStateOf<List<InventoryItem>>(emptyList()) }
    var customersList by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Live Snapshot Observation of Orders and Inventory
    LaunchedEffect(currentUserId) {
        launch {
            repository.observeOrders(currentUserId).collect { orders ->
                ordersList = orders
                isLoading = false
            }
        }
        launch {
            repository.observeInventory(currentUserId).collect { items ->
                inventoryList = items
            }
        }
        launch {
            repository.observeCustomers(currentUserId).collect { custs ->
                customersList = custs
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = KiranaPrimary
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("दुकान ऑर्डर्स (${ordersList.size})", fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("घर से आर्डर करें", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        if (selectedTabIndex == 0) {
            StoreOrdersManagerView(
                currentUserId = currentUserId,
                orders = ordersList,
                customers = customersList,
                isLoading = isLoading,
                onUpdateStatus = { orderId, newStatus, riderNote ->
                    scope.launch {
                        repository.updateOrderStatus(currentUserId, orderId, newStatus, riderNote)
                        Toast.makeText(context, "आर्डर स्टेटस अपडेट हो गया!", Toast.LENGTH_SHORT).show()
                    }
                },
                onAddOrderToKhata = { order, targetCustomer ->
                    scope.launch {
                        val tx = Transaction(
                            userId = currentUserId,
                            customerId = targetCustomer.id,
                            customerName = targetCustomer.name,
                            type = "UDHAR_DIYA",
                            amount = order.totalAmount,
                            description = "ऑनलाइन आर्डर: ${order.itemsSummary}",
                            paymentMode = "Udhar Order"
                        )
                        val res = repository.addTransactionAndUpdateKhata(tx, targetCustomer)
                        if (res.isSuccess) {
                            Toast.makeText(context, "${targetCustomer.name} के खाते में उधार जुड़ गया!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "खाते में जोड़ने में विफल", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        } else {
            CustomerOrderPortalView(
                currentUserId = currentUserId,
                inventoryItems = inventoryList.filter { it.isAvailableForOnlineOrder },
                orders = ordersList,
                onPlaceOrder = { newOrder ->
                    scope.launch {
                        val res = repository.placeOrder(newOrder)
                        if (res.isSuccess) {
                            Toast.makeText(context, "आर्डर सफलतापूर्वक प्लेस हो गया!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "आर्डर प्लेस करने में विफल", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}

// ----------------- STORE ORDERS TAB -----------------

@Composable
private fun StoreOrdersManagerView(
    currentUserId: String,
    orders: List<StoreOrder>,
    customers: List<Customer>,
    isLoading: Boolean,
    onUpdateStatus: (String, String, String?) -> Unit,
    onAddOrderToKhata: (StoreOrder, Customer) -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("ALL") }
    var orderForKhataDialog by remember { mutableStateOf<StoreOrder?>(null) }

    val filteredOrders = orders.filter { order ->
        when (selectedFilter) {
            "ACTIVE" -> order.status != "DELIVERED" && order.status != "CANCELLED"
            "PLACED" -> order.status == "PLACED"
            "DELIVERED" -> order.status == "DELIVERED"
            else -> true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Status Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("सभी (${orders.size})") }
            )
            FilterChip(
                selected = selectedFilter == "ACTIVE",
                onClick = { selectedFilter = "ACTIVE" },
                label = { Text("चालू (${orders.count { it.status != "DELIVERED" && it.status != "CANCELLED" }})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = KiranaSecondary.copy(alpha = 0.2f),
                    selectedLabelColor = KiranaSecondary
                )
            )
            FilterChip(
                selected = selectedFilter == "DELIVERED",
                onClick = { selectedFilter = "DELIVERED" },
                label = { Text("डिलीवर हुए (${orders.count { it.status == "DELIVERED" }})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = JamaGreen.copy(alpha = 0.15f),
                    selectedLabelColor = JamaGreen
                )
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KiranaPrimary)
            }
        } else if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "अभी कोई आर्डर नहीं है।\n'घर से आर्डर करें' टैब से टेस्ट आर्डर प्लेस करके लाइव ट्रैकिंग देखें!",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredOrders, key = { it.id }) { order ->
                    StoreOrderCard(
                        order = order,
                        onCall = { CommunicationUtil.launchCall(context, order.customerPhone) },
                        onNextStatus = { newStatus -> onUpdateStatus(order.id, newStatus, null) },
                        onAddToKhata = { orderForKhataDialog = order }
                    )
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (orderForKhataDialog != null) {
        val order = orderForKhataDialog!!
        SelectCustomerForKhataDialog(
            order = order,
            customers = customers,
            onDismiss = { orderForKhataDialog = null },
            onSelect = { cust ->
                onAddOrderToKhata(order, cust)
                orderForKhataDialog = null
            }
        )
    }
}

@Composable
private fun StoreOrderCard(
    order: StoreOrder,
    onCall: () -> Unit,
    onNextStatus: (String) -> Unit,
    onAddToKhata: () -> Unit
) {
    val statusColor = when (order.status) {
        "PLACED" -> Color(0xFFD97706)
        "ACCEPTED" -> Color(0xFF2563EB)
        "PACKING" -> Color(0xFF7C3AED)
        "OUT_FOR_DELIVERY" -> Color(0xFFEA580C)
        "DELIVERED" -> JamaGreen
        else -> Color.Gray
    }

    val statusHindi = when (order.status) {
        "PLACED" -> "नया आर्डर प्राप्त"
        "ACCEPTED" -> "दुकानदार द्वारा स्वीकृत"
        "PACKING" -> "सामान पैक हो रहा है"
        "OUT_FOR_DELIVERY" -> "डिलीवरी के लिए रवाना"
        "DELIVERED" -> "डिलीवर हो गया"
        else -> "रद्द"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("store_order_${order.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Customer & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = order.customerName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = order.customerPhone,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = statusHindi,
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Delivery Address
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = order.deliveryAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Items Summary
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "सामान सूची:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = order.itemsSummary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Total Amount & Payment Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "बिल: ₹${String.format("%,.0f", order.totalAmount)}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = KiranaPrimary
                        )
                    )
                    Text(
                        text = "पेमेंट: ${if (order.paymentMethod == "UDHAR") "उधार (खाते में)" else if (order.paymentMethod == "COD") "Cash on Delivery" else "ऑनलाइन UPI"}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (order.paymentMethod == "UDHAR") UdharRed else JamaGreen
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF2563EB))
                    }

                    if (order.paymentMethod == "UDHAR" && order.status == "DELIVERED") {
                        Button(
                            onClick = onAddToKhata,
                            colors = ButtonDefaults.buttonColors(containerColor = UdharRed),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("खाते में जोड़ें", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Real-time Action Workflow Buttons
            if (order.status != "DELIVERED" && order.status != "CANCELLED") {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    when (order.status) {
                        "PLACED" -> {
                            Button(
                                onClick = { onNextStatus("ACCEPTED") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("आर्डर स्वीकार करें")
                            }
                        }
                        "ACCEPTED" -> {
                            Button(
                                onClick = { onNextStatus("PACKING") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("पैकिंग शुरू करें")
                            }
                        }
                        "PACKING" -> {
                            Button(
                                onClick = { onNextStatus("OUT_FOR_DELIVERY") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("डिलीवरी के लिए भेजें")
                            }
                        }
                        "OUT_FOR_DELIVERY" -> {
                            Button(
                                onClick = { onNextStatus("DELIVERED") },
                                colors = ButtonDefaults.buttonColors(containerColor = JamaGreen),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("डिलीवर हो गया")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------- CUSTOMER ORDER PORTAL & LIVE TRACKING TAB -----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerOrderPortalView(
    currentUserId: String,
    inventoryItems: List<InventoryItem>,
    orders: List<StoreOrder>,
    onPlaceOrder: (StoreOrder) -> Unit
) {
    var cartItems = remember { mutableStateMapOf<String, Int>() } // itemId -> quantity
    var showCheckoutSheet by remember { mutableStateOf(false) }

    val totalCartAmount = cartItems.entries.sumOf { (itemId, qty) ->
        val item = inventoryItems.find { it.id == itemId }
        (item?.sellingPrice ?: 0.0) * qty
    }

    val totalItemsCount = cartItems.values.sum()

    // Recent orders placed to track live status
    val activeOrders = orders.filter { it.status != "DELIVERED" && it.status != "CANCELLED" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Real-Time Order Tracking Timeline Banner (if any)
        if (activeOrders.isNotEmpty()) {
            item {
                Text(
                    text = "लाइव आर्डर ट्रैकिंग (Real-time Status)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                activeOrders.forEach { activeOrder ->
                    OrderTrackingTimelineCard(order = activeOrder)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "किराना स्टोर कैटलॉग (घर बैठे आर्डर करें)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (totalItemsCount > 0) {
                    Surface(
                        onClick = { showCheckoutSheet = true },
                        shape = RoundedCornerShape(20.dp),
                        color = KiranaPrimary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("$totalItemsCount सामान (₹${String.format("%.0f", totalCartAmount)})", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (inventoryItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "अभी कोई सामान ऑनलाइन आर्डर के लिए उपलब्ध नहीं है। इन्वेंटरी में सामान जोड़ते समय 'घर बैठे आर्डर के लिए उपलब्ध रखें' चालू करें।",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            items(inventoryItems, key = { it.id }) { item ->
                val quantityInCart = cartItems[item.id] ?: 0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            if (!item.hindiName.isNullOrBlank()) {
                                Text(text = item.hindiName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Text(
                                text = "₹${String.format("%,.0f", item.sellingPrice)} / ${item.unit}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = KiranaPrimary)
                            )
                        }

                        // Add to cart buttons
                        if (quantityInCart == 0) {
                            Button(
                                onClick = { cartItems[item.id] = 1 },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = KiranaPrimary)
                            ) {
                                Text("जोड़ें (+)")
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    onClick = {
                                        val newQty = quantityInCart - 1
                                        if (newQty <= 0) cartItems.remove(item.id) else cartItems[item.id] = newQty
                                    },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Minus", modifier = Modifier.size(16.dp))
                                    }
                                }

                                Text("$quantityInCart", fontWeight = FontWeight.Bold)

                                Surface(
                                    onClick = { cartItems[item.id] = quantityInCart + 1 },
                                    shape = CircleShape,
                                    color = KiranaPrimary,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = "Plus", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    // Checkout Bottom Sheet
    if (showCheckoutSheet) {
        CheckoutBottomSheet(
            currentUserId = currentUserId,
            cartItems = cartItems,
            inventoryItems = inventoryItems,
            totalAmount = totalCartAmount,
            onDismiss = { showCheckoutSheet = false },
            onConfirmOrder = { order ->
                onPlaceOrder(order)
                cartItems.clear()
                showCheckoutSheet = false
            }
        )
    }
}

@Composable
private fun OrderTrackingTimelineCard(order: StoreOrder) {
    val statuses = listOf(
        "PLACED" to "आर्डर प्राप्त",
        "ACCEPTED" to "स्वीकार हुआ",
        "PACKING" to "पैकिंग हो रही है",
        "OUT_FOR_DELIVERY" to "डिलीवरी रवाना",
        "DELIVERED" to "डिलीवर हो गया"
    )

    val currentStatusIndex = when (order.status) {
        "PLACED" -> 0
        "ACCEPTED" -> 1
        "PACKING" -> 2
        "OUT_FOR_DELIVERY" -> 3
        "DELIVERED" -> 4
        else -> 0
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "आर्डर #${order.id.takeLast(6).uppercase()}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "₹${String.format("%,.0f", order.totalAmount)} (${order.paymentMethod})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = KiranaPrimary)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Step Progress Timeline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                statuses.forEachIndexed { index, (_, label) ->
                    val isDone = index <= currentStatusIndex
                    val isCurrent = index == currentStatusIndex

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDone) (if (isCurrent) KiranaSecondary else JamaGreen) else Color.LightGray
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text("${index + 1}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) KiranaSecondary else Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckoutBottomSheet(
    currentUserId: String,
    cartItems: Map<String, Int>,
    inventoryItems: List<InventoryItem>,
    totalAmount: Double,
    onDismiss: () -> Unit,
    onConfirmOrder: (StoreOrder) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("COD") } // "COD", "UDHAR", "UPI"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "घर पर डिलीवरी आर्डर डिटेल",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            // Items Summary text
            val summaryString = cartItems.entries.mapNotNull { (itemId, qty) ->
                inventoryItems.find { it.id == itemId }?.let { "${it.name} (${qty} ${it.unit})" }
            }.joinToString(", ")

            Text("सामान: $summaryString", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

            Text(
                text = "कुल बिल: ₹${String.format("%,.0f", totalAmount)}",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = KiranaPrimary)
            )

            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text("आपका नाम *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("checkout_name_input")
            )

            OutlinedTextField(
                value = customerPhone,
                onValueChange = { customerPhone = it },
                label = { Text("मोबाइल नंबर *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("checkout_phone_input")
            )

            OutlinedTextField(
                value = deliveryAddress,
                onValueChange = { deliveryAddress = it },
                label = { Text("डिलीवरी का पूरा पता *") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth().testTag("checkout_address_input")
            )

            Text("भुगतान का तरीका (Payment Option):", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "COD" to "Cash On Delivery",
                    "UDHAR" to "खाते में उधार",
                    "UPI" to "UPI"
                ).forEach { (code, label) ->
                    FilterChip(
                        selected = paymentMethod == code,
                        onClick = { paymentMethod = code },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (customerName.isNotBlank() && customerPhone.isNotBlank() && deliveryAddress.isNotBlank()) {
                        onConfirmOrder(
                            StoreOrder(
                                userId = currentUserId,
                                customerName = customerName.trim(),
                                customerPhone = customerPhone.trim(),
                                deliveryAddress = deliveryAddress.trim(),
                                totalAmount = totalAmount,
                                paymentMethod = paymentMethod,
                                status = "PLACED",
                                itemsSummary = summaryString
                            )
                        )
                    }
                },
                enabled = customerName.isNotBlank() && customerPhone.isNotBlank() && deliveryAddress.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_order_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KiranaPrimary)
            ) {
                Text("आर्डर कन्फर्म करें", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SelectCustomerForKhataDialog(
    order: StoreOrder,
    customers: List<Customer>,
    onDismiss: () -> Unit,
    onSelect: (Customer) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("खाता चुनें") },
        text = {
            Column {
                Text("यह ₹${String.format("%.0f", order.totalAmount)} का आर्डर किस ग्राहक के उधार खाते में जोड़ना है?")
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(customers) { cust ->
                        Surface(
                            onClick = { onSelect(cust) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cust.name, fontWeight = FontWeight.Bold)
                                Text("बकाया: ₹${String.format("%.0f", cust.totalUdhar)}", color = UdharRed)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("रद्द करें") }
        }
    )
}
