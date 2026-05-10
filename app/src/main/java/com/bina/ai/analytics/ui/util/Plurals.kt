package com.bina.ai.analytics.ui.util

fun plural(n: Int, singular: String, plural: String): String =
    if (n == 1) singular else plural
