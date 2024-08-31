//example inspired from
//https://www3.ntu.edu.sg/home/ehchua/programming/java/javanativeinterface.html

public class Calculator{
	private double a;
	private double b;

	//statically assigning the address of shared lib
	static{
		System.loadLibrary("nativeCalc");
	}
	
	//declating native methods
	private native double add(double a, double b);
	private native double subtract();
	// private native double divide(double a, double b);

	Calculator(double a, double b){
		this.a=a;
		this.b=b;
	}

	public static void main(String[] args) {
		Calculator calc= new Calculator(12.0,4.0);
		double result;
		
		System.out.println("Numbers are "+calc.a+" and "+calc.b);
		
		result=calc.add(calc.a,calc.b);
		System.out.println("sum is "+result );
		
		result=calc.subtract();
		System.out.println("difference is "+result );


	}
}