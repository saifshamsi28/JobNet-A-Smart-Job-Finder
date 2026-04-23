# JobNet Android App

Native Android client for JobNet, a role-based job platform with separate experiences for job seekers and recruiters. The app is written in Java (XML UI) and integrates with a Spring Boot backend plus a Flask service for job data and scraping workflows.

## What Is Implemented

### Job seeker features
- Login and registration with JWT-based session management.
- Home feed with suggested and recent jobs.
- Search by title/location/company and salary filter support.
- Job details with apply flow.
- Saved jobs.
- My applications and application timeline/status tracking.
- Profile and profile edit (skills + core details).
- Notifications screen.
- Categories and category-wise jobs.

### Recruiter features
- Recruiter dashboard.
- Post new job.
- View and manage posted jobs.
- Edit job and update job status.
- View applicants for a posting.
- Recruiter profile and dedicated recruiter edit profile screen.
- Recruiter-facing notifications.

### Backend-connected capabilities used by the app
- Auth: login, register, refresh token, current user.
- Jobs: suggested/recent lists, filtered search, job details.
- Applications: apply, list my applications, update status.
- Recruiter APIs: CRUD-style job management and applicant listing.
- User APIs: profile fetch/update, save/unsave jobs, skills update.
- Skills API available in backend (`/skills`, `/skills/search`).
- Resume upload/finalize endpoints exist in backend (`/user/resume/*`).

## Tech Stack

### Android
- Java 17
- AndroidX + Material Components
- Navigation Component
- Retrofit + OkHttp
- Glide
- ViewBinding

### Backend
- Spring Boot (auth, users, jobs, applications, recruiter APIs)
- MongoDB
- Flask microservice (`/health`, `/jobs`, `/scrape`, `/scrape/<task_id>`)

## App In Action

### Job Seeker Screens

| Dashboard | Search | Saved Jobs | Profile |
| :---: | :---: | :---: | :---: |
| <img src="screenshots/dashboard_jobseeker.jpeg" height="340"/> | <img src="screenshots/search.jpeg" height="340"/> | <img src="screenshots/saved_jobs.jpeg" height="340"/> | <img src="screenshots/profile_jobseeker.jpeg" height="340"/> |

| My Applications | Application Timeline | Job Details 1 | Job Details 2 |
| :---: | :---: | :---: | :---: |
| <img src="screenshots/my_applications.jpeg" height="340"/> | <img src="screenshots/application_progress.jpeg" height="340"/> | <img src="screenshots/job_detail_img1.jpeg" height="340"/> | <img src="screenshots/job_detail_img2.jpeg" height="340"/> |

### Recruiter Screens

| Dashboard | Posted Jobs | Post Job | Applicants |
| :---: | :---: | :---: | :---: |
| <img src="screenshots/dashboard_recruiter.jpeg" height="340"/> | <img src="screenshots/recruiter_posted_jobs.jpeg" height="340"/> | <img src="screenshots/recruiter_post_new_job.jpeg" height="340"/> | <img src="screenshots/job_applicants.jpeg" height="340"/> |

| Notifications | Recruiter Profile | Edit Job | Edit Profile |
| :---: | :---: | :---: | :---: |
| <img src="screenshots/notification_recruiter.jpeg" height="340"/> | <img src="screenshots/profile_recruiter.jpeg" height="340"/> | <img src="screenshots/recruiter_edit_job.jpeg" height="340"/> | <img src="screenshots/edit_profile.jpeg" height="340"/> |

## Project Structure

- `JobNetApp/` Android app (this folder)
- `JobNet-springboot-backend/` main backend APIs
- `JobNet-flask-backend/` scraping/data microservice

## Local Setup

### 1. Start Flask backend
From `JobNet-flask-backend/`:

```bash
pip install -r requirements.txt
python app.py
```

Default port: `5000`

### 2. Start Spring Boot backend
From `JobNet-springboot-backend/`:

1. Create `.env` from `.env.example` and fill values (`MONGO_URI`, `JWT_SECRET`, optional skills/supabase keys).
2. Run:

```bash
./mvnw spring-boot:run
```

Default port: `8080`

Spring reads:
- `MONGO_URI`
- `FLASK_HOSTED_URL`
- `JWT_SECRET`
- skills provider env vars

### 3. Configure Android base URL
`JobNetApp/app/build.gradle` uses:

```gradle
def backendBaseUrl = project.findProperty("JOBNET_BASE_URL") ?: "https://jobnet-gdsn.onrender.com/"
```

For local development, set in `JobNetApp/gradle.properties` (or user-level Gradle properties):

```properties
JOBNET_BASE_URL=http://10.0.2.2:8080/
```

Use `10.0.2.2` for Android emulator to reach host machine localhost.

### 4. Run Android app
- Open `JobNetApp/` in Android Studio.
- Sync Gradle.
- Run on emulator/device.

Current Android config:
- `compileSdk 34`
- `minSdk 26`
- `targetSdk 34`
- Java compatibility: 17

## Notes

- The app supports automatic token refresh through an OkHttp interceptor.
- Resume upload/finalization APIs are wired in Spring (`/user/resume/upload-chunk`, `/user/resume/finalize-upload`).
- If you enable skills provider credentials, `/skills` and `/skills/search` use provider data with fallback behavior.
