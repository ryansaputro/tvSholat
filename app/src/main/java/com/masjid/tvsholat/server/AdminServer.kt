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
        
        // Log headers untuk debug kenapa popup gak muncul
        val authHeader = session.headers["authorization"] ?: session.headers["Authorization"]
        val isSyncRequest = session.headers["x-sync-source"] == "true"
        
        // Kalau request sync dari TV lain, bypass auth (karena sesama device internal)
        // Atau bisa juga tambah simple auth token
        if (!isSyncRequest) {
             android.util.Log.d("ADMIN_SERVER", ">>> [${method}] ${uri} from ${remoteIp} | Auth: ${authHeader != null}")
        }

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
            val response = newFixedLengthResponse(NanoHTTPD.Response.Status.UNAUTHORIZED, MIME_PLAINTEXT, "Silakan login untuk akses Admin.")
            // Penting: Browser butuh header ini buat munculin popup login
            response.addHeader("WWW-Authenticate", "Basic realm=\"Admin Panel TvSholat\"")
            return response
        }
        // ------------------
        
        return when (uri) {
            "/" -> adminPage(session)
            "/save" -> saveConfig(session)
            "/sync" -> handleSyncRequest(session)
            "/peers" -> handlePeersRequest(session)
            "/peers/manage" -> managePeers(session)
            "/peers/test" -> testPeerConnection(session)
            "/ping" -> newFixedLengthResponse(NanoHTTPD.Response.Status.OK, MIME_PLAINTEXT, "PONG")
            "/favicon.ico" -> newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, MIME_PLAINTEXT, "")
            else -> newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404 Not Found")
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
                            </select>
                        </div>
                    </div>

                    <div class="card">
                        <h3>⏱️ Konfigurasi Sholat</h3>
                        <div class="grid">
                            <div class="form-group">
                                <label>Jeda Iqomah (Menit)</label>
                                <input type="number" name="iqomah" value="${config.iqomahMinutes}" required>
                            </div>
                            <div class="form-group">
                                <label>Durasi Sholat (Menit)</label>
                                <input type="number" name="sholat_duration" value="${config.sholatDurationMinutes}" required>
                                <small style="color: #666; font-size: 11px;">Layar hitam setelah iqomah.</small>
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
                            <div class="form-group">
                                <label>Koreksi Tanggal (Hari)</label>
                                <input name="date_offset" type="number" value="${config.dateOffsetDays}">
                            </div>
                        </div>
                        <div style="margin-top: 15px; border-top: 1px dashed #eee; padding-top: 15px;">
                            <label style="display: flex; align-items: center; cursor: pointer;">
                                <input type="checkbox" name="is_time_master" style="width: auto; margin-right: 10px;"value="true" ${if (config.isTimeMaster) "checked" else ""}>
                                <div>
                                    <span style="font-weight: 600; display: block;">Jadikan Pusat Waktu (Master)</span>
                                    <span style="font-size: 11px; color: #666; font-weight: normal;">Centang jika TV ini adalah acuan waktu untuk TV lain (Biar detik sinkron).</span>
                                </div>
                            </label>
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
                                <img id="preview" src="${if (config.backgroundType == "upload" && config.backgroundLocalPath.isNotEmpty()) "file://" + config.backgroundLocalPath else config.backgroundUrl}" onerror="this.src='https://via.placeholder.com/400x200?text=Preview'">
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
                                <label>Durasi (Detik)</label>
                                <input name="info_duration" type="number" value="${config.infoDisplayDuration}">
                            </div>
                        </div>
                        
                        <div style="margin-top:15px; border-top: 1px solid #eee; padding-top:15px;">
                            ${
                                (0..2).joinToString("\n") { index ->
                                    val item = config.infoItems.getOrNull(index) ?: InfoItem("", "")
                                    """
                                    <div style="margin-bottom: 20px; padding: 15px; background: #f9f9f9; border-radius: 12px; border: 1px solid #eee;">
                                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
                                            <label style="color:#1b5e20; font-weight: 800; font-size: 16px;">Info #${index + 1}</label>
                                            <div style="background: #eee; padding: 4px; border-radius: 8px; display: flex; gap: 4px;">
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
                                            <input name="info_title_$index" value="${item.title}" placeholder="Judul Info" style="margin-bottom: 0;">
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
                                                    <input id="image_url_$index" value="${if (item.type == "image") item.content else ""}" placeholder="Atau masukkan URL: https://example.com/poster.jpg" style="margin-bottom: 0; flex: 1;">
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
                                    </div>
                                    """
                                }
                            }
                        </div>
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
                const editors = [];
                [0, 1, 2].forEach(index => {
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
                    editors[index] = quill;
                });

                document.getElementById('mainForm').onsubmit = function() {
                    [0, 1, 2].forEach(index => {
                        const type = document.querySelector('input[name="info_type_' + index + '"]:checked').value;
                        if (type === 'image') {
                            const raw = document.getElementById('image_url_' + index).value;
                            document.getElementById('content_' + index).value = extractImageUrl(raw);
                        } else {
                            const html = editors[index].root.innerHTML;
                            document.getElementById('content_' + index).value = html === '<p><br></p>' ? '' : html;
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

    private fun saveConfig(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        return try {
            // 🔥 WAJIB ADA UNTUK POST
            val files = HashMap<String, String>()
            session.parseBody(files)

            val p = session.parameters
            val isFromSync = session.headers["x-sync-source"] == "true"
            
            // Log params biar keliatan di logcat kalau ada yang aneh
            android.util.Log.d("ADMIN_SERVER", "Received Parameters: ${p.keys}")

            val oldConfig = repo.load()
            
            // 🔥 Ambil dari parameter form (hidden input) sebagai pengaman tambahan kalau repo.load() stale
            val pIsActivated = p["is_activated"]?.firstOrNull()?.toBoolean() ?: oldConfig.isActivated
            val pDeviceId = p["device_id"]?.firstOrNull() ?: oldConfig.deviceId
            
            // Handle file upload if present
            val bgType = p["bg_type"]?.first()?.trim() ?: oldConfig.backgroundType
            var bgLocalPath = p["bg_local_path"]?.first()?.trim() ?: oldConfig.backgroundLocalPath
            
            // Check if there's an uploaded file
            if (bgType == "upload" && files.containsKey("bg_file")) {
                val tempFilePath = files["bg_file"]
                if (!tempFilePath.isNullOrEmpty()) {
                    val tempFile = File(tempFilePath)
                    if (tempFile.exists()) {
                        try {
                            // Compress and save
                            val compressedPath = compressAndSaveImage(tempFile)
                            if (compressedPath != null) {
                                bgLocalPath = compressedPath
                                android.util.Log.d("ADMIN_SERVER", "Image compressed and saved to: $compressedPath")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ADMIN_SERVER", "Error compressing image", e)
                        }
                    }
                }
            }
            
            // 🔥 GUNAKAN .copy() BIAR DATA GAK KEHAPUS / RESET SENDIRI
            val config = oldConfig.copy(
                name = p["name"]?.first()?.trim() ?: oldConfig.name,
                address = p["address"]?.first()?.trim() ?: oldConfig.address,
                latitude = p["lat"]?.first()?.toDoubleOrNull() ?: oldConfig.latitude,
                longitude = p["lng"]?.first()?.toDoubleOrNull() ?: oldConfig.longitude,
                themeName = p["theme_name"]?.first()?.trim() ?: oldConfig.themeName,
                iqomahMinutes = p["iqomah"]?.first()?.toIntOrNull() ?: oldConfig.iqomahMinutes,
                sholatDurationMinutes = p["sholat_duration"]?.first()?.toIntOrNull() ?: oldConfig.sholatDurationMinutes,
                backgroundUrl = p["bg_url"]?.first()?.trim() ?: oldConfig.backgroundUrl,
                backgroundType = bgType,
                backgroundLocalPath = bgLocalPath,
                runningText = p["running_text"]?.first()?.trim() ?: oldConfig.runningText,
                timeOffsetMinutes = p["time_offset"]?.first()?.toIntOrNull() ?: oldConfig.timeOffsetMinutes,
                dateOffsetDays = p["date_offset"]?.first()?.toIntOrNull() ?: oldConfig.dateOffsetDays,
                treasuryBalance = p["treasury_balance"]?.first()?.trim() ?: oldConfig.treasuryBalance,
                treasuryDescription = p["treasury_desc"]?.first()?.trim() ?: oldConfig.treasuryDescription,
                treasuryDisplayInterval = p["treasury_interval"]?.first()?.toIntOrNull() ?: oldConfig.treasuryDisplayInterval,
                treasuryDisplayDuration = p["treasury_duration"]?.first()?.toIntOrNull() ?: oldConfig.treasuryDisplayDuration,
                treasuryAccountInfo = p["treasury_account"]?.first()?.trim() ?: oldConfig.treasuryAccountInfo,
                treasuryQrisData = p["treasury_qris"]?.first()?.trim() ?: oldConfig.treasuryQrisData,
                hadithDisplayInterval = p["hadith_interval"]?.first()?.toIntOrNull() ?: oldConfig.hadithDisplayInterval,
                hadithDisplayDuration = p["hadith_duration"]?.first()?.toIntOrNull() ?: oldConfig.hadithDisplayDuration,
                infoDisplayInterval = p["info_interval"]?.first()?.toIntOrNull() ?: oldConfig.infoDisplayInterval,
                infoDisplayDuration = p["info_duration"]?.first()?.toIntOrNull() ?: oldConfig.infoDisplayDuration,
                isTimeMaster = p["is_time_master"]?.firstOrNull() != null, // Checkbox sends value if checked, nothing if unchecked
                infoItems = (0..2).map { index ->
                    val title = p["info_title_$index"]?.first()?.trim() ?: ""
                    var content = p["info_content_$index"]?.first()?.trim() ?: ""
                    val type = p["info_real_type_$index"]?.first()?.trim() ?: "text"
                    
                    // Handle info image upload
                    if (type == "image" && files.containsKey("info_file_$index")) {
                        val tempPath = files["info_file_$index"]
                        if (!tempPath.isNullOrEmpty()) {
                            val tempFile = File(tempPath)
                            if (tempFile.exists()) {
                                compressAndSaveImage(tempFile)?.let {
                                    content = it
                                }
                            }
                        }
                    }
                    
                    // Always save the item, even if empty (preserves structure)
                    InfoItem(title, content, type)
                },
                isActivated = pIsActivated, // ✅ PASTIIN GAK RESET
                deviceId = pDeviceId,      // ✅ PASTIIN GAK RESET
                lastUpdated = SimpleDateFormat("d MMM yyyy HH:mm", Locale.forLanguageTag("id")).format(Date())
            )

            android.util.Log.d("ADMIN_SERVER", "Saving config: $config")

            // Save in IO thread
            var syncStatus = ""
            runBlocking(Dispatchers.IO) {
                repo.save(config)
                
                // 🔥 Broadcast ONLY if requested by user (Sync button)
                val syncMode = p["sync_mode"]?.firstOrNull()
                if (syncMode == "broadcast" && !isFromSync) {
                     syncStatus = broadcastConfigToPeers(config)
                }
            }

            // Redirect back to home with success param
            val response = newFixedLengthResponse(NanoHTTPD.Response.Status.REDIRECT, MIME_HTML, "")
            val loc = "/?saved=1" + if(syncStatus.isNotEmpty()) "&sync_status=$syncStatus" else ""
            response.addHeader("Location", loc)
            response
        } catch (e: Exception) {
            e.printStackTrace()
            newFixedLengthResponse(
                NanoHTTPD.Response.Status.INTERNAL_ERROR,
                MIME_PLAINTEXT,
                "ERROR: ${e.message}"
            )
        }
    }
    
    private fun compressAndSaveImage(sourceFile: File): String? {
        return try {
            // Decode image
            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath) ?: return null
            
            // Scale down if too large (max 1920px width for Full HD)
            val scaledBitmap = if (bitmap.width > 1920) {
                val newHeight = (bitmap.height * 1920 / bitmap.width)
                Bitmap.createScaledBitmap(bitmap, 1920, newHeight, true)
            } else {
                bitmap
            }
            
            // Save to internal storage
            val bgDir = File(context.filesDir, "backgrounds")
            if (!bgDir.exists()) {
                bgDir.mkdirs()
            }
            
            val fileName = "bg_${System.currentTimeMillis()}.jpg"
            val targetFile = File(bgDir, fileName)
            
            // Compress to JPEG with 85% quality
            FileOutputStream(targetFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
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
                lastUpdated = jsonObj.optString("lastUpdated", oldConfig.lastUpdated),
                
                // Parse InfoItems
                infoItems = try {
                    val arr = jsonObj.optJSONArray("infoItems")
                    val list = mutableListOf<InfoItem>()
                    if (arr != null) {
                        for(i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            list.add(InfoItem(item.optString("title"), item.optString("content")))
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
            } else if (newConfig.backgroundType == "upload" && newConfig.backgroundLocalPath.isEmpty()) {
                 // If sync says upload but no local path (and no base64), might need to keep old one or handle error
                 // For now, let's just keep what we had or empty
            }

            // Update config with potentially new local path
            val finalConfig = newConfig.copy(backgroundLocalPath = finalBgLocalPath)

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
                    put("lastUpdated", config.lastUpdated)
                    put("infoItems", JSONArray().apply {
                        config.infoItems.forEach { 
                            put(JSONObject().apply {
                                put("title", it.title)
                                put("content", it.content)
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
    }

