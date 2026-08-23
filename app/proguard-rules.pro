# StockCuba ProGuard/R8 Rules
# Generated for release builds

# ===== GENERAL =====
# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
# Keep annotations for reflection
-keepattributes *Annotation*
# Keep generic signatures for Kotlin reflection
-keepattributes Signature
# Keep inner classes
-keepattributes InnerClasses
# Keep EnclosingMethod for lambda serialization
-keepattributes EnclosingMethod

# ===== KOTLIN =====
# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
# Keep Kotlin companion objects
-keepclassmembers class * {
    @kotlin.jvm.JvmStatic *;
    @kotlin.jvm.JvmField *;
}
# Keep Kotlin data classes
-keepclassmembers class * {
    <init>(...);
    copy(...);
    component*();
    toString();
    equals(...);
    hashCode();
}

# ===== HILT (Dagger) =====
# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp { *; }
-keep class * extends dagger.hilt.android.HiltViewModel { *; }
-keep class * extends dagger.hilt.android.HiltActivity { *; }
-keep class * extends dagger.hilt.android.HiltFragment { *; }
-keep class * extends dagger.hilt.android.HiltService { *; }
-keep class * extends dagger.hilt.android.HiltBroadcastReceiver { *; }

# Keep Hilt modules
-keep class * extends dagger.hilt.Module { *; }
-keep class * extends dagger.hilt.InstallIn { *; }
-keep class * extends dagger.hilt.EntryPoint { *; }

# Keep @HiltViewModel, @AndroidEntryPoint, @EntryPoint
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.hilt.android.HiltViewModel class *
-keep @dagger.hilt.EntryPoint interface *

# Keep generated Hilt components
-keep class *HiltComponents { *; }
-keep class *HiltViewModelFactory { *; }
-keep class *HiltViewModel { *; }

# ===== ROOM =====
# Keep Room entities
-keep class * extends androidx.room.Entity { *; }
-keep class * extends androidx.room.Database { *; }
-keep class * extends androidx.room.Dao { *; }
-keep class * extends androidx.room.TypeConverter { *; }
-keep class * extends androidx.room.migration.Migration { *; }

# Keep Room generated classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomOpenHelper { *; }
-keep class * extends androidx.room.RoomDatabase.Callback { *; }

# Keep Room DAO implementations
-keep class * implements *Dao { *; }
-keepclassmembers class * implements *Dao { *; }

# Keep Room entity getters/setters
-keepclassmembers class * {
    @androidx.room.Entity *;
    @androidx.room.PrimaryKey *;
    @androidx.room.ColumnInfo *;
    @androidx.room.ForeignKey *;
    @androidx.room.Index *;
    @androidx.room.TypeConverters *;
}

# Keep Room query methods
-keepclassmembers class * {
    @androidx.room.Query *;
    @androidx.room.Insert *;
    @androidx.room.Update *;
    @androidx.room.Delete *;
    @androidx.room.RawQuery *;
    @androidx.room.Transaction *;
}

# ===== RETROFIT / MOSHI =====
# Keep Retrofit interfaces
-keep interface * {
    @retrofit2.http.* *;
}

# Keep Retrofit generated classes
-keep class * extends retrofit2.Converter.Factory { *; }
-keep class * extends retrofit2.CallAdapter.Factory { *; }
-keep class retrofit2.** { *; }

# Keep Moshi adapters
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-keep class * extends com.squareup.moshi.JsonAdapter.Factory { *; }
-keep class com.squareup.moshi.** { *; }

# Keep Moshi @Json annotated classes
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
    @com.squareup.moshi.JsonClass *;
}

# Keep Moshi generated adapters
-keep class * extends com.squareup.moshi.JsonAdapter {
    <init>(...);
    fromJson(...);
    toJson(...);
}

# Keep KotlinJsonAdapterFactory
-keep class com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory { *; }

# ===== OKHTTP =====
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keep class okhttp3.logging.HttpLoggingInterceptor { *; }

# ===== COROUTINES =====
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }

# ===== COIL (Image Loading) =====
-keep class coil3.** { *; }
-keep class coil.** { *; }

# ===== DATASTORE =====
-keep class androidx.datastore.** { *; }
-keep class androidx.datastore.preferences.** { *; }

# ===== NAVIGATION =====
-keep class androidx.navigation.** { *; }
-keep class androidx.navigation.compose.** { *; }

# ===== LIFECYCLE / VIEWMODEL =====
-keep class androidx.lifecycle.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.LifecycleObserver { *; }

# ===== COMPOSE =====
# Keep Compose generated classes
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }

# Keep @Composable functions
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# ===== KOTLINX SERIALIZATION (if used) =====
# -keep class kotlinx.serialization.** { *; }
# -keepclassmembers class * {
#     @kotlinx.serialization.Serializable *;
# }

# ===== JSR 305 (Nullable annotations) =====
-keep @interface javax.annotation.Nullable
-keep @interface javax.annotation.Nonnull
-keep @interface javax.annotation.ParametersAreNonnullByDefault

# ===== KEEP APP CLASSES =====
# Keep application class
-keep class cu.stockcuba.app.StockCubaApplication { *; }

# Keep MainActivity
-keep class cu.stockcuba.app.MainActivity { *; }

# Keep ViewModels
-keep class cu.stockcuba.app.presentation.**.*ViewModel { *; }

# Keep UseCases
-keep class cu.stockcuba.app.domain.usecase.* { *; }

# Keep Repository implementations
-keep class cu.stockcuba.app.data.repository.* { *; }

# Keep Entities and DTOs
-keep class cu.stockcuba.app.domain.model.* { *; }
-keep class cu.stockcuba.app.data.remote.dto.* { *; }
-keep class cu.stockcuba.app.data.local.entity.* { *; }

# ===== REMOVE LOGGING IN RELEASE =====
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
}
-assumenosideeffects class kotlin.jvm.internal.InlineMarker {
    public static *** throwIfNot...(...);
}

# ===== OPTIMIZATIONS =====
# Allow optimization of unused code
-allowaccessmodification
# Merge interfaces
-mergeinterfacesaggressively
# Overload aggressively
-overloadaggressively