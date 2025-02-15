package com.saif.jobnet.Models;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity(
        tableName = "jobs",
        indices = {
                @Index(value = "title"), // Index on title for faster searches
                @Index(value = "jobId", unique = true), // Unique index on stringId
                @Index(value = "url") // Index on URL for faster lookups
        }
)
public class Job implements Parcelable {
    @PrimaryKey
    @SerializedName("id")
    @NonNull
    private String jobId;

    @SerializedName("title")
    private String title;

    @SerializedName("company")
    private String company;

    @SerializedName("location")
    private String location;

    @SerializedName("salary")
    private String salary;

    @SerializedName("minSalary")
    private long minSalary;

    @SerializedName("maxSalary")
    private long maxSalary;

    @SerializedName("link")
    private String url;

    @SerializedName("rating")
    private String rating;

    @SerializedName("reviews")
    private String review;

    @SerializedName("post_date")
    private String postDate;

    @SerializedName("openings")
    @ColumnInfo(name = "openings")
    private String openings;

    @SerializedName("applicants")
    @ColumnInfo(name = "applicants")
    private String applicants;

    @ColumnInfo(typeAffinity = ColumnInfo.TEXT)
    @SerializedName("description")
    private String shortDescription;

    @ColumnInfo(typeAffinity = ColumnInfo.TEXT)
    @SerializedName("full_description")
    private String fullDescription;

    // Constructor
    public Job() {}

    // Parcelable implementation
    protected Job(Parcel in) {
        jobId = in.readString();
        title = in.readString();
        company = in.readString();
        location = in.readString();
        salary = in.readString();
        url = in.readString();
        rating = in.readString();
        review = in.readString();
        postDate = in.readString();
        openings = in.readString();
        applicants = in.readString();
        shortDescription = in.readString();
        fullDescription = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(jobId);
        dest.writeString(title);
        dest.writeString(company);
        dest.writeString(location);
        dest.writeString(salary);
        dest.writeString(url);
        dest.writeString(rating);
        dest.writeString(review);
        dest.writeString(postDate);
        dest.writeString(openings);
        dest.writeString(applicants);
        dest.writeString(shortDescription);
        dest.writeString(fullDescription);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Job> CREATOR = new Creator<Job>() {
        @Override
        public Job createFromParcel(Parcel in) {
            return new Job(in);
        }

        @Override
        public Job[] newArray(int size) {
            return new Job[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return "Job{" +
                "jobId='" + jobId + '\'' +
                ", title='" + title + '\'' +
                ", company='" + company + '\'' +
                ", location='" + location + '\'' +
                ", salary='" + salary + '\'' +
                ", url='" + url + '\'' +
                ", rating='" + rating + '\'' +
                ", review='" + review + '\'' +
                ", postDate='" + postDate + '\'' +
                ", shortDescription='" + shortDescription + '\'' +
                ", fullDescription='" + fullDescription + '\'' +
                '}';

    }


    public void setJobId(@NonNull String stringId) {
        this.jobId = stringId;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setSalary(String salary) {
        this.salary = salary;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public void setPostDate(String postDate) {
        this.postDate = postDate;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public long getMinSalary() {
        return minSalary;
    }

    public void setMinSalary(long minSalary) {
        this.minSalary = minSalary;
    }

    public long getMaxSalary() {
        return maxSalary;
    }

    public void setMaxSalary(long maxSalary) {
        this.maxSalary = maxSalary;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public void setOpenings(String openings) {
        this.openings = openings;
    }
    public void setApplicants(String applicants) {
        this.applicants = applicants;
    }

    @NonNull
    public String getJobId() {
        return jobId;
    }
    public String getTitle() {
        return title;
    }

    public String getCompany() {
        return company;
    }

    public String getLocation() {
        return location;
    }

    public String getSalary() {
        return salary;
    }

    public String getOpenings() {
        return openings;
    }
    public String getApplicants() {
        return applicants;
    }
    public String getUrl() {
        return url;
    }

    public String getRating() {
        return rating;
    }
    public String getPostDate() {
        return postDate;
    }

    public String getShortDescription() {
        return shortDescription;
    }
    public String getReview() {
        return review;
    }

    public String getFullDescription() {
        return fullDescription;
    }

    public void setFullDescription(String fullDescription) {
        this.fullDescription = fullDescription;
    }
}
