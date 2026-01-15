import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_HOST = "localhost"; // Hostname of the server to connect to
    private static final int SERVER_PORT = 5000;  // Port the server is listening on

    private Socket socket;    // Socket representing the connection to the server
    private BufferedReader in;   // For reading text messages from the server
    private PrintWriter out;   // For sending messages to the server
    private String clientName;   // Name assigned by the server
    private Scanner scanner;    // Reads user input from console

    public Client() {
        scanner = new Scanner(System.in);
    }  // Create scanner for input

    public void start() {
        try {

            System.out.println("Connecting to server at " + SERVER_HOST + ":" + SERVER_PORT);
            socket = new Socket(SERVER_HOST, SERVER_PORT);  // Create connection socket

            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Setup reader
            out = new PrintWriter(socket.getOutputStream(), true); // // Setup writer with auto flush


            Thread listenerThread = new Thread(new ServerListener()); // Create listener thread to listen to new messages
            listenerThread.start();


            Thread.sleep(100);    // Wait briefly for server


            System.out.println("\n=== Chat Client ===");
            System.out.println("Commands:");
            System.out.println("  - Type any message to send to server");
            System.out.println("  - Type 'list' to see available files");
            System.out.println("  - Type 'get file name' to download a file");
            System.out.println("  - Type 'exit' to disconnect");
            System.out.println("==================\n");

            String message; // Variable to store user input
            while (true) { // loop to continuously read user input
                System.out.print(clientName + "-->");
                message = scanner.nextLine(); // Read user input

                if (message.trim().isEmpty()) {  // Check if message is empty
                    continue;
                }


                if (message.toLowerCase().startsWith("get ")) { // Check if user wants to stream a file
                    String fileName = message.substring(4).trim(); // get the file name
                    out.println("GET_FILE:" + fileName);  // Send file request to server
                } else {
                    out.println(message);
                }

                if (message.equalsIgnoreCase("exit")) {  // Check if user wants to exit
                    break;
                }
            }


            listenerThread.join(2000); // Wait up to 2 seconds for listener thread to finish

        } catch (IOException e) { // catch errors
            System.err.println("Connection error: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Thread interrupted: " + e.getMessage());
        } finally {
            cleanup(); // Close all resources
        }
    }

    class ServerListener implements Runnable {  // Inner class to handle incoming server messages
        private boolean receiving = true; // Flag to track if still receiving messages


        @Override
        public void run() { // Main method that runs when thread starts
            try {
                String response; // Variable to store each line received from server
                while ((response = in.readLine()) != null) { // Loop continuously reading server responses

                    if (response.startsWith("ASSIGNED_NAME:")) { // Check if server is assigning a name
                        clientName = response.substring(14);  // get name after "ASSIGNED_NAME:" prefix( which is 14)
                        System.out.println("Connected! Assigned name: " + clientName); // Display assigned name

                    } else if (response.equals("SERVER_FULL: Maximum clients reached. Please try again later.")) {
                        System.out.println("\n" + response);
                        receiving = false; // Stop receiving flag, so it knows when to stop accepting clients
                        break;

                    } else if (response.startsWith("FILE_LIST:")) { // Check if server is sending file list
                        handleFileList(response); // Process file list

                    } else if (response.startsWith("FILE_ERROR:")) {
                        System.out.println("Error: " + response.substring(11));

                    } else if (response.startsWith("ACK:")) { // Check if server is acknowledging a message
                        System.out.println("Server: " + response);
                        if (response.contains("Goodbye")) { // Check if server is saying goodbye
                            receiving = false;
                            break;
                        }
                    } else { // For any other message
                        // Treat everything else as regular messages or file content
                        System.out.println("Server: " + response);
                    }
                }
            } catch (IOException e) {
                if (receiving) {
                    System.err.println("Error receiving from server: " + e.getMessage());
                }
            }
        }

        private void handleFileList(String response) {  // Method to display file list from server
            String fileList = response.substring(10);
            if (fileList.equals("No files available")) { // Check if there are no files
                System.out.println("\nNo files available on server.");
            } else {
                System.out.println("\nAvailable files:");
                String[] files = fileList.split(","); // Split comma-separated file names
                for (int i = 0; i < files.length; i++) {
                    System.out.println(files[i]); // display file list
                }
            }
        }
    }
    private void cleanup () {
        try {
            if (scanner != null) scanner.close(); //close scanner
            if (in != null) in.close();// close input reader
            if (out != null) out.close(); // close output writer
            if (socket != null) socket.close(); // close socket
            System.out.println("Disconnected from server.");
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    public static void main (String[]args){ //main method for running client
        Client client = new Client();
        client.start();
    }
}




