package com.masjid.tvsholat.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.masjidDataStore by preferencesDataStore("masjid_config")
