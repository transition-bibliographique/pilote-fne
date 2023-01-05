

$containerWikibase = "wikibase-docker_wikibase_1"

Write-Output "Indexation Elastic Search"

$current = [System.DateTime]::Now

docker exec $containerWikibase bash -c 'php extensions/CirrusSearch/maintenance/ForceSearchIndex.php --skipLinks –indexOnSkip'
docker exec $containerWikibase bash -c 'php extensions/CirrusSearch/maintenance/ForceSearchIndex.php –skipParse'
docker exec $containerWikibase bash -c 'php maintenance/runJobs.php'

$end= [System.DateTime]::Now

$diff= $end.Subtract($current)

Write-Output "Le temps d'execution est : $diff"