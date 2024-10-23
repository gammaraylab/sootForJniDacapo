class Harness{
	public static void main(String[] args) {
		SimpleRecursion sr= new SimpleRecursion();

		Complex k= new Complex(6,4);

		Complex result=sr.factorial(k);

		System.out.println("Factoral of "+k.r+"+"+k.i+" is "+result.r+"+"+result.i);	
	}
}

class Complex{
	long r;
	long i;
	Complex(){
		r=0;
		i=0;
	}
	Complex(long a, long b){
		r=a;
		i=b;
	}

	public Complex subtract(Complex other){
		Complex tmp= new Complex();
		tmp.r= r-other.r;
		tmp.i= i-other.i;

		return tmp;
	}
	public Complex multiply(Complex other){
		Complex tmp= new Complex();
		tmp.r= r*other.r-i*other.i;
		tmp.i= r*other.i+i*other.r;

		return tmp;
	}
	public boolean isUnity(){
		return r==1 || i==1;
	}

}
class SimpleRecursion{
	Complex  factorial(Complex n){
		if(n.isUnity())
			return new Complex(1,1);
		return n.multiply(factorial(n.subtract(new Complex(1,1))));
	}
}