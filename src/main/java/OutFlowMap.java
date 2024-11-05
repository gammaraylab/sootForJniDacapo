import soot.Unit;
import java.util.HashMap;
//maps unit to out flow
public class OutFlowMap{
    private final HashMap<Unit, PointsToGraph> flow= new HashMap<>();
    void setFlow(Unit u, PointsToGraph ptg){
        flow.put(u,ptg);
    }
    PointsToGraph getFlow(Unit u){
        return flow.getOrDefault(u, new PointsToGraph());
    }
}
