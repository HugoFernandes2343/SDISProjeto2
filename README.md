SDIS PROJ_2

Correr todos os comandos na src

(cd src)

Criar pasta "build" se esta não existir

### Terminal 1

javac -d build *.java

java -classpath build Peer 1 8001 8001

###  Terminal 2

java -classpath build Peer 2 8002 8001

###  Terminal 3

java -classpath build Peer 3 8003 8001

###  Terminal 4

java -classpath build TestApp 1 BACKUP 5rpc.pdf 1

java -classpath build TestApp 1 RESTORE 5rpc.pdf 

java -classpath build TestApp 2 RECLAIM 0

java -classpath build TestApp 1 DELETE 5rpc.pdf
