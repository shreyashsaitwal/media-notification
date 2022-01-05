-keep public class io.shreyash.medianotification** {
    public *;
 }
-keeppackagenames gnu.kawa**, gnu.expr**

-optimizationpasses 4
-allowaccessmodification
-mergeinterfacesaggressively

-repackageclasses 'io/shreyash/medianotification/repack'
-flattenpackagehierarchy
-dontpreverify
