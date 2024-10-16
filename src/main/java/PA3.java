import soot.*;

public class PA3{
    public static void main(String[] args) {
        String dir ="./src/testCases/batik-small";
        String refl_log = "reflection-log:" + dir + "/refl.log";
        String mainClass= "Harness";
        String outputDir= dir+"/sootOutput";
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
                "-output-format", "jimple",
                "-output-dir", outputDir,
                "-ire",
                "-i", "jdt.*",
                "-i", "jdk.*",
                "-i", "java.*",
                "-i", "org.*",
                "-i", "com.*",
                "-i", "sun.*",
        };

        // Create transformer for analysisLibrary
        AnalysisTransformer analysisTransformer = new AnalysisTransformer();
        // Add transformer to appropriate pack in PackManager; PackManager will run all packs when soot.Main.main is called
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.jni", analysisTransformer));
        // Call Soot's main method with arguments
        try {
            soot.Main.main(sootArgs2);
        }catch (RuntimeException e){
            e.printStackTrace();
        }
    }
}