package de.nasi.organizer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * Unsichtbare Aktivität für Klicks im Widget.
 * "toggle" hakt die Aufgabe ab, ohne die App zu öffnen.
 * "open" öffnet die App mit den Schnellaktionen der Aufgabe.
 */
class WidgetActionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(EXTRA_ID)
        when (intent.getStringExtra(EXTRA_ACTION)) {
            "toggle" -> if (id != null) {
                Store.toggle(applicationContext, id)
                try {
                    @Suppress("DEPRECATION")
                    (getSystemService(VIBRATOR_SERVICE) as? Vibrator)?.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
                } catch (_: Exception) {}
            }
            "open" -> startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(MainActivity.EXTRA_TASK, id)
            )
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    companion object {
        const val EXTRA_ACTION = "action"
        const val EXTRA_ID = "id"
    }
}
