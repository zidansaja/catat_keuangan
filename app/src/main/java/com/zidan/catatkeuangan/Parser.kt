package com.zidan.catatkeuangan

enum class Jenis { MASUK, KELUAR, PINDAH }

data class Parsed(val jenis: Jenis, val nominal: Long, val sumber: String, val keterangan: String)

// Aturan pembaca berdasarkan contoh notifikasi asli. Format yang belum dikenal menghasilkan null
// (tetap tersimpan di log mentah, tidak masuk daftar transaksi).
object Parser {
    private val opts = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)

    // Satu grup tangkap: angka nominal, mis. "15.000" dari "Rp15.000" atau "Rp20.000,00".
    private const val AMT = "Rp\\.?\\s?(\\d[\\d.]*)(?:,\\d{1,2})?"

    private fun toLong(s: String): Long? = s.replace(".", "").toLongOrNull()

    fun parse(pkg: String, title: String, text: String): Parsed? {
        return when (pkg) {
            "com.gojek.gopaymerchant" -> gopayMerchant(title, text)
            "id.dana" -> dana(text)
            "id.co.bri.brimo" -> brimo(text)
            else -> null
        }
    }

    private fun gopayMerchant(title: String, text: String): Parsed? {
        val all = "$title $text"
        if (!all.contains("Pembayaran", ignoreCase = true) ||
            !all.contains("diterima", ignoreCase = true)
        ) return null
        val m = Regex(AMT, opts).find(all) ?: return null
        val n = toLong(m.groupValues[1]) ?: return null
        return Parsed(Jenis.MASUK, n, "GoPay Merchant", "Pembayaran QRIS diterima")
    }

    private fun dana(text: String): Parsed? {
        val m = Regex("$AMT\\s+telah dikirim ke\\s+(.+)", opts).find(text) ?: return null
        val n = toLong(m.groupValues[1]) ?: return null
        val ke = m.groupValues[2].trim().replace(Regex("\\s*[^\\p{L}\\p{N}*]+$"), "")
        return Parsed(Jenis.KELUAR, n, "DANA", "Kirim ke $ke")
    }

    private fun brimo(text: String): Parsed? {
        val m = Regex("Transaksi\\s+(.+?)\\s+sebesar\\s+$AMT\\s+BERHASIL", opts).find(text)
            ?: return null
        val jenisTrx = m.groupValues[1]
            .replace(Regex("\\d{6,}"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        val n = toLong(m.groupValues[2]) ?: return null
        // Top up dianggap perpindahan saldo, bukan pengeluaran (belum dikonfirmasi pengguna).
        val jenis = if (jenisTrx.startsWith("Top Up", ignoreCase = true)) Jenis.PINDAH else Jenis.KELUAR
        return Parsed(jenis, n, "BRI", jenisTrx)
    }
}
