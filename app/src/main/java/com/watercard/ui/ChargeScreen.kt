package com.watercard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watercard.*
import com.watercard.data.AppDatabase
import com.watercard.data.Subscriber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChargeScreen(db: AppDatabase, usb: UsbSerialManager, service: ChargeService) {
    val scope = rememberCoroutineScope()

    var userNoText by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf("5") }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("existing") }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var subscriber by remember { mutableStateOf<Subscriber?>(null) }

    suspend fun openReader(): CardReader {
        val device = usb.listDevices().firstOrNull()
            ?: throw RuntimeException("لا يوجد قارئ متصل")
        val port = usb.open(device)
        return CardReader(port)
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("شحن", fontSize = 24.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == "existing", onClick = { mode = "existing" }, label = { Text("مشترك موجود") })
            FilterChip(selected = mode == "new", onClick = { mode = "new" }, label = { Text("مشترك جديد") })
        }

        if (mode == "existing") {
            OutlinedTextField(
                value = userNoText, onValueChange = { userNoText = it.filter(Char::isDigit) },
                label = { Text("رقم المشترك") }, modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                val n = userNoText.toIntOrNull() ?: return@Button
                scope.launch {
                    subscriber = withContext(Dispatchers.IO) { db.subscriberDao().byUserNo(n) }
                    if (subscriber == null) status = "لا يوجد مشترك بالرقم $n"
                    else status = "المشترك: ${subscriber!!.name} — الدفعة القادمة: ${subscriber!!.nextCount}"
                }
            }) { Text("بحث") }
        } else {
            OutlinedTextField(value = newName, onValueChange = { newName = it },
                label = { Text("اسم المشترك") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = newPhone, onValueChange = { newPhone = it },
                label = { Text("الهاتف") }, modifier = Modifier.fillMaxWidth())
        }

        OutlinedTextField(value = qtyText, onValueChange = { qtyText = it.filter(Char::isDigit) },
            label = { Text("الكمية (م³)") }, modifier = Modifier.fillMaxWidth())

        Button(
            enabled = !busy && qtyText.toIntOrNull() != null,
            onClick = {
                busy = true; status = "جارٍ..."
                scope.launch {
                    try {
                        val qty = qtyText.toInt()
                        val reader = openReader()
                        withContext(Dispatchers.IO) {
                            if (mode == "existing") {
                                val sub = db.subscriberDao().byUserNo(userNoText.toInt())
                                    ?: throw RuntimeException("لا يوجد مشترك")
                                service.charge(reader, sub, qty)
                            } else {
                                service.registerAndFirstCharge(
                                    reader, newName, newPhone, "", 0.1, qty
                                )
                            }
                        }
                        status = "تمت العملية بنجاح!"
                    } catch (e: Exception) {
                        status = "خطأ: ${e.message}"
                    } finally {
                        usb.close(); busy = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "جارٍ..." else "قراءة → توليد → كتابة → تحقق") }

        Text(status, fontSize = 14.sp)
    }
}
