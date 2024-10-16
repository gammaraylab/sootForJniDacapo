
import java.util.*;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.toolkits.callgraph.Edge;

public class  AnalysisTransformer extends SceneTransformer {
    @Override
    protected void internalTransform(String arg0, Map<String, String> arg1) {
        SootMethod entryPoint = Scene.v().getMainMethod();
        // Get the list of methods reachable from the main method
        // Note: This can be done bottom up manner as well. Might be easier to model.
        PTGWL.map.put(entryPoint, new PointsToAnalysis(entryPoint.getActiveBody()));
        // Process main method's PTG
        try {
            PTGWL.map.get(entryPoint).doAnalysis();
        } catch (Exception e) {
            e.printStackTrace();
        }
        PTGWL.printResults();
    }
}