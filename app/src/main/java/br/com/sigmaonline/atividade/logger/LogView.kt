package br.com.sigmaonline.atividade.logger

import android.app.Activity
import android.content.Context

class LogView(context: Context) : androidx.appcompat.widget.AppCompatTextView(context) {

    fun println(msg: String?) {
        // In case this was originally called from an AsyncTask or some other off-UI thread,
        // make sure the update occurs within the UI thread.
        (context as Activity).runOnUiThread(Thread {
            if (msg != null) {
                // Display the text we just generated within the LogView.
                appendToLog(msg)
            }
        })
    }

    private fun appendToLog(s: String) {
        append("\n" + s)
    }

    fun clearLog() {
        text = ""
    }
}