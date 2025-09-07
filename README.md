# JobNet: A Smart Job Search Platform

[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)]()  
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)]()  
[![MongoDB](https://img.shields.io/badge/MongoDB-4EA94B?style=for-the-badge&logo=mongodb&logoColor=white)]()  
[![Flask](https://img.shields.io/badge/Flask-000000?style=for-the-badge&logo=flask&logoColor=white)]()  
[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)]()

---

> 🚀 **JobNet** – A smart job search platform with an Android app powered by **Spring Boot + MongoDB backend** and a **Flask scraper**. Features **secure JWT auth, Room DB caching, and resume-based job recommendations**.

---

## 📌 Project Duration
**Nov 2024 – Jan 2025**

---

## 🛠️ Technologies Used
- **Android (Java, XML, Room Database)** – Frontend
- **Spring Boot, Maven, JWT, MongoDB** – Backend
- **Python, Flask, Selenium** – Job Scraper
- **Resume Parsing (Python)** – For personalized job recommendations

---

## ✨ Key Features
- 🔍 **Job Aggregation**: Scrapes job postings from **Naukri** and **Indeed** using a Flask microservice.
- 🔐 **Secure Authentication**: Implemented **JWT-based authentication** for user login/authorization.
- ⚡ **Fast & Optimized**: Cached scraped jobs in **MongoDB**, reducing API response time significantly.
- 📱 **Android App with Room DB**: Stores frequently accessed job data locally, reducing redundant API calls and boosting speed by **70%**.
- 📄 **Resume Parsing & Recommendations**: Extracts **skills, experience, and profile links** from resumes to suggest relevant jobs.
- 🖥️ **Cross-Platform Support**:
    - Android app for users.
    - Backend services for job storage, parsing, and serving APIs.

---

## 📱 Android App Highlights
- Built in **Java + XML**.
- Integrated **Room Database** for offline caching.
- Search/filter jobs by **title, salary, location, and company**.
- Smooth **splash screen + shimmer effect** while loading jobs.
- Profile section with **resume upload and parsing support**.

---

## 🖼️ System Architecture

```mermaid
flowchart LR
    User[Android App] -->|API Calls| SpringBootBackend
    SpringBootBackend -->|Fetch Jobs| MongoDB
    SpringBootBackend -->|If Not Found| FlaskScraper
    FlaskScraper -->|Scrapes Jobs| ExternalSites[Naukri/Indeed]
    SpringBootBackend -->|Return Jobs| User
    User -->|Upload Resume| SpringBootBackend
    SpringBootBackend -->|Parse| FlaskParser
    SpringBootBackend -->|Recommend Jobs| User
