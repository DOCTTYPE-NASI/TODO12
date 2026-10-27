package de.nasi.organizer

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate

/**
 * Gemeinsamer Datenspeicher für App (WebView), Widget und Erinnerungen.
 * Die Daten sind ein JSON-Objekt im gleichen Format wie in der Web-App.
 */
object Store {
    private const val PREF = "organizer"
    private const val KEY = "state"

    private val WD = arrayOf("MO", "DI", "MI", "DO", "FR", "SA", "SO")
    private val MO = arrayOf("JAN", "FEB", "MÄR", "APR", "MAI", "JUN", "JUL", "AUG", "SEP", "OKT", "NOV", "DEZ")

    @Synchronized
    fun load(ctx: Context): String? =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, null)

    @Synchronized
    fun save(ctx: Context, json: String, notify: Boolean = true) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, json).commit()
        if (notify) {
            Reminders.rescheduleAll(ctx)
            TodoWidget.refresh(ctx)
        }
    }

    fun state(ctx: Context): JSONObject? = load(ctx)?.let {
        try { JSONObject(it) } catch (e: Exception) { null }
    }

    fun today(): String = LocalDate.now().toString()

    /** Liest einen String-Wert; JSON null wird zu "" */
    fun str(o: JSONObject, k: String): String = if (!o.has(k) || o.isNull(k)) "" else o.optString(k, "")

    fun tasks(s: JSONObject): List<JSONObject> {
        val a = s.optJSONArray("tasks") ?: return emptyList()
        return (0 until a.length()).mapNotNull { a.optJSONObject(it) }
    }

    private fun isFolder(t: JSONObject, all: List<JSONObject>): Boolean {
        if (t.optBoolean("folder")) return true
        val id = str(t, "id")
        return all.any { str(it, "parent") == id }
    }

    private fun score(t: JSONObject, td: String): Int {
        var s = t.optInt("imp", 2) * t.optInt("urg", 2)
        val due = str(t, "due")
        if (due.isNotEmpty() && due < td) s += 8 else if (due == td) s += 4
        if (str(t, "flag") == "f1") s += 2
        return s
    }

    /** Alle offenen Aufgaben (keine Ordner), wichtigste zuerst – wie die Heute-Liste der App */
    fun openActions(s: JSONObject): List<JSONObject> {
        val all = tasks(s)
        val td = today()
        return all.filter { !it.optBoolean("done") && !isFolder(it, all) }
            .sortedByDescending { score(it, td) }
    }

    fun doneToday(s: JSONObject): Int {
        val all = tasks(s)
        val td = today()
        return all.count { it.optBoolean("done") && str(it, "doneAt") == td && !isFolder(it, all) }
    }

    fun fmtDay(d: String): String {
        if (d.isEmpty()) return ""
        val t = LocalDate.now()
        val date = try { LocalDate.parse(d) } catch (e: Exception) { return d }
        return when (date) {
            t -> "HEUTE"
            t.plusDays(1) -> "MORGEN"
            t.minusDays(1) -> "GESTERN"
            else -> WD[date.dayOfWeek.value - 1] + " " + "%02d".format(date.dayOfMonth) + " " + MO[date.monthValue - 1]
        }
    }

    fun meta(s: JSONObject, t: JSONObject): String {
        val names = HashMap<String, String>()
        s.optJSONArray("contexts")?.let { a ->
            for (i in 0 until a.length()) a.optJSONObject(i)?.let { names[str(it, "id")] = str(it, "name") }
        }
        val bits = ArrayList<String>()
        t.optJSONArray("ctx")?.let { a ->
            for (i in 0 until a.length()) names[a.optString(i)]?.let { bits.add("@" + it.uppercase()) }
        }
        val due = str(t, "due")
        if (due.isNotEmpty()) {
            val time = str(t, "time")
            bits.add(fmtDay(due) + if (time.isNotEmpty()) " $time" else "")
        }
        when (str(t, "repeat")) {
            "daily" -> bits.add("TÄGL.")
            "weekly" -> bits.add("WÖCH.")
            "monthly" -> bits.add("MONATL.")
        }
        return bits.joinToString(" · ")
    }

    fun isLate(t: JSONObject): Boolean {
        val due = str(t, "due")
        return due.isNotEmpty() && due < today() && !t.optBoolean("done")
    }

    fun find(s: JSONObject, id: String): JSONObject? = tasks(s).firstOrNull { str(it, "id") == id }

    /**
     * Hakt eine Aufgabe ab (gleiche Logik wie in der App):
     * wiederkehrende Aufgaben springen zum nächsten Termin, andere werden erledigt.
     * Mit onlyComplete=true wird eine erledigte Aufgabe nicht wieder geöffnet.
     */
    @Synchronized
    fun toggle(ctx: Context, id: String, onlyComplete: Boolean = false): JSONObject? {
        val s = state(ctx) ?: return null
        val t = find(s, id) ?: return null
        if (t.optBoolean("done")) {
            if (onlyComplete) return t
            t.put("done", false)
        } else {
            val rep = str(t, "repeat").ifEmpty { "none" }
            if (rep != "none") {
                val td = LocalDate.now()
                val base = try { LocalDate.parse(str(t, "due").ifEmpty { td.toString() }) } catch (e: Exception) { td }
                var next = when (rep) {
                    "daily" -> base.plusDays(1)
                    "weekly" -> base.plusWeeks(1)
                    else -> base.plusMonths(1)
                }
                if (!next.isAfter(td)) next = when (rep) {
                    "daily" -> td.plusDays(1)
                    "weekly" -> td.plusDays(7)
                    else -> td.plusDays(30)
                }
                t.put("due", next.toString())
                val r = str(t, "remind")
                if (r.length >= 16) {
                    t.put("remind", next.toString() + "T" + r.substring(11, 16))
                    t.put("fired", false)
                }
            } else {
                t.put("done", true)
                t.put("doneAt", today())
            }
        }
        save(ctx, s.toString())
        return t
    }

    @Synchronized
    fun markFired(ctx: Context, id: String) {
        val s = state(ctx) ?: return
        val t = find(s, id) ?: return
        t.put("fired", true)
        save(ctx, s.toString(), notify = false)
    }
}
