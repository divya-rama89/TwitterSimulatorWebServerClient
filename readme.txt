TwitterSimulator
----------------
Team members: Mugdha (UFID:54219168), Palak Shah (UFID:55510961), Divya Ramachandran (UFID:46761308)

A program written as part of Distributed Operating Systems course to design and simulate the Twitter system as a web service. Used Spray IO and message passing in JSON.

Arguments:
----------
on server:
Usage : Server <Total Number of Users> <Total Number of Clients> <Server IP number> <Server port number> <run time for server in milliseconds>
eg: 
runMain Server 100000 2 192.168.1.5 8080 200

on client: 
Usage : Client <Number of Users> <Number of Client Machines> <ClientID> <Server IP> <Server Port> <run time of clientin milliseconds>>
on client 1:
runMain Client 100000 1 0 192.168.1.5 8080 180

Output :: Number of Tweets and Timeline requests per second are printed and the average value of tweets/sec and requests/second are calculated. Functionality: This code emulates Twitter. Clients from client machines act as logged in users. They send tweets that are updated on their own and their followers' timelines. When a client requests for its timeline (emulating the backend client requests), the prestored generated timeline maintained by the server for that client is returned.
Average Throughput will be a sum of tweets/sec and (requests/sec*average queue size) => average tweets/sec
Instructions {How to run}

Tested for the following versions: Scala : 2.11.2 Akka : 2.3.6 SBT : 0.13.6

Commands to Run:
----------------
From inside root folder 'TwitterSimulatorRestJson', copy the folder 'SprayClient' on client machine(s) and ' on the server machine. Go to project rootFolder by doing cd TwitterSimulator/ For example, on client => type cd TwitterSimulator/Client on server => type cd TwitterSimulator/Server

Do the following commands on client and server machines

Now from the initial outer folder (Server or Client) do:

sbt publishLocal

Run command: On server run this sbt "runMain Server <args>"

On client run this sbt "runMain Client <args>"


Note
----
Please refer to report document and excel sheet in the project root folder for our test results.
