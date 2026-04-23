# JobNet — Premium Android Job Portal

JobNet is a professional, microservice-powered Android application designed to bridge the gap between talented job seekers and top recruiters. Built natively in Java with a modern XML-based UI, JobNet delivers a seamless, high-performance experience with an elegant design system.

The platform goes beyond standard job portals by integrating a **dual-backend microservice architecture** (Spring Boot + Flask) to provide advanced capabilities like AI Resume Parsing and standardized Skills Extraction (powered by EMSI/Lightcast).

---

## Architecture & Tech Stack

JobNet is built using modern development practices, ensuring high performance, scalability, and maintainability across the full stack.

### 📱 Android Frontend (Java)
* **UI Design:** Native XML with Material Design Components, FlexboxLayout, CoordinatorLayout.
* **Architecture:** MVC/MVVM principles with robust Data Binding and Navigation Graph.
* **Networking & Media:** Retrofit / OkHttp for API integration, Glide for efficient image loading.
* **Storage:** SharedPreferences for secure session token management.

### ⚙️ Primary Backend (Spring Boot / Java)
* **Security:** Stateless JWT-based authentication with refresh token rotation.
* **Database:** MongoDB with highly optimized compound indexes for lightning-fast job and applicant queries.
* **API Integration:** Seamless connection with external APIs like EMSI/Lightcast for standardized skill taxonomy.

### 🧠 AI & Parsing Backend (Flask / Python)
* **Resume Processing:** A dedicated Python microservice handling intelligent NLP Resume Parsing (`resume_parser.py`) to automatically extract candidate details and skills from uploaded PDFs.

---

## Key Capabilities & Features

### 👨‍💻 For Job Seekers
* **Smart Search & Filters:** Find the perfect role with real-time search and advanced filters (Full Time, Remote, Internship, etc.).
* **AI Resume Parsing:** Upload your resume and let the Python microservice automatically parse your skills and experience.
* **Standardized Skills:** Real-time skills extraction using the EMSI/Lightcast API.
* **Detailed Job Listings:** View comprehensive job descriptions, salary brackets, required skills, and location details.
* **One-Tap Apply:** Streamlined application process with a beautifully animated progress tracker.
* **Saved Jobs:** Bookmark interesting opportunities to review and apply later.
* **Profile Management:** Showcase your skills, education, and experience with a dynamic profile dashboard.
* **Application Tracking:** Monitor the status of your applications across the entire hiring pipeline (Applied → Reviewed → Interviewing).

### 🏢 For Recruiters
* **Recruiter Dashboard:** A dedicated hub to oversee all active job postings and recent applicant activity.
* **Job Management:** Easily post new jobs, edit existing listings, and manage application deadlines.
* **Applicant Tracking System (ATS):** Review applicant profiles, download resumes, and update candidate statuses in real-time.
* **Real-time Notifications:** Stay informed when new candidates apply to your job postings.
* **Company Profile:** Manage your employer brand and recruiter details.

---

## App in Action

### Job Seeker View

| Dashboard | Search & Filters | Saved Jobs | Profile & Stats |
| :---: | :---: | :---: | :---: |
| <img src="screenshots/dashboard_jobseeker.jpeg" height="320"/> | <img src="screenshots/search.jpeg" height="320"/> | <img src="screenshots/saved_jobs.jpeg" height="320"/> | <img src="screenshots/profile_jobseeker.jpeg" height="320"/> |

<br>

| My Applications | Application Progress | Job Details (1) | Job Details (2) |
| :---: | :---: | :---: | :---: |
| <img src="screenshots/my_applications.jpeg" height="320"/> | <img src="screenshots/application_progress.jpeg" height="320"/> | <img src="screenshots/job_detail_img1.jpeg" height="320"/> | <img src="screenshots/job_detail_img2.jpeg" height="320"/> |

<br><br>

### Recruiter View

| Dashboard | Posted Jobs | Post New Job | Job Applicants |
| :---: | :---: | :---: | :---: |
| <img src="screenshots/dashboard_recruiter.jpeg" height="320"/> | <img src="screenshots/recruiter_posted_jobs.jpeg" height="320"/> | <img src="screenshots/recruiter_post_new_job.jpeg" height="320"/> | <img src="screenshots/job_applicants.jpeg" height="320"/> |

<br>

| Notifications | Recruiter Profile | Edit Job | Edit Profile |
| :---: | :---: | :---: | :---: |
| <img src="screenshots/notification_recruiter.jpeg" height="320"/> | <img src="screenshots/profile_recruiter.jpeg" height="320"/> | <img src="screenshots/recruiter_edit_job.jpeg" height="320"/> | <img src="screenshots/edit_profile.jpeg" height="320"/> |

---

## Setup Instructions

### 1. Open in Android Studio
- Launch Android Studio and select **File → Open**.
- Navigate to and select the cloned `android/` folder.
- Android Studio will automatically sync the Gradle files and download required dependencies.

### 2. Configure API Endpoints
Update your networking client's base URL to point to your running Spring Boot server. Make sure the Spring Boot server is correctly linked to the MongoDB instance and the Flask microservice.

### 3. Build & Run
- Recommended Minimum SDK: `minSdk 24` (Android 7.0 Nougat or higher).
- Target SDK: `targetSdk 34`.
- Click the **Run** button (Shift + F10) to deploy the app to an emulator or connected physical device.
