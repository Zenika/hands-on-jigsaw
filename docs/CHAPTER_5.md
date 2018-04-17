# Chapter 5 - Nothing must hold you back, remove all unecessary things!

Vous devez vous libérer de ce qui vous ralenti dans votre quête. Et réduire aux maximum vos dépendances.

## Une version sans vertx

Votre co-équipier vous a préparé une version allégée sans vertx.
Pour cela rendez-vous sur la branche GIT `j9-http-server`.

## Une version sans tout le JDK

Utilisez jlink pour rendre votre application encore plus légère et qui démarre à la vitesse de l'éclair. (cf. [TIPS](./TIPS.md) - Génération de modules "linké" et JRE optimisé)

> Vous vous sentez encore capable d'avancer? Changez votre JDK pour passer à Java 10!

> Vous pensez avoir encore des réserves et vous voulez aller jusqu'au bout ? Tentez de faire une image docker basée sur une debian-slim (pas une alpine) qui ne contiendra que votre application assemblée avec jlink grace au build multi-stage de docker!
