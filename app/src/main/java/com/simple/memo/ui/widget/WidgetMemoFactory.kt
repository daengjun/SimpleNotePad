package com.simple.memo.ui.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViewsService
import android.appwidget.AppWidgetManager
import android.graphics.Color
import android.widget.RemoteViews
import com.simple.memo.R

class WidgetMemoFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private var fullContent: String = ""
    private val widgetId: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val prefs = context.getSharedPreferences("widget_memo_prefs", Context.MODE_PRIVATE)
        fullContent = prefs.getString("memo_content_$widgetId", "") ?: ""
    }

    override fun getCount(): Int = 1

    override fun getViewAt(position: Int): RemoteViews {
        val view = RemoteViews(context.packageName, R.layout.widget_item_line)
        view.setTextViewText(R.id.tv_widget_line, fullContent)

        val fillInIntent = Intent()
        fillInIntent.putExtra("memo_id", widgetId)
        view.setOnClickFillInIntent(R.id.tv_widget_line, fillInIntent)
        view.setInt(R.id.tv_widget_line, "setBackgroundColor", Color.TRANSPARENT)
        return view
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = 0
    override fun hasStableIds(): Boolean = true
    override fun onDestroy() {}
}
