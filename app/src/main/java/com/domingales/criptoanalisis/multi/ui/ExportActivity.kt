package com.domingales.criptoanalisis.multi.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.domingales.criptoanalisis.multi.data.AppDatabase
import java.io.File
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ExportActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val exporting = AtomicBoolean(false)
    private lateinit var status: TextView
    private var pendingFormat: ExportFormat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = Ui.root(this)
        root.addView(Ui.title(this, "Exportación global"))
        root.addView(Ui.subtitle(this, "Incluye configuración, evaluaciones, rechazos, señales, resultados, snapshots exactos, seguimientos, alertas, alarmas de precio, posibles explosiones independientes y diagnóstico."))
        status = Ui.subtitle(this, "Preparado. Los archivos se generan en segundo plano para no bloquear la pantalla.")
        root.addView(status)
        root.addView(Ui.button(this, "COPIAR TODO AL PORTAPAPELES") { copyToClipboard() })
        root.addView(Ui.button(this, "COMPARTIR INFORME COMPLETO") { shareReport() })
        root.addView(Ui.button(this, "GUARDAR TXT") { chooseDestination(ExportFormat.TXT) })
        root.addView(Ui.button(this, "GUARDAR CSV PARA EXCEL") { chooseDestination(ExportFormat.CSV) })
        root.addView(Ui.button(this, "GUARDAR WORD / RTF") { chooseDestination(ExportFormat.RTF) })
        setContentView(root)
    }

    private fun copyToClipboard() = runExport("Preparando texto para el portapapeles…") { db ->
        val writer = LimitedStringWriter(MAX_CLIPBOARD_CHARS)
        db.writeAuditText(writer)
        val text = writer.toString()
        runOnUiThread {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("CriptoAnálisis Multi", text))
            completed("Informe global copiado (${text.length} caracteres)")
        }
    }

    private fun shareReport() = runExport("Generando archivo TXT para compartir…") { db ->
        val directory = File(cacheDir, "exports").apply { mkdirs() }
        val file = File(directory, fileName(ExportFormat.TXT))
        file.bufferedWriter(Charsets.UTF_8).use(db::writeAuditText)
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        runOnUiThread {
            completed("Informe generado; elige dónde compartirlo")
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = ExportFormat.TXT.mime
                putExtra(Intent.EXTRA_SUBJECT, "Exportación global CriptoAnálisis Multi")
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("Informe completo", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Compartir exportación global"))
        }
    }

    private fun chooseDestination(format: ExportFormat) {
        if (exporting.get()) return notifyBusy()
        pendingFormat = format
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = format.mime
            putExtra(Intent.EXTRA_TITLE, fileName(format))
        }, REQUEST_SAVE)
    }

    @Deprecated("Compatibilidad con Activity")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_SAVE) return
        val format = pendingFormat
        pendingFormat = null
        val uri = data?.data
        if (resultCode != RESULT_OK || uri == null || format == null) return
        saveTo(uri, format)
    }

    private fun saveTo(uri: Uri, format: ExportFormat) = runExport("Generando ${format.extension.uppercase(Locale.ROOT)}…") { db ->
        contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8).use { writer ->
            requireNotNull(writer) { "No se pudo abrir el archivo de destino" }
            when (format) {
                ExportFormat.TXT -> db.writeAuditText(writer)
                ExportFormat.CSV -> db.writeCsv(writer)
                ExportFormat.RTF -> db.writeRtf(writer)
            }
        }
        runOnUiThread { completed("Exportación guardada correctamente") }
    }

    private fun runExport(message: String, operation: (AppDatabase) -> Unit) {
        if (!exporting.compareAndSet(false, true)) return notifyBusy()
        status.text = message
        executor.execute {
            try {
                val db = AppDatabase.get(this)
                db.syncConfiguration(this)
                operation(db)
            } catch (_: ExportTooLargeException) {
                runOnUiThread { failed("El informe supera el límite seguro del portapapeles. Guárdalo o compártelo como archivo.") }
            } catch (t: Throwable) {
                runOnUiThread { failed("No se pudo exportar: ${t.message ?: "error desconocido"}") }
            } finally {
                exporting.set(false)
            }
        }
    }

    private fun completed(message: String) {
        status.text = message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun failed(message: String) {
        status.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun notifyBusy() = Toast.makeText(this, "Ya hay una exportación en curso", Toast.LENGTH_SHORT).show()

    private fun fileName(format: ExportFormat): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ROOT).format(Date())
        return "CriptoAnalisisMulti_global_$stamp.${format.extension}"
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    private enum class ExportFormat(val mime: String, val extension: String) {
        TXT("text/plain", "txt"), CSV("text/csv", "csv"), RTF("application/rtf", "rtf")
    }

    private class ExportTooLargeException : RuntimeException()

    private class LimitedStringWriter(private val limit: Int) : Writer() {
        private val content = StringBuilder()
        override fun write(buffer: CharArray, offset: Int, length: Int) {
            if (content.length + length > limit) throw ExportTooLargeException()
            content.append(buffer, offset, length)
        }
        override fun write(value: Int) {
            if (content.length + 1 > limit) throw ExportTooLargeException()
            content.append(value.toChar())
        }
        override fun flush() = Unit
        override fun close() = Unit
        override fun toString(): String = content.toString()
    }

    companion object {
        private const val REQUEST_SAVE = 5301
        private const val MAX_CLIPBOARD_CHARS = 750_000
    }
}
