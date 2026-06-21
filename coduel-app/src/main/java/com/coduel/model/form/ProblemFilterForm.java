package com.coduel.model.form;

import com.coduel.model.constant.ProblemStatusFilter;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// Combinable filter criteria for the problem list, bound straight from the request query params.
// Any field left empty/null is simply not applied, so the filters stack (search AND ratings AND tags
// AND status). Frontend-supplied only — the userId that backs solved/unsolved comes from the session,
// never from here.
@Getter
@Setter
public class ProblemFilterForm {

    private String q;
    private String sort;
    private List<Integer> ratings;
    private List<String> tags;
    private ProblemStatusFilter status; // ALL (or null) = no solve-state filter
}
