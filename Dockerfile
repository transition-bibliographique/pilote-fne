###
# Image pour la compilation du batch pilote-fne

# Test en local :
# docker build -f Dockerfile -t pilotefne .
FROM maven:3.8.1-openjdk-17-slim as build-image
WORKDIR /build/
# Installation et configuration de la locale FR
RUN apt update && DEBIAN_FRONTEND=noninteractive apt -y install locales
RUN sed -i '/fr_FR.UTF-8/s/^# //g' /etc/locale.gen && \
    locale-gen
ENV LANG fr_FR.UTF-8
ENV LANGUAGE fr_FR:fr
ENV LC_ALL fr_FR.UTF-8
# On lance la compilation
# si on a un .m2 local on peut décommenter la ligne suivante pour
# éviter à maven de retélécharger toutes les dépendances
# COPY ./.m2/    /root/.m2/

COPY ./ /build/
RUN mvn --batch-mode \
        -Dmaven.test.skip=false \
        -Duser.timezone=Europe/Paris \
        -Duser.language=fr \
        -f /build/pom.xml \
        package

###
# Image pour le batch pilote-fne
FROM openjdk:17 as pilote-fne
COPY --from=build-image /build/target/*.jar /app/pilote-fne.jar
#ENTRYPOINT exec java $JAVA_OPTS -jar /app/pilote-fne.jar -s
CMD ["java", "-Xmx2G", "-jar", "/app/pilote-fne.jar", "-s"]
