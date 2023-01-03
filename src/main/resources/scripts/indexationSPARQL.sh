#!/bin/bash

containerWikibase="wikibase-docker_wikibase_1"
containerWDQS = "wikibase-docker_wdqs_1"

echo "Indexation WDQS / SPARQ"

current=$(date +"%s")

sudo docker exec $containerWikibase bash -c 'php extensions/Wikibase/repo/maintenance/dumpRdf.php --server http://${WIKIBASE_HOST} | gzip > MyWiki.ttl.gz'
sudo docker cp ${containerWikibase}:/var/www/html/MyWiki.ttl.gz .
sudo docker cp MyWiki.ttl.gz ${containerWDQS}:/wdqs
sudo docker exec $containerWDQS bash -c 'curl http://${WDQS_HOST}:${WDQS_PORT}/bigdata/namespace/wdq/sparql --data-urlencode """update=LOAD <file:///wdqs/MyWiki.ttl.gz>;"""'

end=$(date +"%s")

diff=$((end-current))

echo "Le temps d'execution est : $diff"