package com.jobnet.app.util;

import com.jobnet.app.R;
import com.jobnet.app.data.model.Job;
import com.jobnet.app.data.model.JobCategory;

import java.util.ArrayList;
import java.util.List;

public class SampleData {

    public static List<JobCategory> getCategories() {
        List<JobCategory> categories = new ArrayList<>();
        categories.add(new JobCategory("Design", R.drawable.ic_edit, 248, R.color.primary));
        categories.add(new JobCategory("Engineering", R.drawable.ic_briefcase, 512, R.color.accent));
        categories.add(new JobCategory("Marketing", R.drawable.ic_chart, 189, R.color.teal));
        categories.add(new JobCategory("Finance", R.drawable.ic_dollar, 134, R.color.success));
        categories.add(new JobCategory("HR", R.drawable.ic_users, 97, R.color.warning));
        categories.add(new JobCategory("Education", R.drawable.ic_graduation, 73, R.color.primary_dark));
        return categories;
    }

    public static List<Job> getFeaturedJobs() {
        List<Job> jobs = new ArrayList<>();
        jobs.add(new Job("1", "Senior Product Designer", "Google",
                "Mountain View, CA", "$120k — $160k", "Full Time", "Hybrid",
                "We are looking for a talented Senior Product Designer to join our UX team. " +
                "You will lead end-to-end design for consumer products used by billions of people.",
                "2 days ago", R.drawable.ic_briefcase));

        jobs.add(new Job("2", "Lead iOS Engineer", "Apple",
                "Cupertino, CA", "$150k — $200k", "Full Time", "On-site",
                "Join Apple's iOS platform team and work on next-generation mobile experiences. " +
                "You will architect and implement iOS frameworks that ship to hundreds of millions of devices.",
                "1 day ago", R.drawable.ic_briefcase));

        jobs.add(new Job("3", "Data Scientist", "Meta",
                "Menlo Park, CA", "$130k — $175k", "Full Time", "Remote",
                "Meta is seeking a Data Scientist to join our AI Research team. " +
                "You will develop and apply advanced ML models to solve complex problems at scale.",
                "3 days ago", R.drawable.ic_chart));

        jobs.add(new Job("4", "Frontend Engineer", "Stripe",
                "San Francisco, CA", "$140k — $180k", "Full Time", "Hybrid",
                "Build the financial infrastructure of the internet. " +
                "Join Stripe's frontend team and create elegant, performant interfaces for our dashboard.",
                "5 days ago", R.drawable.ic_briefcase));

        return jobs;
    }

    public static List<Job> getRecommendedJobs() {
        List<Job> jobs = new ArrayList<>();
        jobs.add(new Job("5", "UI/UX Designer", "Figma",
                "San Francisco, CA", "$90k — $130k", "Full Time", "Remote",
                "Join Figma and help design the future of collaborative design tools.",
                "Today", R.drawable.ic_edit));
        jobs.get(0).setRating(4.8f);

        jobs.add(new Job("6", "React Native Developer", "Airbnb",
                "Remote", "$100k — $145k", "Full Time", "Remote",
                "Build the mobile experiences for Airbnb's global community of hosts and guests.",
                "Yesterday", R.drawable.ic_briefcase));
        jobs.get(1).setRating(4.6f);

        jobs.add(new Job("7", "Product Manager", "Spotify",
                "New York, NY", "$110k — $150k", "Full Time", "Hybrid",
                "Lead product strategy for Spotify's creator tools and monetization platform.",
                "2 days ago", R.drawable.ic_chart));
        jobs.get(2).setRating(4.7f);

        jobs.add(new Job("8", "DevOps Engineer", "Netflix",
                "Los Gatos, CA", "$130k — $170k", "Full Time", "On-site",
                "Scale Netflix's infrastructure to deliver content to 240 million subscribers worldwide.",
                "3 days ago", R.drawable.ic_settings));
        jobs.get(3).setRating(4.5f);

        jobs.add(new Job("9", "Brand Designer", "Notion",
                "San Francisco, CA", "$85k — $115k", "Full Time", "Remote",
                "Shape the visual identity of Notion's brand across all touchpoints.",
                "4 days ago", R.drawable.ic_edit));
        jobs.get(4).setRating(4.9f);

        return jobs;
    }

    public static List<Job> getSavedJobs() {
        List<Job> saved = new ArrayList<>();
        Job job1 = getFeaturedJobs().get(0);
        job1.setSaved(true);
        saved.add(job1);

        Job job2 = getRecommendedJobs().get(1);
        job2.setSaved(true);
        saved.add(job2);

        Job job3 = getRecommendedJobs().get(4);
        job3.setSaved(true);
        saved.add(job3);
        return saved;
    }

        public static List<Job> getAllJobs() {
                List<Job> all = new ArrayList<>();
                all.addAll(getFeaturedJobs());
                all.addAll(getRecommendedJobs());
                return all;
        }
}
