package com.coduel.dao;

import com.coduel.entity.Submission;
import org.springframework.stereotype.Repository;

@Repository
public class SubmissionDao extends BaseDao<Submission> {

    public SubmissionDao() {
        super(Submission.class);
    }
}
