package model;

import java.awt.Color;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;
import org.graphstream.util.VerboseSink;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import logic.Atom;
import logic.Formula;
import logic.Knows;

public class Model extends MultiGraph implements ViewerListener {
	
	private int worldCount;
	
	private ArrayList<String> clickedWorlds = new ArrayList<>();

	private ArrayList<Node> selectedNodes = new ArrayList<>();
	private ArrayList<String> messages = new ArrayList<>();

	public Model() {
		super("Arbitrary String #1");
		this.worldCount = 0;

		Socket socket;
		try {

			JPanel panel = new JPanel(new GridLayout(0, 1));
			panel.add(new JLabel("You can play the game without hosting at: https://mas-ek.herokuapp.com/"));
			panel.add(new JLabel("When hosting yourself, make sure to use port 3000 on localhost."));

			String[] buttons = { "Connect to Heroku Server", "Connect Localhost (localhost:3000)", "Exit" };

			int result = JOptionPane.showOptionDialog(null, panel, "Connect to game", JOptionPane.WARNING_MESSAGE, 0,
					null, buttons, buttons[0]);

			if (result == 0) {
				socket = IO.socket("https://mas-ek.herokuapp.com/");
			} else if (result == 1) {
				socket = IO.socket("http://localhost:3000");
			} else {
				socket = IO.socket("https://mas-ek.herokuapp.com/");
				System.exit(0);
			}

			socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

				@Override
				public void call(Object... args) {
					socket.emit("connection", "hi");
					System.out.println("Connected");
				}

			}).on("message", new Emitter.Listener() {

				@Override
				public void call(Object... args) {
					String message = args[0].toString();
					messages.add(message);
					String[] substrings = message.split(" ");
					if(substrings.length > 0){
						String type = substrings[0];
						ArrayList<String> arguments = new ArrayList<String>(Arrays.asList(Arrays.copyOfRange(substrings,1,substrings.length)));
						System.out.println("New message of type " + type + " with arguments " + arguments.toString());
						update(type,arguments);
					}else{
						System.err.println("Invalid message: " + message);
					}
				}

			}).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

				@Override
				public void call(Object... args) {
					System.out.println("Disconnected");
				}

			});
			socket.connect();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		addNode(getWorldName());
		addNode(getWorldName());
		addNode(getWorldName());

		Iterator<Node> nodes = getNodeIterator();
		Random r = new Random();
		while (nodes.hasNext()) {
			Node n = nodes.next();
			if (r.nextBoolean())
				addAtom(n.getId(), "p");
			if (r.nextBoolean())
				addAtom(n.getId(), "q");
			n.setAttribute("ui.label", n.getAttribute("atoms").toString());
		}

		addRelation("w1", "w2", "Henk");//declare relation with one initial agent
		addRelation("w1", "w1", "Henk");
		addRelation("w1", "w3", "Up");
		addRelation("w3", "w1", "Down");
		addRelation("w2", "w3", "Henry");
		addRelation("w1", "w2", "Joost");

		System.out.println(new Atom("p").evaluate(getNode("w1")));
		System.out.println(new Atom("p").evaluate(getNode("w2")));
		System.out.println(new Atom("p").evaluate(getNode("w3")));
		Formula f = new Knows(new Atom("p"), "Henk");
		System.out.println(f.evaluate(getNode("w1")));

		System.out.println();

		ViewerPipe viewPipe = display().newViewerPipe();
		viewPipe.addViewerListener(this);
        viewPipe.addSink(new VerboseSink());
        viewPipe.pump();
            	
		
        while (true) {
            viewPipe.pump();
        }
	}
	
	private String getWorldName(){
		//generate the next world id
		return "w" + ++worldCount;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Node addNode(String id) {
		Node n = super.addNode(id);
		n.setAttribute("atoms", new ArrayList<String>());
		return n;
	}

	public void addAtom(String node, String atom) {
		Node n = getNode(node);
		ArrayList<String> atoms = n.getAttribute("atoms");
		atoms.add(atom);
	}
	
	public void constructFromFile(String s)
	{
		try {
			BufferedReader in = new BufferedReader(new FileReader(s));
			
			String line;
			while((line = in.readLine()) != null)
			{
			    System.out.println(line);
			    String[] args = line.split(" ");
			    if (args.length == 2)
			    	addAtom(args[0], args[1]);
			    else if (args.length == 4 && args[2].equals("B"))
			    {
			    	addRelation(args[0], args[1], args[3]);
			    	addRelation(args[1], args[0], args[3]);
			    }
			    else if (args.length == 4 && args[2].equals("D"))
			    	addRelation(args[0], args[1], args[3]);
			}
			in.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Edge addEdge(String agent, String idFrom, String idTo) {
		//Adds an edge between two world, with at least one agent
		//DIRECT USE NOT RECOMMENDED
		//This method assumes the edge doesn't already exist, you'll have to check this before
		//Using addRelation() does this for you.
		//(but we could not make this method private as it is inherited)
		Edge e = super.addEdge(idFrom + idTo, idFrom, idTo, true);
		if (getEdge(idTo + idFrom) != null) {
			// symmetric relation, do some styling
			e.setAttribute("ui.class", "symmetric");// tag only applies to one
													// side to separate the
													// labels
		}
		if (idFrom.equals(idTo)) {
			// reflexive relation, tag it
			e.setAttribute("ui.class", "reflexive");
		}
		ArrayList<String> agents = new ArrayList<String>();
		agents.add(agent);
		e.setAttribute("agents", agents);
		return e;
	}

	public void addRelation(String idFrom, String idTo, String agent) {
		//adds a relation for an agent between two worlds
		Edge e = getEdge(idFrom+idTo);
		if(e == null){
			//need to add the edge
			e = addEdge(agent,idFrom,idTo);
		}else{
			ArrayList<String> agents = e.getAttribute("agents");
			agents.add(agent);
		}
	}
	
	public void removeRelation(String idFrom, String idTo, String agent){
		//remove a relation for an agent between two world
		Edge e = getEdge(idFrom+idTo);
		if(e != null){
			ArrayList<String> agents = e.getAttribute("agents");
			if(agents.contains(agent)){
				agents.remove(agent);
				if(agents.isEmpty()){
					removeEdge(idFrom+idTo);
				}
				return;
			}
		}
		System.err.println("Tried to remove agent " + agent + "on relation " + idFrom + "->" + idTo + "while that relation wasn't there!");
	}

	public ArrayList<String> getAtoms(String node) {
		return getNode(node).getAttribute("atoms");
	}

	@Override
	public Viewer display() {
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		addAttribute("ui.antialias");
		addAttribute("ui.quality");// remove if real-time rendering becomes laggy

		String stylesheet;
		try {
			Scanner s = new Scanner(new File("graphstyle.css"));
			stylesheet = s.useDelimiter("\\Z").next();
			s.close();
		} catch (FileNotFoundException e1) {
			System.err.println("Stylesheet not found!");
			stylesheet = "";
			e1.printStackTrace();
		}
		addAttribute("ui.stylesheet", stylesheet);

		Iterator<Node> nodes = getNodeIterator();
	
		while (nodes.hasNext()) {
			Node n = nodes.next();
			n.addAttribute("ui.color", new Color(0, 0, 0));
			n.setAttribute("ui.label", " " + n.getId() + ": " + n.getAttribute("atoms").toString());
		}
		Iterator<Edge> edges = getEdgeIterator();
		while (edges.hasNext()) {
			Edge e = edges.next();
			e.setAttribute("ui.label", "");	
		}
		
		return super.display();
	}
	
	private void update(String type, ArrayList<String> args){
		switch(type){
			case "STF":
				
				break;
			case "BS":
				
				break;
			case "NS":
				
				break;
			case "DS":
				
				break;
			case "NP":
				
				break;
			case "SH":
				
				break;
			case "EK":
				
				break;
			case "ATT":
				
				break;
			case "FV":
				
				break;
			case "S1":
				Node n = addNode(getWorldName());
				addAtom(n.getId(),"c1");
				break;
			case "S3":
				
				break;
			case "AF":
				
				break;
			case "SP":
				
				break;
			case "DC":
				
				break;
			default:
				
				break;
		}
	}

	public static void main(String[] args) {
		new Model();
	}

	@Override
	public void viewClosed(String viewName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void buttonPushed(String id) {
		// TODO Auto-generated method stub
		
		Iterator<Edge> it = getNode(id).getEachEdge().iterator();
		
		if (selectedNodes.contains(getNode(id)))
		{
			selectedNodes.remove(getNode(id));
			getNode(id).removeAttribute("ui.color");
			getNode(id).addAttribute("ui.color", new Color(0, 0, 0));
		}
		else
		{
			selectedNodes.add(getNode(id));
			getNode(id).removeAttribute("ui.color");
			getNode(id).addAttribute("ui.color", new Color(255, 0, 0));
		}
		
		Iterator<Node> it3 = super.getNodeIterator();
		
		while (it3.hasNext())
		{
			Node n3 = it3.next();
			Iterator<Node> it4 = super.getNodeIterator();
			while (it4.hasNext())
			{
				Node n4 = it4.next();
				if (n3 != null && n4 != null)
				{
					System.out.println(n3.getId() + " " + n4.getId());
					Edge e2 = getEdge(n3.getId() + n4.getId());
					if (e2 != null && selectedNodes.contains(n3) && selectedNodes.contains(n4))
					{
						e2.setAttribute("ui.label", e2.getAttribute("agents").toString());
					}
					else if (e2 != null)
					{
						e2.setAttribute("ui.label", "");
					}
				}
			}
		}
		
	}

	@Override
	public void buttonReleased(String id) {
		// TODO Auto-generated method stub
		
	}
}