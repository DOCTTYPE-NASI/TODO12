package de.nasi.organizer

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews

/** Das To-Do-Widget für den Homescreen */
class TodoWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(ctx, mgr, it) }
        mgr.notifyAppWidgetViewDataChanged(ids, R.id.list)
    }

    companion object {
        fun refresh(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, TodoWidget::class.java))
            if (ids.isEmpty()) return
            ids.forEach { update(ctx, mgr, it) }
            mgr.notifyAppWidgetViewDataChanged(ids, R.id.list)
        }

        private fun update(ctx: Context, mgr: AppWidgetManager, widgetId: Int) {
            val v = RemoteViews(ctx.packageName, R.layout.widget_todo)
            val s = Store.state(ctx)
            val open = if (s != null) Store.openActions(s).size else 0
            val done = if (s != null) Store.doneToday(s) else 0
            v.setTextViewText(R.id.count, "%02d OFFEN".format(open))
            v.setTextViewText(R.id.footer, "$done HEUTE ERLEDIGT")

            val svc = Intent(ctx, WidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            @Suppress("DEPRECATION")
            v.setRemoteAdapter(R.id.list, svc)
            v.setEmptyView(R.id.list, R.id.empty)

            val openApp = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            v.setOnClickPendingIntent(R.id.open, openApp)
            v.setOnClickPendingIntent(R.id.header, openApp)

            // Vorlage für Klicks auf einzelne Aufgaben (Kreis = abhaken, Text = verwalten)
            val mutable = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            val template = PendingIntent.getActivity(
                ctx, 1, Intent(ctx, WidgetActionActivity::class.java),
                mutable or PendingIntent.FLAG_UPDATE_CURRENT
            )
            v.setPendingIntentTemplate(R.id.list, template)

            mgr.updateAppWidget(widgetId, v)
        }
    }
}
