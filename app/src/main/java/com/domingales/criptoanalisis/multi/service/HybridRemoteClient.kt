package com.domingales.criptoanalisis.multi.service

import android.content.Context
import com.domingales.criptoanalisis.multi.data.AppDatabase
import com.domingales.criptoanalisis.multi.util.Prefs
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object HybridRemoteClient {
    private val executor = Executors.newSingleThreadExecutor()

    fun register(context: Context) {
        val app = context.applicationContext
        val endpoint = Prefs.remoteEndpoint(app)
        val key = Prefs.remoteRegistrationKey(app)
        if (endpoint.isBlank() || key.isBlank() || FirebaseApp.getApps(app).isEmpty()) {
            AppDatabase.get(app).log("SYSTEM", "REMOTE", "Modo local activo; conexión Cloudflare/Firebase pendiente de configurar")
            return
        }
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            executor.execute { sendRegistration(app, endpoint, key, token, false) }
        }.addOnFailureListener { error ->
            AppDatabase.get(app).log("SYSTEM", "REMOTE", "No se obtuvo token FCM: ${error.message}")
        }
    }

    fun unregister(context: Context) {
        val app = context.applicationContext
        val endpoint = Prefs.remoteEndpoint(app)
        val key = Prefs.remoteRegistrationKey(app)
        if (endpoint.isBlank() || key.isBlank() || FirebaseApp.getApps(app).isEmpty()) return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            executor.execute { sendRegistration(app, endpoint, key, token, true) }
        }
    }

    fun registerToken(context: Context, token: String) {
        val endpoint = Prefs.remoteEndpoint(context)
        val key = Prefs.remoteRegistrationKey(context)
        if (endpoint.isNotBlank() && key.isNotBlank()) executor.execute { sendRegistration(context.applicationContext, endpoint, key, token, false) }
    }

    private fun sendRegistration(context: Context, endpoint: String, key: String, token: String, remove: Boolean) {
        try {
            val connection = (URL("$endpoint/${if (remove) "unregister" else "register"}").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 10_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $key")
            }
            val body = JSONObject().put("installationId", Prefs.installationId(context)).put("token", token).toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            connection.disconnect()
            AppDatabase.get(context).log("SYSTEM", "REMOTE", if (code in 200..299) "Registro híbrido ${if (remove) "eliminado" else "activo"}" else "Registro híbrido rechazado HTTP $code")
        } catch (t: Throwable) {
            AppDatabase.get(context).log("SYSTEM", "REMOTE", "Cloudflare no disponible: ${t.message}")
        }
    }
}
