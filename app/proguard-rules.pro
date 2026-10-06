# Preserve line numbers and source attributes for stack trace re-tracing
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Notification Listener Service callbacks
-keep class com.notificationhistory.service.NotificationCaptureService { *; }

# Room Database Entities
-keep class com.notificationhistory.data.local.NotificationEntity { *; }
