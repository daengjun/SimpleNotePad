package com.simple.memo.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.simple.memo.R
import com.simple.memo.ui.main.MainActivity

class SimpleMemoWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == "com.simple.memo.ACTION_MEMO_SELECTED") {
            val widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            val memoId = intent.getIntExtra("selectedMemoId", -1)
            val memoContent = intent.getStringExtra("selectedMemoContent")

            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID && memoId != -1 && memoContent != null) {
                val prefs = context.getSharedPreferences("widget_memo_prefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putString("memo_content_$widgetId", memoContent)
                    putInt("memo_id_$widgetId", memoId)
                    apply()
                }
                updateWidget(context, AppWidgetManager.getInstance(context), widgetId)
            }
        }
    }

    companion object {

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val prefs = context.getSharedPreferences("widget_memo_prefs", Context.MODE_PRIVATE)
            val memoContent = prefs.getString("memo_content_$widgetId", null)

            Log.d("Widget", "Updating widgetId=$widgetId, content=$memoContent")
            val views = RemoteViews(context.packageName, R.layout.widget_simple_memo)

            if (memoContent != null) {
                val serviceIntent = Intent(context, WidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                }
                views.setRemoteAdapter(R.id.lv_widget_memo, serviceIntent)

                views.setViewVisibility(R.id.memoLayout, View.VISIBLE)
                views.setViewVisibility(R.id.btn_select_memo, View.GONE)

                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra("from_widget", true)
                    putExtra("memo_id", prefs.getInt("memo_id_$widgetId", -1))
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, widgetId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setPendingIntentTemplate(R.id.lv_widget_memo, pendingIntent)

                views.setOnClickPendingIntent(R.id.memoLayout, pendingIntent)
                views.setOnClickPendingIntent(R.id.block_click_layer, pendingIntent)

                appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.lv_widget_memo)

            } else {
                views.setViewVisibility(R.id.memoLayout, View.GONE)
                views.setViewVisibility(R.id.btn_select_memo, View.VISIBLE)

                val pickIntent = Intent(context, MemoFlowActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pickPendingIntent = PendingIntent.getActivity(
                    context, widgetId, pickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_select_memo, pickPendingIntent)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}