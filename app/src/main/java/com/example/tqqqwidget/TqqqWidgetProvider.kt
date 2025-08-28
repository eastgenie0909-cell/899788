package com.example.tqqqwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class TqqqWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://query1.finance.yahoo.com/v8/finance/chart/TQQQ?interval=1d&range=200d")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    body?.let {
                        val json = JSONObject(it)
                        val result = json.getJSONObject("chart")
                            .getJSONArray("result").getJSONObject(0)

                        val closes = result.getJSONObject("indicators")
                            .getJSONArray("quote").getJSONObject(0)
                            .getJSONArray("close")

                        val lastClose = closes.getDouble(closes.length() - 1)
                        var sum = 0.0
                        for (i in 0 until closes.length()) {
                            sum += closes.optDouble(i, lastClose)
                        }
                        val ma200 = sum / closes.length()
                        val envUpper = ma200 * 1.1

                        views.setTextViewText(R.id.priceText, "현재가: $lastClose")
                        views.setTextViewText(R.id.maText, "200MA: %.2f".format(ma200))
                        views.setTextViewText(R.id.envText, "ENV상단: %.2f".format(envUpper))

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            }
        }
    }
}
