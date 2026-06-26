package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.dao.ConversationSettingDao;
import com.coduel.entity.ConversationSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(rollbackFor = Exception.class)
public class ConversationSettingApi extends AbstractApi {

    @Autowired
    private ConversationSettingDao conversationSettingDao;

    /** The owner's saved settings for this peer, or null if the thread was never customized (= defaults). */
    public ConversationSetting find(Long ownerUserId, Long peerUserId) {
        return conversationSettingDao.selectForPair(ownerUserId, peerUserId);
    }

    public List<ConversationSetting> getForOwner(Long ownerUserId) {
        return conversationSettingDao.selectForOwner(ownerUserId);
    }

    // Threads with disappearing messages enabled — drives the retention sweep.
    public List<ConversationSetting> getWithDisappearing() {
        return conversationSettingDao.selectWithDisappearing();
    }

    // Persist the aggregate the caller built/mutated via ConversionHelper: a brand-new row is inserted,
    // an already-managed one is dirty-checked on commit. Mirrors the thin persist pattern of the slice.
    public ConversationSetting save(ConversationSetting setting) {
        return Objects.isNull(setting.getId()) ? conversationSettingDao.persist(setting) : setting;
    }
}
