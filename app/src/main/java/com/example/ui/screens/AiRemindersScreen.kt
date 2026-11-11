package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.repository.AiReminderRepository
import com.example.data.repository.KiranaRepository
import com.example.ui.theme.KiranaPrimary
import com.example.ui.theme.KiranaSecondary
import com.example.ui.theme.UdharRed
import com.example.util.CommunicationUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRemindersScreen(
    currentUserId: String,
    kiranaRepository: KiranaRepository,
    aiReminderRepository: AiReminderRepository,
    preselectedCustomer: Customer? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(preselectedCustomer) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Tones: POLITE, WEEKEND, FIRM, DISCOUNT
    var selectedTone by remember { mutableStateOf("WEEKEND") }
    var storeName by remember { mutableStateOf("किराना स्टोर") }

    var generatedMessage by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    var aiAdviceText by remember { mutableStateOf<String?>(null) }
    var isGeneratingAdvice by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserId) {
        kiranaRepository.observeCustomers(currentUserId).collect { list ->
            customers = list
            if (selectedCustomer == null && list.isNotEmpty()) {
                // Preselect first customer with pending udhar
                selectedCustomer = list.find { it.totalUdhar > 0 } ?: list.first()
            }
        }
    }

    LaunchedEffect(preselectedCustomer) {
        if (preselectedCustomer != null) {
            selectedCustomer = preselectedCustomer
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = KiranaPrimary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Reminders",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "AI वीकेंड पेमेंट तकादा",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "ग्राहक के लिए स्वचालित, विनम्र व असरदार कॉल/SMS रिमाइंडर",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    )
                }
            }
        }

        // Customer Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "1. ग्राहक चुनें जिसका तकादा करना है",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(10.dp))

                val pendingCustomers = customers.filter { it.totalUdhar > 0 }

                if (pendingCustomers.isEmpty()) {
                    Text(
                        text = "अभी किसी ग्राहक पर उधार बकाया नहीं है। जब किसी ग्राहक पर उधार होगा, तो वे यहाँ दिखाई देंगे।",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCustomer?.let { "${it.name} (बकाया: ₹${String.format("%.0f", it.totalUdhar)})" } ?: "ग्राहक चुनें",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("select_customer_dropdown"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            pendingCustomers.forEach { cust ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(cust.name, fontWeight = FontWeight.Bold)
                                            Text(
                                                "फोन: ${cust.phone} | बाकी: ₹${String.format("%.0f", cust.totalUdhar)}",
                                                style = MaterialTheme.typography.bodySmall.copy(color = UdharRed)
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCustomer = cust
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tone & Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "2. रिमाइंडर का अंदाज (Tone) चुनें",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedTone == "WEEKEND",
                            onClick = { selectedTone = "WEEKEND" },
                            label = { Text("📅 वीकेंड तकादा (स्टॉक मंगवाना है)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedTone == "POLITE",
                            onClick = { selectedTone = "POLITE" },
                            label = { Text("🕊️ विनम्र याददाश्त (Polite)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedTone == "FIRM",
                            onClick = { selectedTone = "FIRM" },
                            label = { Text("⚡ अति आवश्यक (Urgent)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FilterChip(
                        selected = selectedTone == "DISCOUNT",
                        onClick = { selectedTone = "DISCOUNT" },
                        label = { Text("🎁 तुरंत हिसाब चुकता विशेष आभार / ऑफर") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("आपकी दुकान का नाम") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Generate Button
                Button(
                    onClick = {
                        val cust = selectedCustomer
                        if (cust != null) {
                            isGenerating = true
                            scope.launch {
                                val result = aiReminderRepository.generatePaymentReminder(
                                    customerName = cust.name,
                                    amount = cust.totalUdhar,
                                    tone = selectedTone,
                                    storeName = storeName.ifBlank { "किराना स्टोर" }
                                )
                                isGenerating = false
                                if (result.isSuccess) {
                                    generatedMessage = result.getOrThrow()
                                } else {
                                    Toast.makeText(
                                        context,
                                        result.exceptionOrNull()?.localizedMessage ?: "AI जनरेट करने में त्रुटि",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "कृपया पहले ग्राहक चुनें", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isGenerating && selectedCustomer != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("ai_generate_reminder_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KiranaSecondary)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("AI रिमाइंडर तैयार कर रहा है...", color = Color.White)
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI से तकादा मैसेज बनाएं", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Generated Reminder & Action Buttons
        if (generatedMessage.isNotEmpty()) {
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
                            text = "3. तैयार AI मैसेज (आप बदलाव भी कर सकते हैं):",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Kirana Reminder", generatedMessage)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "मैसेज कॉपी हो गया!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = generatedMessage,
                        onValueChange = { generatedMessage = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generated_reminder_text"),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 4,
                        maxLines = 8
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Direct Action Buttons: Call, SMS, WhatsApp
                    Text(
                        text = "तुरंत भेजें:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // WhatsApp
                        Button(
                            onClick = {
                                selectedCustomer?.let { cust ->
                                    CommunicationUtil.launchWhatsApp(context, cust.phone, generatedMessage)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("whatsapp_send_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // SMS
                        Button(
                            onClick = {
                                selectedCustomer?.let { cust ->
                                    CommunicationUtil.launchSms(context, cust.phone, generatedMessage)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("sms_send_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SMS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Direct Call
                        Button(
                            onClick = {
                                selectedCustomer?.let { cust ->
                                    CommunicationUtil.launchCall(context, cust.phone)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("direct_call_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("कॉल करें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Smart Kirana Business Advisor
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = KiranaSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI किराना व्यापार व उधारी सलाहकार",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "AI आपके दुकान के आंकड़े देखकर वीकेंड पर उधारी रिकवर करने व माल भरने के खास सुझाव देगा।",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (aiAdviceText != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = aiAdviceText!!,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedButton(
                    onClick = {
                        isGeneratingAdvice = true
                        scope.launch {
                            val totalUdhar = customers.sumOf { it.totalUdhar }
                            val pendingCount = customers.count { it.totalUdhar > 0 }
                            val result = aiReminderRepository.generateShopFinancialAdvice(
                                totalUdhar = totalUdhar,
                                pendingCustomerCount = pendingCount,
                                totalCustomers = customers.size,
                                inventoryCount = 0
                            )
                            isGeneratingAdvice = false
                            if (result.isSuccess) {
                                aiAdviceText = result.getOrThrow()
                            } else {
                                Toast.makeText(context, "सलाह जनरेट करने में त्रुटि", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isGeneratingAdvice,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isGeneratingAdvice) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI विश्लेषण कर रहा है...")
                    } else {
                        Text("दुकान के लिए AI सुझाव प्राप्त करें")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
