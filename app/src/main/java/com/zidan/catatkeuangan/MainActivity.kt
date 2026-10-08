package com.zidan.catatkeuangan

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        status = TextView(this).apply {
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
        }
        root.addView(status)
        root.addView(makeButton("Buka pengaturan akses notifikasi") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        root.addView(makeButton("Muat ulang") { refresh() })
        root.addView(makeButton("Salin semua (nomor panjang disamarkan)") { copyAll() })
        root.addView(makeButton("Hapus semua") {
            NotifStore.file(this).delete()
            refresh()
        })

        logView = TextView(this).apply {
            textSize = 12f
            setTextIsSelectable(true)
        }
        root.addView(logView)

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun makeButton(label: String, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = label
        b.isAllCaps = false
        b.setOnClickListener { onClick() }
        return b
    }

    private fun listenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        return flat.contains(packageName)
    }

    private fun format(e: Entry, mask: Boolean): String {
        val t = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date(e.time))
        var s = "[$t] ${e.pkg}\n${e.title}\n${e.text}"
        if (mask) s = s.replace(Regex("\\d{9,}"), "[nomor]")
        return s
    }

    private fun refresh() {
        status.text = if (listenerEnabled()) {
            "Akses notifikasi: AKTIF"
        } else {
            "Akses notifikasi: BELUM AKTIF. Tekan tombol pertama, lalu aktifkan Catat Keuangan."
        }
        val entries = NotifStore.readAll(this)
        logView.text = if (entries.isEmpty()) {
            "\nBelum ada notifikasi keuangan yang tertangkap."
        } else {
            "\n" + entries.joinToString("\n\n---\n\n") { format(it, false) }
        }
    }

    private fun copyAll() {
        val entries = NotifStore.readAll(this)
        val text = entries.joinToString("\n\n---\n\n") { format(it, true) }
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("notifikasi", text))
        Toast.makeText(this, "Tersalin. Cek dan samarkan nama sebelum dikirim.", Toast.LENGTH_LONG).show()
    }
}
