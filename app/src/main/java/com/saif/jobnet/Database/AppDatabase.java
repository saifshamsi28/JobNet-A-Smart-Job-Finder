package com.saif.jobnet.Database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.saif.jobnet.Course;
import com.saif.jobnet.Models.Education.EducationDetails;
import com.saif.jobnet.Models.Education.GraduationDetails;
import com.saif.jobnet.Models.Education.EducationTypeConverter;
import com.saif.jobnet.Models.Job;
import com.saif.jobnet.Models.User;
import com.saif.jobnet.Utils.Converters;

@Database(entities = {User.class, Job.class, EducationDetails.class, GraduationDetails.class, Course.class}, version = 2)
@TypeConverters({Converters.class, EducationTypeConverter.class})

public abstract class AppDatabase extends RoomDatabase {
    public abstract JobDao jobDao();
}