package com.fc.HadesLauncher.fsaf.callback

import android.content.Intent

interface StartActivityCallbacks {
  fun myStartActivityForResult(intent: Intent, requestCode: Int)
}