# Venue Booking Management System

A professional desktop application built with Java AWT and SQLite for managing venues, customers, and bookings.

## Customer Features
- Browse available venues and submit pending booking requests.
- View customer-specific booking history and cancel pending bookings.
- Review completed bookings, total spending, and generate invoice text.
- See booking totals, upcoming events, and smart-slot offers from the dashboard.

## Requirements
- Java 17+
- Maven

## How to Build and Run
1. Clean and build the executable JAR:
   `mvn clean package`
2. Run tests:
   `mvn test`
3. Run the application:
   `java -jar target/venue-booking-system-1.0-SNAPSHOT.jar`

## First Launch
On the first launch, the SQLite database (`data/venue.db`) will be initialized automatically. Use default admin credentials (admin / admin) or create the first super admin via the UI if implemented.
