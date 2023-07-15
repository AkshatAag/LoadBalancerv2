# Load Balancer for Media Servers

## Overview
This project is a load balancer that acts as an intermediary between incoming calls and media servers. It is built using Java, Spring Boot and MongoDB technologies. The load balancer, referred to as "LB," responds to API calls from the control layer and returns the appropriate media server number to which the new call should be redirected. It stores the current state of the media servers in a cloud database and modifies it on the basis of new calls from control layer, or events generated from the media layer.

## Database Connection
Before running the program, follow these steps:
1. Clone the project repository.
2. Set the appropriate MongoDB database connection string in the `application.properties` file.
3. Set the value of `spring.main.allow-bean-definition-overriding` to `true` in the `application.properties` file.

## Multiple Instances
The load balancer has two instances running at ports 8080 and 8081, respectively. Additionally, there is another Spring Boot application running at port 8082. This application serves as a scheduled bookkeeping function that ensures the database always represents the true state of the media servers.

## Prerequisites
Make sure you have the following dependencies installed:
- Java Development Kit (JDK)
- Postman (Any other service to make API calls)
  
## Installation
1. Clone the project repository.
2. Set the appropriate database connection string in the `application.properties` file.
3. Set `spring.main.allow-bean-definition-overriding=true` in the `application.properties` file.
4. Build and run the project using your preferred Java development environment or the command line.

## API Reference
<ul>
<li><b>Getting media server number</b></li>
<br>To retrieve the media server number to which a new call should be redirected, send a POST request to the following API endpoint:

```http
  POST /controller/control_layer/{alg}
```
<br>
The load balancing algorithm is determined by the `alg` parameter in the API call.<br> 

For `alg = 1`, LB uses the least connections algorithm.<br>
For `alg = 2`, LB uses the round-robin algorithm.

<br>
The body of the POST request should be:

```
{
  "legId": "value1",
  "conversationId": "value2",
}
```
<br>
- value1 and value2 are Strings.

---
  
<li><b>Processing event from media layer</b></li>
<br>
To process event from media server and update the state of in the database, send a POST request to the following API endpoint:
<br>

```http
  POST /controller/new_event
```

<br>
The body of the POST request should be:
```
{
  "Event-Name": "value1",
  "Core-UUID": "value2",
}
```
<br>
-value1 can be an event's name such as "CHANNEL_MUTE","CHANNEL_HANGUP"
-value2 is the legId of the call.

---

`<b>Initialization</b></li>
<br>
To initialize 3 new media servers and clear all the existing databases, send a GET request to the following API endpoint:
<br>

```http
  POST /controller/init
```
<br>
-The new media servers will have the default attributes.

---

<li><b>Adding a new media layer</b></li>
<br>
To add a new media server to the list of servers, send a POST request to the following API endpoint:
<br>

```http
  POST /controller/add_new_layer
```
<br>

The body of the POST request should be:
```
{
  "layerNumber": "value1",
}
```
<br>
-value1 is the IP Address of the new media server.

---

<li><b>Changing the load status of a server</b></li>
<br>
The health status of the media server is defined by an attribute "status" of the media server. There are 4 colors which depict the health status green->yellow->orange->red. To change the health status of the media server, send a GET request to the following API endpoint:
<br>

```http
  POST /controller/change_status/{layerNumber}/{color}
```
<br>
-There is no body of this POST request
-Replace `{layerNumber}` with the IP address of the media server who's health status you want to change.
-Replace `{color}` with the health status based on the current congestion on the server.

---

<li><b>Changing the faulty status of a server</b></li>
<br>
It might happen that the servers have too much load and they start faulting. It is also possible that the server is under maintenance and we do not want new calls to be redirected to that server. To change the faulty status of the media server, send a GET request to the following API endpoint:
<br>

```http
  POST /controller/set_faulty_status/{layerNumber}/{faulty}
```
<br>
-There is no body of this POST request
-Replace `{layerNumber}` with the IP address of the media server who's faulty status you want to change.
-Replace `{faulty}` with the faulty status. It can be true or false.

---
</ul>


## Contact
For any inquiries or questions, please contact the project maintainer at agrawalakshat049@gmail.com
