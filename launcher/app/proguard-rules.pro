# kotlinx.serialization: keep generated serializers for config classes.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.ekrtf.capitv.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.ekrtf.capitv.**$$serializer { *; }
