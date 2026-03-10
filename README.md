# Show Sphere

Event and show booking platform inspired by BookMyShow. Users can register (18+), browse events, book seats, apply coupons, and manage or cancel bookings. Admins manage events, theaters, shows, and coupons and can view booking reports in JSON or CSV.

Tech: Java 21, Spring Boot, Eureka, API Gateway, JWT, MySQL, Thymeleaf, Jenkins.

---

## Run locally

1. Start MySQL and create databases: `bookmyshow_auth`, `bookmyshow_booking`, `bookmyshow_coupon`, and any others required by the services (see each service `application.yml`).
2. From project root, start Eureka: `mvn spring-boot:run`
3. Start API Gateway, then auth, user, event, booking, coupon, then frontend. In each module: `cd <module> && mvn spring-boot:run`
4. Open the app at http://localhost:9090

---

## Repository structure

auth-service, event-service, booking-service, coupon-service, user-service, api-gateway, frontend. Eureka server runs from the root module.
