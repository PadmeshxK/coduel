package com.coduel.model.result;

import com.coduel.entity.Match;
import com.coduel.entity.MatchParticipant;
import com.coduel.entity.Problem;
import com.coduel.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/** Internal carrier: a match with its problem and its participants (carrying forfeit state), with
 *  profiles looked up by userId. */
@Getter
@AllArgsConstructor
public class MatchDetailResult {

    private Match match;
    private Problem problem;
    private List<MatchParticipant> participants;
    private Map<Long, User> profiles;
}
