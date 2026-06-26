package com.coduel.dao;

import com.coduel.entity.ConversationSetting;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ConversationSettingDao extends BaseDao<ConversationSetting> {

    private static final String SELECT_FOR_PAIR =
            "SELECT s FROM ConversationSetting s WHERE s.ownerUserId = :owner AND s.peerUserId = :peer";
    // All of an owner's customized threads — batched once to decorate the inbox without an N+1.
    private static final String SELECT_FOR_OWNER =
            "SELECT s FROM ConversationSetting s WHERE s.ownerUserId = :owner";
    // Threads with disappearing messages enabled — drives the retention sweep.
    private static final String SELECT_DISAPPEARING =
            "SELECT s FROM ConversationSetting s WHERE s.disappearingTtlSeconds IS NOT NULL";

    public ConversationSettingDao() {
        super(ConversationSetting.class);
    }

    public List<ConversationSetting> selectWithDisappearing() {
        return createQuery(SELECT_DISAPPEARING, ConversationSetting.class).getResultList();
    }

    public ConversationSetting selectForPair(Long ownerUserId, Long peerUserId) {
        return selectSingleOrNull(createQuery(SELECT_FOR_PAIR, ConversationSetting.class)
                .setParameter("owner", ownerUserId)
                .setParameter("peer", peerUserId));
    }

    public List<ConversationSetting> selectForOwner(Long ownerUserId) {
        return createQuery(SELECT_FOR_OWNER, ConversationSetting.class)
                .setParameter("owner", ownerUserId)
                .getResultList();
    }
}
