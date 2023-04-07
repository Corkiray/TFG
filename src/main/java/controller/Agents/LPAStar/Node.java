package controller.Agents.LPAStar;

import tools.Vector2d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import core.game.Observation;
import core.game.StateObservation;
import core.vgdl.VGDLRegistry;
import ontology.Types.ACTIONS;
import ontology.Types.WINNER;


//Clase nodo que usaré para las diferentes búsquedas
public class Node {
	public final ArrayList<Vector2d> orientations = (ArrayList<Vector2d>) Collections.unmodifiableList(
		    new ArrayList<Vector2d>() {{
		        add(new Vector2d(-1, 0));
		        add(new Vector2d(1, 0));
		        add(new Vector2d(0, -1));
		        add(new Vector2d(0, 1));
		    }});
		
	//Variable global del factor de escala
	public static Vector2d fescala;

	//Variables globales para contabilizar los objetivos
	static boolean target_goto;
	static boolean target_exists;
	static boolean target_notExists;
	static boolean target_exit;
	
	static String target_name;
	static int target_x;
	static int target_y;
	
	//Variables locales, que definen cada nodo.
	public double x; //Posición en el eje x dentro del mapa del juego
	public double y; //Posición en el eje y dentro del mapa del juego
	Node padre; //Nodo padre
	public ACTIONS accion; //Acción que se realizó para llegar a e´l
	public double f; //(g + h)
	public double g; //Coste para ir del nodo inicial a este
	public double h; //coste hurístico para llegar
	public double rhs;
	StateObservation state = null;
	Vector2d orientation;
	boolean inPath;
	double score;
	Node support;
	int drops;
	
	Map<Node, ACTIONS> pred;

	
	//Constructor en base a un estado
	public Node(StateObservation stateObservation){
		padre = null;
		accion = ACTIONS.ACTION_NIL;
		f = 0;
		g = 0;
		h = 0;
		rhs = 0;
		state = stateObservation;
		x = (int) (state.getAvatarPosition().x / fescala.x);
		y = (int) (state.getAvatarPosition().y / fescala.y);
		orientation = state.getAvatarOrientation();
		inPath=false;
		score = stateObservation.getGameScore();
		support=null;
		pred = new HashMap<>();
		drops=0;
	}
	
	//Constructor en base a un vector
	public Node(Vector2d pos){
		x = (int) pos.x;
		y = (int) pos.y;
		padre = null;
		accion = ACTIONS.ACTION_NIL;
		f = 0;
		g = 0;
		h = 0;
	}
	//Constructor en base a un vector que almacena como padre al nodo pasado como segundo parámetro
	public Node(Vector2d pos, Node p){
		x = (int) pos.x;
		y = (int) pos.y;
		padre = p;
		accion = ACTIONS.ACTION_NIL;
		f = 0;
		g = 0;
		h = 0;
	}
	
	//Constructor sin parámetros, inicializa todo a 0
	public Node() {
		x = y = 0;
		padre = null;
		accion = ACTIONS.ACTION_NIL;
		f = 0;
		g = 0;
		h = 0;
		state = null;
		orientation = null;
		drops=0;
	}
	
	//Constructor de copia
	public Node(Node original) {
		x = original.x;
		y = original.y;
		padre = original.padre;
		accion = original.accion;
		f = original.f;
		g = original.g;
		h = original.h;
		state = original.state.copy();
		orientation = original.orientation;
		inPath=original.inPath;
		support=original.support;
		pred = original.pred;

	}
	
	
	
	public static void setObjetive(ArrayList<String> objetive, StateObservation state){
		target_goto=false;
		target_exists=false;
		target_notExists=false;
		target_exit=false;
				
		if(objetive.get(0).contentEquals("take")) {
			target_notExists=true;
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
			target_exit=true;
			ArrayList<Observation>[] posiciones = state.getPortalsPositions();
			for(ArrayList<Observation> observationList : posiciones){			
				for (Observation observation : observationList) {
					String name = VGDLRegistry.GetInstance().getRegisteredSpriteKey(observation.itype);
					System.out.print(name+"\n");
					if(name.contentEquals("closedoor")) {
						System.out.print("ayawasca\n");
						target_goto = true;
						target_x = (int) (observation.position.x / fescala.x);
						target_y = (int) (observation.position.y / fescala.y);
					}
				}
			}	
		}
		
		if(objetive.get(0).contentEquals("exists")){
			target_exists=true;
			target_name = objetive.get(1);
		}

		if(objetive.get(0).contentEquals("not_exists")){
			target_notExists=true;
			target_name = objetive.get(1);
		}
	}
	
	public double calcular_h(){
		h = 1000;
		
		if(target_exit)
			if (state.isGameOver()) {
				h=0;
				return h;
			}

		//Tengo en cuenta la existencia del objeto
		boolean exists = false;
		if(target_exists || target_notExists) {
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
				h-=score*10;
			}
			else {
				h=0;
			}
			
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
    	
    	if(!e.orientation.equals(orientation))
    		return false;
    	
    	if(e.state.getGameScore() != state.getGameScore()) {
    		return false;
    	}
    	
    	return true;
    }
    
    //Función auxiliar que devuelve el valor asociado a la acción que tiene el nodo.
    //Están ordenados de menor a mayor valor en el orden dado por el guión, para poder facilmente establecer un orden de prioridad
    public int valorAccion() {
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
    			", h= " + h +
    			", inPath " + inPath +
    			"}\n";
    }

	public Node advance(ACTIONS action) {
		state.advance(action);
		return new Node(state);
	}
	
	//Comparador que se usa para el orden en la cola de prioridad
	static Comparator<Node> LPA_NodeComparator = new Comparator<Node>() {
		public int compare(Node n1, Node n2) {
			double k_1 = Math.min(n1.h, n1.rhs);
			double k_2 = Math.min(n2.h, n2.rhs);
			
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

	
	public static Comparator<Node> G_Comparator = new Comparator<Node>() {
		public int compare(Node n1, Node n2) {
			double diff = n1.g - n2.g;
			if (diff > 0) return 1;
			else if (diff < 0) return -1;
			else {
				diff = n1.valorAccion() - n2.valorAccion();
				if(diff > 0) return 1;
				else if(diff < 0) return -1;
				else return 0;
			}
		}
	};

	public Node getFrom(ArrayList<Node> array) {
		for(Node node:array) {
			if(node.equals(this)){
				return node;
			}
		}
		return null;
	}
	
	public Node update_father() {
		Node bestNode = null;
		for (Node node : pred.keySet()) {
			if(bestNode == null) {
				bestNode = node;
			}
			else if(node.g < bestNode.g) {
				bestNode = node;
			}
		}
		padre = bestNode;
		accion = pred.get(bestNode);
		return padre;
	}
	
	ArrayList<Node> generate_pred(){
		ArrayList<Node> pred = new ArrayList<Node>();
		ACTIONS action = get_action(orientation);
		Node aux;
		
		for(Vector2d ori : orientations) {
			aux = new Node(this);
			if(orientation == ori) {
				aux.x = x - ori.x;
				aux.y = y - ori.y;
			}
			else {
				aux.x = x;
				aux.y = y;
			}
			aux.padre = this;
			aux.accion = action;
			aux.orientation = ori;
			g = g - 1;
			
			pred.add(aux);
			
		}	
		aux = new Node(this);
		aux.accion=ACTIONS.ACTION_USE;
		aux.padre=this;
		aux.drops -= 1;
		aux.g-=1;
		
		return pred;
	}
	
	ArrayList<Node> generate_succ(StateObservation state){
		ArrayList<Node> succ = new ArrayList<Node>();
		Node aux;
		
		for(Vector2d ori : orientations) {
			aux = new Node(this);
			if(orientation == ori) {
				aux.x = x + ori.x;
				aux.y = y + ori.y;
			}
			else {
				aux.x = x;
				aux.y = y;
			}
			aux.padre = this;
			aux.orientation=ori;
			aux.accion = get_action(ori);
			aux.g = g + 1;
			
			succ.add(aux);
		}	
		aux = new Node(this);
		aux.accion=ACTIONS.ACTION_USE;
		aux.padre=this;
		aux.drops += 1;
		aux.g+=1;
		
		succ.add(aux);

		return succ;
	}

	ACTIONS get_action(Vector2d orientation) {
		if(orientation.y == 1)
			return ACTIONS.ACTION_DOWN;
		if(orientation.y == -1)
			return ACTIONS.ACTION_UP;
		if(orientation.x == 1)
			return ACTIONS.ACTION_RIGHT;
		if(orientation.x == 1)
			return ACTIONS.ACTION_LEFT;
		
		return ACTIONS.ACTION_NIL;
	}

}
