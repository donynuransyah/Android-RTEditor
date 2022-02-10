package com.onegravity.rteditor

import android.util.Log

class Logma {
    companion object{
        val tag = "bambang"
        @JvmStatic
        fun debugger(message : Any){
            Log.e(tag,"$message")
        }
    }

}
