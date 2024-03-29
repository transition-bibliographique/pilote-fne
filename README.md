# pilote-fne

Programme de chargement de données d'autorités vers un Wikibase.

Ce programme (Spring Batch) utilise des instructions SQL directes pour charger des données.

Les données présentes sous forme de fichiers XML, dans le répertoire "/dump" (voir application.properties), sont transformées en "entity" Wikibase (utilisation de la libraire WDTK-datamodel).  
Des exemples de répertoires dump sont présents dans resources/dump et resources/dump-bnf

WDTK-datamodel : https://github.com/Wikidata/Wikidata-Toolkit

L'insertion directe en SQL est très largement inspirée de ces 2 dépôts :  
https://github.com/jze/wikibase-insert  
et  
https://github.com/UB-Mannheim/RaiseWikibase/blob/main/RaiseWikibase/dbconnection.py

Ce programme peut être démarré avec comme paramètre :   
-s (chargement par insertion directe en SQL)  
-f (création des propriétés et type d'entités, en utilisant l'API Wikibase)  

L'application est automatiquement packagée en image Docker (voir Github action : build-test-pub-pilote-fne-todockerhub.yml).  
L'image est disponible ici : https://hub.docker.com/repository/docker/transitionbibliographique/pilote-fne/general

Pour utiliser cette application, il faut avoir un Wikibase vide et modifier l'url de l'API en conséquence (voir application.properties)

Exemple d'installation Wikibase : https://github.com/transition-bibliographique/pilote-fne-wb-docker

Ce programme est utilisé par la BnF et l'Abes, dans le cadre d'un projet "Pilote-FNE", voici un exemple d'installation contenant en plus les outils Grafana et Prometheus :
https://github.com/transition-bibliographique/pilote-fne-docker
