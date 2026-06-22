package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// The values the problem list can be filtered by — the distinct ratings and tags that actually exist
// in the catalog. The Practice page uses these to render its rating/tag filter controls, so it only
// ever offers options that will return results.
@Getter
@Setter
public class FilterOptionsData {

    private List<Integer> ratings;
    private List<String> tags;
}
