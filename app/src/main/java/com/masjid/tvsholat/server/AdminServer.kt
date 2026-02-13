package com.masjid.tvsholat.server

import com.masjid.tvsholat.data.*
import com.masjid.tvsholat.domain.model.InfoItem
import kotlinx.coroutines.*
import fi.iki.elonen.NanoHTTPD
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.masjid.tvsholat.BuildConfig
import org.json.JSONObject
import org.json.JSONArray


class AdminServer private constructor(
    private val repo: MasjidConfigRepository,
    private val context: Context
) : NanoHTTPD(null, 9090) {

    private val networkDiscovery = NetworkDiscovery(context)

    companion object {
        private var instance: AdminServer? = null

        fun getInstance(repo: MasjidConfigRepository, context: Context): AdminServer {
            if (instance == null) {
                instance = AdminServer(repo, context.applicationContext)
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
        
        
        // Start Discovery Listener
        CoroutineScope(Dispatchers.IO).launch {
            networkDiscovery.listenForDiscovery {
                repo.load().deviceId
            }
        }
    }

    override fun serve(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val uri = session.uri
        val method = session.method
        val remoteIp = session.remoteIpAddress
        
        // --- 1. BYPASS AUTH FOR SYNC, FILES, & PING ---
        // These are internal TV-to-TV communications
        val isSyncRequest = session.headers["x-sync-source"] == "true" || uri == "/sync"
        val isFileRequest = uri.startsWith("/files/")
        val isPingRequest = uri == "/ping"

        if (isSyncRequest || isFileRequest || isPingRequest) {
             return when {
                 isFileRequest -> serveFile(uri)
                 uri == "/sync" -> handleSyncRequest(session)
                 uri == "/ping" -> newFixedLengthResponse(NanoHTTPD.Response.Status.OK, MIME_PLAINTEXT, "PONG")
                 else -> newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404 Not Found")
             }
        }

        // --- 2. AUTH CHECK FOR OTHERS (ADMIN PANEL) ---
        val authHeader = session.headers["authorization"] ?: session.headers["Authorization"]
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
            val response = newFixedLengthResponse(NanoHTTPD.Response.Status.UNAUTHORIZED, MIME_PLAINTEXT, "Silakan login untuk akses Admin.")
            // Penting: Browser butuh header ini buat munculin popup login
            response.addHeader("WWW-Authenticate", "Basic realm=\"Admin Panel TvSholat\"")
            return response
        }
        
        // --- 3. PROTECTED ENDPOINTS ---
        return when (uri) {
            "/" -> adminPage(session)
            "/save" -> saveConfig(session)
            "/peers" -> handlePeersRequest(session)
            "/peers/manage" -> managePeers(session)
            "/peers/test" -> testPeerConnection(session)
            "/favicon.ico" -> newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, MIME_PLAINTEXT, "")
            else -> newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404 Not Found")
        }
    }

    private fun serveFile(uri: String): NanoHTTPD.Response {
        return try {
            // URI format: /files/subfolder/filename.jpg
            val parts = uri.split("/")
            if (parts.size < 4) return newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Invalid file path")
            
            val subFolder = parts[2] // backgrounds, logos, info, or audio
            val filename = parts[3]
            
            val file = File(File(context.filesDir, subFolder), filename)
            if (!file.exists()) {
                android.util.Log.e("ADMIN_SERVER", "File not found: ${file.absolutePath}")
                return newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found")
            }
            
            val mimeType = when (file.extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                "mp3" -> "audio/mpeg"
                "wav" -> "audio/wav"
                "ogg" -> "audio/ogg"
                else -> "application/octet-stream"
            }
            
            newChunkedResponse(NanoHTTPD.Response.Status.OK, mimeType, file.inputStream())
        } catch (e: Exception) {
            android.util.Log.e("ADMIN_SERVER", "Error serving file: $uri", e)
            newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: ${e.message}")
        }
    }

    private fun saveAudioFile(sourceFile: File): String? {
        return try {
            val audioDir = File(context.filesDir, "audio")
            if (!audioDir.exists()) audioDir.mkdirs()
            
            val extension = sourceFile.extension.lowercase().ifEmpty { "mp3" }
            val fileName = "tarhim_${System.currentTimeMillis()}.$extension"
            val targetFile = File(audioDir, fileName)
            
            sourceFile.inputStream().use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            android.util.Log.d("ADMIN_SERVER", "Audio file saved to: ${targetFile.absolutePath}")
            targetFile.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("ADMIN_SERVER", "Error saving audio file", e)
            null
        }
    }

    private fun downloadFileFromPeer(peerIp: String, subFolder: String, filename: String): String? {
        return try {
            val url = java.net.URL("http://$peerIp:9090/files/$subFolder/$filename")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 10000
            
            // Add Basic Auth
            val auth = "musholakita:mars123!"
            val encodedAuth = android.util.Base64.encodeToString(auth.toByteArray(), android.util.Base64.NO_WRAP)
            connection.setRequestProperty("Authorization", "Basic $encodedAuth")
            
            if (connection.responseCode == 200) {
                val targetDir = File(context.filesDir, subFolder)
                if (!targetDir.exists()) targetDir.mkdirs()
                
                val targetFile = File(targetDir, filename)
                connection.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                android.util.Log.d("ADMIN_SERVER", "Successfully downloaded $filename from $peerIp")
                targetFile.absolutePath
            } else {
                android.util.Log.e("ADMIN_SERVER", "Failed to download $filename from $peerIp. Code: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ADMIN_SERVER", "Error downloading $filename from $peerIp", e)
            null
        }
    }

    private fun adminPage(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val config = repo.load()
        val isSaved = session.parameters["saved"] != null
        val syncStatus = session.parameters["sync_status"]?.firstOrNull() // success, partial, failed
        
        val statusMsg = when {
            syncStatus == "success" -> "✅ Penyimpanan & Sinkronisasi SUKSES ke semua perangkat."
            syncStatus == "partial" -> "⚠️ Penyimpanan sukses, tapi sebagian perangkat gagal dihubungi."
            syncStatus?.startsWith("failed") == true -> "❌ Penyimpanan sukses, tapi GAGAL sinkronisasi.\\nError: " + syncStatus.substringAfter("failed:")
            else -> "Update Berhasil!\\n\\nSinkronisasi berjalan di background."
        }
        
        val successScript = if (isSaved) "<script>alert('${statusMsg} (ID: ${config.deviceId})'); window.history.replaceState({}, '', '/');</script>" else ""
        
        val html = """
            <!DOCTYPE html>
            <html lang="id">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Admin TvSholat - ${config.name}</title>
                <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;800&display=swap" rel="stylesheet">
                <link href="https://cdn.quilljs.com/1.3.6/quill.snow.css" rel="stylesheet">
                <script src="https://cdn.quilljs.com/1.3.6/quill.js"></script>
                <style>
                    .ql-editor { min-height: 200px; font-size: 16px; }
                    .editor-container { background: white; border-radius: 0 0 8px 8px; }
                    .ql-toolbar { border-radius: 8px 8px 0 0; background: #eee; }
                </style>
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
                
                <form action="/save" method="POST" enctype="multipart/form-data" id="mainForm">
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
                                <option value="elegant" ${if (config.themeName == "elegant") "selected" else ""}>✨ Elegant Premium (Big Font)</option>
                                <option value="classic" ${if (config.themeName == "classic") "selected" else ""}>🕌 Classic Green</option>
                                <option value="dashboard" ${if (config.themeName == "dashboard") "selected" else ""}>📊 Dashboard Sidebar</option>
                                <option value="grand" ${if (config.themeName == "grand") "selected" else ""}>👑 Grand Premium (Big & Clear)</option>
                                <option value="premium" ${if (config.themeName == "premium") "selected" else ""}>💎 Premium Glass (Modern)</option>
                            </select>
                        </div>
                        
                        <div class="form-group" style="margin-top: 20px; border-top: 1px dashed #ddd; padding-top: 15px;">
                            <label>Logo Masjid</label>
                            <div style="font-size: 11px; color: #666; margin-bottom: 8px;">Tampil di sebelah nama masjid pada semua tema.</div>
                            
                            <div style="display: flex; gap: 15px; align-items: flex-start;">
                                <div style="flex: 1;">
                                    <div style="margin-bottom: 8px; display: flex; gap: 15px;">
                                        <label style="font-weight: 400; font-size: 12px; cursor: pointer; display: flex; align-items: center;">
                                            <input type="radio" name="logo_type" value="url" ${if (config.logoType == "url") "checked" else ""} onchange="toggleLogoInput()" style="width: auto; margin-right: 5px;"> URL
                                        </label>
                                        <label style="font-weight: 400; font-size: 12px; cursor: pointer; display: flex; align-items: center;">
                                            <input type="radio" name="logo_type" value="upload" ${if (config.logoType == "upload") "checked" else ""} onchange="toggleLogoInput()" style="width: auto; margin-right: 5px;"> Upload
                                        </label>
                                    </div>
                                    
                                    <div id="logoUrlInput" style="display: ${if (config.logoType == "url") "block" else "none"};">
                                        <input name="logo_url" id="logoUrlField" value="${config.logoUrl}" placeholder="https://example.com/logo.png" oninput="updateLogoPreview(this.value)" style="font-size: 12px;">
                                    </div>
                                    <div id="logoUploadInput" style="display: ${if (config.logoType == "upload") "block" else "none"};">
                                        <input type="file" name="logo_file" accept="image/*" onchange="handleLogoFileSelect(event)" style="font-size: 12px;">
                                    </div>
                                    <input type="hidden" name="logo_local_path" id="logoLocalPath" value="${config.logoLocalPath}">
                                </div>
                                
                                <div style="width: 80px; height: 80px; border: 1px solid #ddd; border-radius: 8px; overflow: hidden; background: #f9f9f9; display: flex; align-items: center; justify-content: center;">
                                    <img id="logoPreview" src="${if (config.logoLocalPath.isNotEmpty()) getFilesUrl(config.logoLocalPath) else if (config.logoUrl.isNotEmpty()) config.logoUrl else "https://via.placeholder.com/80?text=Logo"}" style="max-width: 100%; max-height: 100%; object-fit: contain;">
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="card">
                        <h3>⏱️ Konfigurasi Sholat</h3>
                        <div class="grid">
                            <div class="form-group">
                                <label>Durasi Sholat (Menit)</label>
                                <input type="number" name="sholat_duration" value="${config.sholatDurationMinutes}" required>
                                <small style="color: #666; font-size: 11px;">Layar hitam setelah iqomah.</small>
                            </div>
                        </div>
                        
                        <div style="margin-top: 15px; border-top: 1px dashed #eee; padding-top: 15px;">
                            <label style="font-weight: 600; font-size: 13px; color: var(--primary); display: block; margin-bottom: 10px;">Jeda Iqomah per Waktu (Menit)</label>
                            
                            <div style="display: flex; gap: 10px; align-items: flex-end; margin-bottom: 15px; background: #f1f8e9; padding: 10px; border-radius: 8px;">
                                <div style="flex: 1;">
                                    <label style="font-size: 11px; color: #555;">Set Semua Jadwal:</label>
                                    <input type="number" id="iqomah_all_val" placeholder="Menit" style="padding: 6px 10px;">
                                </div>
                                <button type="button" onclick="setAllIqomah()" style="width: auto; padding: 8px 15px; margin: 0; font-size: 12px; background: var(--primary-light);">Terapkan ke Semua</button>
                            </div>

                            <div class="grid" style="grid-template-columns: 1fr 1fr 1fr;">
                                <div class="form-group">
                                    <label>Subuh</label>
                                    <input type="number" name="iqomah_subuh" value="${config.iqomahSubuh}" class="iqomah-input">
                                </div>
                                <div class="form-group">
                                    <label>Dzuhur</label>
                                    <input type="number" name="iqomah_dzuhur" value="${config.iqomahDzuhur}" class="iqomah-input">
                                </div>
                                <div class="form-group">
                                    <label>Ashar</label>
                                    <input type="number" name="iqomah_ashar" value="${config.iqomahAshar}" class="iqomah-input">
                                </div>
                                <div class="form-group">
                                    <label>Maghrib</label>
                                    <input type="number" name="iqomah_maghrib" value="${config.iqomahMaghrib}" class="iqomah-input">
                                </div>
                                <div class="form-group">
                                    <label>Isya</label>
                                    <input type="number" name="iqomah_isya" value="${config.iqomahIsya}" class="iqomah-input">
                                </div>
                                <div class="form-group">
                                    <label>Juma'at</label>
                                    <input type="number" name="iqomah_jumat" value="${config.iqomahJumat}" class="iqomah-input">
                                </div>
                            </div>
                            <!-- Hidden input for backward compatibility or default -->
                            <input type="hidden" name="iqomah" id="iqomah_default" value="${config.iqomahMinutes}">
                        </div>

                        <div style="margin-top: 15px; border-top: 1px dashed #eee; padding-top: 15px;">
                            <label style="display: flex; align-items: center; cursor: pointer;">
                                <input type="checkbox" name="enable_tarhim" style="width: auto; margin-right: 10px;" value="true" ${if (config.enableTarhim) "checked" else ""}>
                                <div>
                                    <span style="font-weight: 600; display: block;">Aktifkan Sholawat Tarhim</span>
                                    <span style="font-size: 11px; color: #666; font-weight: normal;">Menampilkan teks Arab & Terjemahan Sholawat Tarhim otomatis saat waktu IMSAK tiba.</span>
                                </div>
                            </label>
                            
                            <div style="margin-top: 12px; padding-left: 28px; border-left: 2px solid #e0e0e0; margin-left: 10px;">
                                <label style="font-size: 11px; color: #555; font-weight: 600; display: block; margin-bottom: 5px;">Suara Sholawat Tarhim (Optional)</label>
                                <div style="display: flex; gap: 15px; margin-bottom: 8px;">
                                    <label style="font-weight: 400; font-size: 11px; cursor: pointer; display: flex; align-items: center;">
                                        <input type="radio" name="tarhim_audio_type" value="url" ${if (config.tarhimAudioType == "url") "checked" else ""} onchange="toggleTarhimAudioInput()" style="width: auto; margin-right: 5px;"> URL
                                    </label>
                                    <label style="font-weight: 400; font-size: 11px; cursor: pointer; display: flex; align-items: center;">
                                        <input type="radio" name="tarhim_audio_type" value="upload" ${if (config.tarhimAudioType == "upload") "checked" else ""} onchange="toggleTarhimAudioInput()" style="width: auto; margin-right: 5px;"> Upload File
                                    </label>
                                </div>
                                <div id="tarhimAudioUrlInput" style="display: ${if (config.tarhimAudioType == "url") "block" else "none"};">
                                    <input name="tarhim_audio_url" value="${config.tarhimAudioUrl}" placeholder="https://example.com/tarhim.mp3" style="font-size: 12px; padding: 8px;">
                                </div>
                                <div id="tarhimAudioUploadInput" style="display: ${if (config.tarhimAudioType == "upload") "block" else "none"};">
                                    <input type="file" name="tarhim_audio_file" accept="audio/*" style="font-size: 11px;">
                                    ${if (config.tarhimAudioLocalPath.isNotEmpty()) """<div style="font-size: 10px; color: #2e7d32; margin-top: 4px;">✅ File tersimpan secara lokal</div>""" else ""}
                                </div>
                                <input type="hidden" name="tarhim_audio_local_path" value="${config.tarhimAudioLocalPath}">
                            </div>
                        </div>

                        <!-- 🚀 UPDATE APLIKASI (OTA) -->
                        <div style="margin-top: 15px; border-top: 2px solid var(--primary); padding-top: 15px;">
                            <h3 style="color: var(--primary); margin-bottom: 10px;">🚀 Update Aplikasi Otomatis</h3>
                            <p style="font-size: 11px; color: #666; margin-bottom: 12px;">Upload file APK baru di sini. Versi baru akan otomatis dikirim dan diinstall di semua TV Slave yang terhubung.</p>
                            
                            <div style="background: #e3f2fd; padding: 15px; border-radius: 8px; border: 1px solid #bbdefb;">
                                <div class="form-group" style="margin-bottom: 0;">
                                    <label style="font-weight: 600;">Upload File APK (.apk)</label>
                                    <input type="file" name="apk_file" accept=".apk" style="font-size: 12px;">
                                    <div style="font-size: 10px; color: #1976d2; margin-top: 5px;">
                                        Versi Saat Ini: <b>v${BuildConfig.VERSION_NAME}</b>
                                        ${if (config.latestApkVersionCode > 0) "<br>File Terupload: " + config.latestApkLocalPath.substringAfterLast("/") else ""}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="card" style="border: 1px solid ${if (config.isActivated) "#81c784" else "#ef9a9a"}; background: ${if (config.isActivated) "#fafffa" else "#fffafa"}; overflow: hidden; position: relative;">
                        <h3 style="color: ${if (config.isActivated) "#2e7d32" else "#c62828"};">📱 Informasi Lisensi</h3>
                        <div class="grid">
                            <div class="form-group">
                                <label>Nomor Seri (Device ID)</label>
                                <div style="font-family: monospace; font-weight: 800; font-size: 17px; color: #333; letter-spacing: 1px;">${config.deviceId}</div>
                            </div>
                            <div class="form-group">
                                <label>Status Perangkat</label>
                                <div style="margin-top: 5px;">
                                    <span style="display: inline-block; padding: 5px 15px; border-radius: 20px; font-weight: 800; font-size: 13px; background: ${if (config.isActivated) "#e8f5e9" else "#ffebee"}; color: ${if (config.isActivated) "#2e7d32" else "#c62828"}; border: 1px solid ${if (config.isActivated) "#81c784" else "#ef9a9a"};">
                                        ${if (config.isActivated) "✅ TERAKTIVASI" else "❌ BELUM AKTIF"}
                                    </span>
                                </div>
                            </div>
                        </div>
                        <p style="font-size: 11px; color: var(--muted); margin: 10px 0 0 0; font-style: italic;">
                            ${if (config.isActivated) "Lisensi aktif. Perangkat Anda sudah terdaftar di sistem." else "Gunakan Nomor Seri di atas untuk aktivasi melalui Admin Telegram."}
                        </p>
                        <input type="hidden" name="is_activated" value="${config.isActivated}">
                            <input type="hidden" name="device_id" value="${config.deviceId}">
                            <input type="hidden" name="enable_power_saving_real" id="enable_power_saving_real" value="${config.enablePowerSaving}">
                        </div>

                    <div class="card">
                        <h3>🌐 Perangkat Terhubung (Satu Jaringan)</h3>
                        <p style="font-size: 12px; color: var(--muted); margin-top: -10px; margin-bottom: 15px;">
                            Daftar TV lain yang akan otomatis tersinkronisasi saat Anda menyimpan konfigurasi.
                        </p>
                        <div id="peersList" style="border: 1px solid #eee; border-radius: 8px; overflow: hidden;">
                            <div style="padding: 15px; text-align: center; color: var(--muted);">
                                Memindai perangkat...
                            </div>
                        </div>
                        <button type="button" onclick="fetchPeers()" style="margin-top: 10px; background: #fff; color: var(--primary); border: 1px solid var(--primary);">
                            🔄 Refresh Daftar Perangkat
                        </button>
                         <div style="margin-top: 20px; padding-top: 15px; border-top: 1px dashed #eee;">
                            <label style="font-size:13px; color:#555;">Tambah Perangkat Manual (IP Address)</label>
                            <div style="margin-top:5px; display:flex; gap:10px;">
                                <input id="newPeerIp" placeholder="Contoh: 192.168.1.10" style="flex:1;">
                                <button type="button" onclick="addPeer()" style="width:auto; margin:0; padding:10px 20px;">+ Tambah</button>
                            </div>
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
                        <div style="margin-top: 15px; border-top: 1px dashed #eee; padding-top: 15px;">
                            <label style="display: flex; align-items: center; cursor: pointer;">
                                <input type="checkbox" name="is_time_master" style="width: auto; margin-right: 10px;" value="true" ${if (config.isTimeMaster) "checked" else ""}>
                                <div>
                                    <span style="font-weight: 600; display: block;">Jadikan Pusat Waktu (Master)</span>
                                    <span style="font-size: 11px; color: #666; font-weight: normal;">Centang jika TV ini adalah acuan waktu untuk TV lain (Biar detik sinkron).</span>
                                </div>
                            </label>
                        </div>
                        <div style="margin-top: 15px; border-top: 1px dashed #eee; padding-top: 15px;">
                            <label style="display: flex; align-items: center; cursor: pointer;">
                                <input type="checkbox" name="enable_power_saving" style="width: auto; margin-right: 10px;" value="true" ${if (config.enablePowerSaving) "checked" else ""}>
                                <div>
                                    <span style="font-weight: 600; display: block;">Mode Hemat Daya (Standby)</span>
                                    <span style="font-size: 11px; color: #666; font-weight: normal;">Layar TV otomatis hitam jika jauh dari waktu sholat (Hemat umur TV).</span>
                                </div>
                            </label>
                            
                            <div style="margin-top: 10px; padding-left: 28px; display: flex; gap: 15px;">
                                <div style="flex: 1;">
                                    <label style="font-size: 11px; color: #555;">Nyala SEBELUM Sholat (Menit)</label>
                                    <input type="number" name="power_saving_pre" value="${config.powerSavingPreMinutes}" placeholder="Default: 60" style="padding: 6px 10px; font-size: 12px;">
                                </div>
                                <div style="flex: 1;">
                                    <label style="font-size: 11px; color: #555;">Nyala SETELAH Sholat (Menit)</label>
                                    <input type="number" name="power_saving_post" value="${config.powerSavingPostMinutes}" placeholder="Default: 60" style="padding: 6px 10px; font-size: 12px;">
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="card">
                        <h3>Konten</h3>
                        <div class="form-group">
                            <label>Teks Berjalan</label>
                            <textarea name="running_text" rows="3">${config.runningText}</textarea>
                        </div>
                        
                        <div class="form-group">
                            <label>Background Layar</label>
                            <div style="margin-bottom: 10px;">
                                <label style="display: inline-flex; align-items: center; margin-right: 20px; font-weight: 400;">
                                    <input type="radio" name="bg_type" value="url" ${if (config.backgroundType == "url") "checked" else ""} onchange="toggleBgInput()"> 
                                    <span style="margin-left: 6px;">URL Gambar</span>
                                </label>
                                <label style="display: inline-flex; align-items: center; font-weight: 400;">
                                    <input type="radio" name="bg_type" value="upload" ${if (config.backgroundType == "upload") "checked" else ""} onchange="toggleBgInput()"> 
                                    <span style="margin-left: 6px;">Upload Foto</span>
                                </label>
                            </div>
                            
                            <div id="urlInput" style="display: ${if (config.backgroundType == "url") "block" else "none"};">
                                <input name="bg_url" id="bgUrlField" oninput="updatePreview(this.value)" value="${config.backgroundUrl}" placeholder="https://example.com/gambar.jpg">
                            </div>
                            
                            <div id="uploadInput" style="display: ${if (config.backgroundType == "upload") "block" else "none"};">
                                <input type="file" name="bg_file" id="bgFileField" accept="image/*" onchange="handleFileSelect(event)" style="margin-bottom: 8px;">
                                <div style="font-size: 12px; color: var(--muted);">Foto akan otomatis di-compress (max 1920px, HD quality)</div>
                            </div>
                            
                            <input type="hidden" name="bg_local_path" id="bgLocalPath" value="${config.backgroundLocalPath}">
                            
                            <div class="preview-box" style="margin-top: 10px;">
                                <img id="preview" src="${if (config.backgroundType == "upload" && config.backgroundLocalPath.isNotEmpty()) getFilesUrl(config.backgroundLocalPath) else config.backgroundUrl}" onerror="this.src='https://via.placeholder.com/400x200?text=Preview'">
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

                    <div class="card">
                        <h3>Papan Informasi</h3>
                        <div class="grid">
                            <div class="form-group">
                                <label>Interval (Menit)</label>
                                <input name="info_interval" type="number" value="${config.infoDisplayInterval}">
                            </div>
                            <div class="form-group">
                                <label>Durasi Total (Detik)</label>
                                <input name="info_duration" type="number" value="${config.infoDisplayDuration}">
                            </div>
                        </div>
                        
                        <div id="infoItemsContainer" style="margin-top:15px; border-top: 1px solid #eee; padding-top:15px;">
                            ${
                                config.infoItems.mapIndexed { index, item ->
                                    val safeContent = item.content.replace("\"", "&quot;")
                                    val safeTitle = item.title.replace("\"", "&quot;")
                                    """
                                    <div class="info-item" id="info_item_$index" style="margin-bottom: 20px; padding: 15px; background: #f9f9f9; border-radius: 12px; border: 1px solid #eee; position: relative;">
                                        <button type="button" onclick="removeInfoItem($index)" style="position: absolute; top: 10px; right: 10px; background: #ffebee; color: #c62828; border: none; padding: 5px 10px; border-radius: 6px; font-size: 11px; width: auto; box-shadow: none;">hapus</button>
                                        
                                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
                                            <label style="color:#1b5e20; font-weight: 800; font-size: 16px;">Info #${index + 1}</label>
                                            <div style="background: #eee; padding: 4px; border-radius: 8px; display: flex; gap: 4px; margin-right: 50px;">
                                                <label style="font-size: 12px; cursor: pointer; padding: 4px 10px; border-radius: 6px; display: flex; align-items: center;" id="label_text_$index">
                                                    <input type="radio" name="info_type_$index" value="text" ${if (item.type == "text") "checked" else ""} onchange="updateInfoUI($index)" style="margin-right: 5px;"> Teks Kaya
                                                </label>
                                                <label style="font-size: 12px; cursor: pointer; padding: 4px 10px; border-radius: 6px; display: flex; align-items: center;" id="label_image_$index">
                                                    <input type="radio" name="info_type_$index" value="image" ${if (item.type == "image") "checked" else ""} onchange="updateInfoUI($index)" style="margin-right: 5px;"> Poster (Full)
                                                </label>
                                            </div>
                                        </div>
                                        
                                        <div id="title_group_$index" style="margin-bottom: 10px; ${if (item.type == "image") "display:none;" else ""}">
                                            <label style="font-size: 12px; color: #666; display: block; margin-bottom: 4px;">Judul Info</label>
                                            <input name="info_title_$index" value="$safeTitle" placeholder="Judul Info" style="margin-bottom: 0;">
                                        </div>
                                        
                                        <div id="text_editor_group_$index" style="${if (item.type == "image") "display:none;" else ""}">
                                            <label style="font-size: 12px; color: #666; display: block; margin-bottom: 4px;">Isi Konten (Rich Text)</label>
                                            <div id="editor_$index" class="editor-container">${if (item.type == "text") item.content else ""}</div>
                                        </div>

                                        <div id="image_url_group_$index" style="${if (item.type == "text") "display:none;" else ""}">
                                            <label style="font-size: 12px; color: #666; display: block; margin-bottom: 4px;">Poster Gambar (Upload / URL)</label>
                                            <div style="display: flex; flex-direction: column; gap: 8px;">
                                                <input type="file" name="info_file_$index" id="info_file_$index" accept="image/*" onchange="handleInfoFileSelect(event, $index)" style="font-size: 12px;">
                                                 <div style="display: flex; gap: 8px;">
                                                    <input id="image_url_$index" value="${if (item.type == "image") (if (item.content.startsWith("/")) getFilesUrl(item.content) else safeContent) else ""}" placeholder="Atau masukkan URL: https://example.com/poster.jpg" style="margin-bottom: 0; flex: 1;">
                                                    <button type="button" onclick="previewInfo($index)" style="margin:0; padding: 0 15px; width: auto; background: #2196f3;">Preview</button>
                                                </div>
                                            </div>
                                            <p style="font-size: 10px; color: #888; margin-top: 4px;">*Pilih file untuk upload poster baru atau masukkan URL gambar.</p>
                                        </div>
                                        
                                        <div id="preview_area_$index" style="margin-top: 10px; display: none; padding: 10px; background: #eee; border-radius: 8px; text-align: center;">
                                            <label style="font-size: 10px; color: #666; display: block; margin-bottom: 5px;">LIVE PREVIEW</label>
                                            <img id="preview_img_$index" src="" style="max-width: 100%; max-height: 200px; border-radius: 4px; display: none;">
                                            <div id="preview_text_$index" style="display: none; background: white; padding: 10px; border-radius: 4px; text-align: left; font-size: 12px;"></div>
                                        </div>

                                        <input type="hidden" name="info_content_$index" id="content_$index" value="">
                                        <input type="hidden" name="info_real_type_$index" id="real_type_$index" value="${item.type}">
                                        <input type="hidden" name="info_index" value="$index"> <!-- Marker for processing -->
                                    </div>
                                    """
                                }.joinToString("\n")
                            }
                        </div>
                        
                        <button type="button" onclick="addInfoItem()" style="background: #e3f2fd; color: #1565c0; border: 1px dashed #1565c0; margin-top: 10px;">+ Tambah Info Baru</button>
                    </div>


                    <div class="grid">
                        <button type="submit" name="sync_mode" value="local" style="background: #7f8c8d;">💾 Simpan (Lokal)</button>
                        <button type="submit" name="sync_mode" value="broadcast" style="background: var(--primary);">📡 Simpan & Sinkronisasi</button>
                    </div>
                    <div style="height: 40px;"></div>
                </form>
            </div>
            <script>
                function toggleBgInput() {
                    const type = document.querySelector('input[name="bg_type"]:checked').value;
                    document.getElementById('urlInput').style.display = type === 'url' ? 'block' : 'none';
                    document.getElementById('uploadInput').style.display = type === 'upload' ? 'block' : 'none';
                    
                    // Update preview
                    if (type === 'url') {
                        updatePreview(document.getElementById('bgUrlField').value);
                    }
                }
                
                function updatePreview(url) {
                    document.getElementById('preview').src = url || 'https://via.placeholder.com/400x200?text=Preview';
                }

                function toggleLogoInput() {
                    const type = document.querySelector('input[name="logo_type"]:checked').value;
                    document.getElementById('logoUrlInput').style.display = type === 'url' ? 'block' : 'none';
                    document.getElementById('logoUploadInput').style.display = type === 'upload' ? 'block' : 'none';
                }

                function updateLogoPreview(url) {
                    document.getElementById('logoPreview').src = url || 'https://via.placeholder.com/80?text=Logo';
                }

                function toggleTarhimAudioInput() {
                    const type = document.querySelector('input[name="tarhim_audio_type"]:checked').value;
                    document.getElementById('tarhimAudioUrlInput').style.display = type === 'url' ? 'block' : 'none';
                    document.getElementById('tarhimAudioUploadInput').style.display = type === 'upload' ? 'block' : 'none';
                }

                function handleLogoFileSelect(event) {
                    const file = event.target.files[0];
                    if (file) {
                        const reader = new FileReader();
                        reader.onload = function(e) {
                            document.getElementById('logoPreview').src = e.target.result;
                        };
                        reader.readAsDataURL(file);
                    }
                }
                
                function handleFileSelect(event) {
                    const file = event.target.files[0];
                    if (file) {
                        // Show preview
                        const reader = new FileReader();
                        reader.onload = function(e) {
                            document.getElementById('preview').src = e.target.result;
                        };
                        reader.readAsDataURL(file);
                    }
                }

                function handleInfoFileSelect(event, index) {
                    const file = event.target.files[0];
                    if (file) {
                        const reader = new FileReader();
                        reader.onload = function(e) {
                            document.getElementById('preview_img_' + index).src = e.target.result;
                            document.getElementById('preview_area_' + index).style.display = 'block';
                            document.getElementById('preview_img_' + index).style.display = 'block';
                            document.getElementById('preview_text_' + index).style.display = 'none';
                        };
                        reader.readAsDataURL(file);
                    }
                }
                
                // Quill Initialization
                const editors = {};
                
                function initQuill(index, content) {
                     const quill = new Quill('#editor_' + index, {
                        theme: 'snow',
                        modules: {
                            toolbar: [
                                [{'header': [1, 2, 3, false]}],
                                ['bold', 'italic', 'underline', 'strike'],
                                [{'color': []}, {'background': []}],
                                [{'list': 'ordered'}, {'list': 'bullet'}],
                                ['link', 'image'],
                                ['clean']
                            ]
                        }
                    });
                    if (content) quill.root.innerHTML = content;
                    editors[index] = quill;
                }

                // Init existing editors
                ${config.infoItems.indices.joinToString("\n") { "initQuill($it, '');" } }
                
                // --- DYNAMIC ITEMS LOGIC ---
                let nextIndex = ${config.infoItems.size};
                
                function addInfoItem() {
                    const container = document.getElementById('infoItemsContainer');
                    const index = nextIndex++;
                    
                    const html = `
                        <div class="info-item" id="info_item_${'$'}{index}" style="margin-bottom: 20px; padding: 15px; background: #f9f9f9; border-radius: 12px; border: 1px solid #eee; position: relative;">
                             <button type="button" onclick="removeInfoItem(${'$'}{index})" style="position: absolute; top: 10px; right: 10px; background: #ffebee; color: #c62828; border: none; padding: 5px 10px; border-radius: 6px; font-size: 11px; width: auto; box-shadow: none;">hapus</button>
                             
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
                                <label style="color:#1b5e20; font-weight: 800; font-size: 16px;">Info #${'$'}{index + 1} (Baru)</label>
                                <div style="background: #eee; padding: 4px; border-radius: 8px; display: flex; gap: 4px; margin-right: 50px;">
                                    <label style="font-size: 12px; cursor: pointer; padding: 4px 10px; border-radius: 6px; display: flex; align-items: center;">
                                        <input type="radio" name="info_type_${'$'}{index}" value="text" checked onchange="updateInfoUI(${'$'}{index})" style="margin-right: 5px;"> Teks Kaya
                                    </label>
                                    <label style="font-size: 12px; cursor: pointer; padding: 4px 10px; border-radius: 6px; display: flex; align-items: center;">
                                        <input type="radio" name="info_type_${'$'}{index}" value="image" onchange="updateInfoUI(${'$'}{index})" style="margin-right: 5px;"> Poster (Full)
                                    </label>
                                </div>
                            </div>
                            
                            <div id="title_group_${'$'}{index}" style="margin-bottom: 10px;">
                                <label style="font-size: 12px; color: #666; display: block; margin-bottom: 4px;">Judul Info</label>
                                <input name="info_title_${'$'}{index}" placeholder="Judul Info" style="margin-bottom: 0;">
                            </div>
                            
                            <div id="text_editor_group_${'$'}{index}">
                                <label style="font-size: 12px; color: #666; display: block; margin-bottom: 4px;">Isi Konten (Rich Text)</label>
                                <div id="editor_${'$'}{index}" class="editor-container"></div>
                            </div>

                            <div id="image_url_group_${'$'}{index}" style="display:none;">
                                <label style="font-size: 12px; color: #666; display: block; margin-bottom: 4px;">Poster Gambar (Upload / URL)</label>
                                <div style="display: flex; flex-direction: column; gap: 8px;">
                                    <input type="file" name="info_file_${'$'}{index}" id="info_file_${'$'}{index}" accept="image/*" onchange="handleInfoFileSelect(event, ${'$'}{index})" style="font-size: 12px;">
                                    <div style="display: flex; gap: 8px;">
                                        <input id="image_url_${'$'}{index}" placeholder="Atau masukkan URL: https://example.com/poster.jpg" style="margin-bottom: 0; flex: 1;">
                                        <button type="button" onclick="previewInfo(${'$'}{index})" style="margin:0; padding: 0 15px; width: auto; background: #2196f3;">Preview</button>
                                    </div>
                                </div>
                                <p style="font-size: 10px; color: #888; margin-top: 4px;">*Pilih file untuk upload poster baru atau masukkan URL gambar.</p>
                            </div>
                            
                            <div id="preview_area_${'$'}{index}" style="margin-top: 10px; display: none; padding: 10px; background: #eee; border-radius: 8px; text-align: center;">
                                <img id="preview_img_${'$'}{index}" src="" style="max-width: 100%; max-height: 200px; border-radius: 4px; display: none;">
                            </div>

                            <input type="hidden" name="info_content_${'$'}{index}" id="content_${'$'}{index}" value="">
                            <input type="hidden" name="info_real_type_${'$'}{index}" id="real_type_${'$'}{index}" value="text">
                            <input type="hidden" name="info_index" value="${'$'}{index}">
                        </div>
                    `;
                    
                    // Append HTML safely
                    const div = document.createElement('div');
                    div.innerHTML = html;
                    container.appendChild(div.firstElementChild);
                    
                    // Init Quill for new item
                    initQuill(index, '');
                }

                function removeInfoItem(index) {
                    if (confirm('Hapus info ini?')) {
                        const el = document.getElementById('info_item_' + index);
                        if (el) el.remove();
                        // Note: We don't remove from `editors` object to avoid index collision, just let it be.
                        // The form submission logic only checks for existing `info_index` inputs.
                    }
                }

                function setAllIqomah() {
                    const val = document.getElementById('iqomah_all_val').value;
                    if (!val) return;
                    document.querySelectorAll('.iqomah-input').forEach(input => {
                        input.value = val;
                    });
                    document.getElementById('iqomah_default').value = val;
                }

                document.getElementById('mainForm').onsubmit = function() {
                    // Force Sync Power Saving Checkbox to Hidden Input
                    const psCheckbox = document.querySelector('input[name="enable_power_saving"]');
                    if (psCheckbox) {
                         document.getElementById('enable_power_saving_real').value = psCheckbox.checked ? 'true' : 'false';
                    }

                    // Update default iqomah from subuh field if not explicitly set
                    document.getElementById('iqomah_default').value = document.getElementsByName('iqomah_subuh')[0].value;
                    
                    // Collect all visible indices
                    const indices = Array.from(document.querySelectorAll('input[name="info_index"]')).map(el => el.value);
                    
                    indices.forEach(index => {
                        const typeInput = document.querySelector('input[name="info_type_' + index + '"]:checked');
                        if (!typeInput) return; // Should not happen
                        
                        const type = typeInput.value;
                        if (type === 'image') {
                            const raw = document.getElementById('image_url_' + index).value;
                            document.getElementById('content_' + index).value = extractImageUrl(raw);
                        } else {
                            if (editors[index]) {
                                const html = editors[index].root.innerHTML;
                                document.getElementById('content_' + index).value = html === '<p><br></p>' ? '' : html;
                            }
                        }
                        document.getElementById('real_type_' + index).value = type;
                    });
                };

                function updateInfoUI(index) {
                    const type = document.querySelector('input[name="info_type_' + index + '"]:checked').value;
                    const titleGroup = document.getElementById('title_group_' + index);
                    const editorGroup = document.getElementById('text_editor_group_' + index);
                    const imageGroup = document.getElementById('image_url_group_' + index);
                    
                    if (type === 'image') {
                        titleGroup.style.display = 'none';
                        editorGroup.style.display = 'none';
                        imageGroup.style.display = 'block';
                    } else {
                        titleGroup.style.display = 'block';
                        editorGroup.style.display = 'block';
                        imageGroup.style.display = 'none';
                    }
                }

                function extractImageUrl(html) {
                    if (!html) return '';
                    if (html.startsWith('http') || html.startsWith('/') || html.startsWith('file')) return html;
                    const match = html.match(/src=["']([^"']+)["']/);
                    return match ? match[1] : html;
                }

                function previewInfo(index) {
                    const type = document.querySelector('input[name="info_type_' + index + '"]:checked').value;
                    const area = document.getElementById('preview_area_' + index);
                    const img = document.getElementById('preview_img_' + index);
                    const txt = document.getElementById('preview_text_' + index);
                    
                    area.style.display = 'block';
                    if (type === 'image') {
                         const url = extractImageUrl(document.getElementById('image_url_' + index).value);
                         img.src = url;
                         img.style.display = 'block';
                         txt.style.display = 'none';
                    } else {
                         txt.innerHTML = editors[index].root.innerHTML;
                         txt.style.display = 'block';
                         img.style.display = 'none';
                    }
                }
                
                // Set initial UI
                [0, 1, 2].forEach(index => updateInfoUI(index));

                // Fetch Peers
                fetchPeers();
                
                function fetchPeers() {
                    fetch('/peers')
                        .then(response => response.json())
                        .then(data => {
                            const list = document.getElementById('peersList');
                            if (data.length === 0) {
                                list.innerHTML = '<div style="padding:10px; color:#7f8c8d; font-style:italic;">Belum ada perangkat lain terhubung.</div>';
                                return;
                            }
                            
                            let html = '<table style="width:100%; border-collapse:collapse;">';
                            html += '<tr style="background:#f1f1f1; text-align:left;"><th style="padding:8px; border-bottom:1px solid #ddd;">IP Address</th><th style="padding:8px; border-bottom:1px solid #ddd;">Device ID / Status</th><th style="padding:8px; border-bottom:1px solid #ddd; width:80px;">Aksi</th></tr>';
                            
                            data.forEach(peer => {
                                html += '<tr>';
                                html += '<td style="padding:8px; border-bottom:1px solid #eee;">' + peer.ip + '</td>';
                                html += '<td style="padding:8px; border-bottom:1px solid #eee; font-family:monospace;">' + peer.deviceId + '</td>';
                                html += '<td style="padding:8px; border-bottom:1px solid #eee;">';
                                html += '<button type="button" onclick="testPeer(\'' + peer.ip + '\')" style="margin-right:5px; padding:4px 8px; background:#2196f3; color:white; border:none; border-radius:4px; font-size:11px; cursor:pointer;">Test</button>';
                                html += '<button type="button" onclick="removePeer(\'' + peer.ip + '\')" style="margin:0; padding:4px 8px; background:#e53935; color:white; border:none; border-radius:4px; font-size:11px; cursor:pointer;">Hapus</button>';
                                
                                if (peer.deviceId !== 'MANUAL') {
                                     html += '<span style="font-size:10px; color:green; margin-left:5px;">(Auto)</span>';
                                }
                                html += '</td></tr>';
                            });
                            
                            html += '</table>';
                            list.innerHTML = html;
                        })
                        .catch(err => {
                            console.error('Error fetching peers:', err);
                            document.getElementById('peersList').innerHTML = '<div style="color:red;">Gagal memuat daftar perangkat.</div>';
                        });
                }

                function addPeer() {
                    const ip = document.getElementById('newPeerIp').value;
                    if (!ip) return;
                    
                    fetch('/peers/manage', {
                        method: 'POST',
                        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                        body: 'action=add&ip=' + encodeURIComponent(ip)
                    }).then(() => {
                        document.getElementById('newPeerIp').value = '';
                        fetchPeers();
                    });
                }

                function removePeer(ip) {
                    if (!confirm('Apakah Anda yakin ingin menghapus/mengabaikan perangkat ' + ip + '?')) return;
                    
                    fetch('/peers/manage', {
                        method: 'POST',
                        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                        body: 'action=remove&ip=' + encodeURIComponent(ip)
                    }).then(() => {
                        fetchPeers();
                    });
                }
                
                function testPeer(ip) {
                    const btn = event.target;
                    const originalText = btn.innerText;
                    btn.innerText = '...';
                    btn.disabled = true;
                    
                    fetch('/peers/test?ip=' + encodeURIComponent(ip))
                        .then(response => response.text())
                        .then(msg => {
                            alert(msg);
                            btn.innerText = originalText;
                            btn.disabled = false;
                        })
                        .catch(err => {
                            alert('Error: ' + err);
                            btn.innerText = originalText;
                            btn.disabled = false;
                        });
                }
            </script>
            </body>
            </html>
        """.trimIndent()
        return newFixedLengthResponse(NanoHTTPD.Response.Status.OK, MIME_HTML, html)
    }

    private fun getFilesUrl(localPath: String): String {
        if (localPath.isEmpty()) return ""
        val filesDir = context.filesDir.absolutePath
        if (localPath.startsWith(filesDir)) {
            val relative = localPath.substring(filesDir.length)
            return "/files$relative"
        }
        return localPath
    }
    
    private fun saveConfig(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        return try {
            val files = HashMap<String, String>()
            session.parseBody(files)
            val p = session.parameters
            val isFromSync = session.headers["x-sync-source"] == "true"
            val oldConfig = repo.load()
            
            val pIsActivated = p["is_activated"]?.firstOrNull()?.toBoolean() ?: oldConfig.isActivated
            val pDeviceId = p["device_id"]?.firstOrNull() ?: oldConfig.deviceId
            
                // Ultra-robust checkbox logic
                // Cek di segala penjuru: parameters, files, query string?
                // NanoHTTPD kadang naruh checkbox di 'params' tapi listnya null kalau cuma keys.
                val powerSavingParam = p["enable_power_saving"]
                val enablePowerSavingVal = 
                    p["enable_power_saving_real"]?.firstOrNull() == "true" || // HIGHEST PRIORITY: Hidden Input set by JS
                    powerSavingParam?.firstOrNull() == "true" || // Standard: key=true
                    p.containsKey("enable_power_saving") || // Standard checkbox: key present = checked
                    files.containsKey("enable_power_saving") // Weird edge case
                
                val config = oldConfig.copy(
                    enablePowerSaving = enablePowerSavingVal,
                    powerSavingPreMinutes = p["power_saving_pre"]?.firstOrNull()?.takeIf { it.isNotBlank() }?.toIntOrNull() ?: 60,
                    powerSavingPostMinutes = p["power_saving_post"]?.firstOrNull()?.takeIf { it.isNotBlank() }?.toIntOrNull() ?: 60,
                    name = p["name"]?.firstOrNull() ?: oldConfig.name,
                    address = p["address"]?.firstOrNull() ?: oldConfig.address,
                latitude = p["lat"]?.firstOrNull()?.toDoubleOrNull() ?: oldConfig.latitude,
                longitude = p["lng"]?.firstOrNull()?.toDoubleOrNull() ?: oldConfig.longitude,
                themeName = p["theme_name"]?.firstOrNull() ?: oldConfig.themeName,
                iqomahSubuh = p["iqomah_subuh"]?.firstOrNull()?.toIntOrNull() ?: oldConfig.iqomahSubuh,
                iqomahDzuhur = p["iqomah_dzuhur"]?.firstOrNull()?.toIntOrNull() ?: oldConfig.iqomahDzuhur,
                iqomahAshar = p["iqomah_ashar"]?.firstOrNull()?.toIntOrNull() ?: oldConfig.iqomahAshar,
                iqomahMaghrib = p["iqomah_maghrib"]?.firstOrNull()?.toIntOrNull() ?: oldConfig.iqomahMaghrib,
                iqomahIsya = p["iqomah_isya"]?.firstOrNull()?.toIntOrNull() ?: oldConfig.iqomahIsya,
                iqomahJumat = p["iqomah_jumat"]?.firstOrNull()?.toIntOrNull() ?: oldConfig.iqomahJumat,
                sholatDurationMinutes = p["sholat_duration"]?.firstOrNull()?.toIntOrNull() ?: oldConfig.sholatDurationMinutes,
                backgroundUrl = p["bg_url"]?.firstOrNull() ?: oldConfig.backgroundUrl,
                backgroundType = p["bg_type"]?.firstOrNull() ?: oldConfig.backgroundType,
                logoUrl = p["logo_url"]?.firstOrNull() ?: oldConfig.logoUrl,
                logoType = p["logo_type"]?.firstOrNull() ?: oldConfig.logoType,
                runningText = p["running_text"]?.firstOrNull() ?: oldConfig.runningText,
                timeOffsetMinutes = p["time_offset"]?.firstOrNull()?.toIntOrNull() ?: oldConfig.timeOffsetMinutes,
                dateOffsetDays = p["date_offset"]?.firstOrNull()?.toIntOrNull() ?: oldConfig.dateOffsetDays,
                treasuryBalance = p["treasury_balance"]?.firstOrNull() ?: oldConfig.treasuryBalance,
                treasuryDescription = p["treasury_desc"]?.firstOrNull() ?: oldConfig.treasuryDescription,
                treasuryDisplayInterval = p["treasury_interval"]?.firstOrNull()?.toIntOrNull() ?: oldConfig.treasuryDisplayInterval,
                treasuryDisplayDuration = p["treasury_duration"]?.firstOrNull()?.toIntOrNull() ?: oldConfig.treasuryDisplayDuration,
                treasuryAccountInfo = p["treasury_account"]?.firstOrNull() ?: oldConfig.treasuryAccountInfo,
                treasuryQrisData = p["treasury_qris"]?.firstOrNull() ?: oldConfig.treasuryQrisData,
                hadithDisplayInterval = p["hadith_interval"]?.firstOrNull()?.toIntOrNull() ?: oldConfig.hadithDisplayInterval,
                hadithDisplayDuration = p["hadith_duration"]?.firstOrNull()?.toIntOrNull() ?: oldConfig.hadithDisplayDuration,
                infoDisplayInterval = p["info_interval"]?.firstOrNull()?.toIntOrNull() ?: oldConfig.infoDisplayInterval,
                infoDisplayDuration = p["info_duration"]?.firstOrNull()?.toIntOrNull() ?: oldConfig.infoDisplayDuration,
                isTimeMaster = p["is_time_master"]?.firstOrNull() == "true",
                isActivated = pIsActivated,
                deviceId = pDeviceId,
                enableTarhim = p["enable_tarhim"]?.firstOrNull() == "true",
                tarhimAudioUrl = p["tarhim_audio_url"]?.firstOrNull() ?: oldConfig.tarhimAudioUrl,
                tarhimAudioType = p["tarhim_audio_type"]?.firstOrNull() ?: oldConfig.tarhimAudioType,
                lastUpdated = SimpleDateFormat("d MMM yyyy HH:mm", Locale.forLanguageTag("id")).format(Date()),
                infoItems = p["info_index"].orEmpty().map { strIndex ->
                    val index = strIndex.toIntOrNull()
                    if (index != null) {
                        val title = p["info_title_$index"]?.firstOrNull() ?: ""
                        var content = p["info_content_$index"]?.firstOrNull() ?: ""
                        val type = p["info_real_type_$index"]?.firstOrNull() ?: "text"
                        if (type == "image" && files.containsKey("info_file_$index")) {
                            files["info_file_$index"]?.let { tempPath ->
                                val tempFile = File(tempPath)
                                if (tempFile.exists()) {
                                    compressAndSaveImage(tempFile, "info")?.let { content = it }
                                }
                            }
                        }
                        if (title.isNotEmpty() || content.isNotEmpty()) InfoItem(title, content, type) else null
                    } else null
                }.filterNotNull()
            )

            // Handle Uploads & CRITICAL: Reset local path if switching to URL
            var finalBgLocalPath = if (config.backgroundType == "url") "" else config.backgroundLocalPath
            if (config.backgroundType == "upload" && files.containsKey("bg_file")) {
                files["bg_file"]?.let { tempPath ->
                    val tempFile = File(tempPath)
                    if (tempFile.exists()) {
                        compressAndSaveImage(tempFile, "backgrounds")?.let { finalBgLocalPath = it }
                    }
                }
            }

            var finalLogoLocalPath = if (config.logoType == "url") "" else config.logoLocalPath
            if (config.logoType == "upload" && files.containsKey("logo_file")) {
                files["logo_file"]?.let { tempPath ->
                    val tempFile = File(tempPath)
                    if (tempFile.exists()) {
                        compressAndSaveImage(tempFile, "logos")?.let { finalLogoLocalPath = it }
                    }
                }
            }

            var finalAudioLocalPath = if (config.tarhimAudioType == "url") "" else config.tarhimAudioLocalPath
            if (config.tarhimAudioType == "upload" && files.containsKey("tarhim_audio_file")) {
                files["tarhim_audio_file"]?.let { tempPath ->
                    val tempFile = File(tempPath)
                    if (tempFile.exists()) {
                        saveAudioFile(tempFile)?.let { finalAudioLocalPath = it }
                    }
                }
            }

            var finalApkLocalPath = config.latestApkLocalPath
            var finalApkVersionCode = config.latestApkVersionCode
            if (files.containsKey("apk_file")) {
                files["apk_file"]?.let { tempPath ->
                    val tempFile = File(tempPath)
                    if (tempFile.exists()) {
                        saveApkFile(tempFile)?.let {
                            finalApkLocalPath = it
                            finalApkVersionCode = (System.currentTimeMillis() / 1000).toInt()
                        }
                    }
                }
            }

            val finalConfig = config.copy(
                backgroundLocalPath = finalBgLocalPath,
                logoLocalPath = finalLogoLocalPath,
                tarhimAudioLocalPath = finalAudioLocalPath,
                latestApkLocalPath = finalApkLocalPath,
                latestApkVersionCode = finalApkVersionCode
            )

            var syncStatus = ""
            runBlocking(Dispatchers.IO) {
                repo.save(finalConfig)
                if (p["sync_mode"]?.firstOrNull() == "broadcast" && !isFromSync) {
                    syncStatus = broadcastConfigToPeers(finalConfig)
                }
            }

            val response = newFixedLengthResponse(NanoHTTPD.Response.Status.REDIRECT, MIME_HTML, "")
            val loc = "/?saved=1" + if(syncStatus.isNotEmpty()) "&sync_status=$syncStatus" else ""
            response.addHeader("Location", loc)
            response
        } catch (e: Exception) {
            e.printStackTrace()
            newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "ERROR: ${e.message}")
        }
    }
    
    private fun compressAndSaveImage(sourceFile: File, subFolder: String): String? {
        return try {
            // Robust detection of format via BitmapFactory.Options
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(sourceFile.absolutePath, options)
            val mimeType = options.outMimeType ?: ""
            val isPng = mimeType.equals("image/png", ignoreCase = true)
            
            // Decode image with Alpha protection if PNG
            val decodeOptions = BitmapFactory.Options().apply {
                if (isPng) {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
            }
            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions) ?: return null
            
            // Scale down if too large (max 1920px width for backgrounds, maybe smaller for logos)
            val maxWidth = if (subFolder == "logos") 400 else 1920
            val scaledBitmap = if (bitmap.width > maxWidth) {
                val newHeight = (bitmap.height * maxWidth / bitmap.width)
                Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
            } else {
                bitmap
            }
            
            // Save to internal storage
            val targetDir = File(context.filesDir, subFolder)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            
            // Detect extension from discovered format
            val format = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            val extension = if (isPng) "png" else "jpg"
            val quality = if (isPng) 100 else 85 // PNG is lossless, quality param is ignored but 100 is safe
            
            val fileName = "${subFolder.take(2)}_${System.currentTimeMillis()}.$extension"
            val targetFile = File(targetDir, fileName)
            
            // Compress to appropriate format
            FileOutputStream(targetFile).use { out ->
                scaledBitmap.compress(format, quality, out)
            }
            
            // Clean up
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            bitmap.recycle()
            
            android.util.Log.d("ADMIN_SERVER", "Compressed: ${sourceFile.length()} -> ${targetFile.length()} bytes")
            
            targetFile.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("ADMIN_SERVER", "Error compressing image", e)
            null
        }
    }

    override fun stop() {
        super.stop()
        android.util.Log.d("ADMIN_SERVER", "🛑 Server stop")
    }

    private fun handleSyncRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        return try {
            val map = HashMap<String, String>()
            session.parseBody(map)
            // 🔥 Param harus diambil dari session.parameters untuk x-www-form-urlencoded
            val jsonString = session.parameters["postData"]?.firstOrNull()
            
            if (jsonString.isNullOrEmpty()) {
                android.util.Log.e("ADMIN_SERVER", "Sync payload empty. Content-Type: ${session.headers["content-type"]}")
                return newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "No data received")
            }
            
            android.util.Log.d("ADMIN_SERVER", "Received SYNC payload from ${session.remoteIpAddress}: $jsonString")
            
            val jsonObj = JSONObject(jsonString)
            val oldConfig = repo.load()
            
            // Reconstruct config from JSON, BUT KEEP deviceId and isActivated from local
            val newConfig = oldConfig.copy(
                name = jsonObj.optString("name", oldConfig.name),
                address = jsonObj.optString("address", oldConfig.address),
                latitude = jsonObj.optDouble("latitude", oldConfig.latitude),
                longitude = jsonObj.optDouble("longitude", oldConfig.longitude),
                iqomahMinutes = jsonObj.optInt("iqomahMinutes", oldConfig.iqomahMinutes),
                iqomahSubuh = jsonObj.optInt("iqomahSubuh", oldConfig.iqomahSubuh),
                iqomahDzuhur = jsonObj.optInt("iqomahDzuhur", oldConfig.iqomahDzuhur),
                iqomahAshar = jsonObj.optInt("iqomahAshar", oldConfig.iqomahAshar),
                iqomahMaghrib = jsonObj.optInt("iqomahMaghrib", oldConfig.iqomahMaghrib),
                iqomahIsya = jsonObj.optInt("iqomahIsya", oldConfig.iqomahIsya),
                iqomahJumat = jsonObj.optInt("iqomahJumat", oldConfig.iqomahJumat),
                backgroundUrl = jsonObj.optString("backgroundUrl", oldConfig.backgroundUrl),
                backgroundType = jsonObj.optString("backgroundType", oldConfig.backgroundType),
                themeName = jsonObj.optString("themeName", oldConfig.themeName),
                runningText = jsonObj.optString("runningText", oldConfig.runningText),
                timeOffsetMinutes = jsonObj.optInt("timeOffsetMinutes", oldConfig.timeOffsetMinutes),
                dateOffsetDays = jsonObj.optInt("dateOffsetDays", oldConfig.dateOffsetDays),
                sholatDurationMinutes = jsonObj.optInt("sholatDurationMinutes", oldConfig.sholatDurationMinutes),
                treasuryBalance = jsonObj.optString("treasuryBalance", oldConfig.treasuryBalance),
                treasuryDescription = jsonObj.optString("treasuryDescription", oldConfig.treasuryDescription),
                treasuryDisplayInterval = jsonObj.optInt("treasuryDisplayInterval", oldConfig.treasuryDisplayInterval),
                treasuryDisplayDuration = jsonObj.optInt("treasuryDisplayDuration", oldConfig.treasuryDisplayDuration),
                treasuryAccountInfo = jsonObj.optString("treasuryAccountInfo", oldConfig.treasuryAccountInfo),
                treasuryQrisData = jsonObj.optString("treasuryQrisData", oldConfig.treasuryQrisData),
                hadithDisplayInterval = jsonObj.optInt("hadithDisplayInterval", oldConfig.hadithDisplayInterval),
                hadithDisplayDuration = jsonObj.optInt("hadithDisplayDuration", oldConfig.hadithDisplayDuration),
                infoDisplayInterval = jsonObj.optInt("infoDisplayInterval", oldConfig.infoDisplayInterval),
                infoDisplayDuration = jsonObj.optInt("infoDisplayDuration", oldConfig.infoDisplayDuration),
                enableTarhim = jsonObj.optBoolean("enableTarhim", oldConfig.enableTarhim),
                logoUrl = jsonObj.optString("logoUrl", oldConfig.logoUrl),
                logoType = jsonObj.optString("logoType", oldConfig.logoType),
                logoLocalPath = jsonObj.optString("logoLocalPath", oldConfig.logoLocalPath),
                tarhimAudioUrl = jsonObj.optString("tarhimAudioUrl", oldConfig.tarhimAudioUrl),
                tarhimAudioType = jsonObj.optString("tarhimAudioType", oldConfig.tarhimAudioType),
                tarhimAudioLocalPath = jsonObj.optString("tarhimAudioLocalPath", oldConfig.tarhimAudioLocalPath),
                latestApkVersionCode = jsonObj.optInt("latestApkVersionCode", oldConfig.latestApkVersionCode),
                latestApkLocalPath = jsonObj.optString("latestApkLocalPath", oldConfig.latestApkLocalPath),
                lastUpdated = jsonObj.optString("lastUpdated", oldConfig.lastUpdated),
                
                // Parse InfoItems
                infoItems = try {
                    val arr = jsonObj.optJSONArray("infoItems")
                    val list = mutableListOf<InfoItem>()
                    if (arr != null) {
                        for(i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            var content = item.optString("content")
                            val type = item.optString("type", "text")
                            
                            // Check if this is a local image path that needs downloading
                            if (type == "image" && content.startsWith("/") && !File(content).exists()) {
                                val filename = content.substringAfterLast("/")
                                downloadFileFromPeer(session.remoteIpAddress, "info", filename)?.let {
                                    content = it
                                }
                            }
                            
                            list.add(InfoItem(
                                title = item.optString("title"),
                                content = content,
                                type = type
                            ))
                        }
                    }
                    list
                } catch(e: Exception) { oldConfig.infoItems }
            )

            // Save without triggering another broadcast (since it is already handled by repository, but wait, repository doesn't trigger broadcast, the saveConfig handler did)
            // But wait, repo.save() is clean.
            // We just need to make sure we don't trigger broadcast here. 
            // Broadasting is done in /save endpoint handler, not in repo.
            // So calling repo.save(newConfig) here is safe from loop.
            
            // Handle Base64 Background Image
            val bgBase64 = jsonObj.optString("backgroundImageBase64", "")
            var finalBgLocalPath = newConfig.backgroundLocalPath
            
            if (bgBase64.isNotEmpty()) {
                try {
                    val imageBytes = android.util.Base64.decode(bgBase64, android.util.Base64.DEFAULT)
                    val bgDir = File(context.filesDir, "backgrounds")
                    if (!bgDir.exists()) bgDir.mkdirs()
                    
                    val fileName = "bg_sync_${System.currentTimeMillis()}.jpg"
                    val targetFile = File(bgDir, fileName)
                    
                    FileOutputStream(targetFile).use { out ->
                        out.write(imageBytes)
                    }
                    
                    finalBgLocalPath = targetFile.absolutePath
                    android.util.Log.d("ADMIN_SERVER", "Synced background image saved to: $finalBgLocalPath")
                } catch (e: Exception) {
                    android.util.Log.e("ADMIN_SERVER", "Error saving synced background image", e)
                }
            } else if (newConfig.backgroundType == "upload" && finalBgLocalPath.isNotEmpty() && !File(finalBgLocalPath).exists()) {
                // Try download from peer
                val filename = finalBgLocalPath.substringAfterLast("/")
                downloadFileFromPeer(session.remoteIpAddress, "backgrounds", filename)?.let {
                    finalBgLocalPath = it
                }
            }

            // Handle Base64 Logo
            val logoBase64 = jsonObj.optString("logoImageBase64", "")
            var finalLogoLocalPath = newConfig.logoLocalPath
            
            if (logoBase64.isNotEmpty()) {
                try {
                    val imageBytes = android.util.Base64.decode(logoBase64, android.util.Base64.DEFAULT)
                    val logoDir = File(context.filesDir, "logos")
                    if (!logoDir.exists()) logoDir.mkdirs()
                    
                    val fileName = "lg_sync_${System.currentTimeMillis()}.jpg"
                    val targetFile = File(logoDir, fileName)
                    
                    FileOutputStream(targetFile).use { out ->
                        out.write(imageBytes)
                    }
                    
                    finalLogoLocalPath = targetFile.absolutePath
                    android.util.Log.d("ADMIN_SERVER", "Synced logo image saved to: $finalLogoLocalPath")
                } catch (e: Exception) {
                    android.util.Log.e("ADMIN_SERVER", "Error saving synced logo image", e)
                }
            } else if (finalLogoLocalPath.isNotEmpty() && !File(finalLogoLocalPath).exists()) {
                // Try download from peer
                val filename = finalLogoLocalPath.substringAfterLast("/")
                downloadFileFromPeer(session.remoteIpAddress, "logos", filename)?.let {
                    finalLogoLocalPath = it
                }
            }

            // Handle APK Update Sync
            var finalApkLocalPath = newConfig.latestApkLocalPath
            if (newConfig.latestApkVersionCode > BuildConfig.VERSION_CODE) {
                if (finalApkLocalPath.isNotEmpty() && !File(finalApkLocalPath).exists()) {
                    val filename = finalApkLocalPath.substringAfterLast("/")
                    downloadFileFromPeer(session.remoteIpAddress, "updates", filename)?.let {
                        finalApkLocalPath = it
                        
                        // 🔥 TRIGGER UPDATE INSTALLATION
                        installApk(it)
                    }
                } else if (finalApkLocalPath.isNotEmpty() && File(finalApkLocalPath).exists()) {
                    // File already exists, trigger install if not already on this version
                    installApk(finalApkLocalPath)
                }
            }

            // Handle Audio Sync
            var finalAudioLocalPath = newConfig.tarhimAudioLocalPath
            if (finalAudioLocalPath.isNotEmpty() && !File(finalAudioLocalPath).exists()) {
                val filename = finalAudioLocalPath.substringAfterLast("/")
                downloadFileFromPeer(session.remoteIpAddress, "audio", filename)?.let {
                    finalAudioLocalPath = it
                }
            }

            // Update config with potentially new local paths
            val finalConfig = newConfig.copy(
                backgroundLocalPath = finalBgLocalPath,
                logoLocalPath = finalLogoLocalPath,
                tarhimAudioLocalPath = finalAudioLocalPath,
                latestApkLocalPath = finalApkLocalPath
            )

            // Save synchronously to ensure data is written before responding
            try {
                repo.save(finalConfig)
                android.util.Log.d("ADMIN_SERVER", "Sync config saved successfully for ${finalConfig.name}")
            } catch (e: Exception) {
                android.util.Log.e("ADMIN_SERVER", "Error saving sync config", e)
                throw e
            }
            
            newFixedLengthResponse(NanoHTTPD.Response.Status.OK, MIME_PLAINTEXT, "Sync Success")
        } catch (e: Exception) {
            e.printStackTrace()
            newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Sync Error: ${e.message}")
        }
    }

    private fun managePeers(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        return try {
            val map = HashMap<String, String>()
            session.parseBody(map)
            val p = session.parameters
            
            val action = p["action"]?.firstOrNull()
            val ip = p["ip"]?.firstOrNull()?.trim()
            
            if (action != null && !ip.isNullOrEmpty()) {
                val oldConfig = repo.load()
                val currentManualList = oldConfig.manualPeers.toMutableList()
                val currentIgnoredList = oldConfig.ignoredPeers.toMutableList()
                
                if (action == "add") {
                    if (!currentManualList.contains(ip)) {
                        currentManualList.add(ip)
                         // If it was ignored, remove from ignored
                        currentIgnoredList.remove(ip)
                    }
                } else if (action == "remove") {
                    if (currentManualList.contains(ip)) {
                        currentManualList.remove(ip)
                    } else {
                        // If not in manual list (meaning it's auto), user wants to ignore it
                        if (!currentIgnoredList.contains(ip)) {
                            currentIgnoredList.add(ip)
                        }
                    }
                }
                
                val newConfig = oldConfig.copy(
                    manualPeers = currentManualList,
                    ignoredPeers = currentIgnoredList
                )
                runBlocking {
                    repo.save(newConfig)
                }
            }
            
            val response = newFixedLengthResponse(NanoHTTPD.Response.Status.REDIRECT, MIME_HTML, "")
            response.addHeader("Location", "/")
            response
        } catch (e: Exception) {
            e.printStackTrace()
            newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: ${e.message}")
        }
    }


    private fun handlePeersRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        return try {
            val myConfig = repo.load()
            val discoveredPeers = runBlocking { networkDiscovery.findPeers(myConfig.deviceId) }
            val manualPeers = myConfig.manualPeers.map { com.masjid.tvsholat.server.PeerInfo(it, "MANUAL") }
            
            // Filter out ignored peers
            val allPeers = (discoveredPeers + manualPeers)
                .distinctBy { it.ip }
                .filter { !myConfig.ignoredPeers.contains(it.ip) }
            
            val jsonArray = JSONArray()
            allPeers.forEach { peer ->
                val obj = JSONObject()
                obj.put("ip", peer.ip)
                obj.put("deviceId", peer.deviceId)
                // Nanti bisa ditambah logic request name ke /ping endpoint peer jika mau lebih lengkap
                // Tapi untuk sekarang IP + DeviceID dulu
                jsonArray.put(obj)
            }
            
            newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", jsonArray.toString())
        } catch (e: Exception) {
             newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: ${e.message}")
        }
    }
    
    private fun testPeerConnection(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val ip = session.parameters["ip"]?.firstOrNull()
        if (ip.isNullOrEmpty()) return newFixedLengthResponse("IP required")
        
        return try {
            val url = java.net.URL("http://$ip:9090/ping")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            
            // Add Auth Header
            val auth = "musholakita:mars123!"
            val encodedAuth = android.util.Base64.encodeToString(auth.toByteArray(), android.util.Base64.NO_WRAP)
            conn.setRequestProperty("Authorization", "Basic $encodedAuth")
            
            val responseCode = conn.responseCode
            val msg = if (responseCode == 200) "Sukses! Perangkat terhubung (Online)." else "Gagal! Response code: $responseCode"
            newFixedLengthResponse(NanoHTTPD.Response.Status.OK, MIME_PLAINTEXT, msg)
        } catch (e: Exception) {
            newFixedLengthResponse(NanoHTTPD.Response.Status.OK, MIME_PLAINTEXT, "Gagal terkoneksi: ${e.message}")
        }
    }

    private suspend fun broadcastConfigToPeers(config: com.masjid.tvsholat.data.MasjidConfig): String {
             // Return "success", "partial", or "failed"
             var successCount = 0
             var failCount = 0
        try {
                // Serialize config to JSON
                val json = JSONObject().apply {
                    put("name", config.name)
                    put("address", config.address)
                    put("latitude", config.latitude)
                    put("longitude", config.longitude)
                    put("iqomahMinutes", config.iqomahMinutes)
                    put("iqomahSubuh", config.iqomahSubuh)
                    put("iqomahDzuhur", config.iqomahDzuhur)
                    put("iqomahAshar", config.iqomahAshar)
                    put("iqomahMaghrib", config.iqomahMaghrib)
                    put("iqomahIsya", config.iqomahIsya)
                    put("iqomahJumat", config.iqomahJumat)
                    put("backgroundUrl", config.backgroundUrl)
                    put("backgroundType", config.backgroundType)
                    put("themeName", config.themeName)
                    put("runningText", config.runningText)
                    put("timeOffsetMinutes", config.timeOffsetMinutes)
                    put("dateOffsetDays", config.dateOffsetDays)
                    put("sholatDurationMinutes", config.sholatDurationMinutes)
                    put("treasuryBalance", config.treasuryBalance)
                    put("treasuryDescription", config.treasuryDescription)
                    put("treasuryDisplayInterval", config.treasuryDisplayInterval)
                    put("treasuryDisplayDuration", config.treasuryDisplayDuration)
                    put("treasuryAccountInfo", config.treasuryAccountInfo)
                    put("treasuryQrisData", config.treasuryQrisData)
                    put("hadithDisplayInterval", config.hadithDisplayInterval)
                    put("hadithDisplayDuration", config.hadithDisplayDuration)
                    put("infoDisplayInterval", config.infoDisplayInterval)
                    put("infoDisplayDuration", config.infoDisplayDuration)
                    put("enableTarhim", config.enableTarhim)
                    put("logoUrl", config.logoUrl)
                    put("logoType", config.logoType)
                    put("logoLocalPath", config.logoLocalPath)
                    put("tarhimAudioUrl", config.tarhimAudioUrl)
                    put("tarhimAudioType", config.tarhimAudioType)
                    put("tarhimAudioLocalPath", config.tarhimAudioLocalPath)
                    put("latestApkVersionCode", config.latestApkVersionCode)
                    put("latestApkLocalPath", config.latestApkLocalPath)
                    put("lastUpdated", config.lastUpdated)
                    put("infoItems", JSONArray().apply {
                        config.infoItems.forEach { 
                            put(JSONObject().apply {
                                put("title", it.title)
                                put("content", it.content)
                                put("type", it.type)
                            })
                        }
                    })
                    
                    // Encode Background Image if needed
                    if (config.backgroundType == "upload" && config.backgroundLocalPath.isNotEmpty()) {
                        try {
                            val file = File(config.backgroundLocalPath)
                            if (file.exists()) {
                                val bytes = file.readBytes()
                                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                                put("backgroundImageBase64", base64)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ADMIN_SERVER", "Error encoding background image", e)
                        }
                    }

                    // Encode Logo Image if needed
                    if (config.logoLocalPath.isNotEmpty()) {
                        try {
                            val file = File(config.logoLocalPath)
                            if (file.exists()) {
                                val bytes = file.readBytes()
                                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                                put("logoImageBase64", base64)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ADMIN_SERVER", "Error encoding logo image", e)
                        }
                    }
                }
                
                val jsonString = json.toString()
                
                // Find peers with shorter timeout for faster sync feel
                val discoveredPeers = networkDiscovery.findPeers(config.deviceId, 1000)
                
                // Combine with Manual Peers, but Filter out Ignored Peers
                val distinctPeers = (discoveredPeers + config.manualPeers.map { 
                    com.masjid.tvsholat.server.PeerInfo(it, "MANUAL") 
                }).distinctBy { it.ip }
                  .filter { !config.ignoredPeers.contains(it.ip) }
                
                android.util.Log.d("ADMIN_SERVER", "Syncing to ${distinctPeers.size} peers (Discovered: ${discoveredPeers.size}, Manual: ${config.manualPeers.size}, Ignored: ${config.ignoredPeers.size})")
                
                if (distinctPeers.isEmpty()) return "success" // No one to sync to is considered success locally
                
                var lastError = ""
                
                distinctPeers.forEach { peer ->
                    try {
                        val url = java.net.URL("http://${peer.ip}:9090/sync")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.doOutput = true
                        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                        conn.setRequestProperty("X-Sync-Source", "true") 
                        conn.connectTimeout = 3000
                        conn.readTimeout = 3000
                        
                        // Add Auth Header
                        val auth = "musholakita:mars123!"
                        val encodedAuth = android.util.Base64.encodeToString(auth.toByteArray(), android.util.Base64.NO_WRAP)
                        conn.setRequestProperty("Authorization", "Basic $encodedAuth")
                        
                        val encodedJson = java.net.URLEncoder.encode(jsonString, "UTF-8")
                        val postData = "postData=$encodedJson"
                        val input = postData.toByteArray(java.nio.charset.StandardCharsets.UTF_8)
                        
                        conn.setRequestProperty("Content-Length", input.size.toString())
                        
                        conn.outputStream.use { os ->
                            os.write(input, 0, input.size)
                        }
                        
                        val responseCode = conn.responseCode
                        android.util.Log.d("ADMIN_SERVER", "Sync sent to ${peer.ip}: Code $responseCode")
                        if (responseCode == 200) successCount++ else {
                            failCount++
                            lastError = "HTTP $responseCode from ${peer.ip}"
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ADMIN_SERVER", "Failed to sync to ${peer.ip}", e)
                        failCount++
                        lastError = "${e.javaClass.simpleName}: ${e.message} to ${peer.ip}"
                    }
                }
                
                return if (failCount == 0) "success" 
                       else if (successCount > 0) "partial" 
                       else "failed:$lastError"
                
            } catch (e: Exception) {
                android.util.Log.e("ADMIN_SERVER", "Error broadcasting config", e)
                return "failed:${e.message}"
            }
        }

    private fun installApk(apkPath: String) {
        try {
            val file = File(apkPath)
            if (!file.exists()) return
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file),
                    "application/vnd.android.package-archive"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("ADMIN_SERVER", "Update failed: ${e.message}")
        }
    }

    private fun saveApkFile(sourceFile: File): String? {
        return try {
            val updateDir = File(context.filesDir, "updates")
            if (!updateDir.exists()) updateDir.mkdirs()
            
            // Hapus file lama agar hemat storage
            updateDir.listFiles()?.forEach { it.delete() }
            
            val fileName = "update_${System.currentTimeMillis()}.apk"
            val targetFile = File(updateDir, fileName)
            sourceFile.inputStream().use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("ADMIN_SERVER", "Error saving APK", e)
            null
        }
    }
}

