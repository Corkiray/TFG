package tracks.singlePlayer.evaluacion.src_PUERTA_MERINO_ISRAEL;

import tools.Vector2d;
import ontology.Types.ACTIONS;


//Clase nodo que usaré para las diferentes búsquedas
public class Nodo {
	int x; //Posición en el eje x dentro del mapa del juego
	int y; //Posición en el eje y dentro del mapa del juego
	Nodo padre; //Nodo padre
	ACTIONS accion; //Acción que se realizó para llegar a e´l
	int f; //(g + h)
	int g; //Coste para ir del nodo inicial a este
	int h; //coste hurístico para llegar
	
	//Constructor en base a un vector
	public Nodo(Vector2d pos){
		x = (int) pos.x;
		y = (int) pos.y;
		padre = null;
		accion = ACTIONS.ACTION_NIL;
		f = 0;
		g = 0;
		h = 0;
	}
	
	//Constructor en base a un vector que almacena como padre al nodo pasado como segundo parámetro
	public Nodo(Vector2d pos, Nodo p){
		x = (int) pos.x;
		y = (int) pos.y;
		padre = p;
		accion = ACTIONS.ACTION_NIL;
		f = 0;
		g = 0;
		h = 0;
	}
	
	//Constructor sin parámetros, inicializa todo a 0
	public Nodo() {
		x = y = 0;
		padre = null;
		accion = ACTIONS.ACTION_NIL;
		f = 0;
		g = 0;
		h = 0;
	}
	
	//Constructor de copia
	public Nodo(Nodo original) {
		x = original.x;
		y = original.y;
		padre = original.padre;
		accion = original.accion;
		f = original.f;
		g = original.g;
		h = original.h;
	}
	
	//Constructor de copia que, además, recalcula los valores f y h (para ello, necesita el nodo objetivo de referencia)
	public Nodo(Nodo original, Nodo objetivo) {
		x = original.x;
		y = original.y;
		padre = original.padre;
		accion = original.accion;
		g = original.g;
		calcular_h(objetivo);
		calcular_f();
	}
	
	//Función auxiliar que, dado un nodo objetivo, calcula y establece el coste heurístico de llegar según la distancia Manhattan
	public int calcular_h(Nodo objetivo) {
		h = Math.abs(objetivo.x - x) + Math.abs(objetivo.y - y);
		return h;
	}
	
	//Funcion auxiliar que recalcula f
	public int calcular_f() {
		f = h+g;
		return f;
	}
	   
    //Criterio que se usa para comparar si dos nodos son iguales. 
    //Para ello, solo se tiene en cuenta la posición
    public boolean equals (Object o) {
    	Nodo e = (Nodo) o;
    	if((e.x == this.x) && (e.y == this.y)) return true;
    	else return false;
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
   		default:
    		return 5;
    	}
    }
    
    @Override
    public String toString() {
    	return "Nodo{" + 
    			"posicion= (" + x + ", " + y + ") " +
    			", accion= " + accion + 
    			", h= " + h +
    			"}\n";
    }
}
