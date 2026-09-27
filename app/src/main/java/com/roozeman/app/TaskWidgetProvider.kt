package com.roozeman.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

class TaskWidgetProvider : AppWidgetProvider() {
    companion object {
        const val ACTION_TOGGLE = "com.roozeman.app.ACTION_WIDGET_TOGGLE_TASK"
        const val EXTRA_TASK_ID = "taskId"
        val rowIds = intArrayOf(R.id.widget_task_1, R.id.widget_task_2, R.id.widget_task_3, R.id.widget_task_4, R.id.widget_task_5)

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                TaskWidgetProvider().onUpdate(context, manager, ids)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val db = DbHelper(context).readableDatabase
        val tasks = loadTasks(db).filter { !it.done }.take(5)

        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.task_widget)

            for (i in rowIds.indices) {
                if (i < tasks.size) {
                    val task = tasks[i]
                    views.setTextViewText(rowIds[i], "☐ " + task.title)
                    views.setViewVisibility(rowIds[i], View.VISIBLE)

                    val toggleIntent = Intent(context, TaskWidgetProvider::class.java).apply {
                        action = ACTION_TOGGLE
                        putExtra(EXTRA_TASK_ID, task.id)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context, task.id.toInt(), toggleIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(rowIds[i], pendingIntent)
                } else {
                    views.setViewVisibility(rowIds[i], View.GONE)
                }
            }

            views.setViewVisibility(R.id.widget_empty, if (tasks.isEmpty()) View.VISIBLE else View.GONE)

            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
            if (taskId != -1L) {
                val db = DbHelper(context).writableDatabase
                updateTaskDone(db, taskId, true)
                updateAll(context)
            }
        }
    }
}
