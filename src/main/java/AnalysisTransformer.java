
import java.util.*;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.toolkits.callgraph.Edge;

public class  AnalysisTransformer extends SceneTransformer {
    @Override
    protected void internalTransform(String arg0, Map<String, String> arg1) {
        Set<SootMethod> methods = new HashSet<>();
//        SootMethod entryPoint = Scene.v().getEntryPoints().get(0);
        //entryPoints contains the list of potential entry points into the program
        // since unlike in java which have main as only entry point, android programs are more event driven and have multiple entry points
        Set<SootMethod> entryPoints = new HashSet<>(Scene.v().getEntryPoints());
        // Get the list of methods reachable from the main method
        // Note: This can be done bottom up manner as well. Might be easier to model.
        System.out.println(Scene.v().getEntryPoints().size());
        Scene.v().getEntryPoints().forEach(entryPoint ->{
            if(entryPoint.hasActiveBody()) {
                methods.clear();
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
                    if (m != entryPoint && PTGWL.elementMap.containsKey(m) && m.hasActiveBody()) {
                        CallSiteElement cselement = PTGWL.elementMap.get(m);
                        PointsToGraph init = new PointsToGraph();
                        Set<String> alwaysLiveObjs = new HashSet<>();
                        for (InvokeExpr ie : cselement.callsiteMap.keySet()) {
                            Element e = cselement.callsiteMap.get(ie);
                            PointsToGraph modifiedPTG = PointsToAnalysis.getProcessedPTG(m.getActiveBody().getUnits(), e.initGraph, e.callerParams, e.receiverObj, alwaysLiveObjs);
                            init.add(modifiedPTG);
                        }
                        PointsToAnalysis pta = new PointsToAnalysis(m.getActiveBody(), m.getDeclaringClass().getName() + "_" + m.getName(), init, alwaysLiveObjs);
                        try {
                            pta.doAnalysis();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //add the pta to the method
                        PTGWL.map.put(m, pta);
                        PTGWL.allMethods.add(m);
                    }
                }
            }
        });

        HashMap<String, Integer> maximalEliminationMap = new HashMap<>();
        List<String> methodListSorted = new ArrayList<>();

        for (SootMethod m : methods) {
            if (!PTGWL.map.containsKey(m))
                continue;

            methodListSorted.add(m.getDeclaringClass().getName() + ":" + m.getName());
            for (String heapObj : PTGWL.map.get(m).elimination.keySet()) {
                Integer currSol = PTGWL.map.get(m).elimination.get(heapObj);
                if (maximalEliminationMap.containsKey(heapObj)) {
                    if (maximalEliminationMap.get(heapObj) < currSol)
                        maximalEliminationMap.put(heapObj, currSol);
                }
                else
                    maximalEliminationMap.put(heapObj, currSol);
            }
        }
        Collections.sort(methodListSorted);

        HashMap<String, Set<String>> finalRes = new HashMap<>();

        for (SootMethod m : methods) {
            if (!PTGWL.map.containsKey(m))
                continue;
            String key = m.getDeclaringClass().getName() + ":" + m.getName();
            finalRes.put(key, new HashSet<>());
            for (String heapObj : PTGWL.map.get(m).elimination.keySet()) {
                Integer currSol = PTGWL.map.get(m).elimination.get(heapObj);
                if (maximalEliminationMap.get(heapObj) <= currSol && heapObj.startsWith("O"))
                        finalRes.get(key).add(heapObj.substring(1) + ":" + currSol);
            }
        }
        PTGWL.printResults();

        //print the results
//        for (String m : methodListSorted) {
//            List<String> sortedSoln = new ArrayList<>();
//            System.out.print(m + " ");
//            if (!finalRes.get(m).isEmpty()) {
//                sortedSoln.addAll(finalRes.get(m));
//                Collections.sort(sortedSoln);
//                for (String so : sortedSoln) {
//                    System.out.print(so + " ");
//                }
//            }
//            System.out.println();
//        }
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
            if (!targetMethod.isJavaLibraryMethod())
                getListOfMethods(targetMethod, reachableMethods);
        }
    }
}
