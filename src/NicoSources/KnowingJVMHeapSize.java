package NicoSources;

public class KnowingJVMHeapSize {

    private final static long BYTES = 1;
    private final static long KILOBYTES = 1024;
    private final static long MEGABYTES = 1048576; // 1024 * 1024(1 MB = 1024 KB
                               // and 1 KB = 1024 Bytes)
    private final static long GIGABYTES = 1073741824;//1024 * 1024 * 1024

    public static void main(String... args) {

        // To Get the jvm heap size in Bytes using totalMemory() method.
        long jvmHeapSizeInBytes = Runtime.getRuntime().totalMemory() / BYTES;

        // To Get the jvm heap size in KB using totalMemory() method.
        long jvmHeapSizeInKB = Runtime.getRuntime().totalMemory() / KILOBYTES;

        // To Get the jvm heap size in MB using totalMemory() method.
        long jvmHeapSizeInMB = Runtime.getRuntime().totalMemory() / MEGABYTES;

        // To Get the jvm heap size in GB using totalMemory() method.
        long jvmHeapSizeInGB = Runtime.getRuntime().totalMemory() / GIGABYTES;

        // For printing the jvm heap size in Bytes.
        System.out.println(jvmHeapSizeInBytes + " BYTES");

        // For printing the jvm heap size in KB.
        System.out.println(jvmHeapSizeInKB + " KB");

        // For printing the jvm heap size in MB.
        System.out.println(jvmHeapSizeInMB + " MB");

        // For printing the jvm heap size in GB.
        System.out.println(jvmHeapSizeInGB + " GB");
    }
}