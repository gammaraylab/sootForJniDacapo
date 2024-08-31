public class Complex{
	//statically assigning the address of shared lib
	static{
		System.loadLibrary("ComplexCpp");
	}
	//declating native methods
//	private native ComplexNumber add(ComplexNumber a, ComplexNumber b);
	private native ComplexNumber multiply(ComplexNumber a, ComplexNumber b);
	private ComplexNumber subtract(ComplexNumber a, ComplexNumber b){
		ComplexNumber tmp=new ComplexNumber(0,0);
		tmp.r=a.r-b.r;
		tmp.i=a.i-b.i;
		return tmp;
	}
	public ComplexNumber square(ComplexNumber a){
		ComplexNumber tmp=new ComplexNumber(0,0);
		tmp.r=a.r*a.r;
		tmp.i=a.i*a.i;
		return tmp;
	}
	public static void main(String[] args) {

		ComplexNumber p=new ComplexNumber(1.0,3.0);
		ComplexNumber q=new ComplexNumber(2.0,8.0);

		ComplexNumber result;					//temporay variable to store results

		try{									
//			result=new Complex().add(p,q);		//calling the native method
			result=new Complex().subtract(p,q);			//call to a private java method
			System.out.println(result.i);
			result=new Complex().square(p);			//call to a public java method
			System.out.println(result.i);
			result=new Complex().multiply(p,q);//calling the native method
			result=result.onlyReal();
			System.out.println(result.i);
			
		}catch(NullPointerException e){
            System.out.print("NullPointerException Caught");
        	}
        }
}

class ComplexNumber{
	double r;
	double i;
	ComplexNumber(double r, double i){
		this.r=r;
		this.i=i;
	}
	ComplexNumber onlyReal(){
		ComplexNumber tmp=new ComplexNumber(this.r,0);
		return tmp;
	}
}
