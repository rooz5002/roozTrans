# Anthropic SDK uses Jackson + Kotlin reflection for (de)serialisation
-keep class com.anthropic.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod,KotlinMetadata
-dontwarn **
