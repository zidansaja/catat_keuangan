package com.zidan.catatkeuangan

import android.content.Context
import org.json.JSONObject
import java.io.File

data class Entry(val time: Long, val pkg: String, val title: String, val text: String)

object NotifStore {
    fun file(c: Context) = File(c.filesDir, "notif_log.jsonl")

    @Synchronized
    fun add(c: Context, pkg: String, title: String, text: String) {
        val o = JSONObject()
            .put("t", System.currentTimeMillis())
            .put("pkg", pkg)
            .put("title", title)
            .put("text", text)
        file(c).appendText(o.toString() + "\n")
    }

    @Synchronized
    fun readAll(c: Context, limit: Int = 200): List<Entry> {
        val f = file(c)
        if (!f.exists()) return emptyList()
        return f.readLines()
            .filter { it.isNotBlank() }
            .mapNotNull {
                try {
                    val o = JSONObject(it)
                    Entry(o.getLong("t"), o.getString("pkg"), o.optString("title"), o.optString("text"))
                } catch (e: Exception) {
                    null
                }
            }
            .asReversed()
            .take(limit)
    }
}
