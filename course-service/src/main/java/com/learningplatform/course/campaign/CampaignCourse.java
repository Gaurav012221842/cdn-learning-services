package com.learningplatform.course.campaign;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "campaign_courses")
@IdClass(CampaignCourse.CampaignCourseId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignCourse {

    @Id
    @Column(name = "campaign_id")
    private UUID campaignId;

    @Id
    @Column(name = "course_id")
    private UUID courseId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampaignCourseId implements Serializable {
        private UUID campaignId;
        private UUID courseId;
    }
}
