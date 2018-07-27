FROM hypriot/rpi-java

ADD libjffi-1.2.so .
RUN sudo cp libjffi-1.2.so /usr/lib
 
ADD fognode-2.0.1-SNAPSHOT.jar .
ADD main.properties .
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "fognode-2.0.1-SNAPSHOT.jar"]
