package server_client;

import java.io.*;
import java.net.*;

public class LMS_client {

    public static void main(String[] args) throws IOException {

        // Create a BufferedReader to read input from the user
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        // Create a socket connection to the server
        Socket clientSocket = new Socket("127.0.0.1", 1111);

        // Create output stream to send data to the server
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

        // Create BufferedReader to read data from the server
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        // Perform welcome and authentication process
        WelcomeAndAuth(inFromUser, outToServer, inFromServer);

        // Close the socket connection
        clientSocket.close();
    }

    // Method to handle welcome message and authentication process
    public static void WelcomeAndAuth(BufferedReader inFromUser, DataOutputStream outToServer, BufferedReader inFromServer) throws IOException {
        System.out.println("Welcome To Library Management System (LMS)!");
        System.out.println("Please provide your username and password to proceed:");
        System.out.print("Username: ");
        String username = inFromUser.readLine();
        System.out.print("Password: ");
        String password = inFromUser.readLine();

        // Send username and password to the server for authentication
        outToServer.writeBytes(username + '\n');
        outToServer.writeBytes(password + '\n');

        // Receive authentication response from the server
        String auth = inFromServer.readLine();
        String[] Authinformation = auth.split(" ");

        // Handle different authentication scenarios
        if (Authinformation[0].equalsIgnoreCase("authenticated")) {
            if (Authinformation[1].equalsIgnoreCase("librarian")) {
                // If user is a librarian, display librarian menu
                System.out.println("Welcome " + username + "! Here is the menu available for you as a librarian:");
                libarians(inFromUser, outToServer, inFromServer);
            } else {
                // If user is a member, display member menu
                System.out.println("Welcome " + username + " !");
                member(inFromUser, outToServer, inFromServer);
            }
        } else if (Authinformation[0].equalsIgnoreCase("unauthenticated")) {
            // If authentication fails, display error message
            System.out.println("Wrong credentials!");
        } else {
            // If there's an internal server error, display error message
            System.out.println("Sorry, there's an internal server error! Please try again later.");
        }
    }

    // Method to handle member menu
    public static void member(BufferedReader inFromUser, DataOutputStream outToServer, BufferedReader inFromServer) throws IOException {

        // Display notifications received from the server
        String notification = "";
        do {
            System.out.println(notification); // Print the response received from the server
            notification = inFromServer.readLine();

        } while (!notification.equalsIgnoreCase("Done"));

        // Display reminders received from the server
        String reminder = "";
        do {
            System.out.println(reminder); // Print the response received from the server
            reminder = inFromServer.readLine();

        } while (!reminder.equalsIgnoreCase("Done Borrow"));

        // Display menu options for the member
        System.out.println("Here are the available menu for you: ");
        while (true) {

            System.out.println("--------------------------------------------\n"
                    + "Choose from the menu:\n1. Browse Available Books\n2. Borrow Book by Book Number\n3. Return Book by Book Number\n4.Exit.."
                    + "\n--------------------------------------------");

            // Read user's choice from input
            String choice = inFromUser.readLine();

            // Send the choice to the server
            outToServer.writeBytes(choice + '\n');

            // Handle different menu choices
            switch (choice) {
                case "1":
                    // Browse available books
                    String response1;
                    while (true) {
                        response1 = inFromServer.readLine();
                        if (response1.equalsIgnoreCase("Done Menu")) {
                            break; // Exit the loop when "Done" is received
                        }
                        System.out.println(response1); // Print the response received from the server
                    }
                    break;
                case "2":
                    // Borrow a book
                    System.out.println("Which Book Number would YOU like to borrow? ");
                    String response2 = "";
                    while (true) {
                        response2 = inFromServer.readLine();
                        if (response2.equalsIgnoreCase("Done Menu")) {
                            break; // Exit the loop when "Done" is received
                        }
                        System.out.println(response2); // Print the response received from the server
                    }

                    String BookNo = inFromUser.readLine();//borrow by book number

                    outToServer.writeBytes(BookNo + '\n');
                    String Message = inFromServer.readLine();
                    if (Message.equals("Not Available")) {
                        System.out.println("The book is not available now, would you like to be notified when a copy of this book is available?(y/n)");
                        String notified = inFromUser.readLine();
                        outToServer.writeBytes(notified + '\n');
                        System.out.println("OK..\n");

                    } else {
                        System.out.println(Message);

                    }

                    break;
                case "3":
                    // Return a book
                    System.out.println("Please Enter The Book Number You Would Like to Return ");

                    String BookNo1 = inFromUser.readLine();//borrow by book number
                    outToServer.writeBytes(BookNo1 + '\n');
                    String Message1 = inFromServer.readLine();

                    System.out.println(Message1);
                    break;

                case "4":
                    // Exit
                    System.out.println("GoodBye!");
                    System.exit(0);
                default:
                    // Invalid choice
                    System.out.println("Invalid choice. Please try again.\n");
                    break;
            }
        }
    }

    // Method to handle librarian menu
    public static void libarians(BufferedReader inFromUser, DataOutputStream outToServer, BufferedReader inFromServer) throws IOException {
        while (true) {
            System.out.println("--------------------------------------------\n"
                    + "Choose from the menu:\n1. Add a new book\n2. Update book information\n3. Delete a book\n4.Exit.."
                    + "\n--------------------------------------------");

            // Read librarian's choice from input
            String choiceStr = inFromUser.readLine();
            outToServer.writeBytes(choiceStr + "\n");

            // Handle different librarian menu choices
            switch (choiceStr) {
                case "1": {
                    // Add a new book
                    System.out.println("Please enter the book ID: ");
                    String bookID = inFromUser.readLine();
                    outToServer.writeBytes(bookID + '\n');
                    String response = inFromServer.readLine();
                    if (response.equalsIgnoreCase("Book Not Found")) {
                        // If book not found, prompt for book details and send to server
                        System.out.println("Please enter the book title: ");
                        String bookTitle = inFromUser.readLine();
                        System.out.println("Please enter the number of copies of the book: ");
                        String bookCopNum = inFromUser.readLine();
                        outToServer.writeBytes(bookID + "\t" + bookTitle + "\t" + bookCopNum + "\n"); // Send the info 
                        response = inFromServer.readLine();
                        System.out.println(response); // Print the response received from the server
                    } else {
                        System.out.println(response);
                    }

                    break;
                }
                case "2": {
                    // Update book information
                    System.out.println("Please enter the book number that you want to update its information: ");
                    String bookNo = inFromUser.readLine();
                    outToServer.writeBytes(bookNo + '\n');
                    String response = inFromServer.readLine();
                    if (response.equalsIgnoreCase("Book Found")) {
                        // If book found, prompt for update type and new data
                        System.out.println("Do you want to update the title of the book or the copies number? (t/c) ");
                        String ans = inFromUser.readLine();
                        String dataToUpdate;
                        if ("t".equalsIgnoreCase(ans)) {
                            System.out.println("Please enter the new book title: ");
                        } else {
                            System.out.println("Please enter the number of copies of the book to update: ");
                        }
                        dataToUpdate = inFromUser.readLine();
                        outToServer.writeBytes(dataToUpdate + "\n");
                        response = inFromServer.readLine();
                        System.out.println(response);
                    } else {
                        System.out.println(response);
                    }
                    break;
                }

                case "3": {
                    // Delete a book
                    System.out.println("Please enter the book number of the book that you want to delete: ");
                    String bookNo = inFromUser.readLine();
                    outToServer.writeBytes(bookNo + "\n"); // Send the choice 
                    String response = inFromServer.readLine();
                    System.out.println(response); // Print the response received from the server
                    break;
                }
                case "4": {
                    // Exit
                    System.out.println("See you later.. ");
                    System.exit(0);
                }
                default:
                    // Invalid choice
                    System.out.println("Invalid choice. Please try again.\n");
                    break;
            }
        }
    }
}
