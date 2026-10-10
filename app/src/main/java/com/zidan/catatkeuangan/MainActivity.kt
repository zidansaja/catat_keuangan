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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var summary: TextView
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
        root.addView(makeButton("Salin notifikasi mentah (nomor panjang disamarkan)") { copyAll() })
        root.addView(makeButton("Hapus semua") {
            NotifStore.clearAll(this)
            refresh()
        })

        summary = TextView(this).apply {
            textSize = 14f
            setTextIsSelectable(true)
        }
        root.addView(summary)

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

    private fun rp(n: Long): String = NumberFormat.getInstance(Locale("id", "ID")).format(n)

    private fun waktu(t: Long): String =
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(t))

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

        val trx = NotifStore.readTrx(this)
        val masuk = trx.filter { it.jenis == Jenis.MASUK }.sumOf { it.nominal }
        val keluar = trx.filter { it.jenis == Jenis.KELUAR }.sumOf { it.nominal }
        val pindah = trx.filter { it.jenis == Jenis.PINDAH }.sumOf { it.nominal }
        val daftar = if (trx.isEmpty()) {
            "Belum ada transaksi yang terbaca."
        } else {
            trx.joinToString("\n") {
                val tanda = when (it.jenis) {
                    Jenis.MASUK -> "+"
                    Jenis.KELUAR -> "-"
                    Jenis.PINDAH -> "<>"
                }
                "$tanda Rp ${rp(it.nominal)} | ${it.sumber} | ${it.ket} | ${waktu(it.time)}"
            }
        }
        summary.text = "\nMasuk: Rp ${rp(masuk)}\nKeluar: Rp ${rp(keluar)}\n" +
            "Pindah saldo (top up, tidak dihitung): Rp ${rp(pindah)}\n\nTRANSAKSI TERBACA\n$daftar"

        val entries = NotifStore.readAll(this)
        logView.text = if (entries.isEmpty()) {
            "\nNOTIFIKASI MENTAH\nBelum ada notifikasi keuangan yang tertangkap."
        } else {
            "\nNOTIFIKASI MENTAH\n\n" + entries.joinToString("\n\n---\n\n") { format(it, false) }
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
