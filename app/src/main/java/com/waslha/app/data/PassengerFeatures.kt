package com.waslha.app.data

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timeLabel: String,
    val unread: Boolean = true
)

data class PaymentOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val isDefault: Boolean = false
)

data class SupportCategory(
    val id: String,
    val title: String,
    val description: String
)

data class SupportArticle(
    val id: String,
    val question: String,
    val answer: String
)

object PassengerFeatureDefaults {
    val notifications = listOf(
        NotificationItem("ride", "تحديث الرحلة", "سيظهر هنا أي تحديث مهم متعلق برحلتك.", "الآن"),
        NotificationItem("promo", "عروض وصلها", "تابع العروض والميزات الجديدة داخل التطبيق.", "اليوم", unread = false)
    )

    val payments = listOf(
        PaymentOption("cash", "نقداً", "ادفع للكابتن عند الوصول", isDefault = true),
        PaymentOption("card", "بطاقة بنكية", "سيتم تفعيل الدفع الإلكتروني لاحقاً"),
        PaymentOption("wallet", "محفظة وصلها", "رصيدك داخل التطبيق")
    )

    val supportCategories = listOf(
        SupportCategory("trip", "مشكلة في الرحلة", "الكابتن، السعر، الإلغاء أو تفاصيل الرحلة"),
        SupportCategory("payment", "الدفع والفاتورة", "مشكلة في الدفع أو قيمة الرحلة"),
        SupportCategory("account", "الحساب", "رقم الهاتف، الحساب والخصوصية"),
        SupportCategory("safety", "الأمان", "بلاغ أو مشكلة تتعلق بسلامتك")
    )

    val faq = listOf(
        SupportArticle("cancel", "كيف ألغي الرحلة؟", "من شاشة الرحلة الحالية اختر إلغاء الرحلة وحدد السبب."),
        SupportArticle("fare", "كيف يتم حساب السعر؟", "السعر يعتمد على نوع السيارة والمسافة والوقت والعوامل التي يحددها النظام."),
        SupportArticle("rating", "كيف أقيّم الكابتن؟", "بعد انتهاء الرحلة ستظهر شاشة التقييم ويمكنك اختيار عدد النجوم."),
        SupportArticle("account", "كيف أغيّر بيانات الحساب؟", "من تبويب حسابي يمكنك الوصول إلى إعدادات الحساب والخصوصية.")
    )
}
