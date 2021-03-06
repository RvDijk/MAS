package logic;

import org.graphstream.graph.Node;

public class And implements Formula {

	private Formula f1, f2;
	
	public And(Formula f1, Formula f2) {
		this.f1 = f1;
		this.f2 = f2;
	}

	@Override
	public boolean evaluate(Node n) {
		return f1.evaluate(n) && f2.evaluate(n);
	}
	
	@Override
	public String pprint() {
		return f1.pprint() + " " + Character.toString((char) 8743) +" " + f2.pprint();
	}

}
