package com.watercard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.watercard.data.AppDatabase
import com.watercard.data.Subscriber

@Composable
fun SubscribersScreen(db: AppDatabase, onNew: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val list by produceState<List<Subscriber>>(emptyList(), query) {
        db.subscriberDao().search(query).collect { value = it }
    }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("المشتركون", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            label = { Text("بحث بالاسم أو الرقم") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onNew, modifier = Modifier.fillMaxWidth()) {
            Text("مشترك جديد + أول شحنة")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(list, key = { it.id }) { s ->
                ListItem(
                    headlineContent = { Text("${s.userNo} — ${s.name}") },
                    supportingContent = { Text("الهاتف: ${s.phone} — الدفعة القادمة: ${s.nextCount}") }
                )
                Divider()
            }
        }
    }
}
