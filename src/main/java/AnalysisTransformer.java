import java.util.*;
import soot.*;

public class  AnalysisTransformer extends SceneTransformer {
    @Override
    protected void internalTransform(String arg0, Map<String, String> arg1) {
        SootMethod entryPoint = Scene.v().getMainMethod();
        // Get the list of methods reachable from the main method
        // Note: This can be done bottom up manner as well. Might be easier to model.
        PTGWL.map.put(entryPoint, new PointsToAnalysis(entryPoint.getActiveBody()));

        CallSite cs= PTGWL.addCallSite(entryPoint,null, new ArrayList<>(), "", new Position(-1, -1));
        // Process main method's PTG
        try {
            PTGWL.map.get(entryPoint).doAnalysis(cs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        PTGWL.printResults();
    }
}