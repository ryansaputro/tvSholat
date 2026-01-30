package com.masjid.tvsholat.server

import com.masjid.tvsholat.data.*
import kotlinx.coroutines.*
import fi.iki.elonen.NanoHTTPD
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AdminServer private constructor(
    private val repo: MasjidConfigRepository
) : NanoHTTPD(null, 9090) {

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
            <html lang="id">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Admin TvSholat - ${config.name}</title>
                <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;800&display=swap" rel="stylesheet">
                <style>
                    :root {
                        --primary: #1b5e20;
                        --primary-light: #4c8c4a;
                        --bg: #f5f7f9;
                        --card: #ffffff;
                        --text: #2c3e50;
                        --muted: #7f8c8d;
                    }
                    * { box-sizing: border-box; font-family: 'Inter', sans-serif; }
                    body { 
                        background: var(--bg); color: var(--text); margin: 0; padding: 20px;
                        display: flex; justify-content: center;
                    }
                    .container { width: 100%; max-width: 600px; }
                    .header { text-align: center; margin-bottom: 30px; }
                    .header h1 { margin: 0; color: var(--primary); font-weight: 800; font-size: 24px; }
                    .header p { color: var(--muted); margin: 5px 0; font-size: 14px; }
                    
                    .card { 
                        background: var(--card); border-radius: 12px; padding: 20px;
                        box-shadow: 0 4px 6px rgba(0,0,0,0.05); margin-bottom: 20px;
                        border: 1px solid rgba(0,0,0,0.05);
                    }
                    .card h3 { 
                        margin: 0 0 15px 0; font-size: 16px; color: var(--primary);
                        display: flex; align-items: center;
                    }
                    .card h3::before {
                        content: ''; display: inline-block; width: 4px; height: 16px;
                        background: var(--primary); margin-right: 10px; border-radius: 2px;
                    }

                    .form-group { margin-bottom: 15px; }
                    label { display: block; margin-bottom: 6px; font-weight: 600; font-size: 13px; color: var(--text); }
                    input, select, textarea {
                        width: 100%; padding: 10px 12px; border: 1px solid #ddd;
                        border-radius: 8px; font-size: 14px; outline: none;
                        transition: border-color 0.2s;
                    }
                    input:focus, select:focus, textarea:focus { border-color: var(--primary); }
                    
                    .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
                    
                    .preview-box {
                        width: 100%; height: 120px; border-radius: 8px; overflow: hidden;
                        margin-top: 10px; border: 1px dashed #ccc;
                    }
                    .preview-box img { width: 100%; height: 100%; object-fit: cover; }
                    
                    button {
                        width: 100%; padding: 14px; background: var(--primary);
                        color: white; border: none; border-radius: 8px; font-weight: 600;
                        font-size: 16px; cursor: pointer; transition: background 0.2s;
                        margin-top: 10px; box-shadow: 0 4px 12px rgba(27, 94, 32, 0.2);
                    }
                    button:hover { background: var(--primary-light); }
                    button:active { transform: translateY(1px); }
                </style>
                $successScript
            </head>
            <body>
            <div class="container">
                <div class="header">
                    <h1>TV Sholat Admin</h1>
                    <p>${if (config.lastUpdated.isNotEmpty()) "Update terakhir: ${config.lastUpdated}" else "Panel Konfigurasi Masjid"}</p>
                </div>
                
                <form action="/save" method="POST">
                    <div class="card">
                        <h3>Informasi Masjid</h3>
                        <div class="form-group">
                            <label>Nama Masjid</label>
                            <input name="name" value="${config.name}" placeholder="Contoh: Masjid Al-Kautsar">
                        </div>
                        <div class="form-group">
                            <label>Alamat</label>
                            <input name="address" value="${config.address}" placeholder="Alamat lengkap...">
                        </div>
                        <div class="form-group">
                            <label>Tema Tampilan</label>
                            <select name="theme_name">
                                <option value="simple" ${if (config.themeName == "simple") "selected" else ""}>🌿 Simple Clean (Default)</option>
                                <option value="modern" ${if (config.themeName == "modern") "selected" else ""}>💎 Modern Sleek</option>
                                <option value="classic" ${if (config.themeName == "classic") "selected" else ""}>🕌 Classic Green</option>
                                <option value="dashboard" ${if (config.themeName == "dashboard") "selected" else ""}>📊 Dashboard Sidebar</option>
                            </select>
                        </div>
                    </div>

                    <div class="card">
                        <h3>Lokasi & Waktu</h3>
                        <div class="grid">
                            <div class="form-group">
                                <label>Latitude</label>
                                <input name="lat" value="${config.latitude}">
                            </div>
                            <div class="form-group">
                                <label>Longitude</label>
                                <input name="lng" value="${config.longitude}">
                            </div>
                            <div class="form-group">
                                <label>Koreksi Waktu (Min)</label>
                                <input name="time_offset" type="number" value="${config.timeOffsetMinutes}">
                            </div>
                            <div class="form-group">
                                <label>Koreksi Tanggal (Hari)</label>
                                <input name="date_offset" type="number" value="${config.dateOffsetDays}">
                            </div>
                        </div>
                    </div>

                    <div class="card">
                        <h3>Iqomah & Konten</h3>
                        <div class="form-group">
                            <label>Jeda Iqomah (Menit)</label>
                            <input name="iqomah" type="number" value="${config.iqomahMinutes}">
                        </div>
                        <div class="form-group">
                            <label>Teks Berjalan</label>
                            <textarea name="running_text" rows="3">${config.runningText}</textarea>
                        </div>
                        <div class="form-group">
                            <label>URL Gambar Background</label>
                            <input name="bg_url" oninput="document.getElementById('preview').src=this.value" value="${config.backgroundUrl}">
                            <div class="preview-box">
                                <img id="preview" src="${config.backgroundUrl}" onerror="this.src='https://via.placeholder.com/400x200?text=Preview+Error'">
                            </div>
                        </div>
                    </div>

                    <div class="card">
                        <h3>Laporan Kas DKM</h3>
                        <div class="form-group">
                            <label>Saldo Kas saat ini</label>
                            <input name="treasury_balance" value="${config.treasuryBalance}" placeholder="Contoh: Rp 1.500.000">
                        </div>
                        <div class="form-group">
                            <label>No. Rekening / Info Bank</label>
                            <input name="treasury_account" value="${config.treasuryAccountInfo}" placeholder="Contoh: Bank BSI - 7123456789 (Masjid Al-Kautsar)">
                        </div>
                        <div class="form-group">
                            <label>Link / Data QRIS</label>
                            <input name="treasury_qris" value="${config.treasuryQrisData}" placeholder="Masukkan link atau data QRIS untuk generate QR">
                        </div>
                        <div class="grid">
                            <div class="form-group">
                                <label>Interval (Menit)</label>
                                <input name="treasury_interval" type="number" value="${config.treasuryDisplayInterval}">
                            </div>
                            <div class="form-group">
                                <label>Durasi (Detik)</label>
                                <input name="treasury_duration" type="number" value="${config.treasuryDisplayDuration}">
                            </div>
                        </div>
                    </div>

                    <div class="card">
                        <h3>Syiar & Edukasi (Hadits)</h3>
                        <div class="grid">
                            <div class="form-group">
                                <label>Interval (Menit)</label>
                                <input name="hadith_interval" type="number" value="${config.hadithDisplayInterval}">
                            </div>
                            <div class="form-group">
                                <label>Durasi (Detik)</label>
                                <input name="hadith_duration" type="number" value="${config.hadithDisplayDuration}">
                            </div>
                        </div>
                    </div>

                    <button type="submit">Simpan Konfigurasi</button>
                    <div style="height: 40px;"></div>
                </form>
            </div>
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
                treasuryDisplayDuration = p["treasury_duration"]?.first()?.toIntOrNull() ?: 15,
                treasuryAccountInfo = p["treasury_account"]?.first()?.trim() ?: "",
                treasuryQrisData = p["treasury_qris"]?.first()?.trim() ?: "",
                hadithDisplayInterval = p["hadith_interval"]?.first()?.toIntOrNull() ?: 0,
                hadithDisplayDuration = p["hadith_duration"]?.first()?.toIntOrNull() ?: 20,
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
