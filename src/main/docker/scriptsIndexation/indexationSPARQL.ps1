

$containerWikibase = "wikibase-docker_wikibase_1"
$containerWDQS = "wikibase-docker_wdqs_1"

Write-Output "Indexation WDQS / SPARQ"

$current = [System.DateTime]::Now

docker exec $containerWikibase bash -c 'php extensions/Wikibase/repo/maintenance/dumpRdf.php --server http://${WIKIBASE_HOST} | gzip > MyWiki.ttl.gz'
docker cp ${containerWikibase}:/var/www/html/MyWiki.ttl.gz .
docker cp MyWiki.ttl.gz ${containerWDQS}:/wdqs
docker exec $containerWDQS bash -c 'curl http://${WDQS_HOST}:${WDQS_PORT}/bigdata/namespace/wdq/sparql --data-urlencode """update=LOAD <file:///wdqs/MyWiki.ttl.gz>;"""'

$end= [System.DateTime]::Now

$diff= $end.Subtract($current)

Write-Output "Le temps d'execution est : $diff"