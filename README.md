# JobNet — Premium Job Search Android App (Java + XML)

A Dribbble-quality Android job search application built in Java with XML layouts, following the
UI reference from the "Job Seeker Mobile App UI Kit" design.

---

## Design Reference

Inspired by: https://www.indiamart.com/proddetail/job-seeker-mobile-app-ui-kit-design-in-adobe-xd

Key design choices:
- **Primary**: Royal Blue `#1A56DB` — hero headers, buttons, primary actions
- **Accent Yellow**: `#FFC107` — progress ring on profile screen
- **Accent Green**: `#16A34A` — "SEE ALL" CTAs, success states
- **Typography**: Inter (via Google Fonts provider) — Regular, Medium, SemiBold, Bold
- **Cards**: 16dp corner radius, 4dp elevation, white background
- **Profile hero**: Blue gradient header with circular avatar + yellow progress ring (72%)

---

## Screenshots

*(Add your app screenshots to the `screenshots/` folder and link them here)*

| Home Screen | Search & Filters | Job Details | Profile Dashboard |
| :---: | :---: | :---: | :---: |
| <img src="screenshots/dashboard_jobseeker.png" width="250"/> | <img src="screenshots/search.png" width="250"/> | <img src="screenshots/saved_jobs.png" width="250"/> | <img src="screenshots/profile_jobseeker.png" width="250"/> |

---

## Project Structure

```
android/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/jobnet/app/
│       │   ├── ui/
│       │   │   ├── main/MainActivity.java
│       │   │   ├── home/
│       │   │   │   ├── HomeFragment.java
│       │   │   │   ├── JobsAdapter.java
│       │   │   │   ├── FeaturedJobsAdapter.java
│       │   │   │   └── CategoriesAdapter.java
│       │   │   ├── search/SearchFragment.java
│       │   │   ├── jobdetails/JobDetailsFragment.java
│       │   │   ├── saved/SavedJobsFragment.java
│       │   │   └── profile/ProfileFragment.java
│       │   ├── data/model/
│       │   │   ├── Job.java
│       │   │   └── JobCategory.java
│       │   └── util/SampleData.java
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml       ← BottomNavigationView + NavHostFragment
│           │   ├── fragment_home.xml       ← Hero banner, categories, recruiters, job list
│           │   ├── fragment_search.xml     ← Search bar, filter chips, results RecyclerView
│           │   ├── fragment_job_details.xml ← Collapsing hero, stats, description, apply CTA
│           │   ├── fragment_profile.xml    ← Blue hero, progress ring, stats, job searches
│           │   ├── fragment_saved.xml      ← Saved jobs list with tabs
│           │   ├── item_job_card.xml       ← Job list card item
│           │   ├── item_featured_card.xml  ← Horizontal featured job card
│           │   ├── item_category_card.xml  ← Category grid card
│           │   ├── item_requirement.xml    ← Requirement row with green check
│           │   └── item_skill_tag.xml      ← Skill pill tag
│           ├── values/
│           │   ├── colors.xml
│           │   ├── themes.xml
│           │   ├── strings.xml
│           │   └── dimens.xml
│           ├── drawable/                   ← 30+ vector icons + gradient/shape drawables
│           ├── font/                       ← Inter font (via Google Fonts provider)
│           ├── navigation/nav_graph.xml
│           ├── menu/bottom_nav_menu.xml
│           └── anim/                       ← slide in/out animations
└── build.gradle
```

---

## Setup Instructions

### 1. Open in Android Studio
- File → Open → select the `android/` folder
- Android Studio will sync Gradle automatically

### 2. Add Inter Font TTF files (optional, GMS fallback works without this)
If you have no internet or prefer local fonts:
1. Download from https://fonts.google.com/specimen/Inter
2. Extract and copy:
   - `Inter-Regular.ttf` → `res/font/inter_regular.ttf`
   - `Inter-Medium.ttf` → `res/font/inter_medium.ttf`
   - `Inter-SemiBold.ttf` → `res/font/inter_semibold.ttf`
   - `Inter-Bold.ttf` → `res/font/inter_bold.ttf`
3. Replace the `.xml` font files with the `.ttf` files

### 3. Add FlexboxLayout dependency
Already included in `app/build.gradle`:
```groovy
implementation 'com.google.android.flexbox:flexbox:3.0.0'
```

### 4. Minimum SDK
- `minSdk 24` (Android 7.0+)
- `targetSdk 34`

---

## Screens

| Screen | Description |
|--------|-------------|
| **Home** | Blue hero banner, category grid (Engineering/Fresher/Walk-in/HR), featured horizontal cards, job list |
| **Search** | Sticky search bar + filter chips (All/Full Time/Part Time/Remote/Internship), live filtering results |
| **Job Details** | Collapsing toolbar hero, salary/location/type stats card, description, requirements, skills, Apply Now CTA |
| **Saved Jobs** | Bookmarked jobs list with tab filter |
| **Profile** | Blue hero, circular avatar with animated yellow progress ring (72%), stats row (12 Saved / 22 Applied / 14 New), job search alerts, account menu |

---

## Dependencies

```groovy
implementation 'androidx.appcompat:appcompat:1.7.0'
implementation 'com.google.android.material:material:1.12.0'
implementation 'androidx.navigation:navigation-fragment:2.7.7'
implementation 'androidx.navigation:navigation-ui:2.7.7'
implementation 'androidx.cardview:cardview:1.0.0'
implementation 'com.google.android.flexbox:flexbox:3.0.0'
implementation 'androidx.coordinatorlayout:coordinatorlayout:1.2.0'
implementation 'com.github.bumptech.glide:glide:4.16.0'
```
