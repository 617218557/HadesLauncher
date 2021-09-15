package com.fc.hadeslauncher.fsaf.callback

import android.content.Intent

interface StartActivityCallbacks {
  fun myStartActivityForResult(intent: Intent, requestCode: Int)
}