# TCP Client–Server Chat Application

## Overview
This project implements a simple client–server communication application using **TCP sockets**.  
The system consists of one server and multiple clients that can connect concurrently and exchange messages through a command-line interface (CLI).

The goal of this project is to demonstrate an understanding of basic **networking concepts**, including socket programming, client–server architecture, and connection management.

---

## Features
- TCP-based communication using sockets
- One server serving multiple clients
- Clients are automatically assigned unique names (Client01, Client02, etc.)
- Clients send messages to the server via CLI
- Server echoes received messages back with an `"ACK"` appended
- Clients can terminate the connection by sending the `"exit"` command
- Server maintains an in-memory cache of connected clients with connection timestamps
- Configurable maximum number of concurrent clients

---

## Technologies Used
- Programming Language: **Java** *(or Python — update if needed)*
- Communication Protocol: **TCP**
- Interface: **Command Line Interface (CLI)**

---

## How to Run

### Server
1. Compile the server source code.
2. Run the server program.
3. The server will start listening for incoming client connections.

### Client
1. Compile the client source code.
2. Run the client program.
3. The client will automatically be assigned a name upon connection.
4. Enter messages via the command line to send them to the server.
5. Type `exit` to close the connection.

---

## Assumptions and Design Choices
- The maximum number of clients is fixed and configured on the server.
- All client information is stored in memory only (no file storage).
- Communication is text-based.
- The server handles each client connection independently.

---

## Notes
This project was completed as an individual assignment for **CPSC 441**.  
All code was written independently, and no generative AI tools were used to generate source code.

---

## Possible Improvements
- Add a graphical user interface (GUI)
- Improve error handling and robustness
- Support file transfer between server and clients
- Add authentication for clients
- Log client activity to files instead of memory
