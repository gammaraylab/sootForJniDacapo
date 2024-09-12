
import java.util.*;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.toolkits.callgraph.Edge;

public class  AnalysisTransformer extends SceneTransformer {
    @Override
    protected void internalTransform(String arg0, Map<String, String> arg1) {
        Set<SootMethod> methods = new HashSet<>();
        SootMethod entryPoint = Scene.v().getEntryPoints().get(0);
        // Get the list of methods reachable from the main method
        // Note: This can be done bottom up manner as well. Might be easier to model.
        getListOfMethods(entryPoint, methods);
        PTGWL.map.put(entryPoint, new PointsToAnalysis(entryPoint.getActiveBody(), entryPoint.getDeclaringClass().getName() + "_" + entryPoint.getName()));
        PTGWL.allMethods.add(entryPoint);
        // Process main method's PTG
        try {
            PTGWL.map.get(entryPoint).doAnalysis();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //process and analyze all the reachable methods
        for (SootMethod m : methods) {
            if (m != entryPoint && m.hasActiveBody() && PTGWL.elementMap.containsKey(m) ) {
                CallSiteElement cselement = PTGWL.elementMap.get(m);
                PointsToGraph init = new PointsToGraph();
                Set<String> alwaysLiveObjs = new HashSet<>();
                for (InvokeExpr ie : cselement.callsiteMap.keySet()) {
                    Element e = cselement.callsiteMap.get(ie);
                    PointsToGraph modifiedPTG = PointsToAnalysis.getProcessedPTG(m.getActiveBody().getUnits(), e.initGraph, e.callerParams, e.receiverObj, alwaysLiveObjs);
                    init.add(modifiedPTG);
                }
                PointsToAnalysis pta =
                        new PointsToAnalysis(m.getActiveBody(), m.getDeclaringClass().getName() + "_" + m.getName(), init, alwaysLiveObjs);
                try {
                    PTGWL.printResults();
                    pta.doAnalysis();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //add the pta to the method
                PTGWL.map.put(m, pta);
                PTGWL.allMethods.add(m);
            }
        }

        PTGWL.printResults();
    }

    private static void getListOfMethods(SootMethod method, Set<SootMethod> reachableMethods) {
        // Avoid revisiting methods
        if (reachableMethods.contains(method))
            return;
        // Add the method to the reachable set
        reachableMethods.add(method);

        // Iterate over the edges originating from this method
        Iterator<Edge> edges = Scene.v().getCallGraph().edgesOutOf(method);
        while (edges.hasNext()) {
            Edge edge = edges.next();
            SootMethod targetMethod = edge.tgt();
            // Recursively explore callee methods
//           if (!targetMethod.isJavaLibraryMethod())
                getListOfMethods(targetMethod, reachableMethods);
        }
    }
}
