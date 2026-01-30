package com.masjid.tvsholat.server

import com.masjid.tvsholat.data.*
import kotlinx.coroutines.*
import fi.iki.elonen.NanoHTTPD
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AdminServer private constructor(
    private val repo: MasjidConfigRepository
) : NanoHTTPD("0.0.0.0", 9090) {

    companion object {
        private var instance: AdminServer? = null

        fun getInstance(repo: MasjidConfigRepository): AdminServer {
            if (instance == null) {
                instance = AdminServer(repo)
            }
            return instance!!
        }
    }

    override fun start(timeout: Int, daemon: Boolean) {
        try {
            if (this.isAlive) {
                this.stop()
            }
        } catch (e: Exception) {
            android.util.Log.e("ADMIN_SERVER", "Error stopping existing server", e)
        }
        
        super.start(timeout, daemon)
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method
        val remoteIp = session.remoteIpAddress
        
        // Log headers untuk debug kenapa popup gak muncul
        val authHeader = session.headers["authorization"] ?: session.headers["Authorization"]
        android.util.Log.d("ADMIN_SERVER", ">>> [${method}] ${uri} from ${remoteIp} | Auth: ${authHeader != null}")

        // --- AUTH CHECK ---
        val authorized = if (authHeader != null && authHeader.startsWith("Basic ")) {
            try {
                val base64Credentials = authHeader.substring(6)
                val credentials = String(android.util.Base64.decode(base64Credentials, android.util.Base64.DEFAULT))
                // User: musholakita, Pass: mars123!
                credentials == "musholakita:mars123!"
            } catch (e: Exception) {
                false
            }
        } else false

        if (!authorized) {
            val response = newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_PLAINTEXT, "Silakan login untuk akses Admin.")
            // Penting: Browser butuh header ini buat munculin popup login
            response.addHeader("WWW-Authenticate", "Basic realm=\"Admin Panel TvSholat\"")
            return response
        }
        // ------------------
        
        return when (uri) {
            "/" -> adminPage(session)
            "/save" -> saveConfig(session)
            "/ping" -> newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "PONG")
            "/favicon.ico" -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "")
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404 Not Found")
        }
    }

    private fun adminPage(session: IHTTPSession): Response {
        val config = repo.load()
        val isSaved = session.parameters["saved"] != null
        val successScript = if (isSaved) "<script>alert('Update Berhasil!'); window.history.replaceState({}, '', '/');</script>" else ""
        
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Admin TvSholat</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                $successScript
            </head>
            <body>
            <h2>Admin Masjid</h2>
            ${if (config.lastUpdated.isNotEmpty()) "<p style='color: gray; font-size: 0.8em'>Terakhir diperbarui: ${config.lastUpdated}</p>" else ""}
            <form action="/save" method="POST">
                Nama Masjid:<br><input name="name" value="${config.name}"><br><br>
                Alamat:<br><input name="address" value="${config.address}"><br><br>
                Tema Tampilan:<br>
                <select name="theme_name" style="width: 100%; padding: 8px; border-radius: 4px">
                    <option value="simple" ${if (config.themeName == "simple") "selected" else ""}>Simple (Default)</option>
                    <option value="modern" ${if (config.themeName == "modern") "selected" else ""}>Modern Sleek</option>
                    <option value="classic" ${if (config.themeName == "classic") "selected" else ""}>Classic Green</option>
                    <option value="dashboard" ${if (config.themeName == "dashboard") "selected" else ""}>Dashboard Sidebar</option>
                </select><br><br>
                Latitude:<br><input name="lat" value="${config.latitude}"><br><br>
                Longitude:<br><input name="lng" value="${config.longitude}"><br><br>
                Iqomah (menit):<br><input name="iqomah" value="${config.iqomahMinutes}"><br><br>
                Koreksi Waktu (menit):<br><input name="time_offset" type="number" value="${config.timeOffsetMinutes}"><br><br>
                Koreksi Tanggal (hari):<br><input name="date_offset" type="number" value="${config.dateOffsetDays}"><br><br>
                Teks Berjalan (Anti Burn-in):<br>
                <textarea name="running_text" style="width: 100%; height: 80px; padding: 8px; border-radius: 4px">${config.runningText}</textarea><br><br>
                Background URL:<br><input name="bg_url" id="bg_url" value="${config.backgroundUrl}" style="width: 100%" oninput="document.getElementById('preview').src=this.value"><br><br>
                <div style="width: 100%; height: 150px; border: 1px solid #ccc; overflow: hidden">
                    <img id="preview" src="${config.backgroundUrl}" style="width: 100%; height: 100%; object-fit: cover" onerror="this.src='https://via.placeholder.com/300x150?text=Preview+Error'">
                </div><br>
                
                <div style="background: #f0f0f0; padding: 15px; border-radius: 8px; margin-bottom: 20px">
                    <h3 style="margin-top: 0">Laporan Kas DKM</h3>
                    Saldo Kas:<br><input name="treasury_balance" value="${config.treasuryBalance}" style="width: 100%"><br><br>
                    Keterangan:<br><input name="treasury_desc" value="${config.treasuryDescription}" style="width: 100%"><br><br>
                    Interval Tampil (menit, 0=off):<br><input name="treasury_interval" type="number" value="${config.treasuryDisplayInterval}" style="width: 100%"><br>
                    <small style="color: grey">*Tampil otomatis setiap interval menit selama 15 detik</small>
                </div>

                <div style="background: #f0f0f0; padding: 15px; border-radius: 8px; margin-bottom: 20px">
                    <h3 style="margin-top: 0">Syiar & Edukasi</h3>
                    Interval Tampil Hadits (menit, 0=off):<br><input name="hadith_interval" type="number" value="${config.hadithDisplayInterval}" style="width: 100%"><br>
                    <small style="color: grey">*Tampil otomatis setiap interval menit selama 20 detik</small>
                </div>

                <button type="submit" style="padding: 10px; width: 100%; background: #4CAF50; color: white; border: none; border-radius: 4px">Simpan</button>
            </form>
            </body>
            </html>
        """.trimIndent()
        return newFixedLengthResponse(Response.Status.OK, MIME_HTML, html)
    }

    private fun saveConfig(session: IHTTPSession): Response {
        return try {
            // 🔥 WAJIB ADA UNTUK POST
            val files = HashMap<String, String>()
            session.parseBody(files)

            val p = session.parameters
            
            // Log params biar keliatan di logcat kalau ada yang aneh
            android.util.Log.d("ADMIN_SERVER", "Received Parameters: ${p.keys}")

            val config = MasjidConfig(
                name = p["name"]?.first()?.trim() ?: "",
                address = p["address"]?.first()?.trim() ?: "",
                latitude = p["lat"]?.first()?.toDoubleOrNull() ?: -6.32,
                longitude = p["lng"]?.first()?.toDoubleOrNull() ?: 107.02,
                iqomahMinutes = p["iqomah"]?.first()?.toIntOrNull() ?: 5,
                backgroundUrl = p["bg_url"]?.first()?.trim() ?: "",
                themeName = p["theme_name"]?.first()?.trim() ?: "simple",
                runningText = p["running_text"]?.first()?.trim() ?: "",
                timeOffsetMinutes = p["time_offset"]?.first()?.toIntOrNull() ?: 0,
                dateOffsetDays = p["date_offset"]?.first()?.toIntOrNull() ?: 0,
                treasuryBalance = p["treasury_balance"]?.first()?.trim() ?: "0",
                treasuryDescription = p["treasury_desc"]?.first()?.trim() ?: "Saldo Kas Masjid",
                treasuryDisplayInterval = p["treasury_interval"]?.first()?.toIntOrNull() ?: 0,
                hadithDisplayInterval = p["hadith_interval"]?.first()?.toIntOrNull() ?: 0,
                lastUpdated = SimpleDateFormat("d MMM yyyy HH:mm", Locale.forLanguageTag("id")).format(Date())
            )

            android.util.Log.d("ADMIN_SERVER", "Saving config: $config")

            // Save in IO thread
            CoroutineScope(Dispatchers.IO).launch {
                repo.save(config)
            }

            // Redirect back to home with success param
            val response = newFixedLengthResponse(Response.Status.REDIRECT, MIME_HTML, "")
            response.addHeader("Location", "/?saved=1")
            response
        } catch (e: Exception) {
            e.printStackTrace()
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                MIME_PLAINTEXT,
                "ERROR: ${e.message}"
            )
        }
    }

    override fun stop() {
        super.stop()
        android.util.Log.d("ADMIN_SERVER", "🛑 Server stop")
    }
}
