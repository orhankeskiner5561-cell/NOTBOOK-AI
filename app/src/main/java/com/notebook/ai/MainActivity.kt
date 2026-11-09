package com.notebook.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContent {
   val prefs = remember { getSharedPreferences("notbook_notes", MODE_PRIVATE) }
   var title by remember { mutableStateOf("") }
   var content by remember { mutableStateOf("") }
   var notes by remember { mutableStateOf(prefs.getString("notes", "") ?: "") }
   val navy = Color(0xFF101B33)
   val purple = Color(0xFF8B42F5)
   MaterialTheme(colorScheme = darkColorScheme(primary = purple, background = navy, surface = Color(0xFF1B2945))) {
    Scaffold(containerColor = navy, topBar = { Surface(color = navy) { Text("NOTBOOK AI  •  Not Defterim", modifier = Modifier.padding(20.dp), color = Color.White) } }) { pad ->
     Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
      Text("Gördüğünü anlar, izin verdiğini hatırlar.", color = Color(0xFF82DCE5))
      Spacer(Modifier.height(16.dp))
      OutlinedTextField(title, { title = it }, label = { Text("Not başlığı") }, modifier = Modifier.fillMaxWidth())
      OutlinedTextField(content, { content = it }, label = { Text("Notunuzu yazın") }, modifier = Modifier.fillMaxWidth().height(150.dp))
      Spacer(Modifier.height(12.dp))
      Button(onClick = {
       if (content.isNotBlank()) {
        val stamp = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale("tr", "TR")).format(java.util.Date())
        notes = stamp + " • " + (title.ifBlank { "Başlıksız" }) + "\n" + content + "\n---\n" + notes
        prefs.edit().putString("notes", notes).apply()
        title = ""; content = ""
       }
      }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("NOTU KAYDET") }
      Spacer(Modifier.height(18.dp))
      Text("Kayıtlı notlar", color = Color.White)
      LazyColumn { items(notes.split("\n---\n").filter { it.isNotBlank() }) { note ->
       Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) { Text(note, modifier = Modifier.padding(14.dp)) }
      } }
     }
    }
   }
  }
 }
}