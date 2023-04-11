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
	public final int DROP_PENALTY = 10;

	
	
    public static GameInformation gameInformation;
        
    public static int MAX_X;
    public static int MAX_Y;
	public static Vector2d fescala;

	public static int maxPeso;	
	
	public static StateObservation lastState;
	
	public static ArrayList<String> recursosTotales;
	
	
	//Variables globales para contabilizar los objetivos
	static boolean target_goto;
	static boolean target_enSuelo;
	static boolean target_obtenido;
	static boolean target_exit;
	
	static String target_name;
	static int target_x;
	static int target_y;
	
	
	//Variables locales, que definen cada nodo.

	int x; //Posición en el eje x dentro del mapa del juego
	int y; //Posición en el eje y dentro del mapa del juego
	Vector2d orientation;
	int peso;
	Map<String, Vector2d> recursosSuelo;
	ArrayList<String> recursosObtenidos;

	Node padre; //Nodo padre
	public ACTIONS accion; //Acción que se realizó para llegar a e´l
	public double f; //(g + h)
	public double g; //Coste para ir del nodo inicial a este
	public double h; //coste hurístico para llegar
	

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
		
		setResources(state);
		
		MAX_X = state.getObservationGrid().length;
		MAX_Y =	state.getObservationGrid()[0].length;
		
		//Calculo el factor de escala píxeles -> grid)		
		fescala = new Vector2d(
			state.getWorldDimension().width / MAX_X,
			state.getWorldDimension().height / MAX_Y );
		
		lastState = state.copy();
		maxPeso = gameInformation.values_correspondence.get("maxPeso");
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
		
		recursosSuelo = new HashMap<String, Vector2d>();
		for(ArrayList<Observation> observationList : stateObservation.getResourcesPositions()){			
			for (Observation observation : observationList) {
				String resource = VGDLRegistry.GetInstance().getRegisteredSpriteKey(observation.itype);
				int aux_x = (int) (observation.position.x / fescala.x);
				int aux_y = (int) (observation.position.y / fescala.y);
				recursosSuelo.put(resource, new Vector2d(aux_x, aux_y));
			}
		}
				
		peso = 0;
		recursosObtenidos = new ArrayList<String>();
		for(String recurso : recursosTotales) {
			if(!recursosSuelo.containsKey(recurso)) {
				recursosObtenidos.add(recurso);
				String name_aux = recurso+"_peso";
				peso += gameInformation.values_correspondence.get(name_aux);	
			}
		}	
		
		
		lastState = stateObservation;
		
		
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
	@SuppressWarnings("unchecked")
	public Node(Node original) {
		x = original.x;
		y = original.y;
		padre = original.padre;
		accion = original.accion;
		f = original.f;
		g = original.g;
		h = original.h;
		peso = original.peso;
		orientation = original.orientation;
		inPath=original.inPath;
		support=original.support;
		recursosSuelo = new HashMap<String, Vector2d>(original.recursosSuelo);
		recursosObtenidos = (ArrayList<String>) original.recursosObtenidos.clone();
		
		rhs=Double.POSITIVE_INFINITY;
		pred = new HashMap<Node, ACTIONS>();
		
		childs = new HashMap<Node, ACTIONS>();
		hijo = original.hijo;
	}
	
	
	public static void setObjetive(ArrayList<String> objetive, StateObservation state){
		//lastState=state;
		
		target_goto=false;
		target_enSuelo=false;
		target_obtenido=false;
		target_exit=false;
		target_x = (int) (state.getAvatarPosition().x / fescala.x);
		target_y = (int) (state.getAvatarPosition().y / fescala.y);

		if(objetive.get(0).contentEquals("take")) {
			target_obtenido=true;
			target_name = objetive.get(1);

			ArrayList<Observation>[] posiciones = state.getResourcesPositions();
			for(ArrayList<Observation> observationList : posiciones){			
				for (Observation observation : observationList) {
					String resource = VGDLRegistry.GetInstance().getRegisteredSpriteKey(observation.itype);
					if(resource.contentEquals(target_name)) {
						target_goto = true;
						target_x = (int) (observation.position.x / fescala.x);
						target_y = (int) (observation.position.y / fescala.y);
					}
				}
			}	
		}

		if(objetive.get(0).contentEquals("exit")) {
			ArrayList<Observation>[] posiciones = state.getPortalsPositions();
			for(ArrayList<Observation> observationList : posiciones){			
				for (Observation observation : observationList) {
					String name = VGDLRegistry.GetInstance().getRegisteredSpriteKey(observation.itype);
					if(name.contentEquals("closedoor")) {
						target_goto = true;
						target_x = (int) (observation.position.x / fescala.x);
						target_y = (int) (observation.position.y / fescala.y);
					}
				}
			}	
		}
		
		if(objetive.get(0).contentEquals("exists")){
			target_enSuelo=true;
			target_name = objetive.get(1);
		}

		if(objetive.get(0).contentEquals("not_exists")){
			target_obtenido=true;
			target_name = objetive.get(1);
		}
	}
	
	
	public static Node GoalNode(StateObservation state) {
		Node ret = new Node(state);
		ret.x = target_x;
		ret.y = target_y;
		ret.orientation = new Vector2d(1,0);
		if(target_name.contains("recurso") && !ret.recursosObtenidos.contains(target_name)) {
			ret.peso+=gameInformation.values_correspondence.get(target_name+"_peso");
			ret.recursosObtenidos.add(target_name);
			ret.recursosSuelo.remove(target_name);
		}
		
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
		
		if(start.peso > peso){
			h+=1;
		}
		
		return h;
	}
	
	public double calcular_h(){
		h = 0;
		
		if(target_obtenido) {
			for(String recurso : recursosObtenidos) {
				if(recurso.contentEquals(target_name)) {
					return h;
				}
			}
		}
		
		
		if(target_goto) {
			double dif_x = target_x - x;
			double dif_y = target_y - y;
			//Tengo en cuenta la distancia
			h+=Math.abs(dif_x) + Math.abs(dif_y);			
		
			//Y la orientacion
			if(dif_x != 0 && orientation.x != dif_x/Math.abs(dif_x))
				h+=1;
			if(dif_y != 0 && orientation.y != dif_y/Math.abs(dif_y))
				h+=1;
			
			if(target_obtenido) {
				String aux_name = target_name+"_peso";
				int needed_peso = gameInformation.values_correspondence.get(aux_name);
				if (peso+needed_peso > maxPeso)
					h+=1;
			}
		}
		
		/*
		if(target_exit)
			if (state.isGameOver()) {
				h=0;
				return h;
			}

		//Tengo en cuenta la existencia del objeto
		boolean exists = false;
		if(target_obtenido || target_enSuelo) {
			ArrayList<Observation>[] posiciones = state.getResourcesPositions();
			for(ArrayList<Observation> observationList : posiciones){			
				for (Observation observation : observationList) {
					String resource = VGDLRegistry.GetInstance().getRegisteredSpriteKey(observation.itype);
					if(resource.contentEquals(target_name)) {
						exists = true;
					}
				}
			}				
			if((exists && target_notExists) || (!exists && target_exists))
				h+=1;
			else {
				h=0;
				return 0;
			}
		}	
		
		if(target_goto) {
			double dif_x = target_x - x;
			double dif_y = target_y - y;
			
			//Tengo en cuenta la distancia
			h+=Math.abs(dif_x) + Math.abs(dif_y);			
		
			//Y la orientacion
			if(dif_x != 0 && orientation.x != dif_x/Math.abs(dif_x))
				h+=1;
			if(dif_y != 0 && orientation.y == dif_y/Math.abs(dif_y))
				h+=1;
			
			if(dif_x != 0 || dif_y != 0) {
				h+=drops*DROP_PENALTY;
			}
			else {
				h=0;
			}
			
		}
		*/
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
    	
    	if(!e.orientation.equals(orientation))
    		return false;
    	
    	if(e.peso != peso) {
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
    			", peso= " + peso +
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
	
	static Comparator<Node> H_NodeComparator = new Comparator<Node>() {
		public int compare(Node n1, Node n2) {
			double diff = n1.h - n2.h;
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
					//Si habia un recurso en esta posición y lo tiene, se suelta
					String recurso = aux.extrac_from(lastState, "recurso", x, y);
					if(recurso!=null && recursosObtenidos.contains(recurso)) {
						aux.recursosObtenidos.remove(recurso);
						aux.recursosSuelo.put(recurso,new Vector2d(x, y));
						aux.peso-= gameInformation.values_correspondence.get(recurso+"_peso");
					}
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
		
		aux = new Node(this);
		aux.accion=ACTIONS.ACTION_USE;
		aux.hijo=this;
		
		if(aux.is_able(lastState)){
			String recurso = null;
			for(Entry<String, Vector2d> set : recursosSuelo.entrySet()) {
				if((set.getValue().x == x+orientation.x) && (set.getValue().y == y+orientation.y)) {
					recurso = set.getKey();
					break;
				}
			}
			if(recurso != null) {
				aux.recursosSuelo.remove(recurso);
				aux.recursosObtenidos.add(recurso);
				aux.peso+= gameInformation.values_correspondence.get(recurso+"_peso");
				pred.add(aux);
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
					String recurso = get_resource(aux.x, aux.y);
					if(recurso != null && checkPreconditions(recurso)) {
						String name_aux = recurso+"_peso";
						int needed_peso = gameInformation.values_correspondence.get(name_aux);	
						if(needed_peso+peso <= maxPeso) {
							aux.recursosObtenidos.add(recurso);
							aux.recursosSuelo.remove(recurso);
							aux.peso+=needed_peso;
						}
					}
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
		
		return succ;
	}

	private boolean checkPreconditions(String recurso) {
		int len = recurso.length();
		int num = Character.getNumericValue(recurso.charAt(len-1));
		num--;
		if(num==0)
			return true;
		else {
			for(String str : recursosObtenidos) {
				if(str.equals("recurso"+num))
					return true;
			}
		}
		return false;
	}

	String get_resource(double x, double y) {
		for(Entry<String, Vector2d> set : recursosSuelo.entrySet()) {
			if((set.getValue().x == x) && (set.getValue().y == y))
				return set.getKey();
		}
		return null;
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
		
		if(exists_in(state, "wall", x, y))
			return false;
		if(accion == ACTIONS.ACTION_USE){
			int aux_x = (int) (x+orientation.x);
			int aux_y = (int) (y+orientation.y);
			if(x+orientation.x >= MAX_X || x < 0 || y+orientation.y >= MAX_Y || y < 0)
				return false;
			if(exists_in(state, "wall", aux_x, aux_y))
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
	
	public static void setResources(StateObservation state) {
		recursosTotales = new ArrayList<String>();
		
		for(ArrayList<Observation> observationList : state.getResourcesPositions()){			
			for (Observation observation : observationList) {
				String resource = VGDLRegistry.GetInstance().getRegisteredSpriteKey(observation.itype);
				recursosTotales.add(resource);
			}
		}	
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
