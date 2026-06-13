package com.coduel.controller;

import com.coduel.common.data.PageData;
import com.coduel.common.exception.ApiException;
import com.coduel.dto.ProblemDto;
import com.coduel.model.data.ProblemData;
import com.coduel.model.form.ProblemForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/problems")
public class ProblemController {

    @Autowired
    private ProblemDto problemDto;

    @PostMapping
    public ProblemData create(@RequestBody ProblemForm form) throws ApiException {
        return problemDto.create(form);
    }

    @GetMapping
    public PageData<ProblemData> getPage(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) throws ApiException {
        return problemDto.getPage(page, size);
    }

    @GetMapping("/{slug}")
    public ProblemData getBySlug(@PathVariable("slug") String slug) throws ApiException {
        return problemDto.getBySlug(slug);
    }
}
