(define (domain BoulderDash)
    (:requirements :strips :typing :adl :negative-preconditions :equality)
    (:types
        Gema Salida - Objeto
    )
    (:predicates
        (existe ?g - Objeto)
        (obtenida ?g - Gema) 
    )
    (:action cogerGema
        :parameters (?g - Gema ?s - Salida)
        :precondition
            ( and
                (existe ?g)
                (existe ?s)
                (not (obtenida ?g))
            )
        :effect
            (and
                (obtenida ?g)
            )
    )
    (:action salir
        :parameters(?s - Salida
        )
        :precondition
        (and
                (existe ?s)
        )
        :effect 
            (and 
                (not (existe ?s))
            )
    )
)
