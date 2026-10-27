package de.nasi.organizer

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.json.JSONObject

class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(applicationContext)
}

private class Factory(private val ctx: Context) : RemoteViewsService.RemoteViewsFactory {
    private var state: JSONObject? = null
    private var items: List<JSONObject> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        state = Store.state(ctx)
        items = state?.let { Store.openActions(it) } ?: emptyList()
    }

    override fun onDestroy() {}

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_item)
        val t = items.getOrNull(position) ?: return v
        val s = state ?: return v
        val id = Store.str(t, "id")

        v.setTextViewText(R.id.t_title, Store.str(t, "title"))
        val meta = Store.meta(s, t)
        v.setTextViewText(R.id.t_meta, meta)
        v.setViewVisibility(R.id.t_meta, if (meta.isEmpty()) View.GONE else View.VISIBLE)
        v.setTextColor(R.id.t_meta, if (Store.isLate(t)) 0xFFFF5A5F.toInt() else 0xFF9A9A9A.toInt())

        val ring = when (t.optInt("imp", 2)) {
            3 -> R.drawable.ring_p3
            2 -> R.drawable.ring_p2
            else -> R.drawable.ring_p1
        }
        v.setImageViewResource(R.id.t_ring, ring)
        v.setViewVisibility(R.id.t_flag, if (Store.str(t, "flag").isNotEmpty()) View.VISIBLE else View.GONE)

        v.setOnClickFillInIntent(
            R.id.t_ring,
            Intent().putExtra(WidgetActionActivity.EXTRA_ACTION, "toggle").putExtra(WidgetActionActivity.EXTRA_ID, id)
        )
        v.setOnClickFillInIntent(
            R.id.t_body,
            Intent().putExtra(WidgetActionActivity.EXTRA_ACTION, "open").putExtra(WidgetActionActivity.EXTRA_ID, id)
        )
        return v
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = false
}
