package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.data.model.Customer
import com.example.data.model.Transaction
import com.example.data.repository.KiranaRepository
import com.example.ui.theme.JamaGreen
import com.example.ui.theme.JamaGreenLight
import com.example.ui.theme.KiranaPrimary
import com.example.ui.theme.KiranaSecondary
import com.example.ui.theme.UdharRed
import com.example.ui.theme.UdharRedLight
import com.example.util.CommunicationUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataScreen(
    currentUserId: String,
    repository: KiranaRepository,
    onNavigateToAiReminder: (Customer) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "HAS_UDHAR", "SETTLED"

    var selectedCustomerForLedger by remember { mutableStateOf<Customer?>(null) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    // Real-time observation of customers
    LaunchedEffect(currentUserId) {
        repository.observeCustomers(currentUserId).collect { list ->
            customers = list
            isLoading = false
            // Update active ledger customer if updated
            selectedCustomerForLedger?.let { active ->
                customers.find { it.id == active.id }?.let { updated ->
                    selectedCustomerForLedger = updated
                }
            }
        }
    }

    if (selectedCustomerForLedger != null) {
        CustomerLedgerView(
            customer = selectedCustomerForLedger!!,
            currentUserId = currentUserId,
            repository = repository,
            onBack = { selectedCustomerForLedger = null },
            onSendAiReminder = { onNavigateToAiReminder(selectedCustomerForLedger!!) }
        )
        return
    }

    val totalUdharSum = customers.sumOf { it.totalUdhar }
    val filteredCustomers = customers.filter { cust ->
        val matchesSearch = cust.name.contains(searchQuery, ignoreCase = true) ||
                cust.phone.contains(searchQuery) ||
                (cust.address?.contains(searchQuery, ignoreCase = true) == true)

        val matchesFilter = when (selectedFilter) {
            "HAS_UDHAR" -> cust.totalUdhar > 0
            "SETTLED" -> cust.totalUdhar <= 0
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCustomerDialog = true },
                containerColor = KiranaPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Customer")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("नया ग्राहक जोड़ें", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Stats Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "दुकान कुल उधार बहीखाता",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "₹${String.format("%,.0f", totalUdharSum)}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalUdharSum > 0) UdharRed else JamaGreen
                                )
                            )
                            Text(
                                text = "मार्केट में कुल बकाया राशि",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(KiranaPrimary.copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${customers.size} कुल ग्राहक",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = KiranaPrimary
                                )
                            )
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
                    .testTag("search_customer_input"),
                placeholder = { Text("ग्राहक का नाम या फोन नंबर खोजें...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
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

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("सभी (${customers.size})") }
                )
                FilterChip(
                    selected = selectedFilter == "HAS_UDHAR",
                    onClick = { selectedFilter = "HAS_UDHAR" },
                    label = { Text("बकाया वाले (${customers.count { it.totalUdhar > 0 }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = UdharRedLight,
                        selectedLabelColor = UdharRed
                    )
                )
                FilterChip(
                    selected = selectedFilter == "SETTLED",
                    onClick = { selectedFilter = "SETTLED" },
                    label = { Text("हिसाब चुकता") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JamaGreenLight,
                        selectedLabelColor = JamaGreen
                    )
                )
            }

            // Customer List
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = KiranaPrimary)
                }
            } else if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "कोई ग्राहक नहीं मिला। नया ग्राहक जोड़ें!" else "खोजे गए नाम से कोई ग्राहक नहीं मिला।",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        CustomerCard(
                            customer = customer,
                            onClick = { selectedCustomerForLedger = customer },
                            onCall = { CommunicationUtil.launchCall(context, customer.phone) },
                            onWhatsApp = {
                                CommunicationUtil.launchWhatsApp(
                                    context,
                                    customer.phone,
                                    "नमस्ते ${customer.name} जी, आपके किराना खाते का बकाया ₹${String.format("%.0f", customer.totalUdhar)} है। कृपया सुविधा अनुसार जमा करवाएं। धन्यवाद!"
                                )
                            },
                            onAiReminder = { onNavigateToAiReminder(customer) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // Add Customer Dialog
    if (showAddCustomerDialog) {
        AddCustomerDialog(
            currentUserId = currentUserId,
            onDismiss = { showAddCustomerDialog = false },
            onAdd = { newCustomer ->
                scope.launch {
                    val result = repository.addCustomer(newCustomer)
                    if (result.isSuccess) {
                        Toast.makeText(context, "${newCustomer.name} का खाता बन गया!", Toast.LENGTH_SHORT).show()
                        showAddCustomerDialog = false
                    } else {
                        Toast.makeText(context, "ग्राहक जोड़ने में विफल", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun CustomerCard(
    customer: Customer,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onAiReminder: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("customer_card_${customer.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(KiranaPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = customer.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = KiranaPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = customer.phone,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                if (!customer.address.isNullOrBlank()) {
                    Text(
                        text = customer.address,
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Balance & Quick Actions
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format("%,.0f", customer.totalUdhar)}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (customer.totalUdhar > 0) UdharRed else JamaGreen
                    )
                )
                Text(
                    text = if (customer.totalUdhar > 0) "बकाया बाकी" else "चुकता",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (customer.totalUdhar > 0) UdharRed else JamaGreen,
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call Customer",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onWhatsApp,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "WhatsApp reminder",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (customer.totalUdhar > 0) {
                        IconButton(
                            onClick = onAiReminder,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Reminder",
                                tint = KiranaSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerLedgerView(
    customer: Customer,
    currentUserId: String,
    repository: KiranaRepository,
    onBack: () -> Unit,
    onSendAiReminder: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var isLoadingTransactions by remember { mutableStateOf(true) }

    var showAddTxDialog by remember { mutableStateOf<String?>(null) } // "UDHAR_DIYA" or "JAMA_KIYA"
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(customer.id) {
        repository.observeTransactions(currentUserId, customer.id).collect { list ->
            transactions = list
            isLoadingTransactions = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(customer.name, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(customer.phone, style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { CommunicationUtil.launchCall(context, customer.phone) }) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF2563EB))
                    }
                    IconButton(onClick = onSendAiReminder) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Reminder", tint = KiranaSecondary)
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete customer", tint = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Action Bar for Udhar Diya & Jama Liya
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showAddTxDialog = "UDHAR_DIYA" },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("udhar_diya_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = UdharRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.MoneyOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("उधार दिया (-)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showAddTxDialog = "JAMA_KIYA" },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("jama_kiya_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = JamaGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("जमा लिया (+)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Balance Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (customer.totalUdhar > 0) UdharRedLight else JamaGreenLight
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "कुल बकाया हिसाब",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (customer.totalUdhar > 0) UdharRed else JamaGreen
                            )
                        )
                        Text(
                            text = "₹${String.format("%,.0f", customer.totalUdhar)}",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (customer.totalUdhar > 0) UdharRed else JamaGreen
                            )
                        )
                    }
                    if (customer.totalUdhar > 0) {
                        Button(
                            onClick = onSendAiReminder,
                            colors = ButtonDefaults.buttonColors(containerColor = KiranaSecondary),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI तकादा", fontSize = 13.sp)
                        }
                    }
                }
            }

            Text(
                text = "लेन-देन का इतिहास (बहीखाता)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            if (isLoadingTransactions) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KiranaPrimary)
                }
            } else if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "अभी कोई लेन-देन दर्ज नहीं है।\nनीचे 'उधार दिया' या 'जमा लिया' बटन दबाकर एंट्री करें।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        TransactionRow(transaction = tx)
                    }
                }
            }
        }
    }

    // Add Transaction Dialog
    if (showAddTxDialog != null) {
        val type = showAddTxDialog!!
        AddTransactionDialog(
            type = type,
            customer = customer,
            onDismiss = { showAddTxDialog = null },
            onSave = { amount, description, mode ->
                scope.launch {
                    val tx = Transaction(
                        userId = currentUserId,
                        customerId = customer.id,
                        customerName = customer.name,
                        type = type,
                        amount = amount,
                        description = description,
                        paymentMode = mode
                    )
                    val result = repository.addTransactionAndUpdateKhata(tx, customer)
                    if (result.isSuccess) {
                        Toast.makeText(context, "खाता अपडेट हो गया!", Toast.LENGTH_SHORT).show()
                        showAddTxDialog = null
                    } else {
                        Toast.makeText(context, "एंट्री जोड़ने में विफल", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Delete Customer Confirmation
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("ग्राहक हटाएं?") },
            text = { Text("क्या आप सचमुच ${customer.name} का पूरा खाता हटाना चाहते हैं?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.deleteCustomer(currentUserId, customer.id)
                            showDeleteConfirmDialog = false
                            onBack()
                        }
                    }
                ) {
                    Text("हटाएं", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
private fun TransactionRow(transaction: Transaction) {
    val isUdhar = transaction.type == "UDHAR_DIYA"
    val formattedDate = remember(transaction.createdAt) {
        transaction.createdAt?.toDate()?.let {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(it)
        } ?: "हाल ही में"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isUdhar) "सामान उधार दिया" else "पैसे जमा लिए (${transaction.paymentMode ?: "Cash"})",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isUdhar) UdharRed else JamaGreen
                    )
                )
                if (!transaction.description.isNullOrBlank()) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
            }

            Text(
                text = "${if (isUdhar) "+" else "-"} ₹${String.format("%,.0f", transaction.amount)}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isUdhar) UdharRed else JamaGreen
                )
            )
        }
    }
}

@Composable
private fun AddCustomerDialog(
    currentUserId: String,
    onDismiss: () -> Unit,
    onAdd: (Customer) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var initialUdhar by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("नया ग्राहक जोड़ें", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ग्राहक का नाम *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_customer_name")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("मोबाइल नंबर *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_customer_phone")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("पता / मोहल्ला (वैकल्पिक)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = initialUdhar,
                    onValueChange = { initialUdhar = it },
                    label = { Text("पुराना बकाया उधार (₹) (यदि कोई हो)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("टिप्पणी / नोट (वैकल्पिक)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        val udhar = initialUdhar.toDoubleOrNull() ?: 0.0
                        onAdd(
                            Customer(
                                userId = currentUserId,
                                name = name.trim(),
                                phone = phone.trim(),
                                address = address.takeIf { it.isNotBlank() },
                                totalUdhar = udhar,
                                notes = notes.takeIf { it.isNotBlank() }
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && phone.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = KiranaPrimary)
            ) {
                Text("खाता बनाएं")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

@Composable
private fun AddTransactionDialog(
    type: String, // "UDHAR_DIYA" or "JAMA_KIYA"
    customer: Customer,
    onDismiss: () -> Unit,
    onSave: (Double, String, String) -> Unit
) {
    val isUdhar = type == "UDHAR_DIYA"
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(if (isUdhar) "खतौनी" else "Cash") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isUdhar) "सामान उधार दिया (-)" else "पैसे जमा लिए (+)",
                fontWeight = FontWeight.Bold,
                color = if (isUdhar) UdharRed else JamaGreen
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("ग्राहक: ${customer.name}", fontWeight = FontWeight.SemiBold)

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("रकम (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("transaction_amount_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (isUdhar) "सामान की डिटेल (उदा. 5kg आटा, 1L तेल)" else "विवरण / रसीद नोट") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isUdhar) {
                    Text("भुगतान का तरीका:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Cash", "UPI / PhonePe", "GPay", "Paytm").forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode },
                                label = { Text(mode, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        onSave(amount, description, selectedMode)
                    }
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isUdhar) UdharRed else JamaGreen
                )
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
