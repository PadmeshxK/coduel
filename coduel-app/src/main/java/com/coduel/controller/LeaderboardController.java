package com.coduel.controller;

import com.coduel.common.data.PageData;
import com.coduel.common.exception.ApiException;
import com.coduel.dto.LeaderboardDto;
import com.coduel.model.data.LeaderboardData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {

    @Autowired
    private LeaderboardDto leaderboardDto;

    @GetMapping
    public PageData<LeaderboardData> getPage(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) throws ApiException {
        return leaderboardDto.getPage(page, size);
    }

    @GetMapping("/search")
    public List<LeaderboardData> search(@RequestParam("q") String query) {
        return leaderboardDto.search(query);
    }
}
