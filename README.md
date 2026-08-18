# Swari Sewa Backend

A comprehensive Spring Boot backend for the Swari Sewa vehicle listing platform - a SaaS-based vehicle recondition shop management system.

## 🚀 Features

- **User Management**: Multi-role authentication (Super Admin, Shop Owner, Customer)
- **Shop Management**: Complete shop profile and subscription management
- **Vehicle Management**: Advanced vehicle listings with search and filtering
- **Category Management**: Dynamic vehicle categorization
- **Enquiry System**: Customer enquiries with status tracking
- **Wishlist**: Customer wishlist functionality
- **JWT Security**: Secure token-based authentication
- **RESTful APIs**: Complete CRUD operations for all entities
- **Exception Handling**: Global exception handling with proper error responses
- **Database Integration**: MySQL with JPA/Hibernate
- **Validation**: Comprehensive input validation

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- MySQL 8.0 or higher
- IDE (IntelliJ IDEA, Eclipse, or VS Code)

## 🛠️ Technology Stack

- **Framework**: Spring Boot 3.x
- **Security**: Spring Security with JWT
- **Database**: MySQL with Spring Data JPA
- **ORM**: Hibernate
- **Validation**: Jakarta Bean Validation
- **Build Tool**: Maven
- **Lombok**: Boilerplate code reduction
- **ModelMapper**: DTO-Entity mapping
- **JWT**: JJWT library

## 📦 Dependencies

Key dependencies included in `pom.xml`:

- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Security
- Spring Boot Starter Validation
- Spring Boot Starter Mail
- MySQL Connector
- Lombok
- ModelMapper
- JWT (jjwt-api, jjwt-impl, jjwt-jackson)

## 🏗️ Project Structure

```
src/main/java/swari/sewa/
├── config/                 # Security and configuration
├── controller/             # REST controllers
├── dto/                   # Data Transfer Objects
├── enums/                 # Enum definitions
├── exception/              # Custom exceptions
├── mapper/                # Mapping utilities
├── model/                 # JPA entities
├── repository/             # Spring Data repositories
├── service/               # Service interfaces
├── service/impl/          # Service implementations
└── util/                  # Utility classes
```

## 🚀 Setup Instructions

### 1. Database Setup

```sql
-- Create database
CREATE DATABASE swari_sewa CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Import schema (use src/main/resources/schema.sql)
```

### 2. Configuration

Update `application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/swari_sewa
spring.datasource.username=your_username
spring.datasource.password=your_password

# JWT Configuration
jwt.secret=your-secret-key-here
jwt.expiration=86400000

```

Secrets (mail credentials, Brevo keys) belong in the gitignored
`src/main/resources/application-dev.properties`, or in environment variables —
never in `application.properties`. Copy the provided template to get started:

```bash
cp src/main/resources/application-dev.properties.example \
   src/main/resources/application-dev.properties
```

Email OTPs are delivered through the Brevo SMTP relay (`smtp-relay.brevo.com:587`)
using a Brevo **SMTP key**; `brevo.sender-email` must be a sender verified in the
Brevo dashboard. SMS OTPs are **not** sent in the `dev` profile — Brevo SMS costs
credits, so the code is logged to the console instead. OTPs are never returned in
an API response.

### 3. Build and Run

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Start application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## 📚 API Documentation

### Authentication Endpoints

| Method | Endpoint | Description |
|--------|-----------|-------------|
| POST | `/api/auth/signup` | User registration |
| POST | `/api/auth/login` | User login |
| POST | `/api/auth/check-email` | Check email availability |

### User Management

| Method | Endpoint | Description |
|--------|-----------|-------------|
| GET | `/api/users/{id}` | Get user by ID |
| GET | `/api/users` | Get all users (Admin) |
| PUT | `/api/users/{id}` | Update user |
| DELETE | `/api/users/{id}` | Delete user (Admin) |

### Shop Management

| Method | Endpoint | Description |
|--------|-----------|-------------|
| GET | `/api/shops` | Get all shops |
| GET | `/api/shops/{id}` | Get shop by ID |
| POST | `/api/shops` | Create shop |
| PUT | `/api/shops/{id}` | Update shop |
| GET | `/api/shops/featured` | Get featured shops |

### Vehicle Management

| Method | Endpoint | Description |
|--------|-----------|-------------|
| GET | `/api/vehicles` | Get all vehicles |
| GET | `/api/vehicles/{id}` | Get vehicle by ID |
| POST | `/api/vehicles` | Create vehicle |
| PUT | `/api/vehicles/{id}` | Update vehicle |
| POST | `/api/vehicles/search` | Search vehicles |
| GET | `/api/vehicles/active` | Get active vehicles |

### Category Management

| Method | Endpoint | Description |
|--------|-----------|-------------|
| GET | `/api/categories` | Get all categories |
| GET | `/api/categories/{id}` | Get category by ID |
| POST | `/api/categories` | Create category (Admin) |
| PUT | `/api/categories/{id}` | Update category (Admin) |

### Enquiry Management

| Method | Endpoint | Description |
|--------|-----------|-------------|
| GET | `/api/enquiries` | Get all enquiries |
| GET | `/api/enquiries/{id}` | Get enquiry by ID |
| POST | `/api/enquiries` | Create enquiry |
| PUT | `/api/enquiries/{id}/status` | Update enquiry status |

### Wishlist Management

| Method | Endpoint | Description |
|--------|-----------|-------------|
| POST | `/api/wishlist/add` | Add to wishlist |
| DELETE | `/api/wishlist/remove` | Remove from wishlist |
| GET | `/api/wishlist/customer/{id}` | Get customer wishlist |

## 🔐 Security

### JWT Authentication

- **Token Type**: Bearer
- **Default Expiration**: 24 hours
- **Header Format**: `Authorization: Bearer <token>`

### Roles

- **SUPER_ADMIN**: Full system access
- **SHOP_OWNER**: Shop and vehicle management
- **CUSTOMER**: Browse, search, and enquire

## 📊 Database Schema

The application uses the following main tables:

- `users` - User accounts and authentication
- `shops` - Shop information and profiles
- `categories` - Vehicle categories
- `vehicles` - Vehicle listings
- `enquiries` - Customer enquiries
- `wishlists` - Customer wishlists
- `vehicle_images` - Vehicle image gallery

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Generate test coverage report
mvn jacoco:report
```

## 📝 Logging

Logs are configured for:

- Application logs: `logging.level.swari.sewa=DEBUG`
- SQL logs: `logging.level.org.hibernate.SQL=DEBUG`
- Security logs: `logging.level.org.springframework.security=DEBUG`

## 🚀 Deployment

### Environment Variables

```bash
export DATABASE_URL=jdbc:mysql://your-host:3306/swari_sewa
export DATABASE_USERNAME=your_username
export DATABASE_PASSWORD=your_password
export JWT_SECRET=your-jwt-secret
```

### Docker Support

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/swari-sewa-backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License.

## 📞 Support

For support and queries:
- Email: support@swarisewa.com
- Documentation: [API Docs](http://localhost:8080/swagger-ui.html)

## 🔧 Default Credentials

**Super Admin** (created automatically):
- Email: `admin@swarisewa.com`
- Password: `admin123`

## 📈 Performance Considerations

- Database indexing on frequently queried fields
- Pagination for large datasets
- Caching for static data (categories)
- Connection pooling configured
- Lazy loading for JPA relationships

## 🔄 Version History

- **v1.0.0** - Initial release with core functionality
- Complete CRUD operations
- JWT authentication
- Search and filtering
- Exception handling
