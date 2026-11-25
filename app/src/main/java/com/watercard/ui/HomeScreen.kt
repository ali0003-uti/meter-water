package com.watercard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watercard.ChargeService
import com.watercard.UsbSerialManager
import com.watercard.data.AppDatabase

@Composable
fun HomeScreen(
    db: AppDatabase,
    usb: UsbSerialManager,
    service: ChargeService,
    onGoCharge: () -> Unit
) {
    val subs by db.subscriberDao().all().collectAsState(initial = emptyList())
    val charges by db.chargeDao().recent().collectAsState(initial = emptyList())
    val devices = remember { usb.listDevices() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("الرئيسية", fontSize = 24.sp)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("المشتركون: ${subs.size}")
                Text("عدد الشحنات: ${charges.size}")
                Text("إجمالي المبيعات: ${"%.2f".format(charges.sumOf { it.amount })}")
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("القارئ", fontSize = 18.sp)
                if (devices.isEmpty()) Text("لا يوجد جهاز USB متصل")
                else devices.forEach { d ->
                    Text("${d.productName ?: "USB"} — VID=${d.vendorId} PID=${d.productId}")
                }
            }
        }
        Button(onClick = onGoCharge, modifier = Modifier.fillMaxWidth()) {
            Text("شحن سريع")
        }
    }
}
