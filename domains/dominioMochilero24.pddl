(define (domain dominioMochilero)
  (:requirements :strips :typing :adl :negative-preconditions :equality :fluents)
    (:types
        Recurso
    )    
    (:constants
        Recurso1 Recurso2 Recurso3 Recurso4 Recurso5 Recurso6 Recurso7 Recurso8 Recurso9 Recurso10 Recurso11 Recurso12 Recurso13 Recurso14 Recurso15 Recurso16 Recurso17 Recurso18 Recurso19 Recurso20 Recurso21 Recurso22 Recurso23 Recurso24 - Recurso
    )
    (:functions
        (peso ?r - Recurso)
        (posicion ?r - Recurso)
        (maxPeso)
        (actualPeso)
    )
    (:predicates
        (enSuelo ?e - Recurso)
        (enMochila ?r - Recurso) 
        (llaveCogida ?r - Recurso)
        (requiere ?a - Recurso ?b - Recurso)
        (haySalida)
    )
    (:action coger
        :parameters (?r - Recurso)
        :precondition
            ( and
                (enSuelo ?r)
                (haySalida)
                (forall (?prec - Recurso) 
                    (or
                        (not (requiere ?r ?prec))
                        (llaveCogida ?prec)
                    )
                )
                (>= 
                    (maxPeso)
                    (+ 
                        (actualPeso)
                        (peso ?r)
                    )
                )
            )
        :effect
            (and
                (enMochila ?r)
                (llaveCogida ?r)
                (not (enSuelo ?r))
                (increase (actualPeso) (peso ?r))
            )
    )
    (:action soltar
        :parameters (?r - Recurso)
        :precondition 
        (and 
		(haySalida)
            (enMochila ?r)
            (forall (?pre - Recurso)
                (or
                    (not (enMochila ?pre))
                    (>=
                        (posicion ?pre)
                        (posicion ?r)
                    )
                )
            )
        )
        :effect 
        (and
            (not (enMochila ?r))
            (enSuelo ?r)
            (decrease (actualPeso) (peso ?r))
        )
    )
    (:action salir
        :parameters()
        :precondition
        (and
                (haySalida)
        )
        :effect 
        (and 
            (not (haySalida))
        )
    )
)