package com.coduel.model.result;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// Distinct ratings + tags present in the catalog; the Dto maps it to FilterOptionsData.
// Built via ConversionHelper.toFilterOptionsResult — not a constructor.
@Getter
@Setter
public class FilterOptionsResult {

    private List<Integer> ratings;
    private List<String> tags;
}
