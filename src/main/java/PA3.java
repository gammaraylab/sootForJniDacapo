import soot.*;
import soot.options.Options;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PA3{
    public static void main(String[] args) {
        String dir ="/home/anadi/Programs/mtp/dacapo/out";
        String refl_log = "reflection-log:" + dir + "/refl.log";
        String mainClass= "Harness";
        String outputDir= "/home/anadi/Programs/mtp/dacapo/sootOutput";

        //full apk name
        String apkName="com.emanuelef.remote_capture_1.7.2-75_minAPI21(arm64-v8a,armeabi-v7a,x86,x86_64)(nodpi)_apkmirror.com.apk";

        //soot args for android apk
        String SDKdir= "/home/anadi/.android/sdk";
        String testApkHome="/home/anadi/Programs/mtp/toBeAnalyzedForJNI";
        String apkPath=testApkHome+"/"+apkName;
        String sootDir="/home/anadi/Programs/IDEA/sootOnJNI2/src/main/java";

        String dir2="/home/anadi/Programs/IDEA/sootOnJNI2/src/testcases/";
        String mainClass2="Complex";

        //Set up arguments for java programs
        String[] sootArgs2 = {
                "-w",
                "-app",
                "-allow-phantom-refs",
                "-cp",dir,
                "-prepend-classpath",
                "-keep-line-number",
                "-main-class", mainClass,       //main class to analyze
                "-process-dir", dir,      // directory of classes to analyze
                "-p","cg.spark","on",
                "-p", "cg", refl_log,
                "-output-dir", outputDir,
                "-output-format", "jimple",
                "-ire",
                "-i", "jdt.*",
                "-i", "jdk.*",
                "-i", "java.*",
                "-i", "org.*",
                "-i", "com.*",
                "-i", "sun.*",
        };

        setupSootForAPK(SDKdir,apkPath,sootDir,outputDir);

        // Create transformer for analysis
        AnalysisTransformer analysisTransformer = new AnalysisTransformer();
        // Add transformer to appropriate pack in PackManager; PackManager will run all packs when soot.Main.main is called
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.jni", analysisTransformer));
        // Call Soot's main method with arguments
        try {
//            soot.Main.main(sootArgs2);
            PackManager.v().runPacks();
        }catch (RuntimeException e){
            e.printStackTrace();
        }

    }
    public static void setupSootForAPK(String SDKdir, String apkPath, String sootDir, String outputDir) {
        // Reset the Soot settings (it's necessary if you are analyzing several APKs)
//        G.reset();
        // Generic options
        Options.v().set_whole_program(true);
        Options.v().set_app(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_allow_phantom_elms(true);
        Options.v().set_prepend_classpath(true);
        Options.v().set_output_dir(outputDir);  //output path for jimple
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_keep_line_number(true);
        Options.v().set_ignore_resolving_levels(true);
        Options.v().set_include(Arrays.asList("jdt.*", "jdk.*", "java.*", "org.*", "com.*", "sun.*"));

        // Read (APK Dex-to-Jimple) Options
        Options.v().set_android_jars(SDKdir+"/platforms"); // The path to Android Platforms
        Options.v().set_src_prec(Options.src_prec_apk); // Determine the input is an APK
        Options.v().set_process_dir(Collections.singletonList(apkPath)); // Provide paths to the APK
        Options.v().set_process_multiple_dex(true);  // Inform Dexpler that the APK may have more than one .dex files
        Options.v().set_include_all(true);
        Options.v().setPhaseOption("cg.spark", "on");
        Options.v().setPhaseOption("cg.spark", "string-constants:true");
//        soot.options.Options.v().set_soot_classpath(sootDir+"/soot.jar");

        // Resolve required classes
        Scene.v().loadNecessaryClasses();

        String mainActivity = getMainActivity(apkPath,SDKdir+"/build-tools/34.0.0");

        if (mainActivity != null) {
            List<SootMethod> entryPoints = new ArrayList<>();

            SootClass activityClass = Scene.v().getSootClass(mainActivity);
            // Look at the methods in this Activity subclass.
            for (SootMethod activityMethod : activityClass.getMethods()) {
                // If this method overrides a method from the parent Activity class,
                // consider this an entry-point to the application.
                if (activityClass.declaresMethod(activityMethod.getSubSignature()))
                    entryPoints.add(activityMethod);
            }

            Options.v().set_main_class(mainActivity);
            Scene.v().setEntryPoints(entryPoints);
        }
        else
            System.out.println("Could not determine main activity.");
    }

    public static String getMainActivity(String apkPath, String SDKdir) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(SDKdir+"/aapt", "dump", "badging", apkPath);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String packageName = null;
            String mainActivity = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("package:"))
                    packageName = line.split(" ")[1].split("'")[1];
                else if (line.startsWith("launchable-activity:"))
                    mainActivity = line.split(" ")[1].split("'")[1];
            }
            reader.close();
            if (mainActivity != null && packageName != null)
                return mainActivity.startsWith(".") ? packageName + mainActivity : mainActivity;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<SootMethod> naivelyComputeEntryPoints() {
        // Naively compute entry-points by adding all overridden methods of subclasses
        // of Activity in the application.  The class hierarchy (i.e. which tracks the
        // relationships between classes and subclasses) should have been computed in
        // initializeSoot().
        List<SootMethod> entryPoints = new ArrayList<>();

        SootClass activityClass = Scene.v().getSootClass("android.app.Activity");
        Hierarchy cha = Scene.v().getActiveHierarchy();

        // Look at all subclasses of Activity.
        for (SootClass activitySubClass : cha.getSubclassesOf(activityClass)) {
            // Make sure this is an application class and not a framework one.
            if (!activitySubClass.isApplicationClass())
                continue;

            // Look at the methods in this Activity subclass.
            for (SootMethod activityMethod : activitySubClass.getMethods()) {
                // If this method overrides a method from the parent Activity class,
                // consider this an entry-point to the application.
                if (activityClass.declaresMethod(activityMethod.getSubSignature()))
                    entryPoints.add(activityMethod);
            }
        }
        return entryPoints;
    }
}