public class Position{
    final int lineNo;
    final int colNo;

    Position(int l, int c){
        lineNo=l;
        colNo=c;
    }

    boolean equal(Position other){
        return lineNo==other.lineNo && colNo==other.colNo;
    }
}