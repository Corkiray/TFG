set of int: posibles_recursos = 1..24;

int: maxPeso = 10;

array [posibles_recursos] of int: pesos = [1,2,3,4,2,0, 2, 1, 4, 5, 2, 3, 3, 1, 7, 2, 6, 3, 4, 9, 1, 7, 2, 6];
array [posibles_recursos] of int: valores = [1,2,3,4,5,5, 4, 6, 3, 2, 3, 1, 9, 2, 5, 6, 1, 3, 4, 5, 3, 2, 4, 4];
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
int: recurso8 = 1;
int: recurso9 = 1;
int: recurso10 = 1;
int: recurso11 = 1;
int: recurso12 = 1;
int: recurso13 = 1;
int: recurso14 = 1;
int: recurso15 = 1;
int: recurso16 = 1;
int: recurso17 = 1;
int: recurso18 = 1;
int: recurso19 = 1;
int: recurso20 = 1;
int: recurso21 = 1;
int: recurso22 = 1;
int: recurso23 = 1;
int: recurso24 = 1;

constraint recurso1 == estanEnMapa[1];
constraint recurso2 == estanEnMapa[2];
constraint recurso3 == estanEnMapa[3];
constraint recurso4 == estanEnMapa[4];
constraint recurso5 == estanEnMapa[5];
constraint recurso6 == estanEnMapa[6];
constraint recurso7 == estanEnMapa[7];
constraint recurso8 == estanEnMapa[8];
constraint recurso9 == estanEnMapa[9];
constraint recurso10 == estanEnMapa[10];
constraint recurso11 == estanEnMapa[11];
constraint recurso12 == estanEnMapa[12];
constraint recurso13 == estanEnMapa[13];
constraint recurso14 == estanEnMapa[14];
constraint recurso15 == estanEnMapa[15];
constraint recurso16 == estanEnMapa[16];
constraint recurso17 == estanEnMapa[17];
constraint recurso18 == estanEnMapa[18];
constraint recurso19 == estanEnMapa[19];
constraint recurso20 == estanEnMapa[20];
constraint recurso21 == estanEnMapa[21];
constraint recurso22 == estanEnMapa[22];
constraint recurso23 == estanEnMapa[23];
constraint recurso24 == estanEnMapa[24];

constraint forall(i in posibles_recursos)( (estanEnMapa[i] == 0) -> (estanEscogidos[i] == 0) );
constraint valorTotal == sum(i in posibles_recursos)(valores[i]*estanEscogidos[i]);
constraint peso == sum(i in posibles_recursos)(pesos[i]*estanEscogidos[i]);
constraint peso <= maxPeso;

solve maximize valorTotal;

output ["recurso\(i)=\(estanEscogidos[i])\n" | i in posibles_recursos];



