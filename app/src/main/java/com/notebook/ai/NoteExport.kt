package com.notebook.ai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

internal object NoteExport {
 private fun exportText(notes: List<Note>): String = buildString {
  appendLine("NOTBOOK AI - Hafiza Kayitlari")
  appendLine("Kayit sayisi: ${notes.size}")
  appendLine()
  notes.forEachIndexed { i, n ->
   appendLine("${i+1}. ${n.title}")
   appendLine(n.timestamp)
   appendLine(n.body)
   appendLine()
  }
 }

 fun shareText(activity: Activity, notes: List<Note>) {
  val intent = Intent(Intent.ACTION_SEND).apply {
   type = "text/plain"
   putExtra(Intent.EXTRA_SUBJECT, "NOTBOOK AI kayitlari")
   putExtra(Intent.EXTRA_TEXT, exportText(notes))
  }
  activity.startActivity(Intent.createChooser(intent, "Kayitlari paylas"))
 }

 private fun makePdf(activity: Activity, notes: List<Note>): File {
  val pdf = PdfDocument()
  val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = android.graphics.Color.BLACK }
  val pageWidth = 595
  val pageHeight = 842
  val left = 42f
  val right = 553f
  val bottom = 800f
  var pageNumber = 0
  var page: PdfDocument.Page? = null
  var y = 0f
  fun newPage() {
   page?.let { pdf.finishPage(it) }
   pageNumber++
   page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
   y = 54f
  }
  fun line(value: String) {
   if (page == null || y > bottom) newPage()
   page!!.canvas.drawText(value, left, y, paint)
   y += 17f
  }
  // Android's built-in PDF font may not support all Turkish glyphs on every device.
  // The PDF is generated locally; the original text remains unchanged in memory.
  fun wrap(value: String) {
   if (value.isEmpty()) { line(""); return }
   var current = ""
   value.split(Regex("\\s+")).forEach { word ->
    val candidate = if(current.isEmpty()) word else "$current $word"
    if (paint.measureText(candidate) > right-left && current.isNotEmpty()) {
     line(current)
     current = word
    } else current = candidate
   }
   if(current.isNotEmpty()) line(current)
  }
  try {
   paint.isFakeBoldText = true
   line("NOTBOOK AI - Kayitlar")
   paint.isFakeBoldText = false
   line("Toplam: ${notes.size}")
   line("")
   notes.forEachIndexed { i, note ->
    paint.isFakeBoldText = true
    wrap("${i+1}. ${note.title}")
    paint.isFakeBoldText = false
    wrap(note.timestamp)
    note.body.lines().forEach { wrap(it) }
    line("")
   }
   page?.let { pdf.finishPage(it) }
   val dir = File(activity.cacheDir, "exports").apply { mkdirs() }
   val file = File(dir, "NOTBOOK-AI-kayitlar.pdf")
   FileOutputStream(file).use { pdf.writeTo(it) }
   return file
  } finally { pdf.close() }
 }

 fun sharePdf(activity: Activity, notes: List<Note>) {
  val file = makePdf(activity, notes)
  val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
  val intent = Intent(Intent.ACTION_SEND).apply {
   type = "application/pdf"
   putExtra(Intent.EXTRA_STREAM, uri)
   addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  activity.startActivity(Intent.createChooser(intent, "PDF paylas"))
 }

 fun print(activity: Activity, notes: List<Note>) {
  val file = makePdf(activity, notes)
  val manager = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
  manager.print("NOTBOOK AI Kayitlari", object : PrintDocumentAdapter() {
   override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?,
     cancellationSignal: CancellationSignal?, callback: LayoutResultCallback, extras: Bundle?) {
    if (cancellationSignal?.isCanceled == true) { callback.onLayoutCancelled(); return }
    callback.onLayoutFinished(PrintDocumentInfo.Builder(file.name)
     .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
     .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN).build(), true)
   }
   override fun onWrite(pages: Array<out android.print.PageRange>?, destination: ParcelFileDescriptor?,
     cancellationSignal: CancellationSignal?, callback: WriteResultCallback) {
    if(destination == null) { callback.onWriteFailed("Yazdirma hedefi bulunamadi"); return }
    try {
     FileInputStream(file).use { input ->
      FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) }
     }
     callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
    } catch(e:Exception) { callback.onWriteFailed(e.localizedMessage ?: "Yazdirma hatasi") }
   }
  }, PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
 }
}
