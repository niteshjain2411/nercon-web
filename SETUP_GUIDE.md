# NERCON 2026 Registration System - Backend Setup Guide

## Overview
This is a Spring Boot web application for the NERCON 2026 conference registration system. It includes:
- Frontend registration form (HTML/CSS/JavaScript)
- Backend API for saving registrations
- Excel export functionality using Apache POI

## Project Structure

```
nercon-web/
├── src/
│   ├── main/
│   │   ├── java/org/example/
│   │   │   ├── NerconApplication.java          # Spring Boot Application Main Class
│   │   │   ├── controller/
│   │   │   │   └── RegistrationController.java # REST API Endpoints
│   │   │   ├── model/
│   │   │   │   └── RegistrationData.java       # Data Model
│   │   │   └── service/
│   │   │       └── ExcelService.java           # Excel File Handling
│   │   ├── resources/
│   │   │   ├── application.properties          # Spring Boot Configuration
│   │   │   ├── home.html                       # Home Page
│   │   │   └── registration.html               # Registration Form
│   └── test/
└── pom.xml                                      # Maven Dependencies
```

## Dependencies

The project uses:
- **Spring Boot 3.2.0** - Web framework
- **Apache POI 5.2.5** - Excel file handling
- **Gson 2.10.1** - JSON processing

All dependencies are defined in `pom.xml`

## Running the Application

### Prerequisites
- Java 24 or later
- Maven 3.6+

### Build and Run

1. **Build the project:**
   ```bash
   cd D:\Work\nercon-web
   mvn clean install
   ```

2. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

3. **Access the application:**
   - Home Page: `http://localhost:8080/nercon/`
   - Registration: `http://localhost:8080/nercon/registration.html`
   - API Health: `http://localhost:8080/nercon/api/registration/health`

## API Endpoints

### 1. Save Registration
**POST** `/nercon/api/registration/save`

**Request Body:**
```json
{
  "fullname": "Dr. John Doe",
  "email": "john@example.com",
  "phone": "+91 9876543210",
  "gender": "Male",
  "institute": "AIIMS Delhi",
  "city": "Delhi",
  "state": "Delhi",
  "medcouncil": "MCI",
  "registration": "12345",
  "workshops": ["ws1", "ws2"],
  "accompany": "1",
  "txnid": "UPI12345",
  "txndate": "2026-04-05",
  "totalAmount": "7000",
  "delegateId": "NER-2026-123456"
}
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Registration saved successfully",
  "delegateId": "NER-2026-123456"
}
```

**Response (Error):**
```json
{
  "success": false,
  "message": "Error message here"
}
```

### 2. Get All Registrations
**GET** `/nercon/api/registration/all`

**Response:**
```json
{
  "success": true,
  "count": 150,
  "data": [
    {
      "delegateId": "NER-2026-123456",
      "fullname": "Dr. John Doe",
      ...
    }
  ]
}
```

### 3. Health Check
**GET** `/nercon/api/registration/health`

**Response:**
```json
{
  "status": "OK",
  "message": "Registration API is running"
}
```

## Excel File Output

**Location:** `registrations/NERCON_2026_Registrations.xlsx`

**Columns:**
1. S.No
2. Delegate ID
3. Full Name
4. Email
5. Phone
6. Gender
7. Institute
8. City
9. State
10. Medical Council
11. Registration No.
12. Workshops (comma-separated)
13. Accompanying Persons
14. Total Amount
15. Transaction ID
16. Transaction Date
17. Submission Time (auto-generated)

**Features:**
- Header row with blue background and white text
- Auto-sized columns
- Centered alignment for numeric fields
- Timestamp of submission added automatically
- Thread-safe saving (synchronized method)
- Creates file automatically if it doesn't exist

## Frontend Integration

The registration form (`registration.html`) has been updated to:

1. **Collect form data** from all fields
2. **Send POST request** to backend API
3. **Handle responses** and show success/error messages
4. **Generate unique Delegate ID** for each registration

### Form Submission Flow

```
User clicks "Submit Application"
         ↓
Form data collected and validated
         ↓
JSON payload created
         ↓
POST request to /api/registration/save
         ↓
Backend validates and saves to Excel
         ↓
Response received with Delegate ID
         ↓
Success message displayed with ID
```

## Configuration

**Server Configuration** (`application.properties`):
- Server Port: `8080`
- Context Path: `/nercon`
- Max File Upload Size: `10MB`
- Log File: `logs/nercon.log`

## Logging

Logs are written to:
- **Console:** INFO level and above
- **File:** `logs/nercon.log` (DEBUG level)

## Error Handling

The application includes error handling for:
- Missing required fields
- File I/O errors
- Excel creation errors
- Invalid data formats
- Network/connection errors

All errors are logged to console and file for debugging.

## Security Notes

- CORS is enabled for all origins (can be restricted in production)
- Data validation is performed on both client and server
- No authentication required (can be added for production)
- File permissions should be configured appropriately

## Production Deployment

For production deployment:

1. **Change context path** in `application.properties`
2. **Enable authentication** in controller
3. **Restrict CORS origins** to specific domains
4. **Add database** instead of Excel for better scalability
5. **Configure SSL/HTTPS**
6. **Set up proper file storage** location
7. **Implement backup** strategy for Excel files
8. **Add request rate limiting**

## Troubleshooting

### Port 8080 already in use
Change in `application.properties`:
```properties
server.port=8081
```

### Permission denied for registrations folder
Ensure the application has write permissions to the directory where it's running.

### Excel file locked error
Close Excel files if you have them open. The application needs exclusive access to write.

### CORS errors
Make sure all frontend requests use the correct API endpoint: `/nercon/api/registration/save`

## Support

For issues or questions:
- Email: `nercon2026@gmail.com`
- Check logs: `logs/nercon.log`
- Verify API: `http://localhost:8080/nercon/api/registration/health`

## Version History

- **v1.0** - Initial release with basic registration and Excel export

---

**Last Updated:** April 5, 2026
**Application:** NERCON 2026 Registration System

