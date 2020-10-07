package banking;

import java.sql.*;
import java.util.*;

public class Main {

    static Random random = new Random();
    static String url = "";
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        String dbName = "";
        for (int i = 0; i < args.length; i++) {
            if ("-fileName".equals(args[i])) {
                dbName = args[i + 1];
            }
        }
        url = "jdbc:sqlite:" + dbName;
        createDB();


        Map<String, String> takenCards = new HashMap<>();

        boolean exitMainMenu = false;

        while (!exitMainMenu) {
            printMainMenu();

            switch (Integer.parseInt(scanner.nextLine())) {
                case 1: //  Create account
                    String card = generateCardNumber();
                    String PIN = generatePIN();

                    while (takenCards.containsKey(card)) {
                        card = generateCardNumber();
                    }
                    takenCards.put(card, PIN);

                    storeCard(card, PIN);

                    System.out.println("Your card has been created");
                    System.out.println("Your card number:\n" + card);
                    System.out.println("Your card PIN:\n" + PIN + "\n");
                    break;

                case 2: // Log into account
                    boolean exitLogin = false;

                    System.out.println("Enter your card number:");
                    String inputCardNumber = scanner.nextLine();

                    System.out.println("Enter your PIN:");
                    String inputPIN = scanner.nextLine();
                    if (login(inputCardNumber, inputPIN)) {
                        while (!exitLogin) {
                            printAccountMenu();
                            String selection = scanner.nextLine();

                            switch ("".equals(selection) ? 99 : Integer.parseInt(selection)) {
                                case 1: // Balance
                                    System.out.println("Balance: " + getBalance(inputCardNumber));
                                    break;

                                case 2: // Add income
                                    System.out.println("Amount?");
                                    deposit(inputCardNumber, scanner.nextInt());
                                    break;

                                case 3: // Do transfer
                                    System.out.println("Destination card:");
                                    transfer(inputCardNumber, scanner.nextLine());
                                    break;

                                case 4: // Close account
                                    closeAccount(inputCardNumber);
                                    exitLogin = true;
                                    exitMainMenu = true;
                                    break;

                                case 5: // Log out
                                    exitLogin = true;
                                    break;

                                case 0: // Exit
                                    exitLogin = true;
                                    exitMainMenu = true;
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                    break;
                case 0: // Exit
                    exitMainMenu = true;
                    System.out.println("Bye!");
                    break;
            }
        }
        scanner.close();
    }

    private static void closeAccount(String cardNumber) {
        try (Connection conn = DriverManager.getConnection(url)) {
            try (Statement statement = conn.createStatement()) {
                // Origin account
                statement.executeUpdate("DELETE FROM card WHERE number = " + cardNumber);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private static void transfer(String origin, String destination) {
        int originBalance = 0;
        int destinationBalance = 0;

        // Test same destination/origin
        if (origin.equals(destination)) {
            System.out.println("You can't transfer money to the same account!");
            return;
        }

        // Test destination account
        if (!checkLuhn(destination)) {
            System.out.println("Probably you made mistake in the card number. Please try again!");
            return;
        }

        try (Connection conn = DriverManager.getConnection(url)) {
            try (Statement statement = conn.createStatement()) {
                // Origin account
                ResultSet rs = statement.executeQuery("SELECT balance FROM card WHERE number = " + origin);
                originBalance = rs.getInt(1);

                // Destination account
                rs = statement.executeQuery("SELECT balance FROM card WHERE number = " + destination);
                if (rs.next()) {
                    destinationBalance = rs.getInt(1);
                } else {
                    System.out.println("Such card does not exist.");
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        // All good
        System.out.println("How much do you want to trasfer?");
        int amount = scanner.nextInt();

        // Test balance and transfer
        if (originBalance > amount) {
            try (Connection conn = DriverManager.getConnection(url)) {
                try (Statement statement = conn.createStatement()) {
                    // Origin account
                    statement.executeUpdate("UPDATE FROM card SET balance = " + (originBalance - amount) + " WHERE number = " + origin);
                    statement.executeUpdate("UPDATE FROM card SET balance = " + (destinationBalance + amount) + " WHERE number = " + destination);
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        } else {
            System.out.println("Not enough money!");
            return;
        }
    }

    private static void deposit(String cardNumber, int amount) {
        try (Connection conn = DriverManager.getConnection(url)) {
            try (Statement statement = conn.createStatement()) {
                statement.executeUpdate("UPDATE card SET balance = (balance + " + amount + ") WHERE number = " + cardNumber);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private static void storeCard(String card, String pin) {
        try (Connection conn = DriverManager.getConnection(url)) {
            try (Statement statement = conn.createStatement()) {
                statement.executeUpdate("INSERT INTO card (number, pin) VALUES " +
                        "(" + card + "," + pin + ")");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private static void createDB() {
        try (Connection conn = DriverManager.getConnection(url)) {
            try (Statement statement = conn.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS card(" +
                        "id INTEGER PRIMARY KEY," +
                        "number TEXT NOT NULL," +
                        "pin TEXT NOT NULL," +
                        "balance INTEGER DEFAULT 0)");
                System.out.println("DB created!");
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static double getBalance(String cardNumber) {
        int balance = 0;
        try (Connection conn = DriverManager.getConnection(url)) {
            try (Statement statement = conn.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT balance FROM card WHERE number = " + cardNumber);
                balance = rs.getInt(1);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return balance;
    }

    static void printMainMenu() {
        System.out.print(
                "1. Create an account\n" +
                "2. Log into account\n" +
                "0. Exit\n"
        );
    }

    static void printAccountMenu() {
        System.out.print(
                "1. Balance\n" +
                "2. Add Income\n" +
                "3. Do transfer\n" +
                "4. Close account\n" +
                "5. Log out\n" +
                "0. Exit\n"
        );
    }

    static boolean login(String inputCardNumber, String inputPIN) {
        String error = "\nWrong card number or PIN!\n";
        String success = "\nYou have successfully logged in!\n";

        try (Connection conn = DriverManager.getConnection(url)){
            try (Statement statement = conn.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT id FROM card WHERE number = " + inputCardNumber + " AND pin = " + inputPIN);
                if (rs.next()) {
                    System.out.println(success);
                    return true;
                } else {
                    System.out.println(error);
                }
            }
        } catch (Exception e) {
            System.out.println(error);
        }
        return false;
    }

    static boolean checkLuhn(String cardNo) {
        int nDigits = cardNo.length();

        int nSum = 0;
        boolean isSecond = false;
        for (int i = nDigits - 1; i >= 0; i--)
        {

            int d = cardNo.charAt(i) - '0';

            if (isSecond == true)
                d = d * 2;

            nSum += d / 10;
            nSum += d % 10;

            isSecond = !isSecond;
        }
        return (nSum % 10 == 0);
    }

    static String generatePIN() {
        final int PINLength = 4;
        return generateRandomDigits(PINLength);
    }

    static String generateCAN() {
        final int CANLength = 9;
        return generateRandomDigits(CANLength);
    }

    static int generateChecksum(String creditCardNumber) {
        int[] digits = Arrays.stream(creditCardNumber.split("")).mapToInt(Integer::parseInt).toArray();
        int sum = 0;
        int checksum;

        for (int i = 0; i < digits.length; i++) {
            // Multiply odd digits by 2
            digits[i] = i % 2 == 0 ? digits[i] * 2 : digits[i];

            // Subtract 9 to numbers over 9
            digits[i] = digits[i] > 9 ? digits[i] - 9 : digits[i];

            // Sum all processed digits
            sum += digits[i];
        }

        checksum = 10 - (sum % 10);

        return checksum == 10 ? 0 : checksum;
    }

    static String generateRandomDigits(int length) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            stringBuilder.append(random.nextInt(10));
        }

        return stringBuilder.toString();
    }

    /**
     *  A credit card number should be generated following the pattern: IIN + CAN + checksum
     * */
    static String generateCardNumber() {
        final String IIN = "400000"; // Issuer identifier number (6-digit number)
        final String CAN = generateCAN(); // Customer account number (9-digit number)
        final int checksum = generateChecksum(IIN + CAN); // A single number calculated somehow

        return IIN + CAN + checksum;
    }

}