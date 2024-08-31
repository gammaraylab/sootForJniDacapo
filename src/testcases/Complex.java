public class Complex{
	//statically assigning the address of shared lib
	static{
		System.loadLibrary("libComplexCpp");
	}
	//declating native methods
	private native ComplexNumber add(ComplexNumber a, ComplexNumber b);
	private native ComplexNumber multiply(ComplexNumber a, ComplexNumber b);
	public ComplexNumber test(ComplexNumber n){
		ComplexNumber c= new ComplexNumber(n.r,3.3,null);
		return c;
	}
	private ComplexNumber testPrv(ComplexNumber n){
		ComplexNumber c= new ComplexNumber(n.r,3.3,null);
		ComplexNumber d= new ComplexNumber(n.r,3.3,null);
		ComplexNumber e= new ComplexNumber(n.r,3.3,null);

		return c;
	}
	public static ComplexNumber testStatic(ComplexNumber n){
		n.next= new ComplexNumber(4.3,3.3,null);
		ComplexNumber fun= new ComplexNumber(n.r,3.3,null);
		ComplexNumber pun= new ComplexNumber(5.7,3.3,null);
		ComplexNumber gun= new ComplexNumber(n.r,3.3,null);
		gun.next=fun;
		return gun;
	}
	public static void main(String[] args) {

		ComplexNumber p=new ComplexNumber(1.0,3.0,null);
		ComplexNumber q=new ComplexNumber(2.0,8.0,null);

		ComplexNumber r=new ComplexNumber(1.0,3.0,null);
		r.next=new ComplexNumber(4,34,null);
		ComplexNumber o=new ComplexNumber(2.0,8.0,null);
		o.next=new ComplexNumber(44,364,null);

		ComplexNumber k=new ComplexNumber(1.0,3.0,null);
		ComplexNumber l=new ComplexNumber(2.0,8.0,null);

		ComplexNumber m=new ComplexNumber(2.0,8.0,null);
		ComplexNumber mm=new ComplexNumber(2.0,8.0,null);
		ComplexNumber mmm=new ComplexNumber(2.0,8.0,null);
		ComplexNumber mmmm=new ComplexNumber(2.0,8.0,null);
		int x=Math.negateExact((int)m.r);

		testStatic(mm);
		Complex tmp=new Complex();
		tmp.test(mmmm);
		tmp.testPrv(q);
		testStatic(mmm);
		testStatic(mm);
		l.print(o);
		ComplexNumber result;					//temporay variable to store results
		Complex nativeCaller1=new Complex();
		Complex nativeCaller2=new Complex();

		try{
			result=nativeCaller1.add(r,o);		//calling the native method
			result=nativeCaller1.multiply(k,l);//calling the native method
			nativeCaller2.multiply(o,l);//calling the native method
		}catch(NullPointerException e){
			System.out.print("NullPointerException Caught");
		}
	}


}

class ComplexNumber{
	double r;
	double i;
	ComplexNumber next;
	ComplexNumber(double r, double i, ComplexNumber next){
		this.r=r;
		this.i=i;
		this.next=next;
	}
	public Boolean print(ComplexNumber n){
		ComplexNumber tmp=new ComplexNumber(10.5,5.3,null);
		if (n.r>tmp.r)
			return true;
		return false;
	}
}
