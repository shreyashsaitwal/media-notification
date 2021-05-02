-dontpreverify
-repackageclasses ''
-allowaccessmodification
-optimizations !code/simplification/arithmetic

-dontwarn androidx.media.R*, android.support.v4.media.**
-keeppackagenames gnu.kawa.*, gnu.expr.*

-keep public class * {
    public protected *;
}
