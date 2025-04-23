package com.example.demo6;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.stage.FileChooser;
import javafx.scene.chart.*; 
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*; // SQL imports
import java.time.LocalDate;
import java.time.temporal.ChronoUnit; // Import ChronoUnit
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {
    private Connection connection;
    private String currentUserRole; // Variable to store the role of the logged-in user
    private String css; // Variable to hold the CSS stylesheet path

    // Main method to launch the application
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        css = getClass().getResource("/palesa.css").toExternalForm(); // Load the CSS stylesheet
        connectToDatabase();
        showLoginForm(primaryStage); // Show the login form
    }

    // Method to connect to the database
    private void connectToDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/vehicle_rental", "root", "123456");
            System.out.println("Connected to DB.");
        } catch (Exception e) {
            showAlert("Database Error", e.getMessage());
        }
    }

    private void showLoginForm(Stage primaryStage) {
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Label roleLabel = new Label("Select Role:");
        ToggleGroup roleGroup = new ToggleGroup();
        RadioButton adminBtn = new RadioButton("Admin");
        RadioButton employeeBtn = new RadioButton("Employee");
        adminBtn.setToggleGroup(roleGroup);
        employeeBtn.setToggleGroup(roleGroup);

        Button loginBtn = new Button("Login");
        Button registerBtn = new Button("Register");

        loginBtn.setOnAction(e -> {
            if (roleGroup.getSelectedToggle() == null) {
                showAlert("Error", "Please select a role.");
                return;
            }
            currentUserRole = ((RadioButton) roleGroup.getSelectedToggle()).getText();
            authenticateUser(usernameField.getText(), passwordField.getText(), currentUserRole, primaryStage);
        });

        registerBtn.setOnAction(e -> showRegistrationForm());

        VBox loginBox = new VBox(10, roleLabel, adminBtn, employeeBtn, usernameField, passwordField, loginBtn, registerBtn);
        loginBox.setStyle("-fx-padding: 20; -fx-alignment: center;");
        loginBox.getStylesheets().add(css); // Add the CSS to the scene

        Scene scene = new Scene(loginBox, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Vehicle Rental - Login");
        primaryStage.show();
    }

    private void authenticateUser(String username, String password, String role, Stage stage) {
        String query = "SELECT * FROM users WHERE username=? AND password=? AND role=?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role.toLowerCase()); // Assuming role in DB is in lowercase
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                if (role.equalsIgnoreCase("Admin")) {
                    showAdminDashboard(stage);
                } else {
                    showEmployeeDashboard(stage);
                }
            } else {
                showAlert("Login Failed", "Invalid credentials.");
            }
        } catch (SQLException e) {
            showAlert("Login Error", e.getMessage());
        }
    }

    private void showRegistrationForm() {
        Stage stage = new Stage();
        TextField username = new TextField();
        PasswordField password = new PasswordField();
        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("Admin", "Employee");

        Button register = new Button("Register");
        Button goBackButton = new Button("Go Back"); // Add Go Back button

        goBackButton.setOnAction(e -> stage.close()); // Set Go Back button action

        register.setOnAction(e -> {
            if (username.getText().isEmpty() || password.getText().isEmpty() || roleBox.getValue() == null) {
                showAlert("Error", "Please complete all fields.");
                return;
            }
            String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, username.getText());
                stmt.setString(2, password.getText());
                stmt.setString(3, roleBox.getValue().toLowerCase());
                stmt.executeUpdate();
                showAlert("Success", "User registered.");
                stage.close();
            } catch (SQLException ex) {
                showAlert("Error", ex.getMessage());
            }
        });

        VBox layout = new VBox(10, username, password, roleBox, register, goBackButton);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");
        layout.getStylesheets().add(css); // Add the CSS to the scene

        Scene scene = new Scene(layout, 300, 200);
        stage.setScene(scene);
        stage.setTitle("Register User");
        stage.show();
    }

    private void showAdminDashboard(Stage stage) {
        BorderPane adminLayout = new BorderPane();
        HBox topBar = new HBox(10);
        Label welcomeLabel = new Label("Welcome Admin!");
        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> showLoginForm(stage));

        topBar.getChildren().addAll(welcomeLabel, logoutBtn);
        adminLayout.setTop(topBar);
        adminLayout.setPadding(new Insets(15));

        VBox adminButtons = new VBox(10);
        Button manageVehiclesBtn = new Button("Manage Vehicles");
        manageVehiclesBtn.setOnAction(e -> showManageVehiclesForm());
        Button manageCustomersBtn = new Button("Manage Customers");
        manageCustomersBtn.setOnAction(e -> showManageCustomersForm());
        Button generateReportsBtn = new Button("Generate Reports");
        generateReportsBtn.setOnAction(e -> showGenerateReports(stage));
        Button viewBookingsBtn = new Button("View Bookings");
        viewBookingsBtn.setOnAction(e -> showViewBookingsForm(stage));
        Button exportReportsBtn = new Button("Export Reports");
        exportReportsBtn.setOnAction(e -> showExportReportsOptions());

        adminButtons.getChildren().addAll(manageVehiclesBtn, manageCustomersBtn, generateReportsBtn, viewBookingsBtn, exportReportsBtn);
        adminLayout.setCenter(adminButtons);

        Scene scene = new Scene(adminLayout, 600, 400);
        adminLayout.getStylesheets().add(css); // Add the CSS to the scene
        stage.setScene(scene);
        stage.setTitle("Admin Dashboard");
        stage.show();
    }

    private void showExportReportsOptions() {
        Stage exportStage = new Stage();
        Button exportCustomersBtn = new Button("Export Customers to CSV");
        exportCustomersBtn.setOnAction(e -> exportCustomersToCSV());

        Button exportVehiclesBtn = new Button("Export Vehicles to CSV");
        exportVehiclesBtn.setOnAction(e -> exportVehiclesToCSV());

        Button exportBookingsBtn = new Button("Export Bookings to CSV");
        exportBookingsBtn.setOnAction(e -> exportBookingsToCSV());

        Button exportToPdf = new Button("Export Reports to PDF (Not implemented)");
        exportToPdf.setOnAction(e -> showAlert("PDF Export", "PDF export functionality is not implemented yet."));

        Button goBackButton = new Button("Go Back");
        goBackButton.setOnAction(e -> exportStage.close()); // Go Back action

        VBox exportLayout = new VBox(10, exportCustomersBtn, exportVehiclesBtn, exportBookingsBtn, exportToPdf, goBackButton);
        exportLayout.setPadding(new Insets(20));
        exportLayout.getStylesheets().add(css); // Add CSS

        Scene scene = new Scene(exportLayout, 300, 200);
        exportStage.setScene(scene);
        exportStage.setTitle("Export Reports");
        exportStage.show();
    }

    private void exportCustomersToCSV() {
        String query = "SELECT * FROM customers";
        FileChooser fileChooser = new FileChooser(); // Create a FileChooser
        fileChooser.setTitle("Save Customers Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("customers_report.csv"); // Default file name
        File file = fileChooser.showSaveDialog(null);

        if (file != null) { // Check if a file was selected
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                 Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                writer.write("Customer ID,Customer Name,Contact,License Number\n");
                while (rs.next()) {
                    writer.write(rs.getInt("customer_id") + "," +
                            rs.getString("customer_name") + "," +
                            rs.getString("contact") + "," +
                            rs.getString("license_number") + "\n");
                }
                showAlert("Export Successful", "Customers report exported as " + file.getName());
            } catch (Exception e) {
                showAlert("Export Error", e.getMessage());
            }
        }
    }

    private void exportVehiclesToCSV() {
        String query = "SELECT * FROM vehicles";
        FileChooser fileChooser = new FileChooser(); // Create a FileChooser
        fileChooser.setTitle("Save Vehicles Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("vehicles_report.csv"); // Default file name
        File file = fileChooser.showSaveDialog(null);

        if (file != null) { // Check if a file was selected
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                 Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                writer.write("Vehicle ID,Model,Daily Rate,Availability\n");
                while (rs.next()) {
                    writer.write(rs.getInt("vehicle_id") + "," +
                            rs.getString("model") + "," +
                            rs.getDouble("daily_rate") + "," +
                            rs.getString("availability") + "\n");
                }
                showAlert("Export Successful", "Vehicles report exported as " + file.getName());
            } catch (Exception e) {
                showAlert("Export Error", e.getMessage());
            }
        }
    }

    private void exportBookingsToCSV() {
        String query = "SELECT r.rental_id, c.customer_name, v.model, r.start_date, r.end_date, r.total_amount FROM rentals r " +
                "JOIN customers c ON r.customer_id = c.customer_id JOIN vehicles v ON r.vehicle_id = v.vehicle_id";
        FileChooser fileChooser = new FileChooser(); // Create a FileChooser
        fileChooser.setTitle("Save Bookings Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("bookings_report.csv"); // Default file name
        File file = fileChooser.showSaveDialog(null);

        if (file != null) { // Check if a file was selected
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                 Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                writer.write("Rental ID,Customer Name,Vehicle Model,Start Date,End Date,Total Amount\n");
                while (rs.next()) {
                    writer.write(rs.getInt("rental_id") + "," +
                            rs.getString("customer_name") + "," +
                            rs.getString("model") + "," +
                            rs.getDate("start_date") + "," +
                            rs.getDate("end_date") + "," +
                            rs.getDouble("total_amount") + "\n");
                }
                showAlert("Export Successful", "Bookings report exported as " + file.getName());
            } catch (Exception e) {
                showAlert("Export Error", e.getMessage());
            }
        }
    }

    private void showViewBookingsForm(Stage stage) {
        Stage viewBookingsStage = new Stage();
        VBox viewBookingsForm = new VBox(10);
        ListView<Bookings> bookingsListView = new ListView<>();
        bookingsListView.getItems().addAll(getAllBookings());

        Label selectedBookingLabel = new Label("Selected Booking Details:");
        TextField bookingIdField = new TextField();
        bookingIdField.setPromptText("Booking ID (Read Only)");
        bookingIdField.setEditable(false);
        TextField customerNameField = new TextField();
        customerNameField.setPromptText("Customer Name:");
        TextField vehicleModelField = new TextField();
        vehicleModelField.setPromptText("Vehicle Model:");
        TextField startDateField = new TextField();
        startDateField.setPromptText("Start Date:");
        TextField endDateField = new TextField();
        endDateField.setPromptText("End Date:");
        TextField totalAmountField = new TextField();
        totalAmountField.setPromptText("Total Amount:");

        bookingsListView.setOnMouseClicked(event -> {
            Bookings selectedBooking = bookingsListView.getSelectionModel().getSelectedItem();
            if (selectedBooking != null) {
                bookingIdField.setText(String.valueOf(selectedBooking.getBookingId()));
                customerNameField.setText(selectedBooking.getCustomerName());
                vehicleModelField.setText(selectedBooking.getVehicleModel());
                startDateField.setText(selectedBooking.getStartDate().toString());
                endDateField.setText(selectedBooking.getEndDate().toString());
                totalAmountField.setText("M" + selectedBooking.getTotalAmount()); // Update currency
            }
        });

        Button goBackButton = new Button("Go Back");
        goBackButton.setOnAction(e -> viewBookingsStage.close());

        viewBookingsForm.getChildren().addAll(
                selectedBookingLabel, bookingsListView,
                bookingIdField, customerNameField, vehicleModelField,
                startDateField, endDateField, totalAmountField, goBackButton
        );
        viewBookingsForm.getStylesheets().add(css); // Add CSS

        Scene scene = new Scene(viewBookingsForm, 400, 500);
        viewBookingsStage.setScene(scene);
        viewBookingsStage.setTitle("View Bookings");
        viewBookingsStage.show();
    }

    private List<Bookings> getAllBookings() {
        List<Bookings> bookings = new ArrayList<>();
        String query = "SELECT r.rental_id, c.customer_name, v.model, r.start_date, r.end_date, r.total_amount FROM rentals r " +
                "JOIN customers c ON r.customer_id = c.customer_id JOIN vehicles v ON r.vehicle_id = v.vehicle_id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                bookings.add(new Bookings(
                        rs.getInt("rental_id"),
                        rs.getString("customer_name"),
                        rs.getString("model"),
                        rs.getDate("start_date"),
                        rs.getDate("end_date"),
                        rs.getDouble("total_amount")
                ));
            }
        } catch (SQLException e) {
            showAlert("Error fetching bookings", e.getMessage());
        }
        return bookings;
    }

    private void showManageCustomersForm() {
        Stage stage = new Stage();
        VBox customerForm = new VBox(10);
        ListView<Customer> customersListView = new ListView<>();
        customersListView.getItems().addAll(getAllCustomers());

        Label selectedCustomerLabel = new Label("Selected Customer Details:");
        TextField customerIdField = new TextField();
        customerIdField.setPromptText("Customer ID (Read Only)");
        customerIdField.setEditable(false);
        TextField nameField = new TextField();
        nameField.setPromptText("Customer Name:");
        TextField contactField = new TextField();
        contactField.setPromptText("Contact Information:");
        TextField licenseField = new TextField();
        licenseField.setPromptText("Driving License:");

        // Search Field
        TextField searchCustomerIdField = new TextField();
        searchCustomerIdField.setPromptText("Enter Customer ID to search");
        Button searchCustomerButton = new Button("Search");
        searchCustomerButton.setOnAction(e -> {
            int searchId = Integer.parseInt(searchCustomerIdField.getText());
            Customer foundCustomer = getCustomerById(searchId);
            if (foundCustomer != null) {
                customerIdField.setText(String.valueOf(foundCustomer.getCustomerId()));
                nameField.setText(foundCustomer.getName());
                contactField.setText(foundCustomer.getContact());
                licenseField.setText(foundCustomer.getLicense());
                displayCustomerBookings(foundCustomer.getCustomerId()); // Display bookings for the selected customer
            } else {
                showAlert("Search Result", "Customer not found.");
            }
        });

        // New ListView to show customer bookings
        ListView<Bookings> bookingsListView = new ListView<>();

        // Buttons
        Button addButton = new Button("Add Customer");
        Button updateButton = new Button("Update Customer");
        Button deleteButton = new Button("Delete Customer");
        Button goBackButton = new Button("Go Back");

        // Button Action Handlers
        addButton.setOnAction(e -> {
            String name = nameField.getText();
            String contact = contactField.getText();
            String license = licenseField.getText();
            addCustomer(name, contact, license);
            customersListView.getItems().add(new Customer(name, contact, license)); // Add to ListView
        });

        updateButton.setOnAction(e -> {
            if (!isAdmin()) {
                showAlert("Error", "Only Admins can update customers.");
                return;
            }
            int customerId = Integer.parseInt(customerIdField.getText());
            String name = nameField.getText();
            String contact = contactField.getText();
            String license = licenseField.getText();
            updateCustomer(customerId, name, contact, license);
            customersListView.refresh();
        });

        deleteButton.setOnAction(e -> {
            if (!isAdmin()) {
                showAlert("Error", "Only Admins can delete customers.");
                return;
            }
            int customerId = Integer.parseInt(customerIdField.getText());
            deleteCustomer(customerId);
            customersListView.getItems().remove(customersListView.getSelectionModel().getSelectedItem()); // Remove from ListView
            bookingsListView.getItems().clear(); // Clear bookings when customer is deleted
        });

        customersListView.setOnMouseClicked(event -> {
            Customer selectedCustomer = customersListView.getSelectionModel().getSelectedItem();
            if (selectedCustomer != null) {
                customerIdField.setText(String.valueOf(selectedCustomer.getCustomerId()));
                nameField.setText(selectedCustomer.getName());
                contactField.setText(selectedCustomer.getContact());
                licenseField.setText(selectedCustomer.getLicense());
                displayCustomerBookings(selectedCustomer.getCustomerId(), bookingsListView); // Added display method
            }
        });

        goBackButton.setOnAction(e -> stage.close());

        customerForm.getChildren().addAll(
                selectedCustomerLabel, searchCustomerIdField, searchCustomerButton,
                customersListView, customerIdField, nameField, contactField, licenseField,
                addButton, updateButton, deleteButton, goBackButton,
                new Label("Customer Bookings:"), bookingsListView // Add bookingsListView 
        );

        customerForm.getStylesheets().add(css); // Add CSS
        Scene scene = new Scene(customerForm, 400, 600);
        stage.setScene(scene);
        stage.setTitle("Manage Customers");
        stage.show();
    }

    private void displayCustomerBookings(int customerId) {
    }

    private void displayCustomerBookings(int customerId, ListView<Bookings> bookingsListView) {
        List<Bookings> customerBookings = getBookingsByCustomerId(customerId);
        bookingsListView.getItems().clear(); // Clear existing items

        if (customerBookings.isEmpty()) {
            showAlert("No Bookings", "This customer has no bookings.");
        } else {
            bookingsListView.getItems().addAll(customerBookings); // Add new bookings
        }
    }

    // Method to fetch bookings based on a customer ID
    private List<Bookings> getBookingsByCustomerId(int customerId) {
        List<Bookings> bookings = new ArrayList<>();
        String query = "SELECT r.rental_id, v.model, r.start_date, r.end_date, r.total_amount FROM rentals r " +
                "JOIN vehicles v ON r.vehicle_id = v.vehicle_id WHERE r.customer_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, customerId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                bookings.add(new Bookings(
                        rs.getInt("rental_id"),
                        "Customer Name Placeholder", 
                        rs.getString("model"),
                        rs.getDate("start_date"),
                        rs.getDate("end_date"),
                        rs.getDouble("total_amount")
                ));
            }
        } catch (SQLException e) {
            showAlert("Error fetching bookings", e.getMessage());
        }
        return bookings;
    }

    private Customer getCustomerById(int id) {
        String query = "SELECT * FROM customers WHERE customer_id=?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Customer(
                        rs.getInt("customer_id"),
                        rs.getString("customer_name"),
                        rs.getString("contact"),
                        rs.getString("license_number")
                );
            }
        } catch (SQLException e) {
            showAlert("Error fetching customer", e.getMessage());
        }
        return null;
    }

    private boolean isAdmin() {
        return currentUserRole != null && currentUserRole.equalsIgnoreCase("Admin");
    }

    private void showManageVehiclesForm() {
        Stage stage = new Stage();
        VBox vehicleForm = new VBox(10);
        ListView<Vehicle> vehiclesListView = new ListView<>();
        vehiclesListView.getItems().addAll(getAllVehicles());

        Label selectedVehicleLabel = new Label("Selected Vehicle Details:");
        TextField vehicleIdField = new TextField();
        vehicleIdField.setPromptText("Vehicle ID (Read Only)");
        vehicleIdField.setEditable(false);
        TextField modelField = new TextField();
        modelField.setPromptText("Model:");
        TextField dailyRateField = new TextField();
        dailyRateField.setPromptText("Daily Rate:");
        ComboBox<String> availabilityCombo = new ComboBox<>();
        availabilityCombo.getItems().addAll("Available", "Not Available");

        TextField searchVehicleIdField = new TextField();
        searchVehicleIdField.setPromptText("Enter Vehicle ID to search");
        Button searchVehicleButton = new Button("Search");
        searchVehicleButton.setOnAction(e -> {
            int searchId = Integer.parseInt(searchVehicleIdField.getText());
            Vehicle foundVehicle = getVehicleById(searchId);
            if (foundVehicle != null) {
                vehicleIdField.setText(String.valueOf(foundVehicle.getVehicleId()));
                modelField.setText(foundVehicle.getModel());
                dailyRateField.setText(String.valueOf(foundVehicle.getDailyRate()));
                availabilityCombo.setValue(foundVehicle.getAvailability());
            } else {
                showAlert("Search Result", "Vehicle not found.");
            }
        });

        Button addButton = new Button("Add Vehicle");
        Button updateButton = new Button("Update Vehicle");
        Button deleteButton = new Button("Delete Vehicle");
        Button goBackButton = new Button("Go Back");

        addButton.setOnAction(e -> {
            String model = modelField.getText();
            double dailyRate = Double.parseDouble(dailyRateField.getText());
            String availability = availabilityCombo.getValue();
            addVehicle(model, dailyRate, availability);
            vehiclesListView.getItems().add(new Vehicle(-1, model, dailyRate, availability)); 
        });

        updateButton.setOnAction(e -> {
            int vehicleId = Integer.parseInt(vehicleIdField.getText());
            String model = modelField.getText();
            double dailyRate = Double.parseDouble(dailyRateField.getText());
            String availability = availabilityCombo.getValue();
            updateVehicle(vehicleId, model, dailyRate, availability);
            vehiclesListView.refresh();
        });

        deleteButton.setOnAction(e -> {
            int vehicleId = Integer.parseInt(vehicleIdField.getText());
            deleteVehicle(vehicleId);
            vehiclesListView.getItems().remove(vehiclesListView.getSelectionModel().getSelectedItem()); 
        });

        vehiclesListView.setOnMouseClicked(event -> {
            Vehicle selectedVehicle = vehiclesListView.getSelectionModel().getSelectedItem();
            if (selectedVehicle != null) {
                vehicleIdField.setText(String.valueOf(selectedVehicle.getVehicleId()));
                modelField.setText(selectedVehicle.getModel());
                dailyRateField.setText(String.valueOf(selectedVehicle.getDailyRate()));
                availabilityCombo.setValue(selectedVehicle.getAvailability());
            }
        });

        goBackButton.setOnAction(e -> stage.close());

        vehicleForm.getChildren().addAll(
                selectedVehicleLabel, searchVehicleIdField, searchVehicleButton,
                vehiclesListView, vehicleIdField, modelField, dailyRateField, availabilityCombo,
                addButton, updateButton, deleteButton, goBackButton
        );

        vehicleForm.getStylesheets().add(css); // Add CSS
        Scene scene = new Scene(vehicleForm, 400, 600);
        stage.setScene(scene);
        stage.setTitle("Manage Vehicles");
        stage.show();
    }

    private Vehicle getVehicleById(int id) {
        String query = "SELECT * FROM vehicles WHERE vehicle_id=?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Vehicle(
                        rs.getInt("vehicle_id"),
                        rs.getString("model"),
                        rs.getDouble("daily_rate"),
                        rs.getString("availability")
                );
            }
        } catch (SQLException e) {
            showAlert("Error fetching vehicle", e.getMessage());
        }
        return null;
    }

    private void showEmployeeDashboard(Stage stage) {
        BorderPane employeeLayout = new BorderPane();
        HBox topBar = new HBox(10);
        Label welcomeLabel = new Label("Welcome Employee!");
        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> showLoginForm(stage));

        topBar.getChildren().addAll(welcomeLabel, logoutBtn);
        employeeLayout.setTop(topBar);
        employeeLayout.setPadding(new Insets(15));

        VBox employeeButtons = new VBox(10);
        Button bookingBtn = new Button("Book a Vehicle");
        bookingBtn.setOnAction(e -> showBookingForm());
        Button returnVehicleBtn = new Button("Return Vehicle");
        returnVehicleBtn.setOnAction(e -> showReturnVehicleForm());

        Button goBackButton = new Button("Go Back"); // Add Go Back button
        goBackButton.setOnAction(e -> showLoginForm(stage)); // Go Back to Login

        employeeButtons.getChildren().addAll(bookingBtn, returnVehicleBtn, goBackButton);
        employeeLayout.setCenter(employeeButtons);

        Scene scene = new Scene(employeeLayout, 600, 400);
        employeeLayout.getStylesheets().add(css); // Add CSS
        stage.setScene(scene);
        stage.setTitle("Employee Dashboard");
        stage.show();
    }

    private void showReturnVehicleForm() {
        Stage returnVehicleStage = new Stage();
        VBox returnVehicleForm = new VBox(10);

        TextField bookingIdField = new TextField();
        bookingIdField.setPromptText("Enter Booking ID");

        Button returnBtn = new Button("Return Vehicle");
        Button goBackButton = new Button("Go Back");

        returnBtn.setOnAction(e -> {
            int bookingId;
            try {
                bookingId = Integer.parseInt(bookingIdField.getText());
            } catch (NumberFormatException ex) {
                showAlert("Error", "Please enter a valid Booking ID.");
                return;
            }

            // Fetch booking details
            String query = "SELECT r.start_date, r.end_date, v.daily_rate FROM rentals r " +
                    "JOIN vehicles v ON r.vehicle_id = v.vehicle_id " +
                    "WHERE r.rental_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, bookingId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    LocalDate startDate = rs.getDate("start_date").toLocalDate();
                    LocalDate endDate = rs.getDate("end_date").toLocalDate();
                    double dailyRate = rs.getDouble("daily_rate");

                    // Get today's date
                    LocalDate returnDate = LocalDate.now();

                    // Calculate total amount and late fees if applicable
                    long borrowedDays = ChronoUnit.DAYS.between(startDate, endDate) + 1; // Include end day
                    double totalAmount = dailyRate * borrowedDays;

                    if (returnDate.isAfter(endDate)) {
                        long lateDays = ChronoUnit.DAYS.between(endDate, returnDate);
                        double lateFee = lateDays * (dailyRate * 0.5); 
                        totalAmount += lateFee;

                        showAlert("Invoice", generateInvoice(bookingId, totalAmount, lateDays, true));
                    } else {
                        showAlert("Invoice", generateInvoice(bookingId, totalAmount, 0, false));
                    }

                    // Update vehicle availability in the database
                    String updateQuery = "UPDATE vehicles SET availability = 'Available' WHERE vehicle_id = " +
                            "(SELECT vehicle_id FROM rentals WHERE rental_id = ?)";
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                        updateStmt.setInt(1, bookingId);
                        updateStmt.executeUpdate();
                    }

                    // mark the rental as completed in the database
                    String completeRentalQuery = "UPDATE rentals SET status = 'completed' WHERE rental_id = ?";
                    try (PreparedStatement completeStmt = connection.prepareStatement(completeRentalQuery)) {
                        completeStmt.setInt(1, bookingId);
                        completeStmt.executeUpdate();
                    }

                } else {
                    showAlert("Error", "Booking ID not found.");
                }
            } catch (SQLException ex) {
                showAlert("Database Error", ex.getMessage());
            }
        });

        goBackButton.setOnAction(e -> returnVehicleStage.close());

        returnVehicleForm.getChildren().addAll(bookingIdField, returnBtn, goBackButton);
        returnVehicleForm.setPadding(new Insets(20));
        returnVehicleForm.getStylesheets().add(css); // Add CSS

        Scene scene = new Scene(returnVehicleForm, 300, 200);
        returnVehicleStage.setScene(scene);
        returnVehicleStage.setTitle("Return Vehicle");
        returnVehicleStage.show();
    }

    private String generateInvoice(int bookingId, double totalAmount, long lateDays, boolean isLate) {
        StringBuilder invoice = new StringBuilder();
        invoice.append("Invoice Details\n");
        invoice.append("Booking ID: ").append(bookingId).append("\n");
        if (isLate) {
            invoice.append("Late Days: ").append(lateDays).append("\n");
        }
        invoice.append("Total Amount: M").append(totalAmount).append("\n");
        invoice.append("Thank you for using our service!\n");
        return invoice.toString();
    }

    private void showBookingForm() {
        Stage stage = new Stage();
        VBox bookingForm = new VBox(10);
        TextField userIdField = new TextField(); // Employee inputs the customer ID
        userIdField.setPromptText("Customer ID");

        TextField nameField = new TextField();
        nameField.setPromptText("Customer Name");
        TextField contactField = new TextField();
        contactField.setPromptText("Contact Information");
        TextField licenseField = new TextField();
        licenseField.setPromptText("Driving License");

        ComboBox<Vehicle> vehicleBox = new ComboBox<>();
        vehicleBox.getItems().addAll(getAvailableVehicles());

        Label startDateLabel = new Label("Start Date:");
        DatePicker startDate = new DatePicker();
        Label endDateLabel = new Label("End Date:");
        DatePicker endDate = new DatePicker();
        Button bookButton = new Button("Confirm Booking");

        Button goBackButton = new Button("Go Back");
        goBackButton.setOnAction(e -> stage.close());

        bookButton.setOnAction(e -> {
            // Validate customer details
            String userIdText = userIdField.getText();
            String customerName = nameField.getText();
            String contact = contactField.getText();
            String license = licenseField.getText();

            if (userIdText.isEmpty() || !userIdText.matches("\\d+")) {
                showAlert("Error", "Please enter a valid Customer ID.");
                return;
            }

            int userId = Integer.parseInt(userIdText);
            Vehicle vehicle = vehicleBox.getValue();
            LocalDate start = startDate.getValue();
            LocalDate end = endDate.getValue();

            // Check if customer exists or create a new record
            boolean customerExists = false;
            String query = "SELECT * FROM customers WHERE customer_id=?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    customerExists = true; // Customer exists
                } else { // Create new customer
                    String insertQuery = "INSERT INTO customers (customer_id, customer_name, contact, license_number) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                        insertStmt.setInt(1, userId);
                        insertStmt.setString(2, customerName);
                        insertStmt.setString(3, contact);
                        insertStmt.setString(4, license);
                        insertStmt.executeUpdate();
                    }
                }
            } catch (SQLException ex) {
                showAlert("Error", ex.getMessage());
            }

            if (!customerExists) {
                showAlert("Info", "Customer created. Proceeding with the booking.");
            }

            if (start == null || end == null || vehicle == null) {
                showAlert("Error", "Please select a vehicle and valid dates.");
                return;
            }

            // Create booking
            int bookingId = createBooking(userId, vehicle.getVehicleId(), start, end);
            if (bookingId > 0) {
                showAlert("Success", "Booking created successfully! Your Booking ID is: " + bookingId);
                // Proceed to Payment
                showPaymentForm(bookingId, userId);
            } else {
                showAlert("Error", "Failed to create booking, please try again.");
            }
        });

        bookingForm.getChildren().addAll(userIdField, nameField, contactField, licenseField, vehicleBox, startDateLabel, startDate, endDateLabel, endDate, bookButton, goBackButton);
        bookingForm.getStylesheets().add(css); // Add CSS
        Scene scene = new Scene(bookingForm, 400, 400);
        stage.setScene(scene);
        stage.setTitle("New Booking");
        stage.show();
    }

    private List<Vehicle> getAvailableVehicles() {
        List<Vehicle> vehicles = new ArrayList<>();
        String query = "SELECT * FROM vehicles WHERE availability='Available'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                vehicles.add(new Vehicle(
                        rs.getInt("vehicle_id"),
                        rs.getString("model"),
                        rs.getDouble("daily_rate"),
                        rs.getString("availability")
                ));
            }
        } catch (SQLException e) {
            showAlert("Error fetching vehicles: ", e.getMessage());
        }
        return vehicles;
    }

    private int createBooking(int userId, int vehicleId, LocalDate start, LocalDate end) {
        String sql = "INSERT INTO rentals (customer_id, vehicle_id, start_date, end_date, status, total_amount) VALUES (?, ?, ?, ?, ?, ?)";
        int generatedId = -1; // Initialize as invalid
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, vehicleId);
            stmt.setDate(3, Date.valueOf(start));
            stmt.setDate(4, Date.valueOf(end));
            stmt.setString(5, "booked");
            stmt.setDouble(6, calculateTotalAmount(vehicleId, start, end));
            stmt.executeUpdate();

            // Retrieve the generated booking ID
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                generatedId = generatedKeys.getInt(1);
            }
        } catch (SQLException e) {
            showAlert("Error", e.getMessage());
        }
        return generatedId; // Return the generated booking ID
    }

    private double calculateTotalAmount(int vehicleId, LocalDate start, LocalDate end) {
        double totalAmount = 0;
        String query = "SELECT daily_rate FROM vehicles WHERE vehicle_id=?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, vehicleId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double dailyRate = rs.getDouble("daily_rate");
                long rentalDays = ChronoUnit.DAYS.between(start, end) + 1; // Including end day
                totalAmount = dailyRate * rentalDays;
            }
        } catch (SQLException e) {
            showAlert("Error", e.getMessage());
        }
        return totalAmount;
    }

    private void showPaymentForm(int bookingId, int userId) {
        Stage paymentStage = new Stage();
        VBox paymentForm = new VBox(10);
        Label bookingIdLabel = new Label("Booking ID: " + bookingId);
        Label userIdLabel = new Label("Customer ID: " + userId);
        double totalAmount = calculateTotalAmountForBooking(bookingId);
        Label totalAmountLabel = new Label("Total Amount: M" + totalAmount); // Use Maluti here

        // Payment method selection
        Label paymentMethodLabel = new Label("Select Payment Method:");
        ComboBox<String> paymentMethodCombo = new ComboBox<>();
        paymentMethodCombo.getItems().addAll("Cash", "Credit Card", "Mpesa", "Ecocash", "Online");

        // Services selection
        Label servicesLabel = new Label("Additional Services (if any):");
        CheckBox insuranceCheckBox = new CheckBox("Insurance (M30)");
        CheckBox gpsCheckBox = new CheckBox("GPS (M10)");

        Button confirmPaymentBtn = new Button("Confirm Payment");
        confirmPaymentBtn.setOnAction(e -> {
            double additionalFees = 0.0;
            if (insuranceCheckBox.isSelected()) {
                additionalFees += 30; // Add cost for insurance
            }
            if (gpsCheckBox.isSelected()) {
                additionalFees += 10; // Add cost for GPS
            }

            String paymentMethod = paymentMethodCombo.getValue();
            showAlert("Payment Successful", "Thanks for your payment through " + paymentMethod + ". Total Amount including additional fees: M" + (totalAmount + additionalFees));
            paymentStage.close();
        });

        Button goBackButton = new Button("Go Back");
        goBackButton.setOnAction(e -> paymentStage.close()); // Go Back action

        paymentForm.getChildren().addAll(bookingIdLabel, userIdLabel, totalAmountLabel,
                paymentMethodLabel, paymentMethodCombo, servicesLabel, insuranceCheckBox, gpsCheckBox,
                confirmPaymentBtn, goBackButton);
        paymentForm.setPadding(new Insets(20));
        paymentForm.getStylesheets().add(css); // Add CSS

        Scene scene = new Scene(paymentForm, 400, 300);
        paymentStage.setScene(scene);
        paymentStage.setTitle("Payment");
        paymentStage.show();
    }

    private double calculateTotalAmountForBooking(int bookingId) {
        // Logic to retrieve the total amount for the booking
        return 0; // Placeholder to be implemented
    }

    private void showManagePaymentsForm() {
        // Implementation for managing payments
    }

    private void showGenerateReports(Stage stage) {
        Stage reportStage = new Stage();
        TabPane reportTabPane = new TabPane();

        // Tab for Available Vehicles Report using Pie Chart
        Tab availableVehiclesTab = new Tab("Available Vehicles");
        VBox availableVehiclesLayout = new VBox();
        PieChart pieChart = createPieChart(getAvailableVehicles());
        availableVehiclesLayout.getChildren().add(pieChart);
        availableVehiclesTab.setContent(availableVehiclesLayout);
        reportTabPane.getTabs().add(availableVehiclesTab);

        // Tab for Customer Rental History
        Tab rentalHistoryTab = new Tab("Customer Rental History");
        VBox rentalHistoryLayout = new VBox();
        Button loadHistoryButton = new Button("Load All Customers Rental History");
        ListView<Bookings> rentalHistoryListView = new ListView<>();
        loadHistoryButton.setOnAction(e -> {
            rentalHistoryListView.getItems().clear();
            rentalHistoryListView.getItems().addAll(getAllCustomerRentalHistories());
        });

        rentalHistoryLayout.getChildren().addAll(loadHistoryButton, rentalHistoryListView);
        rentalHistoryTab.setContent(rentalHistoryLayout);
        reportTabPane.getTabs().add(rentalHistoryTab);

        // Tab for Revenue Report
        Tab revenueTab = new Tab("Revenue Report");
        VBox revenueLayout = new VBox();
        Button loadRevenueButton = new Button("Load Revenue by Vehicle");
        LineChart<String, Number> revenueChart = createRevenueChart();

        loadRevenueButton.setOnAction(e -> {
            loadRevenueData(revenueChart);
        });

        revenueLayout.getChildren().addAll(loadRevenueButton, revenueChart);
        revenueTab.setContent(revenueLayout);
        reportTabPane.getTabs().add(revenueTab);

        // Go Back button
        Button goBackButton = new Button("Go Back");
        goBackButton.setOnAction(e -> reportStage.close()); // Go back action

        reportTabPane.getTabs().add(new Tab("Go Back", goBackButton));

        // CSS applied to report tab components
        reportTabPane.getStylesheets().add(css); // Adding CSS to the TabPane
        revenueLayout.getStylesheets().add(css); // Adding CSS to the revenue layout
        rentalHistoryLayout.getStylesheets().add(css); // Adding CSS to the rental history layout
        availableVehiclesLayout.getStylesheets().add(css); // Adding CSS to the available vehicles layout


        Scene scene = new Scene(reportTabPane, 600, 400);
        reportStage.setScene(scene);
        reportStage.setTitle("Generate Reports");
        reportStage.show();
    }

    private PieChart createPieChart(List<Vehicle> vehicles) {
        ObservableList<PieChart.Data> dataItems = FXCollections.observableArrayList();
        for (Vehicle vehicle : vehicles) {
            dataItems.add(new PieChart.Data(vehicle.getModel(), 1));
        }
        PieChart pieChart = new PieChart(dataItems);
        pieChart.setTitle("Available Vehicles");
        return pieChart;
    }

    private List<Bookings> getAllCustomerRentalHistories() {
        List<Bookings> rentals = new ArrayList<>();
        String query = "SELECT r.rental_id, c.customer_name, v.model, r.start_date, r.end_date, r.total_amount FROM rentals r " +
                "JOIN customers c ON r.customer_id = c.customer_id JOIN vehicles v ON r.vehicle_id = v.vehicle_id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                rentals.add(new Bookings(
                        rs.getInt("rental_id"),
                        rs.getString("customer_name"),
                        rs.getString("model"),
                        rs.getDate("start_date"),
                        rs.getDate("end_date"),
                        rs.getDouble("total_amount")
                ));
            }
        } catch (SQLException e) {
            showAlert("Error fetching rental histories", e.getMessage());
        }
        return rentals;
    }

    private LineChart<String, Number> createRevenueChart() {
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Revenue (M)");

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Vehicle Model");

        LineChart<String, Number> revenueChart = new LineChart<>(xAxis, yAxis);
        revenueChart.setTitle("Revenue By Vehicle");
        return revenueChart;
    }

    private void loadRevenueData(LineChart<String, Number> revenueChart) {
        String query = "SELECT v.model, COALESCE(SUM(r.total_amount), 0) AS total_revenue "
                + "FROM vehicles v LEFT JOIN rentals r ON r.vehicle_id = v.vehicle_id "
                + "GROUP BY v.model";
        ObservableList<XYChart.Data<String, Number>> data = FXCollections.observableArrayList();

        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String model = rs.getString("model");
                double totalRevenue = rs.getDouble("total_revenue");
                data.add(new XYChart.Data<>(model, totalRevenue));
            }
        } catch (SQLException e) {
            showAlert("Error fetching revenue data", e.getMessage());
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>(data);
        revenueChart.getData().clear();
        revenueChart.getData().add(series);
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private List<Vehicle> getAllVehicles() {
        List<Vehicle> vehicles = new ArrayList<>();
        String query = "SELECT * FROM vehicles";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                vehicles.add(new Vehicle(
                        rs.getInt("vehicle_id"),
                        rs.getString("model"),
                        rs.getDouble("daily_rate"),
                        rs.getString("availability")
                ));
            }
        } catch (SQLException e) {
            showAlert("Error fetching vehicles: ", e.getMessage());
        }
        return vehicles;
    }

    private void addVehicle(String model, double dailyRate, String availability) {
        String sql = "INSERT INTO vehicles (model, daily_rate, availability) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, model);
            stmt.setDouble(2, dailyRate);
            stmt.setString(3, availability);
            stmt.executeUpdate();
            showAlert("Success", "Vehicle added successfully.");
        } catch (SQLException e) {
            showAlert("Error", e.getMessage());
        }
    }

    private void updateVehicle(int vehicleId, String model, double dailyRate, String availability) {
        String sql = "UPDATE vehicles SET model=?, daily_rate=?, availability=? WHERE vehicle_id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, model);
            stmt.setDouble(2, dailyRate);
            stmt.setString(3, availability);
            stmt.setInt(4, vehicleId);
            stmt.executeUpdate();
            showAlert("Success", "Vehicle updated successfully.");
        } catch (SQLException e) {
            showAlert("Error", e.getMessage());
        }
    }

    private void deleteVehicle(int vehicleId) {
        String sql = "DELETE FROM vehicles WHERE vehicle_id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, vehicleId);
            stmt.executeUpdate();
            showAlert("Success", "Vehicle deleted successfully.");
        } catch (SQLException e) {
            showAlert("Error", e.getMessage());
        }
    }

    private List<Customer> getAllCustomers() {
        List<Customer> customers = new ArrayList<>();
        String query = "SELECT * FROM customers";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                customers.add(new Customer(
                        rs.getInt("customer_id"),
                        rs.getString("customer_name"),
                        rs.getString("contact"),
                        rs.getString("license_number")
                ));
            }
        } catch (SQLException e) {
            showAlert("Error fetching customers: ", e.getMessage());
        }
        return customers;
    }

    private void addCustomer(String name, String contact, String license) {
        String sql = "INSERT INTO customers (customer_name, contact, license_number) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, contact);
            stmt.setString(3, license);
            stmt.executeUpdate();
            showAlert("Success", "Customer added successfully.");
        } catch (SQLException e) {
            showAlert("Error", e.getMessage());
        }
    }

    private void updateCustomer(int customerId, String name, String contact, String license) {
        String sql = "UPDATE customers SET customer_name=?, contact=?, license_number=? WHERE customer_id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, contact);
            stmt.setString(3, license);
            stmt.setInt(4, customerId);
            stmt.executeUpdate();
            showAlert("Success", "Customer updated successfully.");
        } catch (SQLException e) {
            showAlert("Error", e.getMessage());
        }
    }

    private void deleteCustomer(int customerId) {
        String sql = "DELETE FROM customers WHERE customer_id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            stmt.executeUpdate();
            showAlert("Success", "Customer deleted successfully.");
        } catch (SQLException e) {
            showAlert("Error", e.getMessage());
        }
    }
}

// Vehicle Class Definition
class Vehicle {
    private int vehicleId;
    private String model;
    private double dailyRate;
    private String availability;

    // Constructor with all parameters
    public Vehicle(int vehicleId, String model, double dailyRate, String availability) {
        this.vehicleId = vehicleId;
        this.model = model;
        this.dailyRate = dailyRate;
        this.availability = availability;
    }

    // Getters
    public int getVehicleId() {
        return vehicleId;
    }

    public String getModel() {
        return model;
    }

    public double getDailyRate() {
        return dailyRate;
    }

    public String getAvailability() {
        return availability;
    }

    // Setters
    public void setModel(String model) {
        this.model = model;
    }

    public void setDailyRate(double dailyRate) {
        this.dailyRate = dailyRate;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    // Override toString method
    @Override
    public String toString() {
        return model + " - M" + dailyRate;  // Use model and daily rate for display
    }
}

// Customer Class Definition
class Customer {
    private int customerId;
    private String name;
    private String contact;
    private String license;

    // Constructor with all parameters
    public Customer(int customerId, String name, String contact, String license) {
        this.customerId = customerId;
        this.name = name;
        this.contact = contact;
        this.license = license;
    }

    public Customer(String name, String contact, String license) {
        this.name = name;
        this.contact = contact;
        this.license = license;
    }

    // Getters
    public int getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getContact() {
        return contact;
    }

    public String getLicense() {
        return license;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    // Override toString method
    @Override
    public String toString() {
        return name;  // Use name for display
    }
}

// Booking Class Definition
class Bookings {
    private int bookingId;
    private String customerName;
    private String vehicleModel;
    private java.sql.Date startDate;
    private java.sql.Date endDate;
    private double totalAmount;

    // Constructor with all parameters
    public Bookings(int bookingId, String customerName, String vehicleModel, java.sql.Date startDate, java.sql.Date endDate, double totalAmount) {
        this.bookingId = bookingId;
        this.customerName = customerName;
        this.vehicleModel = vehicleModel;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalAmount = totalAmount;
    }

    // Getters
    public int getBookingId() {
        return bookingId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public java.sql.Date getStartDate() {
        return startDate;
    }

    public java.sql.Date getEndDate() {
        return endDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    // Override toString method
    @Override
    public String toString() {
        return "Booking ID: " + bookingId + ", Customer: " + customerName + ", Vehicle: " + vehicleModel + ", Total: M" + totalAmount; // Currency updated
    }
}
