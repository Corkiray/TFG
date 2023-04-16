package controller.Agents;

import tools.Vector2d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

import controller.GameInformation;
import core.game.Observation;
import core.game.StateObservation;
import core.vgdl.VGDLRegistry;
import ontology.Types.ACTIONS;
import ontology.Types.WINNER;

import java.util.HashMap;

public class Node {
	public final ArrayList<Vector2d> orientations = new ArrayList<Vector2d>(
		    new ArrayList<Vector2d>() {{
		        add(new Vector2d(-1, 0));
		        add(new Vector2d(1, 0));
		        add(new Vector2d(0, -1));
		        add(new Vector2d(0, 1));
		    }});
	
	public static boolean RTA_MODE;
	
    public static GameInformation gameInformation;
        
    public static int MAX_X;
    public static int MAX_Y;
	public static Vector2d fescala;
	
	public static StateObservation lastState;
		
		
	static String objetive_type;
	static String target_name;
	static int target_x;
	static int target_y;
	
	
	//Variables locales, que definen cada nodo.

	int x; //Posición en el eje x dentro del mapa del juego
	int y; //Posición en el eje y dentro del mapa del juego
	Vector2d orientation;

	
	Node padre; //Nodo padre
	public ACTIONS accion; //Acción que se realizó para llegar a e´l
	public double f; //(g + h)
	public double g; //Coste para ir del nodo inicial a este
	public double h; //coste hurístico para llegar
	

	//Variables para RTA
	int c;
	
	//Variables para LRTA_K
	boolean inPath;
	Node support;
	
	//Variables para LPA*
	public double rhs;
	Map<Node, ACTIONS> pred;
	
	//Variable para D*
	Map<Node, ACTIONS> childs;
	Node hijo;
	int last_km;

	
	
	public static void initialize(GameInformation gameInf, StateObservation state) {
		gameInformation = gameInf;
				
		MAX_X = state.getObservationGrid().length;
		MAX_Y =	state.getObservationGrid()[0].length;
		
		//Calculo el factor de escala píxeles -> grid)		
		fescala = new Vector2d(
			state.getWorldDimension().width / MAX_X,
			state.getWorldDimension().height / MAX_Y );
		
		lastState = state.copy();
		
		RTA_MODE = false;
	}
	
	public static void initialize(GameInformation gameInf, StateObservation state, boolean rta_mode) {
		initialize(gameInf, state);
		RTA_MODE = rta_mode;
	}
	
	//Constructor en base a un estado
	public Node(StateObservation stateObservation){
		padre = null;
		accion = ACTIONS.ACTION_NIL;
		f = 0;
		g = 0;
		h = 0;
		x = (int) (stateObservation.getAvatarPosition().x / fescala.x);
		y = (int) (stateObservation.getAvatarPosition().y / fescala.y);
		orientation = stateObservation.getAvatarOrientation();
				
		lastState = stateObservation;
		
		c=0;
		
		inPath=false;
		support=null;

		
		rhs=0;
		pred = new HashMap<>();
		
		childs = new HashMap<>();
		hijo = null;
		last_km = 0;
	}

	
	//Constructor en base a un vector que almacena como padre al nodo pasado como segundo parámetro
	//Constructor de copia
	public Node(Node original) {
		x = original.x;
		y = original.y;
		padre = original.padre;
		accion = original.accion;
		f = original.f;
		g = original.g;
		h = original.h;
		c = original.c;
		orientation = original.orientation;
		inPath=original.inPath;
		support=original.support;
		
		rhs=Double.POSITIVE_INFINITY;
		pred = new HashMap<Node, ACTIONS>();
		
		childs = new HashMap<Node, ACTIONS>();
		hijo = original.hijo;
	}
	
	
	public static void setObjetive(ArrayList<String> objetive, StateObservation state){		
		lastState = state;
		
		target_x = (int) (state.getAvatarPosition().x / fescala.x);
		target_y = (int) (state.getAvatarPosition().y / fescala.y);

		if(objetive.get(0).contentEquals("take")) {
			objetive_type = "goto";
			target_name = objetive.get(1);
		}
		if(objetive.get(0).contentEquals("closedoor")) {			
			objetive_type = "goto";
			target_name = objetive.get(0);
		}
		if(objetive.get(0).contentEquals("drop")) {
			objetive_type = "drop";
			target_name = objetive.get(1);
		}

		for (ArrayList<Observation>[] fila : state.getObservationGrid()) {
			for(ArrayList<Observation> celda : fila) {
				for(Observation observation : celda) {
					String obs_name = VGDLRegistry.GetInstance().getRegisteredSpriteKey(observation.itype);
					if(obs_name.contentEquals(target_name)) {
						target_x = (int) (observation.position.x / fescala.x);
						target_y = (int) (observation.position.y / fescala.y);
					}						
				}
			}
		}
	
	}
	
	
	public static Node GoalNode(StateObservation state) {
		Node ret = new Node(state);
		ret.x = target_x;
		ret.y = target_y;
		ret.orientation = new Vector2d(1,0);
		
		return ret;
	}
	
	public double get_relative_h_from(Node start) {
		h=0;
		
		double dif_x = x - start.x;
		double dif_y = y - start.y;
		//Tengo en cuenta la distancia
		h+=Math.abs(dif_x) + Math.abs(dif_y);			
	
		//Y la orientacion
		if(dif_x != 0 && start.orientation.x != dif_x/Math.abs(dif_x))
			h+=1;
		if(dif_y != 0 && start.orientation.y != dif_y/Math.abs(dif_y))
			h+=1;
		
		return h;
	}
	
	public double calcular_h(){
		h = 0;
				
		if(objetive_type.contentEquals("goto")) {
			double dif_x = target_x - x;
			double dif_y = target_y - y;
			//Tengo en cuenta la distancia
			h+=Math.abs(dif_x) + Math.abs(dif_y);			
		
			//Y la orientacion
			if(dif_x != 0 && orientation.x != dif_x/Math.abs(dif_x))
				h+=1;
			if(dif_y != 0 && orientation.y != dif_y/Math.abs(dif_y))
				h+=1;

		}
		
		if(objetive_type.contentEquals("drop")) {
			//POR TERMINAR AAAA
		}
		
		return h;
	}
	
	//Funcion auxiliar que recalcula f
	public double calcular_f() {
		f = h+g;
		return f;
	}
	   
    //Criterio que se usa para comparar si dos nodos son iguales. 
    //Para ello, se tiene en cuenta la posición, orientacion y número de recursos en juego
    public boolean equals (Object o) {
    	if(o==null) return false;
    	
    	Node e = (Node) o;
    	
    	if((e.x != x) || (e.y != y)) 
    		return false;
    	
    	if(!RTA_MODE) {
    		if(!e.orientation.equals(orientation))
    			return false;  		
    	}
    	
    	return true;
    }
    
    //Función auxiliar que devuelve el valor asociado a la acción que tiene el nodo.
    //Están ordenados de menor a mayor valor en el orden dado por el guión, para poder facilmente establecer un orden de prioridad
    public int valorAccion() {
    	if(accion==null)
    		return 6;
    	switch(accion){
    	case ACTION_UP:
    		return 1;
    	case ACTION_DOWN:
    		return 2;
    	case ACTION_LEFT:
    		return 3;
    	case ACTION_RIGHT:
    		return 4;
    	case ACTION_USE:
    		return 5;
   		default:
    		return 6;
    	}
    }
    
    public String toString() {
    	return "Nodo{" + 
    			"posicion= (" + x + ", " + y + ") " +
    			"orientacion= " + orientation +
    			", accion= " + accion + 
    			", g= " + g +
    			", rhs= " + rhs +
    			", last_km= " + last_km +
    			", h= " + h +
    			", inPath " + inPath +
    			"}\n";
    }

	//Comparador que se usa para el orden en la cola de prioridad
	//Primero se tiene en cuenta el valor de f, luego el de g y por último la acción a realizar
	static Comparator<Node> NodeComparator = new Comparator<Node>() {
		public int compare(Node n1, Node n2) {
			double diff = n1.f - n2.f;
			if( diff > 0) return 1;
			else if (diff < 0) return -1;
			else {
				diff = n1.g - n2.g;
				if (diff > 0) return 1;
				else if (diff < 0) return -1;
				else {
					diff = n1.valorAccion() - n2.valorAccion();
					if(diff > 0) return 1;
					else if(diff < 0) return -1;
					else return 0;
				}
			}
		}
	};
	
	static Comparator<Node> RTA_NodeComparator = new Comparator<Node>() {
		public int compare(Node n1, Node n2) {
			double diff = n1.h + n1.c - n2.h - n2.c;
			if( diff > 0) return 1;
			else if (diff < 0) return -1;
			else {
				diff = n1.valorAccion() - n2.valorAccion();
				if(diff > 0) return 1;
				else if(diff < 0) return -1;
				else return 0;
			}
		}
	};
	
	ArrayList<Node> generate_pred(){
		ArrayList<Node> pred = new ArrayList<Node>();
		
		Node aux;
		
		ACTIONS action = get_action(orientation);
		for(Vector2d ori : orientations) {
			aux = new Node(this);
			if(orientation.equals(ori)) {
				aux.x = x - (int) ori.x;
				aux.y = y - (int) ori.y;
				if(aux.is_able(lastState)) {
					aux.orientation = ori;
					aux.hijo=this;
					aux.accion=action;
					pred.add(aux);
				}

			}
			else {
				aux.x = x;
				aux.y = y;
				if(aux.is_able(lastState)) {
					aux.hijo = this;
					aux.accion = action;
					aux.orientation = ori;					
					pred.add(aux);
				}
			}			
		}	
				
		return pred;
	}
	
	ArrayList<Node> generate_succ(){
		ArrayList<Node> succ = new ArrayList<Node>();
		Node aux;
		
		for(Vector2d ori : orientations) {
			aux = new Node(this);
			if(orientation.equals(ori)) {
				aux.x = x + (int) ori.x;
				aux.y = y + (int) ori.y;
				if(aux.is_able(lastState)) {					
					aux.padre = this;
					aux.orientation=ori;
					aux.accion = get_action(ori);
					aux.g = g + 1;
					aux.calcular_h();
					aux.calcular_f();	

					succ.add(aux);
				}
			}
			else {
				aux.x = x;
				aux.y = y;
				
				if(aux.is_able(lastState)) {					
					aux.padre = this;
					aux.orientation=ori;
					aux.accion = get_action(ori);
					aux.g = g + 1;
					aux.calcular_h();
					aux.calcular_f();
					succ.add(aux);
				}
			}			
		}	
		
		
		/*

		aux = new Node(this);
		aux.accion=ACTIONS.ACTION_USE;
		aux.padre=this;
		if(aux.is_able(lastState)
		&& !aux.recursosObtenidos.isEmpty()
		&& !aux.exists_in(lastState, "wall", (int) (aux.x+aux.orientation.x), (int) (aux.y+aux.orientation.y))
		&& !aux.exists_in(lastState, "recurso", (int) (aux.x+aux.orientation.x), (int) (aux.y+aux.orientation.y))){
			String recurso = aux.recursosObtenidos.remove(0);
			String aux_name = recurso+"_peso";
			aux.peso-= gameInformation.values_correspondence.get(aux_name);
			aux.recursosSuelo.put(recurso, new Vector2d(aux.x+aux.orientation.x, aux.y+aux.orientation.y));
			aux.g+=1;
			aux.calcular_h();
			aux.calcular_f();
			succ.add(aux);		
		}
		
		*/
		
		return succ;
	}

	ArrayList<Node> generate_rta_succ(){
		ArrayList<Node> succ = new ArrayList<Node>();
		Node aux;
		
		for(Vector2d ori : orientations) {
			aux = new Node(this);
			aux.x = x + (int) ori.x;
			aux.y = y + (int) ori.y;
			if(aux.is_able(lastState)) {					
				if(orientation.equals(ori)) {
					aux.c = 1;
				}
				else {
					aux.c = 2;
				}
				
				aux.padre = this;
				aux.accion = get_action(ori);
				aux.orientation = ori;
				
				succ.add(aux);
			}				
		}
		
		return succ;
	}
	
	ACTIONS get_action(Vector2d orientation) {
		if(orientation.y == 1)
			return ACTIONS.ACTION_DOWN;
		if(orientation.y == -1)
			return ACTIONS.ACTION_UP;
		if(orientation.x == 1)
			return ACTIONS.ACTION_RIGHT;
		if(orientation.x == -1)
			return ACTIONS.ACTION_LEFT;
		
		return ACTIONS.ACTION_NIL;
	}
	
	boolean exists_in(StateObservation state, String object, int x, int y) {
		if(x >= MAX_X || x < 0 || y >= MAX_Y || y < 0)
			return false;
		for(Observation obs : state.getObservationGrid()[x][y]) {
			String name = VGDLRegistry.GetInstance().getRegisteredSpriteKey(obs.itype).toLowerCase();
			if(name.contains(object))
				return true;
		}
		return false;
	}
	
	String extrac_from(StateObservation state, String object, int x, int y) {
		if(x >= MAX_X || x < 0 || y >= MAX_Y || y < 0)
			return null;
		for(Observation obs : state.getObservationGrid()[x][y]) {
			String name = VGDLRegistry.GetInstance().getRegisteredSpriteKey(obs.itype).toLowerCase();
			if(name.contains(object))
				return name;
		}
		return null;
	}

	public boolean is_able(StateObservation state) {
		if(x >= MAX_X || x < 0 || y >= MAX_Y || y < 0)
			return false;
		for(Observation obs : state.getObservationGrid()[x][y]) {
			String name = VGDLRegistry.GetInstance().getRegisteredSpriteKey(obs.itype).toLowerCase();
			if(name.contains(target_name))
				return true;
			else if(name.contains("wall"))
				return false;
			else if(name.contains("recurso"))
				return false;
			
		}
		
		if(accion == ACTIONS.ACTION_USE){
			int aux_x = (int) (x+orientation.x);
			int aux_y = (int) (y+orientation.y);
			if(x+orientation.x >= MAX_X || x < 0 || y+orientation.y >= MAX_Y || y < 0)
				return false;
			if(exists_in(state, "wall", aux_x, aux_y))
				return false;
			if(exists_in(state, "recurso", aux_x, aux_y))
				return false;
		}
		
		return true;
	}
	
	public Node getFrom(ArrayList<Node> array) {
		for(Node node:array) {
			if(node.equals(this)){
				return node;
			}
		}
		return null;
	}
		
	//Comparador que se usa para el orden en la cola de prioridad
	static Comparator<Node> LPA_NodeComparator = new Comparator<Node>() {
		public int compare(Node n1, Node n2) {
			double k_1 = Math.min(n1.g, n1.rhs);
			double k_2 = Math.min(n2.g, n2.rhs);
			
			double diff = k_1 + n1.h - k_2 - n2.h;
			if( diff > 0) return 1;
			else if (diff < 0) return -1;
			else {
				diff = k_1 - k_2;
				if (diff > 0) return 1;
				else if (diff < 0) return -1;
				else {
					diff = n1.valorAccion() - n2.valorAccion();
					if(diff > 0) return 1;
					else if(diff < 0) return -1;
					else return 0;
				}
			}
		}
	};
	
	public Node update_father() {
		Node bestNode = null;
		for (Node node : pred.keySet()) {
			if(node.is_able(lastState)) {			
				if(bestNode == null) {
					bestNode = node;
				}
				else if(node.g < bestNode.g) {
					bestNode = node;
				}
			}
		}
		padre = bestNode;
		accion = pred.get(bestNode);
		return padre;
	}

	public Node update_bestChild() {
		Node bestNode = null;
		for (Node node : childs.keySet()) {
			if(node.is_able(lastState)) {			
				if(bestNode == null) {
					bestNode = node;
				}
				else if(node.g < bestNode.g) {
					bestNode = node;
				}
			}
		}
		hijo = bestNode;
		accion = childs.get(bestNode);
		return hijo;
	}

	
	//Comparador que se usa para el orden en la cola de prioridad
	static Comparator<Node> D_NodeComparator = new Comparator<Node>() {
		public int compare(Node n1, Node n2) {
			double k_1 = Math.min(n1.g, n1.rhs);
			double k_2 = Math.min(n2.g, n2.rhs);
			
			int diff = (int) ( k_1 + n1.h + n1.last_km - k_2 - n2.h - n2.last_km);
			if( diff > 0) return 1;
			else if (diff < 0) return -1;
			else {
				diff = (int) (k_1 - k_2);
				if (diff > 0) return 1;
				else if (diff < 0) return -1;
				else return 0;
			}
		}
	};



	public void update_key(int km, Node inicial) {
		last_km = km;
		h = get_relative_h_from(inicial);
	}



}
