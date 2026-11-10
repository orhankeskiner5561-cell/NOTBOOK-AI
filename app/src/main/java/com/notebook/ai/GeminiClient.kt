package com.notebook.ai

import android.content.Context
import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// User-provided access token is kept in RAM by the calling screen, never in the APK or preferences.
internal object GeminiClient {
 private const val BASE = "https://notbook-ai.vercel.app"
 fun scan(context: Context, uri: Uri, token: String): String {
  val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
   ?: throw IllegalArgumentException("Görsel açılamadı")
  require(bytes.size <= 5_000_000) { "Fotoğraf 5 MB sınırını aşıyor. Daha küçük görüntü seçin." }
  val mime = context.contentResolver.getType(uri)?.lowercase() ?: "image/jpeg"
  require(mime in listOf("image/jpeg", "image/png", "image/webp")) { "JPEG, PNG veya WebP seçin." }
  val payload = JSONObject().put("mimeType",mime)
   .put("imageBase64",Base64.encodeToString(bytes,Base64.NO_WRAP))
  val result = post("/api/scan", payload, token)
  return result.optString("text").ifBlank { throw IllegalStateException("Gemini metin bulamadı") }
 }
 fun ask(question: String, notes: List<Note>, token: String): String {
  val context = org.json.JSONArray()
  notes.take(15).forEach { n ->
   context.put(JSONObject().put("id",n.id).put("title",n.title.take(200)).put("body",n.body.take(4000)))
  }
  return post("/api/ask",JSONObject().put("question",question).put("context",context),token)
   .optString("answer").ifBlank { "Yanıt alınamadı" }
 }
 private fun post(path:String, body:JSONObject, token:String):JSONObject {
  require(token.isNotBlank()) { "Önce erişim anahtarını Ayarlar bölümüne girin." }
  val conn = URL(BASE+path).openConnection() as HttpURLConnection
  try {
   conn.requestMethod="POST"
   conn.connectTimeout=15000
   conn.readTimeout=30000
   conn.doOutput=true
   conn.setRequestProperty("Content-Type","application/json")
   conn.setRequestProperty("Authorization","Bearer $token")
   conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
   if (conn.responseCode !in 200..299) {
    throw IllegalStateException("Sunucu yanıtı: HTTP ${conn.responseCode}. Anahtarı ve Gemini kotasını kontrol edin.")
   }
   return JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
  } finally { conn.disconnect() }
 }
}
