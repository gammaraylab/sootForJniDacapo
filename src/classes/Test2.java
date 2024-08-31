class Test2Node {
	Test2Node f;
	Test2Node g;
	Test2Node() {}
}

public class Test2 {
	public static Test2Node global;
	public static void main(String[] args) {
		foo();
	}
	public static Test2Node foo(){
		Test2Node x = new Test2Node();	//o13
		Test2Node y = new Test2Node();	//o14
		y.f = new Test2Node();	//o15
		y = new Test2Node();	//o16
		bar(x, y);
		Test2Node z = y.f;
		Test2Node a = x.f;
		return x;
	}
	public static void bar(Test2Node p1, Test2Node p2){
		Test2Node v = new Test2Node();	//o23
		p1.f = v;	
	}
}