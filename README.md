# NERCON Web - Registration Portal

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Java Version](https://img.shields.io/badge/java-17+-blue)
![Spring Boot](https://img.shields.io/badge/spring%20boot-3.2.0-green)

## Project Overview

NERCON Web is a modern, responsive web application for managing conference registrations. It provides a seamless registration experience with multi-step form processing, real-time validation, and data export capabilities. This application is specifically designed for the NERCON 2026 conference.

## Features

- **Multi-Step Registration**: Two-page registration form with terms & conditions on the first page
- **Dynamic Form Fields**: Support for various field types including text inputs, dropdowns, checkboxes, and file uploads
- **Excel Export**: Automatic export of registration data to Excel spreadsheets
- **Responsive Design**: Mobile-friendly interface that works across all devices
- **Data Validation**: Client-side and server-side validation for data integrity
- **Template System**: Reusable HTML templates for headers and footers
- **Email Integration**: Support for confirmation emails and system notifications

## Requirements

- **Java**: JDK 17 or higher
- **Maven**: 3.6.0 or higher
- **Spring Boot**: 3.2.0
- **Apache POI**: For Excel file generation

## Installation

### 1. Clone the Repository
```bash
git clone <repository-url>
cd nercon-web
```

### 2. Build the Project
```bash
mvn clean install
```

### 3. Run the Application

#### Using Maven Spring Boot Plugin
```bash
mvn spring-boot:run
```

#### Using Java Command
```bash
java -jar target/nercon-web-1.0-SNAPSHOT.jar
```

#### Using WAR File
```bash
# Build WAR (ensure packaging is set to war in pom.xml)
mvn clean package

# Deploy to application server (Tomcat, etc.)
# Or run directly with Java
java -jar target/nercon-web-1.0-SNAPSHOT.war
```

## Project Structure

```
nercon-web/
├── src/
│   ├── main/
│   │   ├── java/org/example/
│   │   │   ├── Main.java                    # Application entry point
│   │   │   ├── NerconApplication.java       # Spring Boot application class
│   │   │   ├── config/
│   │   │   │   └── WebMvcConfig.java        # Web configuration
│   │   │   ├── controller/
│   │   │   │   ├── WebController.java       # Web pages controller
│   │   │   │   └── RegistrationController.java  # Registration API controller
│   │   │   ├── model/
│   │   │   │   └── RegistrationData.java    # Registration data model
│   │   │   └── service/
│   │   │       └── ExcelService.java        # Excel export service
│   │   └── resources/
│   │       ├── application.properties       # Application configuration
│   │       ├── home.html                    # Home page
│   │       ├── registration.html            # Registration pages
│   │       ├── header.html                  # Common header template
│   │       ├── footer.html                  # Common footer template
│   │       └── template-loader.js           # Template loading utility
│   └── test/
│       └── java/                            # Test files
├── pom.xml                                  # Maven configuration
├── README.md                                # This file
└── target/                                  # Build output

```

## Configuration

### Application Properties
Edit `src/main/resources/application.properties` to configure:

```properties
# Server Configuration
server.port=8080
server.servlet.context-path=/

# Application Settings
app.name=NERCON 2026
app.email=nercon2026@gmail.com
app.registration.excel.path=registrations/
```

## Usage

### Accessing the Application

1. Start the application (see Installation section)
2. Open your browser and navigate to: `http://localhost:8080`
3. Click on "Registration" to start the registration process
4. Complete the two-page registration form
5. Submit to save your registration data

### Registration Process

**Page 1**: Review terms & conditions
- Delegates registering under PG student category must upload a letter/certificate signed by Head of Department
- Accompanying persons get access to venue, meals, and banquet dinner
- Payment instructions (UPI/NEFT)
- Support email: nercon2026@gmail.com

**Page 2**: Enter registration details
- Personal Information (Name, Email, Phone)
- Professional Details (Institute, Medical Council)
- Location Details (City, State)
- Gender Selection (Male, Female, Other)
- Category Selection
- Document Upload

### Excel Export

Registration data is automatically saved to Excel files in the `registrations/` directory:
- File name format: `NERCON_2026_Registrations_[timestamp].xlsx`
- Includes all registration fields and metadata

## API Endpoints

### Web Pages
- `GET /` - Home page
- `GET /registration` - Registration page

### Registration API
- `POST /api/register` - Submit registration
- `GET /api/registrations` - Get all registrations (admin)
- `GET /api/registrations/{id}` - Get specific registration (admin)

## Troubleshooting

### Common Issues

#### Maven Build Errors
```bash
# Clean cache and rebuild
mvn clean install -U
```

#### Class File Version Error
If you get "Unsupported class file major version", ensure you're using Java 17 or higher:
```bash
java -version
```

#### Port Already in Use
Change the port in `application.properties`:
```properties
server.port=8081
```

#### Excel Export Issues
Ensure the `registrations/` directory exists and is writable.

## Development

### Adding New Fields to Registration

1. Update `RegistrationData.java` model
2. Add field to HTML form (`registration.html`)
3. Update `ExcelService.java` to include new field in export
4. Test registration submission

### Customizing Templates

Edit `header.html` and `footer.html` for common elements.
Update `template-loader.js` if adding new templates.

## Contributing

Please follow these guidelines:
1. Create a feature branch (`git checkout -b feature/AmazingFeature`)
2. Commit your changes (`git commit -m 'Add AmazingFeature'`)
3. Push to the branch (`git push origin feature/AmazingFeature`)
4. Submit a Pull Request

## Support

For technical support and registration queries:
- Email: nercon2026@gmail.com
- Issue Tracker: GitHub Issues
- Documentation: See SETUP_GUIDE.md

## License

This project is proprietary and developed for NERCON 2026 conference.

## Changelog

### Version 1.0 (April 2026)
- Initial release
- Two-page registration form
- Excel export functionality
- Template-based UI
- Email support integration

---

**Last Updated**: April 5, 2026
**Maintainer**: NERCON Development Team
