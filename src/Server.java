import java.io.*;
import java.net.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class Server { // Main server class that handles multiple client connections
    private static final int PORT = 5000;  // Port number of the server
    private static final int MAX_CLIENTS = 3; // max num of clients allowed

    private ServerSocket serverSocket; // Server socket that listens for incoming client connections



    private static final String FILE_REPO = "server_repository/";  // Directory path where files are stored

    private  Map<String, ClientInfo> clientCache;  // Map to store client information

    private Semaphore clientSemaphore; // Semaphore to control maximum number of concurrent clients

    private int clientCounter = 0; // Counter to generate unique client names (Client1, Client2, etc.)

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); // Formatter for displaying timestamps

    public Server() { // Constructor to initialize the server
        clientCache = new ConcurrentHashMap<>();  // Initialize  map for storing client info
        clientSemaphore = new Semaphore(MAX_CLIENTS);  // Initialize semaphore with maximum client limit

    }

    public void start() {

        try {  // first start the connection with the server sockets
            serverSocket = new ServerSocket(PORT); // the socket will be binded with port 5000, creates a server socket
            System.out.println("Server started on port " + PORT);
            System.out.println("Maximum clients allowed: " + MAX_CLIENTS);
            System.out.println("Waiting for client connections...\n");

            File repo = new File(FILE_REPO); //create new repo directory
            if (!repo.exists()) { // if directory doesn't exist then it will create the repo
                repo.mkdir();
                createFiles();
            }

            while (true) { //while loop for accepting new clients
                try {
                    Socket clientSocket = serverSocket.accept(); // waits for a client
                    if (clientSemaphore.tryAcquire()) { // check if there is a spot available using semaphore, which controls access to shared resources
                        String clientName = generateClientName(); //generates client name
                        System.out.println("New connection accepted. Assigned name: " + clientName);// print out accepted new client

                        ClientHandler handler = new ClientHandler(clientSocket, clientName); // create a thread handler for the client to manage the threads
                        new Thread(handler).start(); //runs the client handler on a separate thread so multiple clients can connect simultaneously
                    } else {
                        System.out.println("Server is full. Rejecting new connection.");
                        System.out.println("SERVER_FULL: Maximum clients reached. Please try again later.");
                        clientSocket.close(); //dont allow new clients to connect as it reached max capacity
                    }

                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }

            }

        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        }
    }

    private synchronized String generateClientName() { //generates client name with the format Client 01, client 02, etc.
        clientCounter++;
        return String.format("Client%02d", clientCounter);
    }

    private void createFiles() { //create new files in the directory
        try {
            File f1 =new File(FILE_REPO + "file1.txt");
            File f2 = new File(FILE_REPO + "file2.txt");
            File f3 = new File(FILE_REPO + "file3.txt");


            System.out.println("file1.txt created: " + f1.createNewFile());
            System.out.println("file2.txt created: " + f2.createNewFile());
            System.out.println("file3.txt created: " + f3.createNewFile());


            try (PrintWriter writer = new PrintWriter(f1)) {// Write some content to the files
                writer.println("This is file 1 and it has some data about the users name and birthdate");

            }

            try (PrintWriter writer = new PrintWriter(f2)) {
                writer.println("This is file 2, it contains source code and descriptions");

            }

            try (PrintWriter writer = new PrintWriter(f3)) {
                writer.println("This is file 3, it is a README File.");

            }
        } catch (IOException e) {
            System.err.println("Error creating sample files: " + e.getMessage());
        }

    }


    class ClientHandler implements Runnable { // Inner class that handles communication with a single client, implements Runnable for threading

        private Socket socket;
        private String clientName;
        private BufferedReader in;
        private PrintWriter out;
        private LocalDateTime connectedTime;


        public ClientHandler(Socket socket, String clientName) {// Constructor to initialize a new client handler
            this.socket = socket;   // Store the socket connection
            this.clientName = clientName;   // Store the client's assigned name
            this.connectedTime = LocalDateTime.now(); // Record the current time as the connection time
        }


        @Override
        public void run() { // Main method that runs when the thread starts
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));  // Create input reader from the socket's input stream
                out = new PrintWriter(socket.getOutputStream(), true); // Create output writer to the socket's output stream
                out.println("ASSIGNED_NAME:" + clientName); // Send the assigned name to the client

                ClientInfo clientInfo = new ClientInfo(clientName, connectedTime); // Create a new ClientInfo object to store client details
                clientCache.put(clientName, clientInfo);  // Add the client info to the cache

                System.out.println(clientName + " connected at " + formatter.format(connectedTime));  // record the connection to the server console
                displayClientCache();  // Display all clients info

                String message; // Variable to store incoming messages
                while ((message = in.readLine()) != null) {   // Loop continuously reading messages from the client until null is received
                    System.out.println("Received from " + clientName + ": " + message);

                    if (message.equalsIgnoreCase("exit")) {  // Check if the client wants to exit
                        out.println("ACK: Connection closing. Goodbye!");
                        break;
                    } else if (message.equalsIgnoreCase("list")) {  // Check if the client wants to list available files
                        showList();

                    } else if (message.startsWith("GET_FILE:")) {  // Check if the client is stream a file
                        String fileName = message.substring(9);  // Extract the filename from the message
                        FileRequest(fileName);// Handle the file request
                    } else {

                        out.println("ACK: " + message);  // For any other message, echo it back
                    }
                }

            } catch (IOException e) {
                System.err.println("Error handling client " + clientName + ": " + e.getMessage());
            } finally {
                disconnect(); // Disconnect and clean up resources
            }
        }

        private void disconnect() { // Method to handle client disconnection and cleanup
            try {
                LocalDateTime disconnectedTime = LocalDateTime.now();// Record the current time as the disconnection time


                ClientInfo info = clientCache.get(clientName);  // Retrieve the client's info from the cache
                if (info != null) { // Check if the info exists
                    info.setDisconnectedTime(disconnectedTime);  // Update the info with the disconnection time
                }

                System.out.println(clientName + " disconnected at " + formatter.format(disconnectedTime));
                displayClientCache(); // Display the updated client cache

                if (in != null) in.close(); // Close the input stream if it exists
                if (out != null) out.close();  // Close the output stream if it exists
                if (socket != null) socket.close();  // Close the socket connection if it exists


                clientSemaphore.release(); // Release a permit back to the semaphore which allows another client to connect

            } catch (IOException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }


        }

        private void displayClientCache() { // Method to display all clients in the cache
            System.out.println("\n=== Client Cache ===");
            for (ClientInfo info : clientCache.values()) {
                System.out.println(info);
            }
            System.out.println("====================\n");

        }

        private void FileRequest(String fileName) { // Method to handle file  requests from clients
            File file = new File(FILE_REPO + fileName);

            if (!file.exists() || !file.isFile()) { // Check if the file doesn't exist
                out.println("FILE_ERROR:File not found");
                return;
            }

            try {
                BufferedReader fileReader = new BufferedReader(new FileReader(file)); // Create a BufferedReader to read the file
                String line; //variable to store each line
                while ((line = fileReader.readLine()) != null) {  // Read each line until end of file
                    out.println(line); // Print the current line to output
                }
                fileReader.close(); // Close the file reader
            } catch (IOException e) { // Catch any IO exceptions
                out.println("FILE_ERROR:Error reading file");
            }
        }

        private void showList() {
            File repo = new File(FILE_REPO); // Create a File object

            File[] files = repo.listFiles();// get all files in the array

            if (files != null && files.length > 0) { // Check if the directory exists and contains at least one item
                StringBuilder fileList = new StringBuilder("FILE_LIST:"); // Initialize a StringBuilder with the name "FILE_LIST:"
                for (File file : files) { // go through each file/directory in the array
                    if (file.isFile()) { // Check if the current item is a file
                        fileList.append(file.getName()).append(","); //add file to the list
                    }
                }
                out.println(fileList); //Print the complete file list to output
            } else {
                out.println("FILE_LIST:No files available");
            }
        }
    }

    class ClientInfo { //class to handle client info
        private String name;  // Stores the client's assigned name
        private LocalDateTime connectedTime; // Time when client connected
        private LocalDateTime disconnectedTime;  // Time when client disconnected (null if still connected)


        public ClientInfo(String name, LocalDateTime connectedTime) {
            this.name = name; // Save the client's name
            this.connectedTime = connectedTime;  // Save the time they connected
        }

        public void setDisconnectedTime(LocalDateTime disconnectedTime) {

            this.disconnectedTime = disconnectedTime;  // Record the time the client disconnected
        }

        public String toString() {
            if (disconnectedTime != null) { // There is a disconnect, report disconnected status
                return "Client: " + name +
                        " | Connected: " + formatter.format(connectedTime) +
                        " | Connection Status: " + "Disconnected at: " + formatter.format(disconnectedTime) ;
            } else {
                return "Client: " + name +
                        " | Connected: " + formatter.format(connectedTime) +
                        " | Connection Status: Still connected";
            }

        }
    }

        public static void main(String[] args) { // main method to run the server
            Server server = new Server();
            server.start();
        }
    }








        
   
    



