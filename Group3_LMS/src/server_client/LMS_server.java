package server_client;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class LMS_server {

    //Create global read/Write buffers
    public static File bookDB = new File("Books_DB.txt");
    public static BufferedReader readBook;
    public static BufferedWriter writeToBookDB;

    public static void main(String[] args) throws IOException {
        //statemnet indecating the starting of the server
        System.out.println("Server Start..");

        //create the tcp socket
        ServerSocket TCPsocket = new ServerSocket(1111);

        //create the thread pool that's used to generate fixed Number of threads 
        ExecutorService threadPool = Executors.newFixedThreadPool(10); // Use a thread pool with a fixed number of threads

        while (true) {
            //accept The TCP connection when it's established by the client
            Socket connectionSocket = TCPsocket.accept();

            // Create a new thread for each client connection
            threadPool.execute(new ClientHandler(connectionSocket));
        }
    }

    //create nested class to handle each connection and stream of each user as objects
    private static class ClientHandler implements Runnable {

        //create socket object
        private Socket connectionSocket;

        //constructer for the class
        public ClientHandler(Socket connectionSocket) {
            this.connectionSocket = connectionSocket;
        }

        @Override
        public void run() {
            try (
                    //create xtream for input/output to/from the server
                    BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                    DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream())) {
//Starting Authentecation ----------------------------------------------------------------------------------------------------
                String clientUsername = inFromClient.readLine();
                String clientPassword = inFromClient.readLine();
                String auth = authOperation(clientUsername, clientPassword);

                outToClient.writeBytes(auth + '\n');

//Classifiying members and librarinas ----------------------------------------------------------------------------------------
                if (auth.equalsIgnoreCase("authenticated librarian")) {
                    // Handle librarian operations here
                    String menuChoice;
                    do {
                        // Receive the next menu choice from the client
                        menuChoice = inFromClient.readLine();
                        switch (menuChoice) {
                            case "1":
                                //"add book" opretion
                                String bookInfo;
                                String bookID = inFromClient.readLine();
                                String Bname = getName(bookID);

                                if (Bname.equalsIgnoreCase("Book Not Found")) {
                                    outToClient.writeBytes("Book Not Found\n");
                                    bookInfo = inFromClient.readLine();
                                    addBooks(bookInfo, outToClient);

                                } else {//when librarian trys to add a book that is already exist in the Book_DB, disply this msg
                                    outToClient.writeBytes("The book number is already exist in the database >_<\n");
                                }
                                break;

                            case "2":
                                //"update book" opreation
                                String bookNo = inFromClient.readLine();
                                String name = getName(bookNo);
                                if (!name.equalsIgnoreCase("Book Not Found")) {
                                    outToClient.writeBytes("Book Found\n");
                                    bookInfo = inFromClient.readLine();
                                    if (updateBooks(bookNo, bookInfo, outToClient)) {
                                        outToClient.writeBytes("The book has been updated successfully..\n");
                                    }
                                } else {//when librarian trys to updte info of a book that is not in the Book_DB, disply this msg
                                    outToClient.writeBytes("The book number doesn't exist in the database >_<\n");
                                }
                                break;

                            case "3":
                                //last opreation for the librarian, delete an esistance book
                                bookInfo = inFromClient.readLine();
                                if (deleteBook(bookInfo, outToClient)) {
                                    outToClient.writeBytes("The book has been deleted successfully..\n");
                                } else {
                                    outToClient.writeBytes("The book number doesn't exist in the database><\n");
                                }
                                break;
                        }
                    } while (!menuChoice.equals("4"));

                } else if (auth.equalsIgnoreCase("authenticated member")) {
                    member_server(outToClient, inFromClient, clientUsername);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    connectionSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
//------------------------------------------------------------------------------------------------------------------------------------

    //first function used to try to match the user inputted data with the User_DB, otherwise it will refuse to connect with
    //the user because of the "Wrong Credentials"
    public static String authOperation(String clientUsername, String clientPassword) throws FileNotFoundException {
        File User_DB = new File("User_DB.txt");
        Scanner reader = new Scanner(User_DB);

        while (reader.hasNextLine()) {
            String line = reader.nextLine();
            String[] information = line.split(" ");
            if (information[0].equals(clientUsername) && information[1].equals(clientPassword)) {
                if (information[2].equalsIgnoreCase("yes")) {
                    return "authenticated librarian";
                } else {
                    return "authenticated member";
                }
            }
        }

        return "unauthenticated";
    }

//------------------------------------------------------------------------------------------------------------------------------------
    //here all member opretions is handled
    public static void member_server(DataOutputStream outToClient, BufferedReader inFromClient, String User) throws IOException {

        //check notifecations
        checkBookAva(outToClient, User);
        checkRemainingTime(outToClient, User);

        String menuChoice;

        do {
            //handle menu choice
            menuChoice = inFromClient.readLine(); // Receive the next menu choice from the client

            switch (menuChoice) {
                case "1":
                    browseBooks(outToClient);
                    break;
                case "2":
                    browseBooks(outToClient);
                    String BookNo = inFromClient.readLine();
                    borrowBooks(outToClient, BookNo, inFromClient, User);
                    break;
                case "3":
                    String BookNo1 = inFromClient.readLine();
                    returnBook(outToClient, BookNo1, User);
                    break;

            }

        } while (!menuChoice.equalsIgnoreCase("4"));
    }

    //list all avalible books by reading all Book_DB
    public static void browseBooks(DataOutputStream outToClient) throws FileNotFoundException, IOException {
        readBook = new BufferedReader(new FileReader(bookDB));

        outToClient.writeBytes("Book number\tBook name\t\t  Available copies\n");
        String line;

        while ((line = readBook.readLine()) != null) {
            String[] books = line.split("\t");
            if (!books[2].equals("0")) {
                String formattedLine = String.format("%-15s%-30s%-16s%n", books[0], books[1], books[2]);
                outToClient.writeBytes(formattedLine);
            } else if (books[2].equals("0")) {
                String formattedLine = String.format("%-15s%-30s%-16s%n", books[0], books[1], "Not Available");
                outToClient.writeBytes(formattedLine);
            }
        }
        readBook.close();
        outToClient.writeBytes("Done Menu\n"); // Send "Done" to terminate the loop
    }

    //modify the book info by allowing users to borrow a book, copies of that book will be decreased by 1 whenever successfull borrow
    //opreation is done
    public static void borrowBooks(DataOutputStream outToClient, String BookNo, BufferedReader inFromClient, String User) throws FileNotFoundException, IOException {
        String copies = GetCopies(BookNo);
        if (!copies.equalsIgnoreCase("Book Not Found")) {
            if (!copies.equals("0")) {
                int copiesValue = Integer.parseInt(copies);
                copiesValue--;
                copies = Integer.toString(copiesValue);
                readBook.close();
                updateBooks(BookNo, copies, outToClient);
                outToClient.writeBytes("Book Borrowed Successfully! You have 24 Hours To return The Book. \n");
                logBorrow(User, BookNo);

            } else {
                outToClient.writeBytes("Not Available\n");
                String notified = inFromClient.readLine();
                if (notified.equalsIgnoreCase("y")) {
                    avaLog(User, BookNo);
                }

            }
        } else {
            readBook.close();
            outToClient.writeBytes("Book Not Found \n");
        }
    }

    //method used to log ech borrow info in .txt file for notifcation and reminders
    public static void logBorrow(String user, String bookNo) throws IOException {
        String name = getName(bookNo);
        if (!name.equalsIgnoreCase("Book Not Found")) {
            String logEntry = user + "," + bookNo + "," + getCurrentDay() + "," + name + "\n";
            String fileName = "borrowLog.txt";

            if (!Files.exists(Paths.get(fileName))) {
                // Create the file if it doesn't exist
                Files.createFile(Paths.get(fileName));
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));
            writer.write(logEntry);
            writer.close();
        }
    }

    //create another log file to store the users who wnts to be notefied when some book is avalible 
    public static void avaLog(String user, String bookNo) throws IOException {

        String logEntry = user + "\t" + bookNo + "\t" + getName(bookNo) + "\n";
        String fileName = "AvaLog.txt";

        if (!Files.exists(Paths.get(fileName))) {
            // Create the file if it doesn't exist
            Files.createFile(Paths.get(fileName));
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));
        writer.write(logEntry);
        writer.close();
        //  }
    }

    //functions used in Date/Time
    //----------------------------------------------------------------------------
    private static String getCurrentMonth() {
        LocalDateTime now = LocalDateTime.now();
        Month month = now.getMonth();
        return month.toString();
    }

    //----------------------------------------------------------------------------
    private static String getCurrentDay() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd");
        return now.format(formatter);
    }
    //----------------------------------------------------------------------------

    private static String getTomorrow() {
        LocalDate currentDate = LocalDate.now();
        LocalDate tomorrow = currentDate.plusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd");
        return tomorrow.format(formatter);
    }
    //----------------------------------------------------------------------------

//method to check the remining time of borrowing a book
    public static void checkRemainingTime(DataOutputStream outToClient, String user) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("borrowLog.txt"));
        String line, reminder = "";
        // boolean hasBorrowed = false;
        while ((line = reader.readLine()) != null) {
            String[] borrowInfo = line.split(",");
            if (borrowInfo[0].equalsIgnoreCase(user)) {
                int remainingTime = Integer.parseInt(borrowInfo[2]) - Integer.parseInt(getCurrentDay());
                if (borrowInfo[2].equalsIgnoreCase(getCurrentDay())) {
                    reminder = "You Have 24hr to return the Book \"" + borrowInfo[3] + "\" Number: " + borrowInfo[1] + " by " + getTomorrow() + " Of " + getCurrentMonth();
                    outToClient.writeBytes(reminder + "\n");
                }
            }
        }
        outToClient.writeBytes("Done Borrow" + "\n");
        reader.close();
    }
//method used to check the veliblitiy of book borroy time

    public static void checkBookAva(DataOutputStream outToClient, String user) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("AvaLog.txt"));
        String line, notification = "";
        while ((line = reader.readLine()) != null) {
            String[] notificationInfo = line.split("\t");
            if (notificationInfo[0].equalsIgnoreCase(user)) {
                String Available = GetCopies(notificationInfo[1]);
                if (!Available.equalsIgnoreCase("0")) {
                    notification = "The Book \"" + notificationInfo[2] + "\" Number: " + notificationInfo[1] + " is now available!";
                    outToClient.writeBytes(notification + "\n");
                }
            }
        }
        outToClient.writeBytes("Done" + "\n");
        reader.close();
    }

//retrun a book by adding 1 to it's copies
    public static void returnBook(DataOutputStream outToClient, String BookNo, String User) throws FileNotFoundException, IOException {
        String copies = GetCopies(BookNo);
        boolean isBorrowed = getBorrowedBook(BookNo, User);
        if (isBorrowed && !copies.equalsIgnoreCase("book not found")) {
            int copiesValue = Integer.parseInt(copies);
            copiesValue++;
            copies = Integer.toString(copiesValue);
            updateBooks(BookNo, copies, outToClient);
            outToClient.writeBytes("Book Returned Successfully! Thank You <3 \n");

            deleteBorrowLog(User, BookNo); // Delete the borrow log entry

        } else {
            outToClient.writeBytes("You Haven't Borrowed this book \n");
        }
    }

    //if book reteurnd ofter borrowing then must delete the borrow log from the book file
    public static void deleteBorrowLog(String user, String bookNo) throws IOException {
        String fileName = "borrowLog.txt";
        File inputFile = new File(fileName);
        File tempFile = new File("temp_" + fileName);

        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

        String line;
        while ((line = reader.readLine()) != null) {
            String[] borrowInfo = line.split(",");
            if (!borrowInfo[0].equalsIgnoreCase(user) || !borrowInfo[1].equalsIgnoreCase(bookNo)) {
                writer.write(line + System.lineSeparator());
            }
        }

        writer.close();
        reader.close();

        // Rename the temporary file to the original file
        if (inputFile.delete()) {
            if (!tempFile.renameTo(inputFile)) {
                throw new IOException("Failed to rename temporary file to the original file");
            }
        } else {
            throw new IOException("Failed to delete the original borrow log file");
        }
    }

    //get Copies of sprecific number fo books
    public static String GetCopies(String BookNo) throws FileNotFoundException, IOException {
        readBook = new BufferedReader(new FileReader(bookDB));

        String line;
        while ((line = readBook.readLine()) != null) {
            String[] books = line.split("\t");
            if (books[0].equalsIgnoreCase(BookNo)) {
                readBook.close();
                return books[2];
            }
        }
        readBook.close();
        return "Book Not Found";
    }

    //return the book info by book Number
    public static boolean getBorrowedBook(String BookNo, String User) throws FileNotFoundException, IOException {
        File inputFile = new File("borrowLog.txt");

        BufferedReader reader = new BufferedReader(new FileReader(inputFile));

        String line;
        while ((line = reader.readLine()) != null) {
            String[] books = line.split(",");
            if (books[1].equalsIgnoreCase(BookNo) && books[0].equalsIgnoreCase(User)) {
                reader.close();
                return true;
            }
        }

        reader.close();
        return false;
    }

    //get name of sprecific number fo a book
    public static String getName(String BookNo) throws FileNotFoundException, IOException {
        readBook = new BufferedReader(new FileReader(bookDB));

        String line = readBook.readLine();
        while (line != null) {
            // line = reader.nextLine();
            String[] books = line.split("\t");
            if (books[0].equalsIgnoreCase(BookNo)) {
                readBook.close();
                return books[1];
            }
            line = readBook.readLine();
        }
        readBook.close();

        return "Book Not Found";
    }

    //add new books
    private static void addBooks(String bookInfo, DataOutputStream outToClient) throws IOException {
        writeToBookDB = new BufferedWriter(new FileWriter(bookDB, true));
        writeToBookDB.write(bookInfo + "\n");
        writeToBookDB.close();

        outToClient.writeBytes("The book has been added successfully..\n");
    }

//------------------------------------------------------------------------------------------------------------------------------------
    //here is librarian opreations:
    public static boolean updateBooks(String bookNo, String bookToUpdate, DataOutputStream outToClient) throws IOException {

        boolean isDigit = true;
        for (char c : bookToUpdate.toCharArray()) {
            if (!Character.isDigit(c)) {
                isDigit = false;
                break;
            }
        }
        String book;
        String[] bookInfo;
        boolean flag = false;

        readBook = new BufferedReader(new FileReader(bookDB));

        while ((book = readBook.readLine()) != null) {

            bookInfo = book.split("\t");

            if (bookInfo[0].equals(bookNo)) {
                // to prevent the duplication of the data.
                readBook.close();

                deleteBook(bookNo, outToClient);
                writeToBookDB = new BufferedWriter(new FileWriter(bookDB, true));

                if (isDigit) {// if it is a digit then it is a copies number.
                    writeToBookDB.write(bookNo + "\t" + bookInfo[1] + "\t" + bookToUpdate + "\n");
                } else {
                    writeToBookDB.write(bookNo + "\t" + bookToUpdate + "\t" + bookInfo[2] + "\n");
                }

                writeToBookDB.flush();
                flag = true;
                break;
            }
        }

        writeToBookDB.close();
        readBook.close();
        return flag;

    }

    //delete book everywheres
    private static boolean deleteBook(String bookNoToDelete, DataOutputStream outToClient) throws IOException {
        File inputFile = new File("Books_DB.txt");
        File tempFile = new File("temp.txt");

        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

        String book;
        String[] bookInfo;
        boolean flag = false;

        while ((book = reader.readLine()) != null) {
            bookInfo = book.split("\t");

            if (bookInfo[0].equals(bookNoToDelete)) {
                flag = true;
                continue;
            }
            writer.write(book);
            writer.newLine();
            writer.flush();
        }
        writer.close();
        reader.close();

        if (!inputFile.delete()) {
            System.out.println("Could not delete the original file.\n");
        }
        if (!tempFile.renameTo(inputFile)) {
            System.out.println("Could not rename the temporary file.\n");
        }

        return flag;
    }
}
