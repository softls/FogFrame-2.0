FROM hypriot/rpi-java

ADD libjffi-1.2.so .
RUN sudo cp libjffi-1.2.so /usr/lib 
ADD fogcell-2.0.1-SNAPSHOT.jar .
ADD main.properties .

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "fogcell-2.0.1-SNAPSHOT.jar"]
