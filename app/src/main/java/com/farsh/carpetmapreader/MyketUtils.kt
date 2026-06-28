package com.farsh.carpetmapreader

import android.content.Context
import android.content.Intent
import android.net.Uri

object MyketUtils {
    private const val MYKET_PACKAGE = "ir.mservices.myket"
    private const val APP_PACKAGE = "com.farsh.carpetmapreader"

    fun openAppPage(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$APP_PACKAGE")
                `package` = MYKET_PACKAGE
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://myket.ir/app/$APP_PACKAGE")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("bazaar://details?id=$APP_PACKAGE")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    fun openMoreApps(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://search?q=pub:فرشید حبیبی")
                `package` = MYKET_PACKAGE
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://myket.ir/search?q=فرشید حبیبی")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                openInBrowser(context, "https://myket.ir/search?q=%D9%81%D8%B1%D8%B4%DB%8C%D8%AF+%D8%AD%D8%A8%DB%8C%D8%A8%DB%8C")
            }
        }
    }

    fun sendEmail(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:habibi.farshid75@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "نقشه خوان فرش - بازخورد")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
    }

    fun dialPhone(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:09914310328")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
    }

    private fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
    }

    fun isMyketInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(MYKET_PACKAGE, 0)
            true
        } catch (_: Exception) {
            false
        }
    }
}
