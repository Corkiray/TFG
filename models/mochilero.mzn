set of int: posibles_recursos = 1..7;

int: maxPeso = 10;

array [posibles_recursos] of int: pesos = [1,2,3,4,2,0,2];
array [posibles_recursos] of int: valores = [1,2,3,4,5,5,4];
array [posibles_recursos] of var 0..1: estanEscogidos;
array [posibles_recursos] of var 0..1: estanEnMapa;

var int: valorTotal;
var int: peso;

int: recurso1;
int: recurso2;
int: recurso3;
int: recurso4;
int: recurso5;
int: recurso6;
int: recurso7;

constraint recurso1 == estanEnMapa[1];
constraint recurso2 == estanEnMapa[2];
constraint recurso3 == estanEnMapa[3];
constraint recurso4 == estanEnMapa[4];
constraint recurso5 == estanEnMapa[5];
constraint recurso6 == estanEnMapa[6];
constraint recurso7 == estanEnMapa[7];

constraint forall(i in posibles_recursos)( (estanEnMapa[i] == 0) -> (estanEscogidos[i] == 0) );
constraint valorTotal == sum(i in posibles_recursos)(valores[i]*estanEscogidos[i]);
constraint peso == sum(i in posibles_recursos)(pesos[i]*estanEscogidos[i]);
constraint peso <= maxPeso;

solve maximize valorTotal;

output ["recurso\(i)=\(estanEscogidos[i])\n" | i in posibles_recursos];



