package com.zidan.catatkeuangan

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotifListener : NotificationListenerService() {

    private var lastKey = ""

    // Hanya notifikasi yang memuat nominal uang yang disimpan, supaya chat pribadi tidak ikut tercatat.
    private val moneyPattern =
        Regex("(\\bRp\\.?\\s?\\d|\\bIDR\\b|\\bUSD\\b|\\$\\s?\\d)", RegexOption.IGNORE_CASE)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val body = if (big.length > text.length) big else text
        if (!moneyPattern.containsMatchIn("$title $body")) return

        val key = "${sbn.packageName}|$title|$body"
        if (key == lastKey) return
        lastKey = key

        NotifStore.add(this, sbn.packageName, title, body)
    }
}
