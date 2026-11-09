package com.notebook.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

private val Navy = Color(0xFF0B1427)
private val Panel = Color(0xFF1A2946)
private val Purple = Color(0xFF873BF5)
private val Cyan = Color(0xFF39D5E5)
private val Ink = Color.White
private val Gradient = Brush.horizontalGradient(listOf(Color(0xFF076B83), Color(0xFF5427A0)))

data class Note(val id: Long, val title: String, val body: String, val timestamp: String)

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  @Suppress("DEPRECATION")
  window.statusBarColor = android.graphics.Color.rgb(11,20,39)
  @Suppress("DEPRECATION")
  window.navigationBarColor = android.graphics.Color.rgb(11,20,39)
  window.decorView.systemUiVisibility = 0
  val prefs = getSharedPreferences("notbook_notes_v08", MODE_PRIVATE)
  fun load(): List<Note> {
   return try {
    val arr = JSONArray(prefs.getString("entries", "[]"))
    (0 until arr.length()).map { i -> val o = arr.getJSONObject(i)
     Note(o.getLong("id"), o.getString("title"), o.getString("body"), o.getString("time")) }
   } catch (_: Exception) { emptyList() }
  }
  setContent {
   var page by remember { mutableStateOf("Ana Sayfa") }
   var notes by remember { mutableStateOf(load()) }
   var title by remember { mutableStateOf("") }
   var body by remember { mutableStateOf("") }
   var editId by remember { mutableStateOf<Long?>(null) }
   var search by remember { mutableStateOf("") }
   var deleteId by remember { mutableStateOf<Long?>(null) }
   fun persist(updated: List<Note>) {
    notes = updated
    val arr = JSONArray()
    updated.forEach { n -> arr.put(JSONObject().put("id", n.id).put("title", n.title).put("body", n.body).put("time", n.timestamp)) }
    prefs.edit().putString("entries", arr.toString()).apply()
   }
   MaterialTheme(colorScheme = darkColorScheme(primary=Purple, background=Navy, surface=Panel, onSurface=Ink)) {
    Scaffold(containerColor=Navy, bottomBar={
     Row(Modifier.fillMaxWidth().background(Panel).padding(vertical=8.dp), horizontalArrangement=Arrangement.SpaceEvenly) {
      listOf("Ana Sayfa" to "⌂","Notlar" to "▤","Hafızam" to "◈","Ayarlar" to "⚙").forEach { (name,symbol) ->
       Column(Modifier.weight(1f).clickable { page=name }.padding(4.dp), horizontalAlignment=Alignment.CenterHorizontally) {
        Text(symbol, color=if(page==name) Cyan else Color.LightGray, fontSize=23.sp)
        Text(name, color=if(page==name) Cyan else Color.LightGray, fontSize=10.sp)
       }
      }
     }
    }) { inner ->
     Column(Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(horizontal=16.dp)) {
      Row(Modifier.fillMaxWidth().padding(vertical=17.dp), verticalAlignment=Alignment.CenterVertically) {
       if(page!="Ana Sayfa") Text("‹", color=Cyan, fontSize=30.sp, modifier=Modifier.clickable { page="Ana Sayfa" }.padding(end=16.dp))
       Text(if(page=="Ana Sayfa") "✧  NOTBOOK AI" else page, color=Ink, fontSize=22.sp, fontWeight=FontWeight.Bold)
      }
      when(page) {
       "Ana Sayfa" -> {
        Text("Akıllı Dijital Hafıza", color=Cyan)
        Spacer(Modifier.height(16.dp))
        Text("Gördüğünü anlar, izin verdiğini hatırlar.", color=Ink, modifier=Modifier.fillMaxWidth(), textAlign=TextAlign.Center)
        Spacer(Modifier.height(25.dp))
        val menus = listOf("Ekranı Tara" to "▣", "Kamerayla Tara" to "◎", "Not Defterim" to "✎", "AI'ya Sor" to "◉", "Hafızam" to "▤", "Harcamalar" to "▥")
        menus.chunked(2).forEach { pair ->
         Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(12.dp)) {
          pair.forEach { (label,icon) ->
           Column(Modifier.weight(1f).padding(bottom=12.dp).background(Gradient,RoundedCornerShape(17.dp)).clickable { page=label }.padding(vertical=23.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Text(icon,color=Cyan,fontSize=34.sp)
            Spacer(Modifier.height(8.dp))
            Text(label,color=Ink,fontSize=14.sp,fontWeight=FontWeight.SemiBold)
           }
          }
         }
        }
       }
       "Notlar","Not Defterim" -> {
        Text("Not Defterim",color=Cyan,fontSize=16.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(title,{title=it},label={Text("Not başlığı")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(body,{body=it},label={Text("Notunuzu yazın")},modifier=Modifier.fillMaxWidth().height(160.dp))
        Spacer(Modifier.height(10.dp))
        Button(onClick={
         if(body.isNotBlank()) {
          val stamp=SimpleDateFormat("dd.MM.yyyy HH:mm",Locale("tr","TR")).format(Date())
          val id=editId ?: System.currentTimeMillis()
          val newNote=Note(id,title.ifBlank { "Başlıksız not" },body,stamp)
          persist(listOf(newNote)+notes.filterNot { it.id==id })
          title="";body="";editId=null
         }
        },enabled=body.isNotBlank(),modifier=Modifier.fillMaxWidth()) { Text(if(editId==null) "NOTU KAYDET" else "DEĞİŞİKLİKLERİ KAYDET") }
        OutlinedTextField(search,{search=it},label={Text("Kayıtlı notlarda ara")},modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        notes.filter { search.isBlank() || it.title.contains(search,true) || it.body.contains(search,true) }.forEach { n ->
         Card(Modifier.fillMaxWidth().padding(bottom=10.dp),colors=CardDefaults.cardColors(containerColor=Panel)) {
          Column(Modifier.padding(14.dp)) {
           Text(n.title,color=Ink,fontWeight=FontWeight.Bold)
           Text(n.timestamp,color=Cyan,fontSize=11.sp)
           Text(n.body,color=Ink,modifier=Modifier.padding(vertical=8.dp))
           Row {
            TextButton(onClick={title=n.title;body=n.body;editId=n.id}) { Text("Düzenle") }
            TextButton(onClick={deleteId=n.id}) { Text("Sil",color=Color(0xFFFF8B91)) }
           }
          }
         }
        }
       }
       "Hafızam" -> {
        Text("Kayıtlı notlarınız: ${notes.size}",color=Cyan)
        Spacer(Modifier.height(12.dp))
        Button(onClick={page="Not Defterim"}) { Text("Notları aç") }
        Text("Belge ve ekran tarama kayıtları sonraki güncellemede eklenecek.",color=Ink,modifier=Modifier.padding(top=16.dp))
       }
       "AI'ya Sor" -> {
        Text("Akıllı hafızada arama",color=Cyan)
        OutlinedTextField(search,{search=it},label={Text("Notlarınızda ne arıyorsunuz?")},modifier=Modifier.fillMaxWidth())
        notes.filter { search.isNotBlank() && (it.title.contains(search,true)||it.body.contains(search,true)) }.forEach { n ->
         Card(Modifier.fillMaxWidth().padding(top=10.dp).clickable { page="Not Defterim";title=n.title;body=n.body;editId=n.id }) {
          Column(Modifier.padding(12.dp)) { Text(n.title);Text(n.body,maxLines=3) }
         }
        }
        Text("Bu sürümde yerel metin araması çalışır; yapay zekâ sohbeti henüz eklenmedi.",color=Color.LightGray,modifier=Modifier.padding(top=16.dp))
       }
       "Ekranı Tara","Kamerayla Tara","Harcamalar" -> {
        Text("Bu bölümün tasarımı hazır.",color=Cyan,fontSize=19.sp)
        Spacer(Modifier.height(12.dp))
        Text("Gerçek ${page.lowercase(Locale("tr","TR"))} işlevi henüz etkin değil. Sonraki APK güncellemesinde gerekli Android izinleri ve tarama altyapısı eklenecek.",color=Ink)
       }
       "Ayarlar" -> {
        Text("NOTBOOK AI v0.8 • İlk arayüz sürümü",color=Cyan)
        Text("Notlar bu cihazda yerel olarak saklanır. Henüz şifrelenmez.",color=Ink,modifier=Modifier.padding(top=12.dp))
       }
      }
      Spacer(Modifier.height(24.dp))
     }
     if(deleteId!=null) AlertDialog(onDismissRequest={deleteId=null},title={Text("Not silinsin mi?")},text={Text("Bu işlem geri alınamaz.")},confirmButton={
      TextButton(onClick={persist(notes.filterNot { it.id==deleteId });deleteId=null}) { Text("Sil") }
     },dismissButton={TextButton(onClick={deleteId=null}){Text("Vazgeç")}})
    }
   }
  }
 }
}
